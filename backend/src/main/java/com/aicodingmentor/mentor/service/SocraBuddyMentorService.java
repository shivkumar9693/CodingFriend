package com.aicodingmentor.mentor.service;

import com.aicodingmentor.mentor.dto.SocraBuddyDiagnosisRequest;
import com.aicodingmentor.mentor.dto.SocraBuddyDiagnosisResponse;
import com.aicodingmentor.mentor.dto.SocraBuddyHintRequest;
import com.aicodingmentor.mentor.dto.SocraBuddyHintResponse;

import java.util.UUID;

public interface SocraBuddyMentorService {

    SocraBuddyHintResponse generateHint(UUID userId, SocraBuddyHintRequest request);

    SocraBuddyDiagnosisResponse diagnoseMistake(UUID userId, SocraBuddyDiagnosisRequest request);
}
