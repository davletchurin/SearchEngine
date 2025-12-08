package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.SiteEntity;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;

    @Override
    public StatisticsResponse getStatistics() {

        TotalStatistics total = new TotalStatistics();
        List<SiteEntity> sitesList = siteRepository.findAll();
        total.setSites(sitesList.size());
        total.setIndexing(total.getSites() != 0);

        List<DetailedStatisticsItem> detailed = new ArrayList<>();

        for (SiteEntity siteEntity : sitesList) {
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(siteEntity.getName());
            String url = siteEntity.getUrl().replaceAll(".$", "");
            item.setUrl(url);
            int pages = pageRepository.countBySiteId(siteEntity);
            int lemmas = lemmaRepository.countBySiteId(siteEntity);
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
