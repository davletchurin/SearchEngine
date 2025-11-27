package searchengine.util;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import searchengine.config.RequestSettings;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.model.Status;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
@Setter
@Component
public class SiteIndexer {
    private ForkJoinPool pool;
    private SiteEntity siteEntity;
    private SiteRepository siteRepository;
    private PageRepository pageRepository;
    private LemmaRepository lemmaRepository;
    private IndexRepository indexRepository;
    private RequestSettings jsoupRequestSettings;
    private List<ForkJoinTask<Boolean>> siteIndexerRecursiveTasks = new ArrayList<>();
    public void startIndexing() {
        System.out.println(
                "Старт индексации сайта: "
                        + siteEntity.getName()
                        + ". С адресом: "
                        + siteEntity.getUrl()
        );
        IndexerExecutor executor = createExecutor();
        ForkJoinTask<Boolean> task = pool.submit(executor);
        siteIndexerRecursiveTasks.add(task);
    }

    public void stopIndexing() {
        System.out.println(
            "Начало остановки индексации сайта: "
                    + siteEntity.getName()
                    + ". С адресом: "
                    + siteEntity.getUrl()
        );
        if (siteEntity.getStatus() == Status.INDEXED) {
            return;
        }
        for (ForkJoinTask<Boolean> task : siteIndexerRecursiveTasks) {
            if(!task.isDone()) {
                task.cancel(true);
            }
        }
        siteEntity.setStatus(Status.FAILED);
        siteEntity.setLastError("Индексация остановлена пользователем");
        siteRepository.save(siteEntity);
    }

    public void indexPath(String absUrl) {
        IndexerExecutor executor = createExecutor();
        executor.setRelUrl(getRelUrl(absUrl));
        executor.setIndexPath(true);
        pool.submit(executor);
    }

    public IndexerExecutor createExecutor() {
        IndexerExecutor executor = new IndexerExecutor();
        Set<String> uniqueUrls = new HashSet<>();
        executor.setSiteRepository(siteRepository);
        executor.setPageRepository(pageRepository);
        executor.setJsoupRequestSettings(jsoupRequestSettings);
        executor.setSiteEntity(siteEntity);
        executor.setAbsUrl(siteEntity.getUrl());
        executor.setRelUrl("/");
        executor.setUniqueUrls(uniqueUrls);
        executor.setForkJoinTasks(siteIndexerRecursiveTasks);
        executor.setIndexPath(false);
        executor.setLemmaRepository(lemmaRepository);
        executor.setIndexRepository(indexRepository);
        try {
            executor.setLemmaFinder(LemmaFinder.getInstance());
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    public Set<String> getPaths() {
        List<PageEntity> pageEntities = pageRepository.findAllBySiteId(siteEntity);
        if (pageEntities == null) {
            return new HashSet<>();
        }
        Set<String> paths = new HashSet<>();
        for (PageEntity pageEntity : pageEntities) {
            paths.add(pageEntity.getPath());
        }
        return paths;
    }
}