package org.example.searchengine.dto.statistics;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TotalStatistics {
    private int sites;
    private int pages;
    private int lemmas;
    @JsonProperty("isIndexing")
    private boolean isIndexing;
}
