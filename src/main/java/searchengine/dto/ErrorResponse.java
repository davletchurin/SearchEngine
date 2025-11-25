package searchengine.dto;

import lombok.Getter;
import searchengine.dto.indexing.IndexingResponse;

@Getter
public final class ErrorResponse implements Response, IndexingResponse {
    private final boolean result = false;
    private final String error;

    public ErrorResponse(String error) {
        this.error = error;
    }
}