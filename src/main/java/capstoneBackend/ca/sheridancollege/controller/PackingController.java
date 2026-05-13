package capstoneBackend.ca.sheridancollege.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import capstoneBackend.ca.sheridancollege.beans.PackingListResponse;
import capstoneBackend.ca.sheridancollege.beans.PackingRequest;
import capstoneBackend.ca.sheridancollege.beans.User;
import capstoneBackend.ca.sheridancollege.service.PackingService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/packing")
@AllArgsConstructor
public class PackingController {

    private final PackingService packingService;

    @PostMapping("/suggest")
    public ResponseEntity<PackingListResponse> suggest(
            @AuthenticationPrincipal User user,
            @RequestBody PackingRequest request) {

        log.info("Packing suggestion requested by user {} for {} ({} days)",
                user.getId(), request.getDestination(), request.getTripLengthDays());

        PackingListResponse response = packingService.suggestPacking(user.getId(), request);
        return ResponseEntity.ok(response);
    }
}
