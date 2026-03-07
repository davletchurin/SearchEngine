package org.example.searchengine.dto;

import lombok.Data;
import org.example.searchengine.dto.indexing.IndexingResponse;
import org.example.searchengine.dto.search.SearchResponse;

@Data
public class ErrorResponse implements IndexingResponse, SearchResponse {
    private final boolean result = false;
    private final String error;

    public ErrorResponse(String error) {
        this.error = error;
    }
}