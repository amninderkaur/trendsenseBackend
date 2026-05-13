package capstoneBackend.ca.sheridancollege.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import capstoneBackend.ca.sheridancollege.beans.ShoppingRequest;
import capstoneBackend.ca.sheridancollege.beans.ShoppingSuggestionsResponse;
import capstoneBackend.ca.sheridancollege.beans.User;
import capstoneBackend.ca.sheridancollege.service.ShoppingService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/shopping")
@AllArgsConstructor
public class ShoppingController {

    private final ShoppingService shoppingService;

    @PostMapping("/suggest")
    public ResponseEntity<ShoppingSuggestionsResponse> suggest(
            @AuthenticationPrincipal User user,
            @RequestBody ShoppingRequest request) {

        log.info("Shopping suggestion requested by user {} in '{}', budget {} {}",
                user.getId(), request.getLocation(), request.getBudget(), request.getCurrency());

        ShoppingSuggestionsResponse response = shoppingService.suggest(user.getId(), request);
        return ResponseEntity.ok(response);
    }
}
