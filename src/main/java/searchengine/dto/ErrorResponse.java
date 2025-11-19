package searchengine.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
public final class ErrorResponse implements IndexingResponse {
    private final boolean result = false;
    private final String error;

    public ErrorResponse(String error) {
        this.error = error;
    }
}