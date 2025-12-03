package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import searchengine.dto.ErrorResponse;
import searchengine.dto.search.DetailedDataItem;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.search.SearchResponseDto;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.util.LemmaFinder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SearchService {
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final PageRepository pageRepository;
    private LemmaFinder lemmaFinder = LemmaFinder.getInstance();
    public SearchResponse search(String query, String site, Integer offset, Integer limit) {
        if (query.isEmpty()) {
            return new ErrorResponse("Задан пустой поисковый запрос");
        }

        if (!site.isEmpty() && !siteRepository.existsByUrl(site)) {
            return new ErrorResponse("Указанная страница не найдена");
        }

        List<SiteEntity> siteEntities = new ArrayList<>();
        if (!site.isEmpty() && siteRepository.existsByUrl(site)) {
            siteEntities.add(siteRepository.findByUrl(site).get());
        } else {
            siteEntities = siteRepository.findAll();
        }

        Set<String> uniqueLemmas = lemmaFinder.getLemmaSet(query);
        for (String lemma : uniqueLemmas) {
            List<LemmaEntity> lemmaEntitiesOrderByFrequencyAsc =
                    lemmaRepository.findAll(Sort.by(Sort.Order.asc("frequency")));
            for (LemmaEntity lemmaEntity : lemmaEntitiesOrderByFrequencyAsc) {
                //Set<PageEntity> pageEntities = lemmaEntity.getPageEntities();

            }

        }

        SearchResponseDto searchResponse = new SearchResponseDto();
        searchResponse.setResult(true);
        searchResponse.setCount(0);
        List<DetailedDataItem> data = new ArrayList<>();

//        for () {
//            DetailedDataItem item = new DetailedDataItem();
//            item.setSite();
//            item.setSiteName();
//            item.setUri();
//            item.setTitle();
//            item.setSnippet();
//            item.setRelevance();
//
//            data.add(item);
//        }

        searchResponse.setData(data);
        return searchResponse;
    }
}
