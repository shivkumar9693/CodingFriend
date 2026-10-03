package com.aicodingmentor.problem.service;

import com.aicodingmentor.common.api.PageResponse;
import com.aicodingmentor.problem.domain.Problem;
import com.aicodingmentor.problem.domain.ProblemDifficulty;
import com.aicodingmentor.problem.domain.TestCase;
import com.aicodingmentor.problem.dto.AdminProblemDetailResponse;
import com.aicodingmentor.problem.dto.CreateProblemRequest;
import com.aicodingmentor.problem.dto.CreateTestCaseRequest;
import com.aicodingmentor.problem.dto.ProblemDetailResponse;
import com.aicodingmentor.problem.dto.ProblemSummaryResponse;
import com.aicodingmentor.problem.dto.TestCaseResponse;
import com.aicodingmentor.problem.dto.UpdateProblemRequest;
import com.aicodingmentor.problem.exception.DuplicateProblemSlugException;
import com.aicodingmentor.problem.exception.ProblemNotFoundException;
import com.aicodingmentor.problem.repository.ProblemRepository;
import com.aicodingmentor.problem.repository.ProblemSpecifications;
import com.aicodingmentor.problem.repository.TestCaseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static com.aicodingmentor.common.config.RedisCacheConfig.CACHE_PROBLEMS;
import static com.aicodingmentor.common.config.RedisCacheConfig.CACHE_PROBLEM_DETAILS;

@Service
public class ProblemServiceImpl implements ProblemService {

    private static final Logger log = LoggerFactory.getLogger(ProblemServiceImpl.class);

    private final ProblemRepository problemRepository;
    private final TestCaseRepository testCaseRepository;
    private final com.aicodingmentor.execution.service.DriverHarnessRegistry driverHarnessRegistry;

    public ProblemServiceImpl(
            ProblemRepository problemRepository,
            TestCaseRepository testCaseRepository,
            com.aicodingmentor.execution.service.DriverHarnessRegistry driverHarnessRegistry
    ) {
        this.problemRepository = problemRepository;
        this.testCaseRepository = testCaseRepository;
        this.driverHarnessRegistry = driverHarnessRegistry;
    }

    @Override
    @Cacheable(value = CACHE_PROBLEMS, key = "{#difficulty, #tag, #pageable.pageNumber, #pageable.pageSize, #pageable.sort.toString()}", unless = "#result == null")
    @Transactional(readOnly = true)
    public PageResponse<ProblemSummaryResponse> getProblems(ProblemDifficulty difficulty, String tag, Pageable pageable) {
        Specification<Problem> spec = ProblemSpecifications.filterBy(difficulty, tag);
        Page<Problem> problemsPage = problemRepository.findAll(spec, pageable);
        Page<ProblemSummaryResponse> dtoPage = problemsPage.map(ProblemSummaryResponse::from);
        return PageResponse.from(dtoPage);
    }

    @Override
    @Cacheable(value = CACHE_PROBLEM_DETAILS, key = "#idOrSlug", unless = "#result == null")
    @Transactional(readOnly = true)
    public ProblemDetailResponse getProblemByIdOrSlug(String idOrSlug) {
        if (idOrSlug == null || idOrSlug.trim().isEmpty()) {
            throw new ProblemNotFoundException("Problem identifier must not be empty");
        }

        Problem problem = findByIdOrSlug(idOrSlug);
        List<TestCase> sampleTestCases = testCaseRepository.findByProblemIdAndIsHiddenFalseOrderBySequenceOrderAsc(problem.getId());
        List<TestCaseResponse> testCaseDtos = sampleTestCases.stream()
                .map(tc -> TestCaseResponse.from(tc, false))
                .toList();

        var starterTemplates = driverHarnessRegistry.getAllStarterCodes(problem.getSlug());
        return ProblemDetailResponse.from(problem, testCaseDtos, starterTemplates);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminProblemDetailResponse getAdminProblemById(UUID id) {
        if (id == null) {
            throw new ProblemNotFoundException("Problem ID must not be null");
        }

        Problem problem = problemRepository.findById(id)
                .orElseThrow(() -> new ProblemNotFoundException("Problem not found with id: " + id));

        List<TestCase> allTestCases = testCaseRepository.findByProblemIdOrderBySequenceOrderAsc(problem.getId());
        List<TestCaseResponse> testCaseDtos = allTestCases.stream()
                .map(tc -> TestCaseResponse.from(tc, true))
                .toList();

        return AdminProblemDetailResponse.from(problem, testCaseDtos);
    }

    @Override
    @CacheEvict(value = {CACHE_PROBLEMS, CACHE_PROBLEM_DETAILS}, allEntries = true)
    @Transactional
    public AdminProblemDetailResponse createProblem(CreateProblemRequest request) {
        String slug = generateOrNormalizeSlug(request.slug(), request.title());

        if (problemRepository.existsBySlug(slug)) {
            throw new DuplicateProblemSlugException("A problem with slug '" + slug + "' already exists");
        }

        String language = (request.supportedLanguage() != null && !request.supportedLanguage().isBlank())
                ? request.supportedLanguage().trim().toUpperCase(Locale.ROOT)
                : "JAVA";

        Problem problem = new Problem(
                request.title().trim(),
                slug,
                request.description().trim(),
                request.difficulty(),
                request.constraints(),
                request.inputFormat(),
                request.outputFormat(),
                request.examples(),
                request.starterCode(),
                language,
                request.tags()
        );

        if (request.testCases() != null) {
            for (CreateTestCaseRequest tcReq : request.testCases()) {
                TestCase testCase = new TestCase(
                        tcReq.inputData(),
                        tcReq.expectedOutput(),
                        tcReq.isHidden(),
                        tcReq.sequenceOrder(),
                        tcReq.explanation()
                );
                problem.addTestCase(testCase);
            }
        }

        Problem savedProblem = problemRepository.save(problem);
        log.info("Created problem with ID: {} and slug: {}", savedProblem.getId(), savedProblem.getSlug());

        List<TestCaseResponse> testCaseDtos = savedProblem.getTestCases().stream()
                .map(tc -> TestCaseResponse.from(tc, true))
                .toList();

        return AdminProblemDetailResponse.from(savedProblem, testCaseDtos);
    }

    @Override
    @CacheEvict(value = {CACHE_PROBLEMS, CACHE_PROBLEM_DETAILS}, allEntries = true)
    @Transactional
    public AdminProblemDetailResponse updateProblem(UUID id, UpdateProblemRequest request) {
        if (id == null) {
            throw new ProblemNotFoundException("Problem ID must not be null");
        }

        Problem problem = problemRepository.findById(id)
                .orElseThrow(() -> new ProblemNotFoundException("Problem not found with id: " + id));

        String slug = generateOrNormalizeSlug(request.slug(), request.title());

        if (problemRepository.existsBySlugAndIdNot(slug, id)) {
            throw new DuplicateProblemSlugException("A problem with slug '" + slug + "' already exists");
        }

        String language = (request.supportedLanguage() != null && !request.supportedLanguage().isBlank())
                ? request.supportedLanguage().trim().toUpperCase(Locale.ROOT)
                : problem.getSupportedLanguage();

        problem.setTitle(request.title().trim());
        problem.setSlug(slug);
        problem.setDescription(request.description().trim());
        problem.setDifficulty(request.difficulty());
        problem.setConstraints(request.constraints());
        problem.setInputFormat(request.inputFormat());
        problem.setOutputFormat(request.outputFormat());
        problem.setExamples(request.examples());
        problem.setStarterCode(request.starterCode());
        problem.setSupportedLanguage(language);
        problem.setTags(request.tags());

        if (request.testCases() != null) {
            problem.clearTestCases();
            for (CreateTestCaseRequest tcReq : request.testCases()) {
                TestCase testCase = new TestCase(
                        tcReq.inputData(),
                        tcReq.expectedOutput(),
                        tcReq.isHidden(),
                        tcReq.sequenceOrder(),
                        tcReq.explanation()
                );
                problem.addTestCase(testCase);
            }
        }

        Problem updatedProblem = problemRepository.save(problem);
        log.info("Updated problem with ID: {} and slug: {}", updatedProblem.getId(), updatedProblem.getSlug());

        List<TestCaseResponse> testCaseDtos = updatedProblem.getTestCases().stream()
                .map(tc -> TestCaseResponse.from(tc, true))
                .toList();

        return AdminProblemDetailResponse.from(updatedProblem, testCaseDtos);
    }

    @Override
    @CacheEvict(value = {CACHE_PROBLEMS, CACHE_PROBLEM_DETAILS}, allEntries = true)
    @Transactional
    public void deleteProblem(UUID id) {
        if (id == null) {
            throw new ProblemNotFoundException("Problem ID must not be null");
        }

        Problem problem = problemRepository.findById(id)
                .orElseThrow(() -> new ProblemNotFoundException("Problem not found with id: " + id));

        problemRepository.delete(problem);
        log.info("Deleted problem with ID: {}", id);
    }

    private Problem findByIdOrSlug(String idOrSlug) {
        try {
            UUID uuid = UUID.fromString(idOrSlug.trim());
            return problemRepository.findById(uuid)
                    .orElseGet(() -> problemRepository.findBySlug(idOrSlug.trim())
                            .orElseThrow(() -> new ProblemNotFoundException("Problem not found with identifier: " + idOrSlug)));
        } catch (IllegalArgumentException e) {
            return problemRepository.findBySlug(idOrSlug.trim())
                    .orElseThrow(() -> new ProblemNotFoundException("Problem not found with slug: " + idOrSlug));
        }
    }

    private String generateOrNormalizeSlug(String rawSlug, String title) {
        String base = (rawSlug != null && !rawSlug.trim().isEmpty()) ? rawSlug : title;
        return base.toLowerCase(Locale.ROOT)
                .trim()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }
}
