package com.aicodingmentor.problem.service;

import com.aicodingmentor.common.api.PageResponse;
import com.aicodingmentor.problem.domain.ProblemDifficulty;
import com.aicodingmentor.problem.dto.AdminProblemDetailResponse;
import com.aicodingmentor.problem.dto.CreateProblemRequest;
import com.aicodingmentor.problem.dto.ProblemDetailResponse;
import com.aicodingmentor.problem.dto.ProblemSummaryResponse;
import com.aicodingmentor.problem.dto.UpdateProblemRequest;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ProblemService {

    PageResponse<ProblemSummaryResponse> getProblems(ProblemDifficulty difficulty, String tag, Pageable pageable);

    ProblemDetailResponse getProblemByIdOrSlug(String idOrSlug);

    AdminProblemDetailResponse getAdminProblemById(UUID id);

    AdminProblemDetailResponse createProblem(CreateProblemRequest request);

    AdminProblemDetailResponse updateProblem(UUID id, UpdateProblemRequest request);

    void deleteProblem(UUID id);
}
