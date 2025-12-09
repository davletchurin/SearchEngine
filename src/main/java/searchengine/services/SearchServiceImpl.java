package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
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

import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final PageRepository pageRepository;
    private LemmaFinder lemmaFinder = LemmaFinder.getInstance();
    private Float percent = 0.7F;
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
                LemmaEntity lemmaEntity = lemmaRepository.findBySiteIdAndLemma(siteEntity, lemma).orElse(null);
                int maxFrequency = pageRepository.countBySiteId(siteEntity);
                if (lemmaEntity != null && lemmaEntity.getFrequency() < (maxFrequency * percent)) {
                    lemmaEntities.add(lemmaEntity);
                }
            }
        }

        Set<PageEntity> pageEntities = new HashSet<>();
        for (LemmaEntity lemmaEntity : lemmaEntities) {
            pageEntities.addAll(getPageEntities(lemmaEntity));
        }

        if (pageEntities.size() == 0) {
            return getEmptySearchResponse();
        }

        Map<PageEntity, Float> pagesToRelevance = new HashMap<>();
        for (PageEntity pageEntity : pageEntities) {
            float absRelevance = 0F;
            for (IndexEntity indexEntity : indexRepository.findAllByPageId(pageEntity)) {
                absRelevance = absRelevance + indexEntity.getRank();
            }
            pagesToRelevance.put(pageEntity, absRelevance);
        }

        float maxAbsRelevanceValue = pagesToRelevance.values().stream().max(Comparator.naturalOrder()).get();

        pagesToRelevance.replaceAll((e, v) -> pagesToRelevance.get(e) / maxAbsRelevanceValue);

        List<DetailedDataItem> detailedDataItems = new ArrayList<>();
        for (PageEntity pageEntity : pagesToRelevance.keySet()) {
            DetailedDataItem item = createDataItem(pageEntity, pagesToRelevance.get(pageEntity), queryLemmas);
            detailedDataItems.add(item);
        }

        detailedDataItems = detailedDataItems.stream()
                .sorted(Comparator.comparing(DetailedDataItem::getRelevance).reversed())
                .toList();

        return getFullSearchResponse(detailedDataItems);
    }

    public String createSnippet(String text, Set<String> lemmas) {
        if (text == null || text.isEmpty() || lemmas == null || lemmas.isEmpty()) {
            return text != null && text.length() > 200 ? text.substring(0, 200) + "..." : text;
        }

        String lowerText = text.toLowerCase();

        for (String lemma : lemmas) {
            if (lemma == null || lemma.isEmpty()) continue;

            int index = lowerText.indexOf(lemma.toLowerCase());
            if (index != -1) {

                int start = Math.max(0, index - 50);
                int end = Math.min(text.length(), index + lemma.length() + 100);

                String snippet = text.substring(start, end);

                String safeLemma = Pattern.quote(lemma);
                String highlighted = snippet.replaceAll("(?i)(" + safeLemma + ")", "<b>$1</b>");

                return (start > 0 ? "..." : "") + highlighted + (end < text.length() ? "..." : "");
            }
        }

        return text.length() > 200 ? text.substring(0, 200) + "..." : text;
    }

    private DetailedDataItem createDataItem(PageEntity pageEntity, Float relevance, Set<String> lemmas) {
        DetailedDataItem item = new DetailedDataItem();
        item.setSite(pageEntity.getSiteId().getUrl().replaceAll(".$",""));
        item.setSiteName(pageEntity.getSiteId().getName());
        item.setUri(pageEntity.getPath());

        Document document = Jsoup.parse(pageEntity.getContent());
        String title = document.title();
        item.setTitle(title);

        item.setSnippet(createSnippet(document.outerHtml(), lemmas));
        item.setRelevance(relevance);
        return item;
    }

    private Set<PageEntity> getPageEntities(LemmaEntity lemmaEntity) {
        List<IndexEntity> indexEntities = indexRepository.findAllByLemmaId(lemmaEntity);
        Set<PageEntity> pageEntities = new HashSet<>();
        for (IndexEntity indexEntity : indexEntities) {
            pageEntities.add(indexEntity.getPageId());
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
