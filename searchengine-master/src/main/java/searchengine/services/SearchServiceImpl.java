package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.search.DetailedSearchItem;
import searchengine.dto.search.SearchResponse;
import searchengine.exceptions.*;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService{
    private final SitesList sites;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    @Override
    public SearchResponse search(String query, String site, int offset, int limit) {
        try {
            if (query.isBlank()) {
                throw new EmptySearchQueryException();
            }
            if (!site.isBlank()) {
                return searchOneSite(query, site, offset, limit);
            } else {
                return searchAllSite(query, offset, limit);
            }
        } catch (Exception e) {
            log.info(e.getMessage());
            throw new OtherExceptions(e.getMessage());
        }
    }

    private LinkedHashMap<LemmaEntity, Integer> filteringFrequentLemmasOneSite(String query, SiteEntity siteEntity) throws IOException {
        float frequencyFactor = 0.8F;
        int pageCount = 0;
        Optional<Integer> optionalPageCount = pageRepository.getPageCountWhereSiteId(siteEntity.getId());
        if (optionalPageCount.isPresent()) {
            pageCount = optionalPageCount.get();
        }
        LemmaFinder lemmaFinder = new LemmaFinder();
        Map<String, Integer> lemmaMap = lemmaFinder.collectLemmas(query);
        Map<LemmaEntity, Integer> lemmaEntityMap = new HashMap<>();
        for (Map.Entry<String, Integer> lemma : lemmaMap.entrySet()) {
            Optional<LemmaEntity> optionalLemma = lemmaRepository.findByLemmaAndSite_id(lemma.getKey(), siteEntity.getId());
            if (optionalLemma.isPresent() && pageCount > 0) {
                LemmaEntity lemmaEntity = optionalLemma.get();
                if (pageCount < 10 || lemmaMap.size() < 3) {
                    lemmaEntityMap.put(lemmaEntity, lemmaEntity.getFrequency());
                } else {
                    if (((float) lemmaEntity.getFrequency() / pageCount) < frequencyFactor) {
                        lemmaEntityMap.put(lemmaEntity, lemmaEntity.getFrequency());
                    }
                }
            }
        }
        return lemmaEntityMap.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue())
                .collect(LinkedHashMap::new, (map, entry) ->
                        map.put(entry.getKey(), entry.getValue()), Map::putAll);
    }

    private LinkedHashMap<String, Integer> filteringFrequentLemmasAllSite(String query) throws IOException {
        float frequencyFactor = 0.8F;
        int pageCount = 0;
        Optional<Integer> optionalPageCount = pageRepository.getPageCountWhereSiteIndexed();
        if (optionalPageCount.isPresent()) {
            pageCount = optionalPageCount.get();
        }
        LemmaFinder lemmaFinder = new LemmaFinder();
        Map<String, Integer> lemmaMap = lemmaFinder.collectLemmas(query);
        Map<String, Integer> newLemmaMap = new HashMap<>();
        for (Map.Entry<String, Integer> lemma : lemmaMap.entrySet()) {
            Optional<Integer> optionalInteger = lemmaRepository.getFrequencyLemma(lemma.getKey());
            if (optionalInteger.isPresent() && pageCount > 0) {
                if (pageCount < 10 || lemmaMap.size() < 3) {
                    newLemmaMap.put(lemma.getKey(), optionalInteger.get());
                } else {
                    if (((float) optionalInteger.get() / pageCount) < frequencyFactor) {
                        newLemmaMap.put(lemma.getKey(), optionalInteger.get());
                    }
                }
            }
        }
        return newLemmaMap.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue())
                .collect(LinkedHashMap::new, (map, entry) ->
                        map.put(entry.getKey(), entry.getValue()), Map::putAll);
    }

    private List<PageEntity> recursiveSearchListPages(LemmaEntity lemmaEntity, List<PageEntity> pageEntityList) {
        List<PageEntity> newPageList = new ArrayList<>();
        for (PageEntity pageEntity : pageEntityList) {
            if (indexRepository.existsByLemma_idAndPage_id(lemmaEntity.getId(), pageEntity.getId())) {
                newPageList.add(pageEntity);
            }
        }
        return newPageList;
    }

    private List<PageEntity> recursiveSearchListPagesAllSite(String lemma, List<PageEntity> pageEntityList) {
        List<PageEntity> newPageList = new ArrayList<>();
        for (PageEntity pageEntity : pageEntityList) {
            Optional<IndexEntity> optionalIndexEntityList = indexRepository.findByPageLemma(lemma, pageEntity.getId());
            if (optionalIndexEntityList.isPresent()) {
                newPageList.add(pageEntity);
            }
        }
        return newPageList;
    }

    private LinkedHashMap<PageEntity, Float> getPagesMapByRelevance(LinkedList<LemmaEntity> lemmaEntityList,
                                                                    List<PageEntity> pageEntityList) {
        LinkedHashMap<PageEntity, Float> relevanceMap = new LinkedHashMap<>();
        for (PageEntity pageEntity : pageEntityList) {
            float absRelevance = 0F;
            for (LemmaEntity lemmaEntity : lemmaEntityList) {
                float rank = indexRepository.getRank(lemmaEntity.getId(), pageEntity.getId());
                absRelevance = absRelevance + rank;
            }
            relevanceMap.put(pageEntity, absRelevance);
        }
        relevanceMap.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(LinkedHashMap::new, (map, entry) ->
                        map.put(entry.getKey(), entry.getValue()), Map::putAll);
        List<PageEntity> pageEntityList1 = new ArrayList<>(relevanceMap.keySet());
        float maxRelevance = relevanceMap.get(pageEntityList1.get(0));
        for (Map.Entry<PageEntity, Float> entry : relevanceMap.entrySet()) {
            entry.setValue(entry.getValue() / maxRelevance);
        }
        return relevanceMap;
    }

    private LinkedHashMap<PageEntity, Float> getPagesMapByRelevanceAllSite(LinkedList<String> lemmaList,
                                                                    List<PageEntity> pageEntityList) {
        LinkedHashMap<PageEntity, Float> relevanceMap = new LinkedHashMap<>();
        for (PageEntity pageEntity : pageEntityList) {
            float absRelevance = 0F;
            for (String lemma : lemmaList) {
                float rank = indexRepository.getRankAllSite(lemma, pageEntity.getId());
                absRelevance = absRelevance + rank;
            }
            relevanceMap.put(pageEntity, absRelevance);
        }
        relevanceMap.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(LinkedHashMap::new, (map, entry) ->
                        map.put(entry.getKey(), entry.getValue()), Map::putAll);
        List<PageEntity> pageEntityList1 = new ArrayList<>(relevanceMap.keySet());
        float maxRelevance = relevanceMap.get(pageEntityList1.get(0));
        for (Map.Entry<PageEntity, Float> entry : relevanceMap.entrySet()) {
            entry.setValue(entry.getValue() / maxRelevance);
        }
        return relevanceMap;
    }

    private SearchResponse searchOneSite(String query, String site, int offset, int limit) throws IOException {
        SiteEntity siteEntity;
        List<Site> sitesList = sites.getSites();
        for (Site siteList : sitesList) {
            if (site.startsWith(siteList.getUrl())) {
                log.info("Сайт есть в конфигурации");
                Optional<SiteEntity> optionalSite = siteRepository.findByUrl(siteList.getUrl());
                if (optionalSite.isPresent()) {
                    siteEntity = optionalSite.get();
                    if (!siteEntity.getStatus().equals(Status.INDEXED)) {
                        throw new SiteNotIndexedException();
                    }
                    LinkedHashMap<LemmaEntity, Integer> lemmaMap = filteringFrequentLemmasOneSite(query, siteEntity);
                    if (!lemmaMap.isEmpty()) {
                        LinkedList<LemmaEntity> lemmaEntityLinkedList =
                                new LinkedList<>(lemmaMap.keySet());
                        LemmaEntity firstLemma = lemmaEntityLinkedList.getFirst();
                        List<PageEntity> pageEntityList;
                        Optional<List<PageEntity>> optionalPageEntities =
                                pageRepository.findByLemmaIdSiteId(firstLemma.getId(),
                                        siteEntity.getId());
                        if (optionalPageEntities.isPresent()) {
                            pageEntityList = optionalPageEntities.get();
                            for (LemmaEntity lemmaEntity : lemmaEntityLinkedList) {
                                pageEntityList = recursiveSearchListPages(lemmaEntity, pageEntityList);
                            }
                            if (!pageEntityList.isEmpty()) {
                                LinkedHashMap<PageEntity, Float> pageRankMap =
                                        getPagesMapByRelevance(lemmaEntityLinkedList, pageEntityList);
                                LinkedList<PageEntity> pageEntityLinkedList =
                                        new LinkedList<>(pageRankMap.keySet());
                                List<DetailedSearchItem> searchData = new ArrayList<>();
                                for (int i = offset; i < limit && i < pageEntityLinkedList.size(); i++) {
                                    PageEntity pageEntity = pageEntityLinkedList.get(i);
                                    DetailedSearchItem item = new DetailedSearchItem();
                                    item.setSite(removeLastChar(pageEntity.getSite().getUrl()));
                                    item.setSiteName(pageEntity.getSite().getName());
                                    item.setUri(removeLastChar(pageEntity.getPath()));
                                    item.setTitle(getTitle(pageEntity.getContent()));
                                    item.setSnippet(getSnippet(pageEntity.getContent(), firstLemma.getLemma()));
                                    item.setRelevance(pageRankMap.get(pageEntity));
                                    searchData.add(item);
                                }
                                SearchResponse searchResponse = new SearchResponse();
                                searchResponse.setResult(true);
                                searchResponse.setCount(pageRankMap.size());
                                searchResponse.setData(searchData);
                                return searchResponse;
                            }
                        }
                    }
                    throw new OtherExceptions("The search failed, try entering the query differently");
                }
            }
        }
        throw new SiteNotIndexingListException();
    }

    private SearchResponse searchAllSite(String query, int offset, int limit) throws IOException {
        Optional<List<SiteEntity>> optionalSite = siteRepository.findAllSiteWhereStatusIndexed();
        if (!optionalSite.get().isEmpty()) {
            LinkedHashMap<String, Integer> lemmaMap = filteringFrequentLemmasAllSite(query);
            if (lemmaMap.isEmpty()) {
                throw new OtherExceptions("The search failed, try entering the query differently");
            }
            LinkedList<String> lemmaLinkedList =
                    new LinkedList<>(lemmaMap.keySet());
            String firstLemma = lemmaLinkedList.getFirst();
            List<PageEntity> pageEntityList;
            Optional<List<PageEntity>> optionalPageEntities =
                    pageRepository.findAllPageWhereLemma(lemmaLinkedList.getFirst());
            if (optionalPageEntities.isPresent()) {
                pageEntityList = optionalPageEntities.get();
                for (String lemma : lemmaLinkedList) {
                    pageEntityList = recursiveSearchListPagesAllSite(lemma, pageEntityList);
                }
                if (!pageEntityList.isEmpty()) {
                    LinkedHashMap<PageEntity, Float> pageRankMap =
                            getPagesMapByRelevanceAllSite(lemmaLinkedList, pageEntityList);
                    LinkedList<PageEntity> pageEntityLinkedList =
                            new LinkedList<>(pageRankMap.keySet());
                    List<DetailedSearchItem> searchData = new ArrayList<>();
                    for (int i = offset; i < limit && i < pageEntityLinkedList.size(); i++) {
                        PageEntity pageEntity = pageEntityLinkedList.get(i);
                        DetailedSearchItem item = new DetailedSearchItem();
                        item.setSite(removeLastChar(pageEntity.getSite().getUrl()));
                        item.setSiteName(pageEntity.getSite().getName());
                        item.setUri(removeLastChar(pageEntity.getPath()));
                        item.setTitle(getTitle(pageEntity.getContent()));
                        item.setSnippet(getSnippet(pageEntity.getContent(), firstLemma));
                        item.setRelevance(pageRankMap.get(pageEntity));
                        searchData.add(item);
                    }
                    SearchResponse searchResponse = new SearchResponse();
                    searchResponse.setResult(true);
                    searchResponse.setCount(pageRankMap.size());
                    searchResponse.setData(searchData);
                    return searchResponse;
                }
            }
        }
        throw new SitesNotIndexedException();
    }

    private String removeLastChar(String url) {
        return Optional.ofNullable(url)
                .filter(str -> !str.isEmpty())
                .map(str -> str.substring(0, str.length() - 1))
                .orElse(url);
    }

    private String getTitle(String htmlPage) {
        Document html = Jsoup.parse(htmlPage);
        return html.title();
    }

    private String getSnippet(String htmlPage, String lemma) throws IOException {
        LemmaFinder lemmaFinder = new LemmaFinder();
        log.info("Лемма " + lemma);
        String searchWord;
        Document html = Jsoup.parse(htmlPage);
        String text = html.body().text();
        String[] words = text.replaceAll("[^а-яА-ЯёЁ\\s]", "")
                .trim()
                .split("\\s+");
        for (String word : words) {
            String test = word.toLowerCase();
            if (lemmaFinder.getNormalFormWord(test).contains(lemma)) {
                searchWord = word;
                log.info("Найденное слово " + searchWord);
                if (!searchWord.isBlank()) {
                    Pattern pattern = Pattern.compile("([^\\s]+[\\s]+){5}" + searchWord + "([\\s]+[^\\s]+){10}");
                    Matcher matcher = pattern.matcher(text);
                    while (matcher.find()) {
                        text = text.substring(matcher.start(), matcher.end());
                        log.info("Фрагмент: " + text);
                        pattern = Pattern.compile(searchWord);
                        matcher = pattern.matcher(text);
                        text = matcher.replaceAll("<b>" + searchWord + "</b>");
                        return text;
                    }
                }
            }
        }
        text = "";
        return text;
    }
}
