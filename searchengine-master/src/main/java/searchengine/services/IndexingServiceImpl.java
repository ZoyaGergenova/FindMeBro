package searchengine.services;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.config.UserAgent;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.exceptions.*;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.model.Status;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    private final SitesList sites;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final UserAgent userAgent;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final AtomicBoolean indexingIsRunning = new AtomicBoolean();
    private final AtomicBoolean stopIndexingIsRunning = new AtomicBoolean();
    private final AtomicBoolean pageIndexingIsRunning = new AtomicBoolean();

    private ForkJoinPool pool;

    @Override
    public IndexingResponse startIndexing() {
        try {
            if (indexingIsRunning.get() || stopIndexingIsRunning.get() || pageIndexingIsRunning.get()) {
                throw new IndexingIsAlreadyRunningException();
            } else {
                indexingIsRunning.set(true);
                CompletableFuture.runAsync(this::fullIndexing);
                return new IndexingResponse(true);
            }
        } catch(Exception e){
            e.printStackTrace();
            throw new OtherExceptions(e.getMessage());
        }
    }

    @Override
    public IndexingResponse stopIndexing() {
        try {
            if (!indexingIsRunning.get()) {
                throw new IndexingIsNotRunningException();
            } else {
                indexingIsRunning.set(false);
                stopIndexingIsRunning.set(true);
                CompletableFuture.runAsync(this::stopIndexingProcess);
                return new IndexingResponse(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new OtherExceptions(e.getMessage());
        }
    }

    private void setStatusFailedSiteEntity(String error) {
        List<SiteEntity> siteEntityList = siteRepository.findAll();
        for (SiteEntity siteEntity : siteEntityList) {
            if (siteEntity.getStatus().equals(Status.INDEXING)) {
                siteEntity.setLastError(error);
                siteEntity.setStatus(Status.FAILED);
                siteRepository.save(siteEntity);
            }
        }
    }

    private void setStatusIndexedSiteEntity() {
        List<SiteEntity> siteEntityList = siteRepository.findAll();
        for (SiteEntity siteEntity : siteEntityList) {
            if (siteEntity.getStatus().equals(Status.INDEXING)) {
                siteEntity.setStatus(Status.INDEXED);
                siteRepository.save(siteEntity);
            }
        }
    }

    @Override
    public IndexingResponse indexPage(String pageUrl) {
        try {
            if (pageUrl.lastIndexOf("/") != pageUrl.length() - 1) {
                pageUrl = pageUrl + "/";
            }
            List<Site> sitesList = sites.getSites();
            for (Site site : sitesList) {
                if (pageUrl.startsWith(site.getUrl())) {
                    String finalPageUrl = pageUrl;
                    CompletableFuture.runAsync(() -> indexPageProcess(finalPageUrl));
                    return new IndexingResponse(true);
                }
            }
            throw new InvalidPageAddressException();
        } catch (Exception e) {
            e.printStackTrace();
            throw new OtherExceptions(e.getMessage());
        }
    }

    private void fullIndexing() {
        try {
            pool = new ForkJoinPool();
            siteRepository.deleteAll();
            List<Site> sitesList = sites.getSites();
            for (Site site : sitesList) {
                String url = site.getUrl();
                siteRepository.deleteByUrl(url);
                SiteEntity siteEntity = new SiteEntity();
                siteEntity.setName(site.getName());
                siteEntity.setStatus(Status.INDEXING);
                siteEntity.setUrl(url);
                siteRepository.saveAndFlush(siteEntity);
                PageRecursiveAction pageRecursiveAction = new PageRecursiveAction(siteEntity,
                        url, siteRepository, pageRepository, userAgent, lemmaRepository, indexRepository);
                pool.submit(pageRecursiveAction);
            }
            pool.shutdown();
            while (!pool.isTerminated()) {
            }
            if (!indexingIsRunning.get()) {
                log.info("Индексация остановлена пользователем");
                setStatusFailedSiteEntity("Indexing has been stopped by the user");
            } else {
                log.info("Индексация завершена");
                setStatusIndexedSiteEntity();
                indexingIsRunning.set(false);
            }
        }
        catch (Exception e) {
        log.info("Индексация прервана, произошла ошибка");
        setStatusFailedSiteEntity(e.getMessage());
        indexingIsRunning.set(false);
        e.printStackTrace();
        }
    }

    private void stopIndexingProcess() {
        pool.shutdownNow();
        while (!pool.isTerminated()) {
        }
        stopIndexingIsRunning.set(false);
    }

    private void indexPageProcess(String pageUrl) {
        try {
            pageIndexingIsRunning.set(true);
            LemmaFinder lemmaFinder = new LemmaFinder();
            LemmaIndexing lemmaIndexing = new LemmaIndexing(lemmaRepository, indexRepository);
            URL url = new URL(pageUrl);
            String path = url.getPath();
            List<Site> sitesList = sites.getSites();
            SiteEntity siteEntity;
            for (Site site : sitesList) {
                if (pageUrl.startsWith(site.getUrl())) {
                    Optional<SiteEntity> optionalSite = siteRepository.findByUrl(site.getUrl());
                    if (optionalSite.isPresent()) {
                        siteEntity = optionalSite.get();
                        siteEntity.setStatus(Status.INDEXING);
                        siteEntity.setLastError("");
                        siteRepository.save(siteEntity);
                    } else {
                        siteEntity = new SiteEntity();
                        siteEntity.setName(site.getName());
                        siteEntity.setStatus(Status.INDEXING);
                        siteEntity.setUrl(site.getUrl());
                        siteRepository.saveAndFlush(siteEntity);
                    }
                    pageRepository.deleteByPath(path);
                    PageEntity page = new PageEntity();
                    page.setSite(siteEntity);
                    page.setPath(path);
                    Connection.Response response = Jsoup.connect(pageUrl)
                            .ignoreContentType(true).maxBodySize(0)
                            .userAgent(userAgent.getUserAgentApp())
                            .timeout(5000)
                            .referrer(userAgent.getReferrer())
                            .execute();
                    page.setCode(response.statusCode());
                    Document document = response.parse();
                    page.setContent(document.outerHtml());
                    pageRepository.saveAndFlush(page);
                    Map<String, Integer> lemmaMap = lemmaFinder.collectLemmas(page.getContent());
                    lemmaIndexing.indexingLemmas(lemmaMap, siteEntity, page);
                    siteEntity.setStatus(Status.INDEXED);
                    siteRepository.save(siteEntity);
                    pageIndexingIsRunning.set(false);
                }
            }
        } catch (Exception e) {
            List<Site> sitesList = sites.getSites();
            for (Site site : sitesList) {
                if (pageUrl.startsWith(site.getUrl())) {
                    Optional<SiteEntity> optionalSite = siteRepository.findByUrl(site.getUrl());
                    if (optionalSite.isPresent()) {
                        SiteEntity siteEntity = optionalSite.get();
                        siteEntity.setLastError(e.getMessage());
                        siteEntity.setStatus(Status.FAILED);
                        siteRepository.save(siteEntity);
                        pageIndexingIsRunning.set(false);
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
