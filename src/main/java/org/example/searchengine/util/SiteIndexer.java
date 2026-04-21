package org.example.searchengine.util;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.example.searchengine.config.RequestSettings;
import org.example.searchengine.model.Status;
import org.example.searchengine.services.impl.IndexingServiceImpl;
import org.springframework.stereotype.Component;
import org.example.searchengine.model.SiteEntity;
import org.example.searchengine.repositories.IndexRepository;
import org.example.searchengine.repositories.LemmaRepository;
import org.example.searchengine.repositories.PageRepository;
import org.example.searchengine.repositories.SiteRepository;

import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
@Setter
@Component
@Slf4j
public class SiteIndexer {
    private IndexingServiceImpl indexingService;
    private ForkJoinPool pool;
    private SiteEntity siteEntity;
    private SiteRepository siteRepository;
    private PageRepository pageRepository;
    private LemmaRepository lemmaRepository;
    private IndexRepository indexRepository;
    private RequestSettings jsoupRequestSettings;
    public void startIndexing() {
        log.info("Старт индексации сайта: {}. С адресом: {}", siteEntity.getName(), siteEntity.getUrl());
        indexingService.deleteAllBySiteEntity(siteEntity);
        siteEntity.setStatus(Status.INDEXING);
        siteRepository.save(siteEntity);
        IndexerExecutor executor = createExecutor();
        pool.submit(executor);
    }

    public void stopIndexing() {
        log.info("Начало остановки индексации сайта: {}. С адресом: {}", siteEntity.getName(), siteEntity.getUrl());
        if (siteEntity.getStatus() == Status.INDEXED) {
            return;
        }
        pool.shutdownNow();
        siteEntity.setStatus(Status.FAILED);
        siteEntity.setLastError("Индексация остановлена пользователем");
        siteRepository.save(siteEntity);
    }

    public void indexPath(String absUrl) {
        IndexerExecutor executor = createExecutor();
        executor.setRelUrl(getRelUrl(absUrl));
        executor.setIndexPath(true);
        pool.submit(executor);
        pool.shutdown();
    }

    public IndexerExecutor createExecutor() {
        IndexerExecutor executor = new IndexerExecutor();
        Set<String> uniqueUrls = Collections.synchronizedSet(new HashSet<>());
        executor.setSiteRepository(siteRepository);
        executor.setPageRepository(pageRepository);
        executor.setJsoupRequestSettings(jsoupRequestSettings);
        executor.setSiteEntity(siteEntity);
        executor.setAbsUrl(siteEntity.getUrl());
        executor.setRelUrl("/");
        executor.setUniqueUrls(uniqueUrls);
        executor.setIndexPath(false);
        executor.setLemmaRepository(lemmaRepository);
        executor.setIndexRepository(indexRepository);
        executor.setLemmaFinder(LemmaFinder.getInstance());
        return executor;
    }

    public String getRelUrl(String absUrl) {
        Pattern pattern = java.util.regex.Pattern.compile("https?://[^/]+(/[^?#]*)");
        Matcher matcher = pattern.matcher(absUrl);

        if (matcher.find()) {
            String path = matcher.group(1);
            return path.isEmpty() ? "/" : path;
        }
        return "/";
    }
}