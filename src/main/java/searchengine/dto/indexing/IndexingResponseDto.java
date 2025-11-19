package searchengine.dto.indexing;

import lombok.Getter;
import searchengine.dto.IndexingResponse;


@Getter
public final class IndexingResponseDto implements IndexingResponse {
    private final boolean result = true;
}
