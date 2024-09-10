package searchengine.services;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.config.UserAgent;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;

@Log4j2
public class PageRecursiveAction extends RecursiveAction {
    private SiteEntity siteEntity;
    private String url;
    private SiteRepository siteRepository;
    private PageRepository pageRepository;
    private UserAgent userAgent;
    private LemmaRepository lemmaRepository;
    private IndexRepository indexRepository;

    public PageRecursiveAction(SiteEntity siteEntity, String url,
                               SiteRepository siteRepository, PageRepository pageRepository,
                               UserAgent userAgent, LemmaRepository lemmaRepository, IndexRepository indexRepository) {
        this.siteEntity = siteEntity;
        this.url = url;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.userAgent = userAgent;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
    }

    @SneakyThrows
    @Override
    protected void compute() {
        LemmaFinder lemmaFinder = new LemmaFinder();
        LemmaIndexing lemmaIndexing = new LemmaIndexing(lemmaRepository, indexRepository);
        Thread.sleep(2000);
        Connection.Response response = Jsoup.connect(url)
                .ignoreContentType(true).maxBodySize(0)
                .userAgent(userAgent.getUserAgentApp())
                .timeout(2000)
                .referrer(userAgent.getReferrer())
                .execute();
        if (response.statusCode() == 200) {
            URL absUrl = new URL(url);
            PageEntity page = new PageEntity();
            page.setSite(siteEntity);
            page.setPath(absUrl.getPath());
            page.setCode(response.statusCode());
            Document document = response.parse();
            page.setContent(document.outerHtml());
            pageRepository.saveAndFlush(page);
            siteRepository.save(siteEntity);
            Map<String, Integer> lemmaMap = lemmaFinder.collectLemmas(page.getContent());
            lemmaIndexing.indexingLemmas(lemmaMap, siteEntity, page);
            List<PageRecursiveAction> actions = new ArrayList<>();
            Elements elements = document.select("a[href]");
            for (Element element : elements) {
                String attributeUrl = element.absUrl("href");
                URL absUrl1 = new URL(attributeUrl);
                if (attributeUrl.startsWith(url)
                        && !attributeUrl.contains("#")
                        && !pageRepository.existsByPath(absUrl1.getPath())) {
                    PageRecursiveAction pageRecursiveAction = new PageRecursiveAction(
                            siteEntity, attributeUrl, siteRepository, pageRepository, userAgent, lemmaRepository, indexRepository);
                    actions.add(pageRecursiveAction);
                }
            }
            ForkJoinTask.invokeAll(actions);
        }
    }
}