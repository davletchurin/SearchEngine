package searchengine.dto.search;

import lombok.Data;

import java.util.List;

@Data
public class SearchResponseDto implements SearchResponse {
    private boolean result;
    private Integer count;
    private List<DetailedDataItem> data;
}