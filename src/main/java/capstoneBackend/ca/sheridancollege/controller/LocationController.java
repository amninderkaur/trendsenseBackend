package capstoneBackend.ca.sheridancollege.controller;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import capstoneBackend.ca.sheridancollege.service.LocationService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/location")
@CrossOrigin(origins = "*")
@AllArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @GetMapping("/autocomplete")
    public List<String> autocomplete(@RequestParam("q") String query) {
        log.info("Location autocomplete requested for query '{}'", query);
        return locationService.autocomplete(query);
    }
}
