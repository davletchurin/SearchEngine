package searchengine.util;

import lombok.Getter;
import lombok.Setter;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import searchengine.config.RequestSettings;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;

@Getter
@Setter
@Component
public class IndexerExecutor extends RecursiveTask<Boolean> {
    private SiteRepository siteRepository;
    private PageRepository pageRepository;
    private RequestSettings jsoupRequestSettings;
    private SiteEntity siteEntity;
    private String absUrl;
    private String relUrl;
    private Set<String> uniqueUrls;
    private List<ForkJoinTask<Boolean>> forkJoinTasks;
    private Boolean indexPath;

    @Override
    protected Boolean compute() {
        if (uniqueUrls.contains(relUrl)) {
            return false;
        }
        uniqueUrls.add(relUrl);

        Connection.Response response = getResponse();
        if (response == null) {
            return false;
        }
        Document document = getDocument(response);
        if (document == null) {
            return false;
        }

        PageEntity pageEntity = createPageEntity(response, document);
        pageRepository.save(pageEntity);
        siteEntity.setStatusTime(LocalDateTime.now());
        siteRepository.save(siteEntity);

        if (indexPath) {
            return true;
        }

        Map<String, String> children = getLinks(document);

        List<IndexerExecutor> executors = new ArrayList<>();
        for (String child : children.keySet()) {
            IndexerExecutor task = createExecutor(child, children);
            ForkJoinTask<Boolean> forkJoinTask = task.fork();
            forkJoinTasks.add(forkJoinTask);
            executors.add(task);
        }

        for (IndexerExecutor executor : executors) {
            executor.join();
        }

//        if (relUrl.equals("/")) {
//            System.out.println(siteEntity.getName() + " is indexed");
//            siteEntity.setStatus(Status.INDEXED);
//            siteRepository.save(siteEntity);
//        }

        return true;
    }

    private Connection.Response getResponse() {
        try {
            Thread.sleep(2000);
            return Jsoup.connect(absUrl)
                    .userAgent(jsoupRequestSettings.getAgent())
                    .referrer(jsoupRequestSettings.getReferrer())
                    .execute();
        } catch (HttpStatusException e) {
            System.out.println("Не удалость получить доступ к сайту: " + e.getUrl());
            System.out.println("Статус код: " + e.getStatusCode());
            return null;
        } catch (Exception e) { // TODO: надо ловить 404?
            e.printStackTrace();
            return null;
        }
    }

    private Document getDocument(Connection.Response response) {
        try {
            return response.parse();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Map<String, String> getLinks(Document document) {
        Map<String, String> links = new HashMap<>();
        Elements elements = document.select("a");

        for (Element element : elements) {
            String rel = element.attr("href");
            String abs = element.attr("abs:href");

            if (!rel.matches("^/.*")) {
                continue;
            }

            if (rel.matches(".*\\..*")) {
                if (!rel.matches(".*\\.html")) {
                    continue;
                }
            }

            if (rel.matches(".*#.*")) {
                continue;
            }

            links.put(abs, rel);
        }
        return links;
    }

    private PageEntity createPageEntity(Connection.Response response, Document document) {
        PageEntity entity = new PageEntity();
        entity.setSiteId(siteEntity);
        entity.setPath(relUrl);
        entity.setCode(response.statusCode());
        entity.setContent(document.outerHtml());
        return entity;
    }

    public IndexerExecutor createExecutor(String child, Map<String, String> children) {
        IndexerExecutor executor = new IndexerExecutor();
        executor.setSiteRepository(siteRepository);
        executor.setPageRepository(pageRepository);
        executor.setJsoupRequestSettings(jsoupRequestSettings);
        executor.setSiteEntity(siteEntity);
        executor.setAbsUrl(child);
        executor.setRelUrl(children.get(child));
        executor.setUniqueUrls(uniqueUrls);
        return executor;
    }
}
