package capstoneBackend.ca.sheridancollege.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import capstoneBackend.ca.sheridancollege.beans.MoodBoardMatchRequest;
import capstoneBackend.ca.sheridancollege.beans.MoodBoardRequest;
import capstoneBackend.ca.sheridancollege.beans.MoodBoardResponse;
import capstoneBackend.ca.sheridancollege.beans.User;
import capstoneBackend.ca.sheridancollege.service.MoodBoardService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/moodboard")
@AllArgsConstructor
public class MoodBoardController {

    private final MoodBoardService moodBoardService;

    /** POST /api/moodboard — save a mood board */
    @PostMapping
    public ResponseEntity<MoodBoardResponse> save(
            @AuthenticationPrincipal User user,
            @RequestBody MoodBoardRequest request) {

        log.info("Saving mood board for user {} with mood '{}'", user.getId(), request.getMood());
        MoodBoardResponse response = moodBoardService.saveMoodBoard(user.getId(), request);
        return ResponseEntity.ok(response);
    }

    /** GET /api/moodboard — fetch all mood boards for the user */
    @GetMapping
    public ResponseEntity<List<MoodBoardResponse>> getAll(@AuthenticationPrincipal User user) {
        log.info("Fetching mood boards for user {}", user.getId());
        return ResponseEntity.ok(moodBoardService.getMoodBoards(user.getId()));
    }

    /** POST /api/moodboard/match — match wardrobe items to a mood */
    @PostMapping("/match")
    public ResponseEntity<MoodBoardResponse> match(
            @AuthenticationPrincipal User user,
            @RequestBody MoodBoardMatchRequest request) {

        log.info("Mood board match requested by user {} for mood '{}'", user.getId(), request.getMood());
        MoodBoardResponse response = moodBoardService.matchOutfits(user.getId(), request);
        return ResponseEntity.ok(response);
    }
}
