package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.RequestSettings;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.ErrorResponse;
import searchengine.dto.IndexingResponse;
import searchengine.dto.indexing.IndexPageRequestDto;
import searchengine.dto.indexing.IndexingResponseDto;
import searchengine.model.SiteEntity;
import searchengine.model.Status;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.util.SiteIndexer;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class IndexingService {
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final RequestSettings jsoupRequestSettings;
    private final SitesList list;
    private ForkJoinPool pool = new ForkJoinPool();
    private List<SiteIndexer> siteIndexerList = new ArrayList<>();

    public IndexingResponse startIndexing() {

        if (pool.isTerminated() && pool.isQuiescent()) {
            pool = new ForkJoinPool();
            siteIndexerList.clear();
        }

        if (!pool.isQuiescent()) {
            return new ErrorResponse("Индексация уже запущена");
        }

        for (Site site : list.getSites()) {
            SiteEntity siteEntity = toSiteEntity(site);
            siteRepository.save(siteEntity);
            SiteIndexer siteIndexer = createSiteIndexer(siteEntity);
            siteIndexer.startIndexing();
            siteIndexerList.add(siteIndexer);
        }
        return new IndexingResponseDto();
    }

    public IndexingResponse stopIndexing() {
        if (pool.isShutdown()) {
            return new ErrorResponse("Индексация не запущена");
        }

        // Если зависла кнопка на stop indexing, а пул закончил свою работу - pool.isShutdown() не поможет нужно придумать другое решение

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

    public IndexingResponse indexPage(IndexPageRequestDto requestDto) {
        String url = requestDto.getUrl();

        return new IndexingResponseDto();
    }

    public void deleteSiteWithPages(SiteEntity entity) {
        long id = entity.getId();
        pageRepository.deleteBySiteId(id);
        siteRepository.deleteById(id);
    }

    private SiteEntity toSiteEntity(Site site) {
        Optional<SiteEntity> entityOptional = siteRepository.findByUrl(site.getUrl());
        if (entityOptional.isPresent()) {
            SiteEntity entity = entityOptional.get();
            entity.setStatus(Status.INDEXING);
            entity.setStatusTime(LocalDateTime.now());
            return entity;
        }
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
        return siteIndexer;
    }

    public void printPoolStats() {
        System.out.println('\n' + "=== Состояние пула ===");
        System.out.println("Размер пула: " + pool.getPoolSize());
        System.out.println("Активные потоки: " + pool.getActiveThreadCount());
        System.out.println("Работающие потоки: " + pool.getRunningThreadCount());
        System.out.println("Задачи в очередях: " + pool.getQueuedTaskCount());
        System.out.println("Внешние задачи в очередях: " + pool.getQueuedSubmissionCount());
        System.out.println("Количество краж: " + pool.getStealCount());
        System.out.println("Пул простаивает(проверка на состояние покоя): " + pool.isQuiescent());
        System.out.println("Параллелизм: " + pool.getParallelism());
        System.out.println("Уровень параллелизма: " + pool.getParallelism());
        System.out.println("=====================");
        System.out.println("Пул простаивает(в состояние покоя): " + pool.isQuiescent());
        System.out.println("Пул в работе: " + !pool.isQuiescent() + " - задачи + внешние задачи в очередях: " + pool.getQueuedTaskCount() + " + " + pool.getQueuedSubmissionCount());
        System.out.println("Вызыван метод shutdown(), началось завершение: " + pool.isShutdown());
        System.out.println("Пул в состоянии завершения: " + pool.isTerminating());
        System.out.println("Пул завершил работу: " + pool.isTerminated() + '\n');
    }
}
