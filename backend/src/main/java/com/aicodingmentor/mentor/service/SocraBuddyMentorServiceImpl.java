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

    private enum ProblemDomain {
        TREE,
        GRAPH,
        DYNAMIC_PROGRAMMING,
        LINKED_LIST,
        BINARY_SEARCH,
        SLIDING_WINDOW_TWO_POINTER,
        STACK_QUEUE,
        BACKTRACKING,
        ARRAY_HASH_TABLE
    }

    private static final java.util.regex.Pattern TREE_PATTERN = java.util.regex.Pattern.compile("\\b(tree|trees|treenode|bst|root|leaf|leaves|inorder|preorder|postorder|lca|binary\\s+tree)\\b", java.util.regex.Pattern.CASE_INSENSITIVE);
    private static final java.util.regex.Pattern GRAPH_PATTERN = java.util.regex.Pattern.compile("\\b(graph|graphs|vertex|vertices|edge|edges|adjacency|dijkstra|topological|toposort|union\\s+find|disjoint\\s+set|island|islands)\\b", java.util.regex.Pattern.CASE_INSENSITIVE);
    private static final java.util.regex.Pattern DP_PATTERN = java.util.regex.Pattern.compile("\\b(dp|dynamic\\s+programming|memoization|tabulation|subproblem|subproblems|knapsack|subsequence|coin\\s+change|climbing\\s+stairs|fibonacci)\\b", java.util.regex.Pattern.CASE_INSENSITIVE);
    private static final java.util.regex.Pattern LL_PATTERN = java.util.regex.Pattern.compile("\\b(listnode|linkedlist|linked\\s+list|head|dummy\\s+node|reverse\\s+list|fast\\s+slow)\\b", java.util.regex.Pattern.CASE_INSENSITIVE);
    private static final java.util.regex.Pattern BS_PATTERN = java.util.regex.Pattern.compile("\\b(binary\\s+search|bisect|search\\s+rotated|find\\s+peak)\\b", java.util.regex.Pattern.CASE_INSENSITIVE);
    private static final java.util.regex.Pattern TP_PATTERN = java.util.regex.Pattern.compile("\\b(sliding\\s+window|two\\s+pointer|two\\s+pointers|two-pointer|longest\\s+substring)\\b", java.util.regex.Pattern.CASE_INSENSITIVE);
    private static final java.util.regex.Pattern SQ_PATTERN = java.util.regex.Pattern.compile("\\b(stack|stacks|queue|queues|deque|parentheses|monotonic\\s+stack)\\b", java.util.regex.Pattern.CASE_INSENSITIVE);
    private static final java.util.regex.Pattern BT_PATTERN = java.util.regex.Pattern.compile("\\b(backtrack|backtracking|permutation|permutations|combination|combinations|n-queens|sudoku)\\b", java.util.regex.Pattern.CASE_INSENSITIVE);

    private ProblemDomain detectProblemDomain(Problem problem, String sourceCode, String userQuestion) {
        String combined = (problem.getTitle() + " " + problem.getDescription() + " "
                + (problem.getTags() != null ? String.join(" ", problem.getTags()) : "") + " "
                + (sourceCode != null ? sourceCode : "") + " "
                + (userQuestion != null ? userQuestion : ""));

        if (DP_PATTERN.matcher(combined).find()) {
            return ProblemDomain.DYNAMIC_PROGRAMMING;
        }
        if (TREE_PATTERN.matcher(combined).find()) {
            return ProblemDomain.TREE;
        }
        if (GRAPH_PATTERN.matcher(combined).find()) {
            return ProblemDomain.GRAPH;
        }
        if (LL_PATTERN.matcher(combined).find()) {
            return ProblemDomain.LINKED_LIST;
        }
        if (BS_PATTERN.matcher(combined).find()) {
            return ProblemDomain.BINARY_SEARCH;
        }
        if (TP_PATTERN.matcher(combined).find()) {
            return ProblemDomain.SLIDING_WINDOW_TWO_POINTER;
        }
        if (SQ_PATTERN.matcher(combined).find()) {
            return ProblemDomain.STACK_QUEUE;
        }
        if (BT_PATTERN.matcher(combined).find()) {
            return ProblemDomain.BACKTRACKING;
        }

        return ProblemDomain.ARRAY_HASH_TABLE;
    }

    private SocraBuddyHintResponse generateFallbackHint(Problem problem, SocraBuddyHintRequest request) {
        String problemTitle = problem.getTitle();
        HintLevel lvl = request.hintLevel() != null ? request.hintLevel() : HintLevel.LEVEL_1_NUDGE;
        String sourceCode = request.sourceCode() != null ? request.sourceCode() : "";
        String userQ = request.userQuestion() != null ? request.userQuestion().trim() : "";
        ProblemDomain domain = detectProblemDomain(problem, sourceCode, userQ);

        // 1. Off-topic Prompt Filter
        if (!userQ.isEmpty()) {
            String q = userQ.toLowerCase();
            if (q.contains("joke") || q.contains("weather") || q.contains("story") || q.contains("song")
                    || q.contains("who are you") || q.contains("ignore previous") || q.contains("system prompt")) {
                return generateOffTopicResponse(problemTitle, lvl, domain);
            }
        }

        // 2. Domain-Aware Socratic Response
        return switch (domain) {
            case TREE -> generateTreeHint(problemTitle, lvl, userQ);
            case GRAPH -> generateGraphHint(problemTitle, lvl, userQ);
            case DYNAMIC_PROGRAMMING -> generateDpHint(problemTitle, lvl, userQ);
            case LINKED_LIST -> generateLinkedListHint(problemTitle, lvl, userQ);
            case BINARY_SEARCH -> generateBinarySearchHint(problemTitle, lvl, userQ);
            case SLIDING_WINDOW_TWO_POINTER -> generateTwoPointerHint(problemTitle, lvl, userQ);
            case STACK_QUEUE -> generateStackQueueHint(problemTitle, lvl, userQ);
            case BACKTRACKING -> generateBacktrackingHint(problemTitle, lvl, userQ);
            case ARRAY_HASH_TABLE -> generateArrayHint(problemTitle, lvl, userQ);
        };
    }

    private SocraBuddyHintResponse generateOffTopicResponse(String problemTitle, HintLevel lvl, ProblemDomain domain) {
        String domainTopic = switch (domain) {
            case TREE -> "tree structures and recursion";
            case GRAPH -> "graph traversal and connectivity";
            case DYNAMIC_PROGRAMMING -> "subproblem state transitions";
            case LINKED_LIST -> "node pointer manipulation";
            case BINARY_SEARCH -> "binary search invariants";
            case SLIDING_WINDOW_TWO_POINTER -> "window boundaries and two-pointer tracking";
            case STACK_QUEUE -> "stack/queue LIFO and FIFO invariants";
            case BACKTRACKING -> "state exploration and backtracking choices";
            case ARRAY_HASH_TABLE -> "array indexing and lookup optimization";
        };

        return switch (lvl) {
            case LEVEL_1_NUDGE -> SocraBuddyHintResponse.of(
                    HintLevel.LEVEL_1_NUDGE,
                    "Staying on Track: " + problemTitle,
                    "I'm SocraBuddy, your Socratic AI mentor! Let's stay focused on mastering '" + problemTitle + "'. Consider the problem's fundamental structure involving " + domainTopic + ". What is the smallest valid input case?",
                    "How would you solve this with pencil and paper for a minimal sample case?",
                    List.of("Problem Focus", "Intuition", problemTitle),
                    "SocraBuddy Socratic Engine"
            );
            case LEVEL_2_CONCEPT -> SocraBuddyHintResponse.of(
                    HintLevel.LEVEL_2_CONCEPT,
                    "Core Concept: " + problemTitle,
                    "Let's refocus on the key algorithmic property required for '" + problemTitle + "' (" + domainTopic + "). What invariant or data structure allows you to avoid redundant computation?",
                    "What state or information needs to be remembered as you traverse the data?",
                    List.of("Core Concept", "State Tracking", problemTitle),
                    "SocraBuddy Socratic Engine"
            );
            case LEVEL_3_ALGORITHM -> SocraBuddyHintResponse.of(
                    HintLevel.LEVEL_3_ALGORITHM,
                    "Algorithm Blueprint: " + problemTitle,
                    "Let's focus on the step-by-step logic structure for '" + problemTitle + "':\n\nStep 1: Check edge and base conditions.\nStep 2: Initialize your state tracking structure.\nStep 3: Process elements or nodes iteratively/recursively.\nStep 4: Return the consolidated result.",
                    "What is the base case condition that stops your search or loop?",
                    List.of("Algorithm Structure", "Flow", problemTitle),
                    "SocraBuddy Socratic Engine"
            );
            case LEVEL_4_TARGETED -> SocraBuddyHintResponse.of(
                    HintLevel.LEVEL_4_TARGETED,
                    "Targeted Verification: " + problemTitle,
                    "Let's focus on your concrete code for '" + problemTitle + "'. Inspect your base cases, null pointer guards, and return types.",
                    "Does your implementation handle the empty or single-element boundary case?",
                    List.of("Implementation", "Boundary Conditions", problemTitle),
                    "SocraBuddy Socratic Engine"
            );
        };
    }

    private SocraBuddyHintResponse generateTreeHint(String title, HintLevel lvl, String q) {
        return switch (lvl) {
            case LEVEL_1_NUDGE -> SocraBuddyHintResponse.of(
                    HintLevel.LEVEL_1_NUDGE,
                    "Tree Intuition & Base Cases",
                    "A tree is fundamentally a recursive structure composed of a root and independent left/right subtrees. If you know the solution for the left child and right child, how do you combine them at the current root? Check what base value should be returned when `root == null`.",
                    "What should your function immediately return when `root == null` or when encountering a leaf node?",
                    List.of("Tree Recursion", "Base Case", "Subtree Decomposition"),
                    "SocraBuddy Socratic Engine"
            );
            case LEVEL_2_CONCEPT -> SocraBuddyHintResponse.of(
                    HintLevel.LEVEL_2_CONCEPT,
                    "Tree Traversal & Divide-and-Conquer",
                    "Tree problems generally follow one of two paradigms: (1) Depth-First Search (DFS/Recursion) where subproblems are solved bottom-up or top-down, or (2) Breadth-First Search (BFS/Queue) for level-by-level evaluation. In DFS, the call stack implicitly tracks ancestor nodes.",
                    "Does this problem require information passed down from ancestors (top-down) or aggregated up from children (bottom-up)?",
                    List.of("DFS", "BFS", "Divide and Conquer", "TreeNode"),
                    "SocraBuddy Socratic Engine"
            );
            case LEVEL_3_ALGORITHM -> SocraBuddyHintResponse.of(
                    HintLevel.LEVEL_3_ALGORITHM,
                    "Recursive Tree Algorithm Blueprint",
                    "Step 1: Base Case — If `root == null`, return the default value (e.g. `0`, `null`, or `true`).\nStep 2: Recursive Step — Call your helper function on `root.left` and `root.right`.\nStep 3: Combine — Compute the current node's result using `root.val` and the subtree outcomes.\nStep 4: Return — Pass the combined result up the call stack.",
                    "What specific value must be returned to the parent node at Step 4?",
                    List.of("Tree Blueprint", "Postorder Pattern", "Recursion Flow"),
                    "SocraBuddy Socratic Engine"
            );
            case LEVEL_4_TARGETED -> SocraBuddyHintResponse.of(
                    HintLevel.LEVEL_4_TARGETED,
                    "Targeted Tree Pointer & Null Guards",
                    "Ensure you guard against `NullPointerException` before accessing `root.left` or `root.right`. Make sure your base case `if (root == null)` is the first line of your recursive method.",
                    "Are you checking `root == null` before dereferencing `root.val`?",
                    List.of("Null Guard", "TreeNode Navigation", "Base Return"),
                    "SocraBuddy Socratic Engine"
            );
        };
    }

    private SocraBuddyHintResponse generateGraphHint(String title, HintLevel lvl, String q) {
        return switch (lvl) {
            case LEVEL_1_NUDGE -> SocraBuddyHintResponse.of(
                    HintLevel.LEVEL_1_NUDGE,
                    "Graph Exploration & Cycle Prevention",
                    "Unlike trees, graphs can contain cycles and disconnected components. As you travel from vertex to vertex across edges, how do you prevent visiting the exact same vertex forever?",
                    "What data structure can record vertices you have already processed?",
                    List.of("Graph Traversal", "Cycle Prevention", "Visited State"),
                    "SocraBuddy Socratic Engine"
            );
            case LEVEL_2_CONCEPT -> SocraBuddyHintResponse.of(
                    HintLevel.LEVEL_2_CONCEPT,
                    "Graph Representation & Traversal Invariants",
                    "Graphs are typically modeled as an Adjacency List `Map<Node, List<Node>>` or 2D grid matrix. BFS uses a FIFO Queue to discover the shortest path in unweighted graphs, while DFS uses recursion/stack to explore connected components and detect cycles.",
                    "Why is marking a node as visited when ENQUEUEING it in BFS essential to preventing duplicate work?",
                    List.of("Adjacency List", "BFS Queue", "DFS Visited", "Shortest Path"),
                    "SocraBuddy Socratic Engine"
            );
            case LEVEL_3_ALGORITHM -> SocraBuddyHintResponse.of(
                    HintLevel.LEVEL_3_ALGORITHM,
                    "Graph Traversal Blueprint",
                    "Step 1: Build the Adjacency List from the edge list if not already given.\nStep 2: Initialize a `boolean[] visited` array or `Set<Integer> visited`.\nStep 3: Initialize a traversal queue `Queue<Integer> queue = new LinkedList<>()` (for BFS) or call DFS.\nStep 4: While queue is not empty, poll `curr`, iterate over all unvisited neighbors, mark them visited, and enqueue.\nStep 5: Return your accumulated component count, distance, or boolean verdict.",
                    "Does the graph contain multiple disconnected components that require looping over all vertices?",
                    List.of("Graph Blueprint", "Queue Processing", "Neighbor Exploration"),
                    "SocraBuddy Socratic Engine"
            );
            case LEVEL_4_TARGETED -> SocraBuddyHintResponse.of(
                    HintLevel.LEVEL_4_TARGETED,
                    "Targeted Graph Edge Cases & In-Degree",
                    "For grid graphs, verify your 4-directional boundary checks: `r >= 0 && r < rows && c >= 0 && c < cols`. For directed topological sort, track `inDegree[]` arrays and only enqueue nodes when their in-degree reaches 0.",
                    "Are your grid coordinates strictly inside bounds before accessing `grid[r][c]`?",
                    List.of("Boundary Check", "InDegree", "Visited Mark Timing"),
                    "SocraBuddy Socratic Engine"
            );
        };
    }

    private SocraBuddyHintResponse generateDpHint(String title, HintLevel lvl, String q) {
        return switch (lvl) {
            case LEVEL_1_NUDGE -> SocraBuddyHintResponse.of(
                    HintLevel.LEVEL_1_NUDGE,
                    "Spotting Overlapping Subproblems",
                    "In Dynamic Programming, solving the larger problem depends on knowing the optimal solution to smaller subproblems. If you already knew the answer for index `i - 1` (or subproblem `n - 1`), what decision transitions you to index `i`?",
                    "What are the independent choices you can make at each step, and what is the cost or gain of each choice?",
                    List.of("Subproblems", "State Transition", "Optimal Choice"),
                    "SocraBuddy Socratic Engine"
            );
            case LEVEL_2_CONCEPT -> SocraBuddyHintResponse.of(
                    HintLevel.LEVEL_2_CONCEPT,
                    "State Definition & Recurrence Relation",
                    "Every DP problem rests on 3 pillars: (1) State Definition: What does `dp[i]` (or `dp[i][j]`) represent in plain English? (2) Recurrence Transition: How is `dp[i]` calculated from previous states? (3) Base Cases: What are the known initial values (`dp[0]`, `dp[1]`)?",
                    "If `dp[i]` represents the optimal value up to step `i`, what previous states (`dp[i-1]`, `dp[i-2]`) feed into it?",
                    List.of("DP State", "Recurrence Relation", "Tabulation", "Memoization"),
                    "SocraBuddy Socratic Engine"
            );
            case LEVEL_3_ALGORITHM -> SocraBuddyHintResponse.of(
                    HintLevel.LEVEL_3_ALGORITHM,
                    "Dynamic Programming Tabulation Blueprint",
                    "Step 1: Declare DP table `int[] dp = new int[n + 1];` (or 2D matrix for grid/string matching).\nStep 2: Populate base cases: `dp[0] = ...` and `dp[1] = ...`.\nStep 3: Loop in dependency order from `i = 2` to `n`.\nStep 4: At each step, apply the transition formula `dp[i] = Math.max/min(...)` or sum.\nStep 5: Return `dp[n]` as the final optimal answer.",
                    "What is the base case when input size is 0 or 1, and does your table size `n + 1` prevent index out of bounds?",
                    List.of("DP Blueprint", "Bottom-Up", "Table Allocation"),
                    "SocraBuddy Socratic Engine"
            );
            case LEVEL_4_TARGETED -> SocraBuddyHintResponse.of(
                    HintLevel.LEVEL_4_TARGETED,
                    "Targeted DP Bounds & Space Optimization",
                    "Check array boundary allocation (`new int[n + 1]`). If the recurrence only depends on the previous 1 or 2 states (like Fibonacci or House Robber), you can optimize space from O(N) to O(1) by maintaining just two variables (`prev1`, `prev2`) instead of an array.",
                    "Can your DP table be optimized to O(1) auxiliary space by keeping only the last two variables?",
                    List.of("Space Optimization", "Table Sizing n+1", "Base Case Guard"),
                    "SocraBuddy Socratic Engine"
            );
        };
    }

    private SocraBuddyHintResponse generateLinkedListHint(String title, HintLevel lvl, String q) {
        return switch (lvl) {
            case LEVEL_1_NUDGE -> SocraBuddyHintResponse.of(
                    HintLevel.LEVEL_1_NUDGE,
                    "Pointer Preservation & Head Modification",
                    "In a singly linked list, each node only knows where its `next` pointer points. If you change `curr.next` before storing its original target in a temporary variable, that entire rest of the list is lost in memory forever!",
                    "What temporary pointer must you save BEFORE changing `curr.next`?",
                    List.of("Pointer Preservation", "ListNode", "Reference Tracking"),
                    "SocraBuddy Socratic Engine"
            );
            case LEVEL_2_CONCEPT -> SocraBuddyHintResponse.of(
                    HintLevel.LEVEL_2_CONCEPT,
                    "Dummy Nodes & Two-Pointer Invariants",
                    "Two critical linked list techniques: (1) Dummy Head Pattern: Allocating a `ListNode dummy = new ListNode(0, head)` eliminates edge cases when modifying or removing the first node. (2) Fast & Slow Pointers (Floyd's Cycle Finding): A fast pointer moving 2 steps and slow moving 1 step can find middle nodes or detect cycles in O(N) time with O(1) space.",
                    "How does returning `dummy.next` instead of `head` simplify cases where the initial head node is deleted or swapped?",
                    List.of("Dummy Head", "Fast and Slow Pointers", "Floyd Cycle", "O(1) Space"),
                    "SocraBuddy Socratic Engine"
            );
            case LEVEL_3_ALGORITHM -> SocraBuddyHintResponse.of(
                    HintLevel.LEVEL_3_ALGORITHM,
                    "Linked List Pointer Manipulation Blueprint",
                    "Step 1: Handle edge cases: If `head == null` or `head.next == null`, return `head`.\nStep 2: Initialize `ListNode dummy = new ListNode(0, head);` and iterator pointers `prev = dummy`, `curr = head`.\nStep 3: Loop while `curr != null` (or `fast != null && fast.next != null`).\nStep 4: Save `ListNode nextNode = curr.next;`, execute reference rewiring, and advance pointers.\nStep 5: Return `dummy.next` (or new head).",
                    "Why must your loop condition check `fast != null && fast.next != null` to avoid null dereferencing?",
                    List.of("LinkedList Blueprint", "Pointer Rewiring", "Dummy Pattern"),
                    "SocraBuddy Socratic Engine"
            );
            case LEVEL_4_TARGETED -> SocraBuddyHintResponse.of(
                    HintLevel.LEVEL_4_TARGETED,
                    "Targeted Pointer Rewiring & Null Safety",
                    "To reverse pointers: `ListNode next = curr.next; curr.next = prev; prev = curr; curr = next;`. Always verify that the old head's `next` pointer is properly set to `null` to prevent infinite cyclic references.",
                    "Did you remember to terminate the end of your reversed or merged list with `null`?",
                    List.of("Pointer Swap Order", "ListNode Null Termination", "Dummy Return"),
                    "SocraBuddy Socratic Engine"
            );
        };
    }

    private SocraBuddyHintResponse generateBinarySearchHint(String title, HintLevel lvl, String q) {
        return switch (lvl) {
            case LEVEL_1_NUDGE -> SocraBuddyHintResponse.of(
                    HintLevel.LEVEL_1_NUDGE,
                    "Halving the Search Space",
                    "Binary Search works because the collection has a monotonic or sorted invariant. When you inspect the middle element `mid`, half of the remaining elements can be discarded immediately without inspecting them.",
                    "If `nums[mid] < target`, which half of the array is guaranteed NOT to contain the answer?",
                    List.of("Binary Search", "Search Space", "Halving Invariant"),
                    "SocraBuddy Socratic Engine"
            );
            case LEVEL_2_CONCEPT -> SocraBuddyHintResponse.of(
                    HintLevel.LEVEL_2_CONCEPT,
                    "Logarithmic Time O(log N) & Mid Overflow",
                    "By eliminating half the candidates per iteration, Binary Search operates in O(log N) time. Calculating `mid = left + (right - left) / 2` avoids integer overflow that occurs with `(left + right) / 2` when indices are large.",
                    "Why is `left + (right - left) / 2` mathematically identical to `(left + right) / 2` but immune to integer overflow?",
                    List.of("O(log N)", "Mid Overflow", "Search Range", "Invariants"),
                    "SocraBuddy Socratic Engine"
            );
            case LEVEL_3_ALGORITHM -> SocraBuddyHintResponse.of(
                    HintLevel.LEVEL_3_ALGORITHM,
                    "Binary Search Blueprint",
                    "Step 1: Set boundaries: `int left = 0, right = nums.length - 1;`.\nStep 2: Loop condition: `while (left <= right)`.\nStep 3: Calculate `int mid = left + (right - left) / 2;`.\nStep 4: Check: If `nums[mid] == target`, return `mid`.\nStep 5: If `nums[mid] < target`, set `left = mid + 1`; else set `right = mid - 1`.\nStep 6: Return `-1` if the target was not found.",
                    "Why does setting `left = mid + 1` or `right = mid - 1` guarantee progress and prevent infinite loops?",
                    List.of("Binary Search Blueprint", "Boundary Shrink", "Strict Termination"),
                    "SocraBuddy Socratic Engine"
            );
            case LEVEL_4_TARGETED -> SocraBuddyHintResponse.of(
                    HintLevel.LEVEL_4_TARGETED,
                    "Targeted Boundary Condition Pointer",
                    "Check whether your problem requires exact match `left <= right` or finding an insertion point / lower bound `left < right`. Ensure you update `left = mid + 1` or `right = mid` according to the search interval definition.",
                    "Does your loop terminate correctly when the array has only 1 or 2 elements?",
                    List.of("left <= right", "mid + 1", "Search Termination"),
                    "SocraBuddy Socratic Engine"
            );
        };
    }

    private SocraBuddyHintResponse generateTwoPointerHint(String title, HintLevel lvl, String q) {
        return switch (lvl) {
            case LEVEL_1_NUDGE -> SocraBuddyHintResponse.of(
                    HintLevel.LEVEL_1_NUDGE,
                    "Window Expansion & Contraction",
                    "With sliding windows and two pointers, your `right` pointer expands the window to satisfy a condition, while your `left` pointer contracts the window to restore validity or find the minimal optimal length.",
                    "Under what exact condition should your `left` pointer start advancing to shrink the window?",
                    List.of("Two Pointers", "Sliding Window", "Window Boundaries"),
                    "SocraBuddy Socratic Engine"
            );
            case LEVEL_2_CONCEPT -> SocraBuddyHintResponse.of(
                    HintLevel.LEVEL_2_CONCEPT,
                    "Linear Time O(N) Two-Pointer Invariant",
                    "Even though there is an inner loop contracting `left`, each element enters the window once via `right` and leaves once via `left`. Thus, total operations are bounded by 2N, achieving O(N) linear time.",
                    "Why does monotonic movement of `left` and `right` guarantee O(N) total time instead of O(N^2)?",
                    List.of("Linear Time O(N)", "Amortized Analysis", "Window Invariant"),
                    "SocraBuddy Socratic Engine"
            );
            case LEVEL_3_ALGORITHM -> SocraBuddyHintResponse.of(
                    HintLevel.LEVEL_3_ALGORITHM,
                    "Sliding Window Blueprint",
                    "Step 1: Initialize `left = 0`, result tracking variable (e.g. `maxLen = 0`), and frequency map/array.\nStep 2: Loop `right` from `0` to `n - 1` to expand the window.\nStep 3: Add `nums[right]` into your window state.\nStep 4: While the window state is INVALID, remove `nums[left]` from state and increment `left++`.\nStep 5: Update `maxLen = Math.max(maxLen, right - left + 1)` and continue.\nStep 6: Return result.",
                    "Why is `right - left + 1` the exact formula for the current window size?",
                    List.of("Sliding Window Blueprint", "Expand and Contract", "Window Size"),
                    "SocraBuddy Socratic Engine"
            );
            case LEVEL_4_TARGETED -> SocraBuddyHintResponse.of(
                    HintLevel.LEVEL_4_TARGETED,
                    "Targeted Window Update Order",
                    "Ensure you update the window metric BEFORE advancing `left++` when shrinking, and verify that your window state cleanup (e.g., decrementing frequency in map) occurs before removing the key.",
                    "Are you updating your result variable after the inner contraction loop has guaranteed window validity?",
                    List.of("Window State Invariant", "left++ Timing", "maxLen Calculation"),
                    "SocraBuddy Socratic Engine"
            );
        };
    }

    private SocraBuddyHintResponse generateStackQueueHint(String title, HintLevel lvl, String q) {
        return switch (lvl) {
            case LEVEL_1_NUDGE -> SocraBuddyHintResponse.of(
                    HintLevel.LEVEL_1_NUDGE,
                    "LIFO vs FIFO Matching Intuition",
                    "When solving problems involving nested matching (like parentheses) or nearest greater elements, the most recently opened/seen item must be resolved first. This Last-In-First-Out behavior is naturally modeled by a Stack.",
                    "When you encounter a closing character or smaller element, which item in your storage must it match against?",
                    List.of("Stack LIFO", "Nested Matching", "Queue FIFO"),
                    "SocraBuddy Socratic Engine"
            );
            case LEVEL_2_CONCEPT -> SocraBuddyHintResponse.of(
                    HintLevel.LEVEL_2_CONCEPT,
                    "Monotonic Stack & Amortized Linear Time",
                    "A Monotonic Stack maintains elements in strictly increasing or decreasing order. By popping elements that violate the monotonicity invariant, you can find the 'Next Greater Element' for every index in a single O(N) pass.",
                    "Why does maintaining a strictly decreasing stack allow you to find the next greater element in O(1) amortized time per item?",
                    List.of("Monotonic Stack", "Next Greater Element", "Amortized O(N)"),
                    "SocraBuddy Socratic Engine"
            );
            case LEVEL_3_ALGORITHM -> SocraBuddyHintResponse.of(
                    HintLevel.LEVEL_3_ALGORITHM,
                    "Stack Processing Blueprint",
                    "Step 1: Initialize `Deque<Character> stack = new ArrayDeque<>();` (prefer ArrayDeque over legacy Stack in Java).\nStep 2: Iterate through each token/character.\nStep 3: If it is an opening bracket or candidate, push onto the stack.\nStep 4: If it is a closing bracket, check `stack.isEmpty()` (invalid if empty) and pop top to verify matching pair.\nStep 5: After iteration, valid only if `stack.isEmpty()`.",
                    "Why must `stack.isEmpty()` be checked at the end to catch unmatched opening tokens?",
                    List.of("Stack Blueprint", "ArrayDeque", "Empty Check"),
                    "SocraBuddy Socratic Engine"
            );
            case LEVEL_4_TARGETED -> SocraBuddyHintResponse.of(
                    HintLevel.LEVEL_4_TARGETED,
                    "Targeted Stack Empty Guard",
                    "Always call `!stack.isEmpty()` before calling `stack.peek()` or `stack.pop()` to prevent `EmptyStackException` or `NoSuchElementException`. In Java, use `ArrayDeque` instead of `java.util.Stack` for higher performance.",
                    "Have you guarded every `pop()` call with an `isEmpty()` check?",
                    List.of("EmptyStack Guard", "ArrayDeque", "Matching Map"),
                    "SocraBuddy Socratic Engine"
            );
        };
    }

    private SocraBuddyHintResponse generateBacktrackingHint(String title, HintLevel lvl, String q) {
        return switch (lvl) {
            case LEVEL_1_NUDGE -> SocraBuddyHintResponse.of(
                    HintLevel.LEVEL_1_NUDGE,
                    "Decision Tree & State Restoration",
                    "Backtracking builds candidates step-by-step and abandons ('backtracks') as soon as a candidate cannot lead to a valid solution. When you explore a choice, what state must you restore before exploring alternative choices?",
                    "What does your candidate state look like when it reaches the goal (base case)?",
                    List.of("Backtracking", "Decision Tree", "Undo Choice"),
                    "SocraBuddy Socratic Engine"
            );
            case LEVEL_2_CONCEPT -> SocraBuddyHintResponse.of(
                    HintLevel.LEVEL_2_CONCEPT,
                    "The Choose-Explore-Unchoose Pattern",
                    "The core invariant of backtracking is: (1) Choose: Add candidate to path, (2) Explore: Recurse with updated index or state, (3) Unchoose: Remove candidate from path (`path.remove(path.size() - 1)`). This ensures the path state remains clean for sibling branches.",
                    "Why must you add a deep copy `new ArrayList<>(path)` to the result list instead of adding `path` directly?",
                    List.of("Choose Explore Unchoose", "Deep Copy", "State Cleanliness"),
                    "SocraBuddy Socratic Engine"
            );
            case LEVEL_3_ALGORITHM -> SocraBuddyHintResponse.of(
                    HintLevel.LEVEL_3_ALGORITHM,
                    "Backtracking Template Blueprint",
                    "Step 1: Base Case — If `path.size() == target` or condition met, `result.add(new ArrayList<>(path))` and return.\nStep 2: Loop through available candidates starting at `startIdx`.\nStep 3: Pruning — Skip invalid choices with `if (...) continue;`.\nStep 4: Choose — `path.add(candidate)`.\nStep 5: Explore — `backtrack(startIdx + 1, path, result)`.\nStep 6: Unchoose — `path.remove(path.size() - 1)`.",
                    "How does `path.remove(path.size() - 1)` restore the decision state for the next loop iteration?",
                    List.of("Backtracking Blueprint", "Pruning", "State Restoration"),
                    "SocraBuddy Socratic Engine"
            );
            case LEVEL_4_TARGETED -> SocraBuddyHintResponse.of(
                    HintLevel.LEVEL_4_TARGETED,
                    "Targeted Deep Copy & Duplicate Handling",
                    "Make sure to call `result.add(new ArrayList<>(path));` when saving to the result list. If the input contains duplicates, sort the array first and add pruning: `if (i > start && nums[i] == nums[i - 1]) continue;`.",
                    "Are you creating a `new ArrayList<>(path)` before storing it in your final results list?",
                    List.of("new ArrayList<>(path)", "Duplicate Pruning", "Sort First"),
                    "SocraBuddy Socratic Engine"
            );
        };
    }

    private SocraBuddyHintResponse generateArrayHint(String title, HintLevel lvl, String q) {
        return switch (lvl) {
            case LEVEL_1_NUDGE -> SocraBuddyHintResponse.of(
                    HintLevel.LEVEL_1_NUDGE,
                    "Array Invariants & Lookup Intuition",
                    "Before writing nested scans across the array, consider the mathematical or structural relationship between elements. If you record elements and their positions as you visit them, how can future iterations query this memory instantly?",
                    "What value are you searching for at each step, and how can you check for it in O(1) time?",
                    List.of("Array Traversal", "O(1) Memory", "Pair Match"),
                    "SocraBuddy Socratic Engine"
            );
            case LEVEL_2_CONCEPT -> SocraBuddyHintResponse.of(
                    HintLevel.LEVEL_2_CONCEPT,
                    "Hash Table Lookup & Time-Space Tradeoff",
                    "A nested loop requires O(N^2) time complexity. By storing seen numbers in an auxiliary HashMap `Map<Key, Value>`, you can trade O(N) space for instantaneous O(1) lookup time, reducing the total runtime to linear O(N).",
                    "What should be stored as the Map's key, and what should be stored as the value?",
                    List.of("HashMap", "Key-Value", "O(1) Lookup", "Linear Time O(N)", "Time-Space Tradeoff"),
                    "SocraBuddy Socratic Engine"
            );
            case LEVEL_3_ALGORITHM -> SocraBuddyHintResponse.of(
                    HintLevel.LEVEL_3_ALGORITHM,
                    "One-Pass Hash Table Blueprint",
                    "Step 1: Declare an auxiliary Hash Table / Map.\nStep 2: Loop through the array from index `0` to `n - 1`.\nStep 3: At each index `i`, compute the required target value or invariant.\nStep 4: Check if the Map already contains this required value. If yes, return the result immediately.\nStep 5: If not, insert current element and index into the Map and proceed.\nStep 6: Return default fallback if loop finishes without a match.",
                    "Why is checking the map BEFORE inserting the current element essential to avoid using the same element twice?",
                    List.of("One-Pass Blueprint", "State Insertion", "Hash Table"),
                    "SocraBuddy Socratic Engine"
            );
            case LEVEL_4_TARGETED -> SocraBuddyHintResponse.of(
                    HintLevel.LEVEL_4_TARGETED,
                    "Targeted Array Bounds & Method Signatures",
                    "In Java: Check `if (map.containsKey(targetValue))` inside your loop and return `new int[] { map.get(targetValue), i }`. Ensure your loop index `i < nums.length` does not exceed bounds and return `new int[]{}` as a fallback.",
                    "Does your method return the indices in the correct array format?",
                    List.of("Method Signature", "Array Return", "Lookup Key", "Loop Bounds"),
                    "SocraBuddy Socratic Engine"
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
