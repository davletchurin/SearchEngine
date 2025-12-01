package searchengine.dto;

import lombok.Data;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.search.SearchResponse;

@Data
public class ErrorResponse implements IndexingResponse, SearchResponse {
    private final boolean result = false;
    private final String error;

    public ErrorResponse(String error) {
        this.error = error;
    }
}