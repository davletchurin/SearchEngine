package searchengine.dto.indexing;

import lombok.Data;

@Data
public final class IndexingResponseDto implements IndexingResponse {
    private final boolean result = true;
}
