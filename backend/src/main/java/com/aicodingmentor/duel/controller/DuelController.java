package com.aicodingmentor.duel.controller;

import com.aicodingmentor.auth.security.UserPrincipal;
import com.aicodingmentor.duel.dto.CreateDuelRequest;
import com.aicodingmentor.duel.dto.DuelDto;
import com.aicodingmentor.duel.dto.JoinDuelRequest;
import com.aicodingmentor.duel.service.DuelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/duels")
@Tag(name = "1v1 Code Duels", description = "Endpoints for real-time 1v1 student vs student coding battles and matchmaking")
@SecurityRequirement(name = "BearerAuth")
public class DuelController {

    private final DuelService duelService;

    public DuelController(DuelService duelService) {
        this.duelService = duelService;
    }

    @PostMapping("/queue")
    @Operation(summary = "Queue or create a 1v1 duel", description = "Joins the matchmaking queue for the selected difficulty or creates a new waiting duel room")
    public ResponseEntity<DuelDto> createOrQueue(
            @Valid @RequestBody CreateDuelRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(duelService.createOrQueueDuel(request, principal.getId()));
    }

    @PostMapping("/join")
    @Operation(summary = "Join private duel room", description = "Joins a private duel room using a 6-character room code")
    public ResponseEntity<DuelDto> joinRoom(
            @Valid @RequestBody JoinDuelRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(duelService.joinDuelRoom(request.roomCode(), principal.getId()));
    }

    @GetMapping("/room/{roomCode}")
    @Operation(summary = "Get duel room status", description = "Retrieves live duel details, participant progress, and timer status")
    public ResponseEntity<DuelDto> getDuelRoom(@PathVariable String roomCode) {
        return ResponseEntity.ok(duelService.getDuelByRoomCode(roomCode));
    }

    @PostMapping("/room/{roomCode}/progress")
    @Operation(summary = "Update live duel progress", description = "Updates the current player's testcase / completion progress in the duel")
    public ResponseEntity<DuelDto> updateProgress(
            @PathVariable String roomCode,
            @Valid @RequestBody JoinDuelRequest.DuelProgressUpdate request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(duelService.updateDuelProgress(roomCode, principal.getId(), request.progressPercent()));
    }

    @PostMapping("/room/{roomCode}/complete")
    @Operation(summary = "Declare duel complete", description = "Completes the duel when a player submits an ACCEPTED solution")
    public ResponseEntity<DuelDto> completeDuel(
            @PathVariable String roomCode,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(duelService.completeDuel(roomCode, principal.getId()));
    }

    @PostMapping("/room/{roomCode}/leave")
    @Operation(summary = "Leave or cancel duel room", description = "Leaves/cancels the duel room, deleting waiting rooms to save space or forfeiting active matches")
    public ResponseEntity<Void> leaveRoom(
            @PathVariable String roomCode,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        duelService.leaveOrCancelDuel(roomCode, principal.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/history")
    @Operation(summary = "Get user duel history", description = "Retrieves past duels and outcomes for the authenticated user")
    public ResponseEntity<List<DuelDto>> getHistory(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(duelService.getUserDuelHistory(principal.getId()));
    }
}
