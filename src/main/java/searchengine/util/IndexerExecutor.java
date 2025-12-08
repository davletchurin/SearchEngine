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
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private LemmaFinder lemmaFinder;
    private LemmaRepository lemmaRepository;
    private IndexRepository indexRepository;

    @Override
    protected Boolean compute() {
        if (isCancelled()) {
            return null;
        }

        if (uniqueUrls.contains(relUrl)) {
            return false;
        }
        uniqueUrls.add(relUrl);

        if (indexPath && pageRepository.existsBySiteIdAndPath(siteEntity, relUrl)) {
            PageEntity pageEntity = pageRepository.findBySiteIdAndPath(siteEntity, relUrl).get();
            List<IndexEntity> indexEntities = indexRepository.findAllByPageId(pageEntity);

            List<LemmaEntity> lemmaEntities = new ArrayList<>();
            for (IndexEntity indexEntity : indexEntities) {
                LemmaEntity lemmaEntity = indexEntity.getLemmaId();
                indexRepository.delete(indexEntity);
                lemmaEntities.add(lemmaEntity);
            }
            lemmaRepository.deleteAll(lemmaEntities);
            pageRepository.delete(pageEntity);
        }

        Connection.Response response = getResponse();
        if (response == null) {
            return false;
        }
        Document document = getDocument(response);
        if (document == null) {
            return false;
        }

        PageEntity pageEntity = createPageEntity(response, document);
        pageEntity = pageRepository.save(pageEntity);
        siteEntity.setStatusTime(LocalDateTime.now());
        siteRepository.save(siteEntity);

        createLemmasAndIndexes(pageEntity, document);

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

        if (relUrl.equals("/")) {
            siteEntity.setStatus(Status.INDEXED);
            siteRepository.save(siteEntity);
        }

        return true;
    }

    private void createLemmasAndIndexes(PageEntity pageEntity, Document document) {
        String htmlText = lemmaFinder.removeHtmlTags(document);
        HashMap<String, Integer> lemmas = lemmaFinder.getLemmaMap(htmlText);

        List<IndexEntity> indexEntities = new ArrayList<>();
        for (String lemma : lemmas.keySet()) {
            LemmaEntity lemmaEntity = lemmaRepository.findBySiteIdAndLemma(siteEntity, lemma).orElse(null);
            if (lemmaEntity != null) {
                lemmaEntity.setFrequency(lemmaEntity.getFrequency() + 1);
            } else {
                lemmaEntity = createLemma(lemma);
            }
            lemmaEntity = lemmaRepository.save(lemmaEntity);

            float rank = lemmas.get(lemma);
            IndexEntity indexEntity = createIndex(pageEntity,lemmaEntity,rank);
            indexEntities.add(indexEntity);
        }

        indexRepository.saveAll(indexEntities);
    }

    private IndexEntity createIndex(PageEntity pageEntity, LemmaEntity lemmaEntity, Float rank) {
        IndexEntity indexEntity = new IndexEntity();
        indexEntity.setPageId(pageEntity);
        indexEntity.setLemmaId(lemmaEntity);
        indexEntity.setRank(rank);
        return indexEntity;
    }

    private LemmaEntity createLemma(String lemma) {
        LemmaEntity lemmaEntity = new LemmaEntity();
        lemmaEntity.setLemma(lemma);
        lemmaEntity.setSiteId(siteEntity);
        lemmaEntity.setFrequency(1);
        return lemmaEntity;
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
            String abs = element.attr("abs:href");
            String rel = getRelUrlFromAbs(abs);

            if (!abs.matches(siteEntity.getUrl() + ".*")) {
                continue;
            }

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

    public String getRelUrlFromAbs(String absUrl) {
        Pattern pattern = java.util.regex.Pattern.compile("https?://[^/]+(/[^?#]*)");
        Matcher matcher = pattern.matcher(absUrl);

        if (matcher.find()) {
            String path = matcher.group(1);
            return path.isEmpty() ? "/" : path;
        }
        return "/";
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
        executor.setForkJoinTasks(forkJoinTasks);
        executor.setIndexPath(false);
        executor.setLemmaRepository(lemmaRepository);
        executor.setIndexRepository(indexRepository);
        executor.setLemmaFinder(lemmaFinder);
        return executor;
    }
}
