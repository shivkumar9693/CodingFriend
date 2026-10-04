package com.aicodingmentor.mentor.service;

import com.aicodingmentor.common.error.ResourceNotFoundException;
import com.aicodingmentor.execution.model.EvaluationVerdict;
import com.aicodingmentor.mentor.dto.SocraBuddyDiagnosisRequest;
import com.aicodingmentor.mentor.dto.SocraBuddyDiagnosisResponse;
import com.aicodingmentor.mentor.dto.SocraBuddyHintRequest;
import com.aicodingmentor.mentor.dto.SocraBuddyHintResponse;
import com.aicodingmentor.mentor.model.HintLevel;
import com.aicodingmentor.mistake.model.MistakeClassification;
import com.aicodingmentor.problem.domain.Problem;
import com.aicodingmentor.problem.repository.ProblemRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Service
public class SocraBuddyMentorServiceImpl implements SocraBuddyMentorService {

    private static final Logger log = LoggerFactory.getLogger(SocraBuddyMentorServiceImpl.class);
    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("```[a-zA-Z]*\\n[\\s\\S]*?```");

    private final ProblemRepository problemRepository;
    private final MentorModelClient mentorModelClient;
    private final PromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    private record CachedHintEntry(SocraBuddyHintResponse response, long expiresAt) {}
    private final ConcurrentHashMap<String, CachedHintEntry> memoryCache = new ConcurrentHashMap<>();

    public SocraBuddyMentorServiceImpl(
            ProblemRepository problemRepository,
            MentorModelClient mentorModelClient,
            PromptBuilder promptBuilder,
            ObjectMapper objectMapper
    ) {
        this.problemRepository = problemRepository;
        this.mentorModelClient = mentorModelClient;
        this.promptBuilder = promptBuilder;
        this.objectMapper = objectMapper;
    }

    @Override
    public SseEmitter streamHint(UUID userId, SocraBuddyHintRequest request) {
        SseEmitter emitter = new SseEmitter(60_000L);

        CompletableFuture.runAsync(() -> {
            try {
                String sourceCode = request.sourceCode() != null ? request.sourceCode() : "";
                String userQ = request.userQuestion() != null ? request.userQuestion().trim() : "";
                String cacheKey = "mentor:hint:" + request.problemId() + ":" + request.hintLevel()
                        + ":" + Integer.toHexString(sourceCode.trim().hashCode())
                        + (userQ.isEmpty() ? "" : ":q" + Integer.toHexString(userQ.hashCode()));

                SocraBuddyHintResponse response = getCachedHint(cacheKey);
                if (response == null) {
                    response = generateHint(userId, request);
                    putCachedHint(cacheKey, response, Duration.ofHours(2));
                }

                // Send start event
                emitter.send(SseEmitter.event()
                        .name("start")
                        .data(Map.of(
                                "hintLevel", response.hintLevel(),
                                "title", response.title() != null ? response.title() : "SocraBuddy's Insight",
                                "modelName", response.modelUsed() != null ? response.modelUsed() : "SocraBuddy"
                        )));

                // Tokenize guidance into words/punctuation chunks
                String guidance = response.guidance() != null ? response.guidance() : "";
                String[] words = guidance.split("(?<=\\s)|(?<=[.,!?])");
                for (String word : words) {
                    if (!word.isEmpty()) {
                        emitter.send(SseEmitter.event()
                                .name("token")
                                .data(Map.of("token", word)));
                        Thread.sleep(16); // ~60 words/sec smooth streaming animation
                    }
                }

                if (response.socraticQuestion() != null && !response.socraticQuestion().isBlank()) {
                    emitter.send(SseEmitter.event().name("token").data(Map.of("token", "\n\n💡 ")));
                    String[] qWords = response.socraticQuestion().split("(?<=\\s)|(?<=[.,!?])");
                    for (String word : qWords) {
                        if (!word.isEmpty()) {
                            emitter.send(SseEmitter.event()
                                    .name("token")
                                    .data(Map.of("token", word)));
                            Thread.sleep(16);
                        }
                    }
                }

                // Send complete event with full structured payload
                emitter.send(SseEmitter.event()
                        .name("complete")
                        .data(response));

                emitter.complete();

            } catch (Exception ex) {
                log.error("Error streaming Socratic hint for user {}", userId, ex);
                try {
                    emitter.send(SseEmitter.event().name("error").data(Map.of("message", "Hint stream error: " + ex.getMessage())));
                } catch (Exception ignored) {}
                emitter.completeWithError(ex);
            }
        });

        return emitter;
    }

    private SocraBuddyHintResponse getCachedHint(String cacheKey) {
        if (redisTemplate != null) {
            try {
                String json = redisTemplate.opsForValue().get(cacheKey);
                if (json != null && !json.isBlank()) {
                    return objectMapper.readValue(json, SocraBuddyHintResponse.class);
                }
            } catch (Exception ex) {
                log.warn("Redis hint cache read failed, checking memory fallback: {}", ex.getMessage());
            }
        }
        CachedHintEntry entry = memoryCache.get(cacheKey);
        if (entry != null && System.currentTimeMillis() < entry.expiresAt()) {
            return entry.response();
        }
        return null;
    }

    private void putCachedHint(String cacheKey, SocraBuddyHintResponse response, Duration ttl) {
        if (response == null) return;
        if (redisTemplate != null) {
            try {
                String json = objectMapper.writeValueAsString(response);
                redisTemplate.opsForValue().set(cacheKey, json, ttl);
            } catch (Exception ex) {
                log.warn("Redis hint cache write failed, writing to memory fallback: {}", ex.getMessage());
            }
        }
        memoryCache.put(cacheKey, new CachedHintEntry(response, System.currentTimeMillis() + ttl.toMillis()));
    }

    @Override
    @Transactional(readOnly = true)
    public SocraBuddyHintResponse generateHint(UUID userId, SocraBuddyHintRequest request) {
        Problem problem = problemRepository.findById(request.problemId())
                .orElseThrow(() -> new ResourceNotFoundException("Problem", request.problemId()));

        String systemPrompt = promptBuilder.buildSystemPrompt();
        String userPrompt = promptBuilder.buildHintUserPrompt(problem, request);

        Optional<String> rawLlmResponse = mentorModelClient.generate(systemPrompt, userPrompt);

        if (rawLlmResponse.isPresent()) {
            try {
                JsonNode json = objectMapper.readTree(rawLlmResponse.get());
                String title = json.has("title") ? json.get("title").asText() : "SocraBuddy's Insight";
                String guidance = json.has("guidance") ? json.get("guidance").asText() : "";
                String question = json.has("socraticQuestion") ? json.get("socraticQuestion").asText() : "What is the expected behavior for your edge cases?";

                List<String> keywords = new ArrayList<>();
                if (json.has("conceptKeywords") && json.get("conceptKeywords").isArray()) {
                    for (JsonNode kw : json.get("conceptKeywords")) {
                        keywords.add(kw.asText());
                    }
                }

                // Rule 30: Validate and sanitize LLM response - strip any accidental complete code blocks
                guidance = sanitizeNonSpoilerGuidance(guidance);

                return SocraBuddyHintResponse.of(
                        request.hintLevel(),
                        title,
                        guidance,
                        question,
                        keywords,
                        mentorModelClient.getModelName()
                );
            } catch (Exception ex) {
                log.warn("Failed to parse mentor model JSON response, falling back to rule engine", ex);
            }
        }

        // Rule-Based Socratic Fallback
        return generateFallbackHint(problem, request);
    }

    @Override
    @Transactional(readOnly = true)
    public SocraBuddyDiagnosisResponse diagnoseMistake(UUID userId, SocraBuddyDiagnosisRequest request) {
        Problem problem = problemRepository.findById(request.problemId())
                .orElseThrow(() -> new ResourceNotFoundException("Problem", request.problemId()));

        String systemPrompt = promptBuilder.buildSystemPrompt();
        String userPrompt = promptBuilder.buildDiagnosisPrompt(problem, request);

        Optional<String> rawLlmResponse = mentorModelClient.generate(systemPrompt, userPrompt);

        if (rawLlmResponse.isPresent()) {
            try {
                JsonNode json = objectMapper.readTree(rawLlmResponse.get());
                MistakeClassification classification = MistakeClassification.GENERAL_BUG;
                if (json.has("mistakeClassification")) {
                    try {
                        classification = MistakeClassification.valueOf(json.get("mistakeClassification").asText().toUpperCase());
                    } catch (IllegalArgumentException ignored) {
                    }
                }

                String summary = json.has("diagnosisSummary") ? json.get("diagnosisSummary").asText() : "Execution did not match expected output.";
                String guidance = json.has("socraticGuidance") ? json.get("socraticGuidance").asText() : "Inspect the condition where your loop or calculation terminates.";
                String action = json.has("recommendedAction") ? json.get("recommendedAction").asText() : "Trace with sample inputs on paper.";
                HintLevel nextLevel = HintLevel.LEVEL_1_NUDGE;
                if (json.has("recommendedHintLevel")) {
                    try {
                        nextLevel = HintLevel.valueOf(json.get("recommendedHintLevel").asText().toUpperCase());
                    } catch (IllegalArgumentException ignored) {
                    }
                }

                return new SocraBuddyDiagnosisResponse(
                        classification,
                        summary,
                        guidance,
                        action,
                        nextLevel
                );
            } catch (Exception ex) {
                log.warn("Failed to parse Ollama diagnosis response, falling back to rule diagnosis", ex);
            }
        }

        return generateFallbackDiagnosis(problem, request);
    }

    private String sanitizeNonSpoilerGuidance(String guidance) {
        if (guidance == null) return "";
        // If the AI accidentally returned large markdown code blocks, strip them to protect Socratic integrity
        if (CODE_BLOCK_PATTERN.matcher(guidance).find()) {
            return CODE_BLOCK_PATTERN.matcher(guidance).replaceAll("*(Code block redacted by SocraBuddy: Explore the concept rather than copying code)*");
        }
        return guidance;
    }

    private SocraBuddyHintResponse generateFallbackHint(Problem problem, SocraBuddyHintRequest request) {
        String problemTitle = problem.getTitle();

        if (request.userQuestion() != null && !request.userQuestion().isBlank()) {
            String q = request.userQuestion().toLowerCase();
            if (q.contains("complement")) {
                return SocraBuddyHintResponse.of(
                        request.hintLevel() != null ? request.hintLevel() : HintLevel.LEVEL_2_CONCEPT,
                        "Understanding Complement Arithmetic",
                        "For a given number x and a desired target, the complement is (target - x). Instead of scanning the remaining array elements with a nested loop, you can check whether (target - x) was already seen in previous iterations using a fast lookup table.",
                        "When you inspect number `x` at index `i`, what value `target - x` do you need to check in your lookup structure?",
                        List.of("Complement", "HashMap", "O(1) Lookup"),
                        "SocraBuddy Socratic Heuristics"
                );
            }
            if (q.contains("hashmap") || q.contains("map") || q.contains("lookup")) {
                return SocraBuddyHintResponse.of(
                        request.hintLevel() != null ? request.hintLevel() : HintLevel.LEVEL_2_CONCEPT,
                        "Hash Table Lookup & Space-Time Tradeoff",
                        "A HashMap allows you to trade O(N) auxiliary space for O(1) instantaneous lookup time. By storing each number as the key and its index as the value, you can find the required complement in a single pass.",
                        "What should you store as the key in the HashMap, and what should you store as the value?",
                        List.of("HashMap", "Key-Value", "One-Pass"),
                        "SocraBuddy Socratic Heuristics"
                );
            }
            if (q.contains("slow") || q.contains("time") || q.contains("complexity")) {
                return SocraBuddyHintResponse.of(
                        request.hintLevel() != null ? request.hintLevel() : HintLevel.LEVEL_2_CONCEPT,
                        "Optimizing Time Complexity",
                        "A nested loop solution requires O(N^2) time complexity. To achieve O(N) linear time, eliminate the inner loop by looking up previously encountered values in constant O(1) time using an auxiliary Hash Table.",
                        "How can saving seen elements in memory prevent having to re-scan the array?",
                        List.of("Time Complexity", "Optimization", "Linear Time O(N)"),
                        "SocraBuddy Socratic Heuristics"
                );
            }
            if (q.contains("edge") || q.contains("boundary")) {
                return SocraBuddyHintResponse.of(
                        request.hintLevel() != null ? request.hintLevel() : HintLevel.LEVEL_1_NUDGE,
                        "Examining Edge Cases & Invariants",
                        "Consider: (1) Minimum length input (2 elements), (2) Negative values and zero, (3) Duplicate numbers where an element plus itself equals target (e.g., [3, 3] target 6).",
                        "How does your algorithm avoid using the exact same index twice for the answer?",
                        List.of("Edge Cases", "Duplicates", "Index Uniqueness"),
                        "SocraBuddy Socratic Heuristics"
                );
            }
        }

        return switch (request.hintLevel()) {
            case LEVEL_1_NUDGE -> SocraBuddyHintResponse.of(
                    HintLevel.LEVEL_1_NUDGE,
                    "Examining Invariants & Edge Cases",
                    "Consider the fundamental boundaries of '" + problemTitle + "'. How does your current code behave when given extreme inputs such as an empty structure, a single element, or repeating numbers?",
                    "What assumptions are you making about the ordering or values of the input?",
                    List.of("Edge Cases", "Invariants", problemTitle),
                    "SocraBuddy Socratic Heuristics"
            );
            case LEVEL_2_CONCEPT -> SocraBuddyHintResponse.of(
                    HintLevel.LEVEL_2_CONCEPT,
                    "Core Data Structure & Time Complexity",
                    "To achieve an optimal solution for '" + problemTitle + "', think about trade-offs between time and space. Can a fast lookup structure (like a HashMap or HashSet) or a two-pointer technique reduce a nested scan?",
                    "If you store previously seen elements in memory, what can you check in O(1) time?",
                    List.of("Time-Space Tradeoff", "Fast Lookup", "Pointers"),
                    "SocraBuddy Socratic Heuristics"
            );
            case LEVEL_3_ALGORITHM -> SocraBuddyHintResponse.of(
                    HintLevel.LEVEL_3_ALGORITHM,
                    "Algorithmic Blueprint Breakdown",
                    "Step 1: Initialize your tracking data structure.\nStep 2: Iterate through the elements while computing the required complement or invariant.\nStep 3: If the target state is met, return the result immediately; otherwise update your tracking state.\nStep 4: Handle the default/fallback case after iteration completes.",
                    "How does each step transition state toward the final result?",
                    List.of("Algorithm Flow", "State Transition", "Iteration"),
                    "SocraBuddy Socratic Heuristics"
            );
            case LEVEL_4_TARGETED -> SocraBuddyHintResponse.of(
                    HintLevel.LEVEL_4_TARGETED,
                    "Targeted Logic Pointer",
                    "Look closely at the condition inside your primary loop and how indices/complements are updated. Verify whether you are returning before checking all candidates or accessing an element after modifying the collection.",
                    "Does your loop condition `i < length` or pointer check skip or duplicate the final element?",
                    List.of("Loop Termination", "Pointer Update", "Boundary"),
                    "SocraBuddy Socratic Heuristics"
            );
        };
    }

    private SocraBuddyDiagnosisResponse generateFallbackDiagnosis(Problem problem, SocraBuddyDiagnosisRequest request) {
        String error = request.errorMessage() != null ? request.errorMessage() : "";
        EvaluationVerdict verdict = request.verdict() != null ? request.verdict() : EvaluationVerdict.WRONG_ANSWER;

        if (verdict == EvaluationVerdict.COMPILE_ERROR || error.contains("error:")) {
            return new SocraBuddyDiagnosisResponse(
                    MistakeClassification.SYNTAX_ERROR,
                    "Java compilation failed due to syntax or type mismatch.",
                    "Review compiler messages carefully for line numbers and unmatched tokens.",
                    "Check semicolons, braces, variable declarations, and return types.",
                    HintLevel.LEVEL_1_NUDGE
            );
        }

        if (verdict == EvaluationVerdict.TIME_LIMIT_EXCEEDED) {
            return new SocraBuddyDiagnosisResponse(
                    MistakeClassification.TIME_LIMIT_EXCEEDED,
                    "Execution exceeded maximum allowable time limit.",
                    "Your algorithm may have an infinite loop or a time complexity that is too high (e.g. O(N^2) instead of O(N)).",
                    "Ensure loop index variables are advancing and look for ways to optimize nested iterations.",
                    HintLevel.LEVEL_2_CONCEPT
            );
        }

        if (error.contains("ArithmeticException")) {
            return new SocraBuddyDiagnosisResponse(
                    MistakeClassification.GENERAL_BUG,
                    "Arithmetic error occurred during runtime (likely division or modulo by zero).",
                    "What happens when the divisor evaluates to zero in your mathematical expressions?",
                    "Add a guard check before performing division or modulo operations.",
                    HintLevel.LEVEL_1_NUDGE
            );
        }

        if (error.contains("NullPointerException")) {
            return new SocraBuddyDiagnosisResponse(
                    MistakeClassification.NULL_POINTER,
                    "Null reference was dereferenced at runtime.",
                    "Which object was accessed before ensuring it was non-null?",
                    "Check map lookups or uninitialized array elements before calling methods on them.",
                    HintLevel.LEVEL_4_TARGETED
            );
        }

        if (error.contains("IndexOutOfBoundsException")) {
            return new SocraBuddyDiagnosisResponse(
                    MistakeClassification.INDEX_OUT_OF_BOUNDS,
                    "Array or list index exceeded the valid range [0, length - 1].",
                    "Check your loop boundary condition. Are you accessing `arr[i]` when `i == arr.length`?",
                    "Change loop bounds to strict `< length` instead of `<= length`.",
                    HintLevel.LEVEL_4_TARGETED
            );
        }

        return new SocraBuddyDiagnosisResponse(
                MistakeClassification.LOGIC_INCORRECT,
                "Output diverged from expected solution on test case evaluation.",
                "Your logic produced an incorrect result for the specific input pattern in this test.",
                "Trace the variable values step-by-step with the sample input on paper.",
                HintLevel.LEVEL_2_CONCEPT
        );
    }

    @Override
    @Transactional(readOnly = true)
    public com.aicodingmentor.mentor.dto.InlineAssistResponse generateInlineAssist(UUID userId, com.aicodingmentor.mentor.dto.InlineAssistRequest request) {
        Problem problem = problemRepository.findById(request.problemId())
                .orElseThrow(() -> new ResourceNotFoundException("Problem", request.problemId()));

        String systemPrompt = promptBuilder.buildSystemPrompt("v2");
        String userPrompt = promptBuilder.buildInlineAssistPrompt(problem, request);

        Optional<String> rawLlmResponse = mentorModelClient.generate(systemPrompt, userPrompt);

        if (rawLlmResponse.isPresent()) {
            try {
                JsonNode json = objectMapper.readTree(rawLlmResponse.get());
                String nudgeType = json.has("nudgeType") ? json.get("nudgeType").asText() : "LOGIC_QUESTION";
                String inlineMessage = json.has("inlineMessage") ? json.get("inlineMessage").asText() : "";
                int targetLine = json.has("targetLine") ? json.get("targetLine").asInt() : request.cursorLine();

                inlineMessage = sanitizeNonSpoilerGuidance(inlineMessage);

                if (!inlineMessage.isBlank()) {
                    return com.aicodingmentor.mentor.dto.InlineAssistResponse.of(
                            nudgeType,
                            inlineMessage,
                            targetLine,
                            mentorModelClient.getModelName()
                    );
                }
            } catch (Exception ex) {
                log.warn("Failed to parse inline assist JSON from LLM: {}", ex.getMessage());
            }
        }

        // Rule-Based Socratic Fallback
        return generateFallbackInlineNudge(problem, request);
    }

    private com.aicodingmentor.mentor.dto.InlineAssistResponse generateFallbackInlineNudge(
            Problem problem,
            com.aicodingmentor.mentor.dto.InlineAssistRequest request
    ) {
        String fullCode = request.sourceCode();
        String[] lines = fullCode.split("\\r?\\n");
        int targetLine = request.cursorLine();
        int targetIdx = Math.min(targetLine - 1, lines.length - 1);

        // Examine the current line and immediate preceding lines
        String activeLine = (targetIdx >= 0 && targetIdx < lines.length) ? lines[targetIdx].trim() : "";
        String prevLine = (targetIdx - 1 >= 0 && targetIdx - 1 < lines.length) ? lines[targetIdx - 1].trim() : "";
        String recentLine = !activeLine.isBlank() ? activeLine : prevLine;

        // 1. Detect Detour / Arbitrary variable declarations (e.g. "int ab = 2;", "int x = 5;")
        java.util.regex.Pattern varDeclPattern = java.util.regex.Pattern.compile(
                "(?:int|double|float|long|short|byte|boolean|String|var)\\s+([a-zA-Z0-9_]+)\\s*=\\s*([^;]+);"
        );
        java.util.regex.Matcher varMatcher = varDeclPattern.matcher(recentLine);
        if (varMatcher.find()) {
            String varName = varMatcher.group(1);
            String lowerName = varName.toLowerCase();
            if (!lowerName.equals("target") && !lowerName.equals("complement")
                    && !lowerName.equals("diff") && !lowerName.equals("sum")
                    && !lowerName.equals("result") && !lowerName.equals("ans")
                    && !lowerName.equals("i") && !lowerName.equals("j")
                    && !lowerName.equals("left") && !lowerName.equals("right")
                    && !lowerName.equals("n") && !lowerName.equals("len") && !lowerName.equals("length")) {
                return com.aicodingmentor.mentor.dto.InlineAssistResponse.of(
                        "DETOUR_CHECK",
                        String.format("Consider what role variable '%s' serves in this algorithm. How will it help you find the indices adding to target?", varName),
                        targetLine,
                        "SocraBuddy-RuleEngine"
                );
            }
        }

        // 2. Detect Single Generic Map Syntax Error (e.g., "Map<Integer> map = new HashMap<>();")
        java.util.regex.Pattern singleGenericMapPattern = java.util.regex.Pattern.compile("Map\\s*<\\s*([a-zA-Z0-9_]+)\\s*>\\s*([a-zA-Z0-9_]+)");
        java.util.regex.Matcher mapMatcher = singleGenericMapPattern.matcher(recentLine);
        if (mapMatcher.find()) {
            return com.aicodingmentor.mentor.dto.InlineAssistResponse.of(
                    "SYNTAX_POINTER",
                    "In Java, Map<Key, Value> requires two generic types (e.g. Map<Integer, Integer>). What should the key and value types be?",
                    targetLine,
                    "SocraBuddy-RuleEngine"
            );
        }

        // 3. Detect Nested Loops (O(N^2) indicator)
        String lowerCode = fullCode.toLowerCase();
        if (lowerCode.contains("for") && (lowerCode.indexOf("for") != lowerCode.lastIndexOf("for"))) {
            return com.aicodingmentor.mentor.dto.InlineAssistResponse.of(
                    "OPTIMIZATION_NUDGE",
                    "Can this nested loop be reduced to O(N) using a Hash Table / Map to store previously seen elements?",
                    targetLine,
                    "SocraBuddy-RuleEngine"
            );
        }

        // 4. Proper Map Initialized
        if (recentLine.toLowerCase().contains("map") && recentLine.contains("new HashMap")) {
            return com.aicodingmentor.mentor.dto.InlineAssistResponse.of(
                    "LOGIC_QUESTION",
                    "You've initialized a Map. What key-value pair should you store for each element to enable O(1) lookups for the complement?",
                    targetLine,
                    "SocraBuddy-RuleEngine"
            );
        }

        // 5. Return statement
        if (recentLine.contains("return")) {
            return com.aicodingmentor.mentor.dto.InlineAssistResponse.of(
                    "SYNTAX_POINTER",
                    "Ensure your return value matches the expected output format for all valid input cases.",
                    targetLine,
                    "SocraBuddy-RuleEngine"
            );
        }

        // 6. Generic Next Step
        return com.aicodingmentor.mentor.dto.InlineAssistResponse.of(
                "NEXT_STEP_NUDGE",
                "What is the first step or loop you need to scan elements and check their complement?",
                targetLine,
                "SocraBuddy-RuleEngine"
        );
    }
}
