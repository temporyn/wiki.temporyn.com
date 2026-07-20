package com.temporyn.wiki.controller;

import com.temporyn.wiki.dto.SearchResult;
import com.temporyn.wiki.service.SearchService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** REST API for sidebar full-text search. */
@RestController
public class SearchController {

    private static final int MAX_RESULTS = 30;

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/api/search")
    public List<SearchResult> search(@RequestParam("q") String query) {
        return searchService.search(query, MAX_RESULTS);
    }
}
