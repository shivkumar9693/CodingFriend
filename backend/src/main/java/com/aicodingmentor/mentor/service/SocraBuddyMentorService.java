package com.aicodingmentor.mentor.service;

import com.aicodingmentor.mentor.dto.InlineAssistRequest;
import com.aicodingmentor.mentor.dto.InlineAssistResponse;
import com.aicodingmentor.mentor.dto.SocraBuddyDiagnosisRequest;
import com.aicodingmentor.mentor.dto.SocraBuddyDiagnosisResponse;
import com.aicodingmentor.mentor.dto.SocraBuddyHintRequest;
import com.aicodingmentor.mentor.dto.SocraBuddyHintResponse;

import java.util.UUID;

public interface SocraBuddyMentorService {

    SocraBuddyHintResponse generateHint(UUID userId, SocraBuddyHintRequest request);

    org.springframework.web.servlet.mvc.method.annotation.SseEmitter streamHint(UUID userId, SocraBuddyHintRequest request);

    SocraBuddyDiagnosisResponse diagnoseMistake(UUID userId, SocraBuddyDiagnosisRequest request);

    InlineAssistResponse generateInlineAssist(UUID userId, InlineAssistRequest request);
}
