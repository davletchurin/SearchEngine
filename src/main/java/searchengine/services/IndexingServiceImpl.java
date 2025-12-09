package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.RequestSettings;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.ErrorResponse;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.indexing.IndexingResponseDto;
import searchengine.model.SiteEntity;
import searchengine.model.Status;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.util.SiteIndexer;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final RequestSettings jsoupRequestSettings;
    private final SitesList sitesList;
    private ForkJoinPool pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
    private List<SiteIndexer> siteIndexerList = new ArrayList<>();

    @Override
    public IndexingResponse startIndexing() {

        if (pool.isTerminated() && pool.isQuiescent()) {
            pool = new ForkJoinPool();
            siteIndexerList.clear();
        }

        if (!pool.isQuiescent()) {
            return new ErrorResponse("Индексация уже запущена");
        }

        for (Site site : sitesList.getSites()) {
            SiteEntity siteEntity = createSiteEntity(site);
            siteRepository.save(siteEntity);
            SiteIndexer siteIndexer = createSiteIndexer(siteEntity);
            siteIndexerList.add(siteIndexer);
            siteIndexer.startIndexing();
        }
        return new IndexingResponseDto();
    }

    @Override
    public IndexingResponse stopIndexing() {
        if (pool.isShutdown()) {
            return new ErrorResponse("Индексация не запущена");
        }

        boolean quiet;
        pool.shutdown();
        try {
            quiet = pool.awaitTermination(5, TimeUnit.SECONDS);
            if (quiet) return new IndexingResponseDto();
        } catch (InterruptedException e) {
            pool.shutdownNow();
        }

        for (SiteIndexer siteIndexer : siteIndexerList) {
            siteIndexer.stopIndexing();
        }
        return new IndexingResponseDto();
    }

    @Override
    public IndexingResponse indexPage(String url) {
        if (url.isEmpty()) {
            return new ErrorResponse("Задан пустой запрос");
        }

        HashMap<String, SiteEntity> urls = getUrlsForIndexPath(url);

        if (urls == null) {
            return new ErrorResponse("Данная страница находится за пределами сайтов, " +
                    "указанных в конфигурационном файле");
        }

        for (String pageUrl : urls.keySet()) {
            SiteEntity siteEntity = urls.get(pageUrl);
            SiteIndexer siteIndexer = createSiteIndexer(siteEntity);
            siteIndexer.indexPath(pageUrl);
        }
        return new IndexingResponseDto();
    }

    private HashMap<String, SiteEntity> getUrlsForIndexPath(String url) {
        HashMap<String, SiteEntity> urls = null;

        if (url.matches("^https?://.*/?(.*)?")) {
            urls = getFormattedUrlsByAbsoluteUrl(url);
        }

        if (url.matches("^/[a-z0-9]*/?(.*)?")) {
            urls = getFormattedUrlsByRelativeUrl(url);
        }

        return urls;
    }

    private HashMap<String, SiteEntity> getFormattedUrlsByAbsoluteUrl(String url) {
        HashMap<String, SiteEntity> urls = null;

        String rootUrl = getRootUrl(url);
        for (Site site : sitesList.getSites()) {
            if (site.getUrl().equals(rootUrl)) {
                SiteEntity siteEntity = createSiteEntity(site);
                if (siteEntity.getId() == null) siteRepository.save(siteEntity);
                urls = new HashMap<>();
                urls.put(url, siteEntity);
                break;
            }
        }

        return urls;
    }

    private String getRootUrl(String url) {
        Pattern pattern = Pattern.compile("https?://[^/]+/?");
        Matcher matcher = pattern.matcher(url);
        String rootUrl = "";
        while (matcher.find()) {
            rootUrl = matcher.group();
            if (!rootUrl.matches(".*/$")) {
                rootUrl = rootUrl + "/";
            }
        }
        return rootUrl;
    }

    private HashMap<String, SiteEntity> getFormattedUrlsByRelativeUrl(String url) {
        HashMap<String, SiteEntity> urls = new HashMap<>();

        for (Site site : sitesList.getSites()) {
            SiteEntity siteEntity = createSiteEntity(site);
            if (siteEntity.getId() == null) siteRepository.save(siteEntity);
            String formattedUrl = siteEntity.getUrl() + url.replaceFirst("/", "");
            urls.put(formattedUrl, siteEntity);
        }

        return urls;
    }

    public void deleteSiteWithPages(SiteEntity entity) {
        long id = entity.getId();
        pageRepository.deleteAllBySiteId(entity);
        siteRepository.deleteById(id);
    }

    private SiteEntity createSiteEntity(Site site) {
        Optional<SiteEntity> entityOptional = siteRepository.findByUrl(site.getUrl());
        if (entityOptional.isPresent()) return entityOptional.get();
        SiteEntity entity = new SiteEntity();
        entity.setName(site.getName());
        entity.setUrl(site.getUrl());
        entity.setStatus(Status.INDEXING);
        entity.setStatusTime(LocalDateTime.now());
        return entity;
    }

    private SiteIndexer createSiteIndexer(SiteEntity siteEntity) {
        SiteIndexer siteIndexer = new SiteIndexer();
        siteIndexer.setPool(pool);
        siteIndexer.setSiteRepository(siteRepository);
        siteIndexer.setPageRepository(pageRepository);
        siteIndexer.setJsoupRequestSettings(jsoupRequestSettings);
        siteIndexer.setSiteEntity(siteEntity);
        siteIndexer.setLemmaRepository(lemmaRepository);
        siteIndexer.setIndexRepository(indexRepository);
        return siteIndexer;
    }
}
