package com.aicodingmentor.learning.service;

import com.aicodingmentor.learning.domain.MasteryStatus;
import com.aicodingmentor.learning.dto.CreateRevisionNoteRequest;
import com.aicodingmentor.learning.dto.GeneratePostMortemRequest;
import com.aicodingmentor.learning.dto.PostMortemAnalysisResponse;
import com.aicodingmentor.learning.dto.RevisionNoteResponse;
import com.aicodingmentor.learning.dto.RevisionNotesSummaryDto;
import com.aicodingmentor.learning.dto.UpdateRevisionNoteRequest;

import java.util.List;
import java.util.UUID;

public interface RevisionNoteService {

    PostMortemAnalysisResponse generatePostMortem(UUID userId, GeneratePostMortemRequest request);

    RevisionNoteResponse createNote(UUID userId, CreateRevisionNoteRequest request);

    List<RevisionNoteResponse> getUserNotes(UUID userId, MasteryStatus status, Boolean bookmarked, String search);

    RevisionNoteResponse getNoteById(UUID userId, UUID noteId);

    RevisionNoteResponse updateNote(UUID userId, UUID noteId, UpdateRevisionNoteRequest request);

    void deleteNote(UUID userId, UUID noteId);

    RevisionNoteResponse markReviewed(UUID userId, UUID noteId);

    RevisionNotesSummaryDto getSummary(UUID userId);
}
