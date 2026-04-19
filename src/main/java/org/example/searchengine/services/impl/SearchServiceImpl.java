package org.example.searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import org.example.searchengine.model.SiteEntity;
import org.example.searchengine.repositories.IndexRepository;
import org.example.searchengine.repositories.LemmaRepository;
import org.example.searchengine.repositories.PageRepository;
import org.example.searchengine.repositories.SiteRepository;
import org.example.searchengine.services.SearchService;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import org.example.searchengine.dto.ErrorResponse;
import org.example.searchengine.dto.search.DetailedDataItem;
import org.example.searchengine.dto.search.SearchResponse;
import org.example.searchengine.dto.search.SearchResponseDto;
import org.example.searchengine.model.IndexEntity;
import org.example.searchengine.model.LemmaEntity;
import org.example.searchengine.model.PageEntity;
import org.example.searchengine.util.LemmaFinder;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final PageRepository pageRepository;
    private LemmaFinder lemmaFinder = LemmaFinder.getInstance();
    private Float percent = 1F;
    @Override
    public SearchResponse search(String query, String site, int offset, int limit) {
        if (query.isBlank()) {
            return new ErrorResponse("Задан пустой поисковый запрос");
        }

        List<SiteEntity> siteEntities = site.isEmpty() ? siteRepository.findAll() :
                List.of(siteRepository.findByUrl(site).get());

        Set<String> queryLemmas = lemmaFinder.getLemmaSet(query);

        TreeSet<LemmaEntity> lemmaEntities = new TreeSet<>(Comparator.comparing(LemmaEntity::getFrequency));
        for (String lemma : queryLemmas) {
            for (SiteEntity siteEntity : siteEntities) {
                LemmaEntity lemmaEntity = lemmaRepository.findBySiteAndLemma(siteEntity, lemma).orElse(null);
                int maxFrequency = pageRepository.countBySite(siteEntity);
                if (lemmaEntity != null && lemmaEntity.getFrequency() < (maxFrequency * percent)) {
                    lemmaEntities.add(lemmaEntity);
                }
            }
        }

        Set<PageEntity> pageEntities = new HashSet<>();
        for (LemmaEntity lemmaEntity : lemmaEntities) {
            pageEntities.addAll(getPageEntities(lemmaEntity));
        }

        if (pageEntities.isEmpty()) {
            return getEmptySearchResponse();
        }

        List<IndexEntity> allQueryIndexes = indexRepository.findAllByPageInAndLemmaIn(pageEntities, lemmaEntities);

        Map<PageEntity, List<IndexEntity>> indexesByPage = allQueryIndexes.stream()
                .collect(Collectors.groupingBy(IndexEntity::getPage));

        Map<PageEntity, Float> pageAbsRelevance = new HashMap<>();
        float maxAbsRelevance = 0.0f;

        for (Map.Entry<PageEntity, List<IndexEntity>> entry : indexesByPage.entrySet()) {
            PageEntity pageEntity = entry.getKey();
            List<IndexEntity> queryIndexes = entry.getValue();

            float absRelevance = 0.0f;
            for (IndexEntity indexEntity : queryIndexes) {
                absRelevance += indexEntity.getRank();
            }

            pageAbsRelevance.put(pageEntity, absRelevance);

            if (absRelevance > maxAbsRelevance) {
                maxAbsRelevance = absRelevance;
            }
        }

        List<DetailedDataItem> detailedDataItems = new ArrayList<>();
        for (Map.Entry<PageEntity, Float> entry : pageAbsRelevance.entrySet()) {
            PageEntity pageEntity = entry.getKey();
            float absRev = entry.getValue();

            float relativeRelevance = absRev / maxAbsRelevance;

            DetailedDataItem item = createDataItem(pageEntity, relativeRelevance, query);
            detailedDataItems.add(item);
        }

        detailedDataItems.sort(Comparator.comparing(DetailedDataItem::getRelevance)
                .reversed());

        return getFullSearchResponse(detailedDataItems);
    }

    private String createSnippet(String content, String query) {
        String clearText = Jsoup.parse(content).text();
        Set<String> queryLemmas = lemmaFinder.getLemmaSet(query);

        String[] words = clearText.split("\\s+");
        List<Integer> hitIndices = new ArrayList<>();

        for (int i = 0; i < words.length; i++) {
            String word = words[i].replaceAll("[^а-яА-ЯeEa-zA-Z]", "");
            if (word.isEmpty()) continue;

            Set<String> wordLemmas = lemmaFinder.getLemmaSet(word);
            if (!Collections.disjoint(wordLemmas, queryLemmas)) {
                hitIndices.add(i);
            }
        }

        if (hitIndices.isEmpty()) {
            return clearText.substring(0, Math.min(clearText.length(), 200)) + "...";
        }

        int firstHit = hitIndices.get(0);
        int start = Math.max(0, firstHit - 15);
        int end = Math.min(words.length, firstHit + 15);

        StringBuilder snippet = new StringBuilder();
        if (start > 0) snippet.append("... ");

        for (int i = start; i < end; i++) {
            String currentWord = words[i];
            String strippedWord = currentWord.replaceAll("[^а-яА-ЯeEa-zA-Z]", "");

            Set<String> currentLemmas = lemmaFinder.getLemmaSet(strippedWord);
            if (!Collections.disjoint(currentLemmas, queryLemmas)) {
                snippet.append("<b>").append(currentWord).append("</b>");
            } else {
                snippet.append(currentWord);
            }
            snippet.append(" ");
        }

        if (end < words.length) snippet.append("...");

        return snippet.toString().trim();
    }

    private DetailedDataItem createDataItem(PageEntity pageEntity, Float relevance, String query) {
        DetailedDataItem item = new DetailedDataItem();
        item.setSite(pageEntity.getSite().getUrl().replaceAll(".$",""));
        item.setSiteName(pageEntity.getSite().getName());
        item.setUri(pageEntity.getPath());

        Document document = Jsoup.parse(pageEntity.getContent());
        String title = document.title();
        item.setTitle(title);

        item.setSnippet(createSnippet(document.text(), query));
        item.setRelevance(relevance);
        return item;
    }

    private Set<PageEntity> getPageEntities(LemmaEntity lemmaEntity) {
        List<IndexEntity> indexEntities = indexRepository.findAllByLemma(lemmaEntity);
        Set<PageEntity> pageEntities = new HashSet<>();
        for (IndexEntity indexEntity : indexEntities) {
            pageEntities.add(indexEntity.getPage());
        }
        return pageEntities;
    }

    private SearchResponse getFullSearchResponse(List<DetailedDataItem> data) {
        SearchResponseDto searchResponseDto = new SearchResponseDto();
        searchResponseDto.setResult(true);
        searchResponseDto.setCount(data.size());
        searchResponseDto.setData(data);
        return searchResponseDto;
    }

    private SearchResponse getEmptySearchResponse() {
        SearchResponseDto searchResponseDto = new SearchResponseDto();
        searchResponseDto.setResult(true);
        searchResponseDto.setCount(0);
        searchResponseDto.setData(new ArrayList<>());
        return searchResponseDto;
    }
}
