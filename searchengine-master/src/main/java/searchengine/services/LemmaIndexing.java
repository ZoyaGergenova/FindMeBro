package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class LemmaIndexing {

    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private LemmaEntity lemmaEntity = new LemmaEntity();

    public void indexingLemmas(Map<String, Integer> lemmas, SiteEntity siteEntity, PageEntity pageEntity) {
        for (Map.Entry<String, Integer> entry : lemmas.entrySet()) {
            Optional<LemmaEntity> optionalLemma = lemmaRepository.findByLemmaAndSite_id(entry.getKey(), siteEntity.getId());
            if (optionalLemma.isPresent()) {
                lemmaEntity = optionalLemma.get();
                lemmaEntity.setFrequency(lemmaEntity.getFrequency() + 1);
                lemmaRepository.save(lemmaEntity);
            } else {
                lemmaEntity = new LemmaEntity();
                lemmaEntity.setFrequency(1);
                lemmaEntity.setLemma(entry.getKey());
                lemmaEntity.setSite(siteEntity);
                int lemmaId = lemmaRepository.save(lemmaEntity).getId();
                lemmaEntity = lemmaRepository.findById(lemmaId).orElseThrow();
            }
            IndexEntity indexEntity = new IndexEntity();
            indexEntity.setPage(pageEntity);
            indexEntity.setLemma(lemmaEntity);
            indexEntity.setRank((float)entry.getValue());
            indexRepository.save(indexEntity);
        }
    }
}
