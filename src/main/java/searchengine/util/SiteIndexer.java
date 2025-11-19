package searchengine.util;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import searchengine.config.RequestSettings;
import searchengine.model.SiteEntity;
import searchengine.model.Status;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

@Getter
@Setter
@Component
public class SiteIndexer {
    private ForkJoinPool pool;
    private SiteEntity siteEntity;
    private SiteRepository siteRepository;
    private PageRepository pageRepository;
    private RequestSettings jsoupRequestSettings;
    private List<ForkJoinTask<Boolean>> siteIndexerRecursiveTasks = new ArrayList<>();
    private Boolean start = true;
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
        start = false;
        System.out.println(
            "Начало остановки индексации сайта: "
                    + siteEntity.getName()
                    + ". С адресом: "
                    + siteEntity.getUrl()
        );
        siteEntity.setStatus(Status.FAILED);
        siteEntity.setLastError("Индексация остановлена пользователем");
        siteRepository.save(siteEntity);
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
        return executor;
    }

//    public Set<String> getPaths() {
//        List<String> path = pageRepository.findPathsBySiteId(siteEntity);
//        if (path == null) {
//            return new HashSet<>();
//        }
//        return new HashSet<>(path);
//    }
}