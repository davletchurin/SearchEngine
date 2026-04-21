package org.example.searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import org.example.searchengine.model.SiteEntity;
import org.example.searchengine.repositories.LemmaRepository;
import org.example.searchengine.repositories.PageRepository;
import org.example.searchengine.repositories.SiteRepository;
import org.example.searchengine.services.StatisticsService;
import org.springframework.stereotype.Service;
import org.example.searchengine.dto.statistics.DetailedStatisticsItem;
import org.example.searchengine.dto.statistics.StatisticsData;
import org.example.searchengine.dto.statistics.StatisticsResponse;
import org.example.searchengine.dto.statistics.TotalStatistics;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexingServiceImpl indexingService;

    @Override
    public StatisticsResponse getStatistics() {

        TotalStatistics total = new TotalStatistics();
        List<SiteEntity> sitesList = siteRepository.findAll();
        total.setSites(sitesList.size());
        total.setIndexing(indexingService.isIndexing());

        List<DetailedStatisticsItem> detailed = new ArrayList<>();

        for (SiteEntity siteEntity : sitesList) {
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(siteEntity.getName());
            String url = siteEntity.getUrl().replaceAll(".$", "");
            item.setUrl(url);
            int pages = pageRepository.countBySite(siteEntity);
            int lemmas = lemmaRepository.countBySite(siteEntity);
            item.setPages(pages);
            item.setLemmas(lemmas);
            item.setStatus(siteEntity.getStatus().name());
            item.setError(siteEntity.getLastError() != null ? siteEntity.getLastError() : "");
            item.setStatusTime(
                    siteEntity.getStatusTime()
                            .atZone(ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli()
            );
            total.setPages(total.getPages() + pages);
            total.setLemmas(total.getLemmas() + lemmas);
            detailed.add(item);
        }

        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }
}
