package org.example.searchengine.services;

import org.example.searchengine.dto.search.SearchResponse;

public interface SearchService {
    SearchResponse search(String query, String site, int offset, int limit);
}
