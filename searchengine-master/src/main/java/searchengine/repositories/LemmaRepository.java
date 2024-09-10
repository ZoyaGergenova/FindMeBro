package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.LemmaEntity;
import java.util.Optional;


public interface LemmaRepository extends JpaRepository<LemmaEntity, Integer> {

    @Transactional
    Optional<LemmaEntity> findByLemmaAndSite_id(String lemma, int siteId);

    @Query(value = "select sum(l.frequency) " +
            "from lemma l, site s " +
            "where s.status = 'INDEXED' and l.site_id = s.id and l.lemma=?1", nativeQuery = true)
    Optional<Integer> getFrequencyLemma(String lemma);

    @Query(value = "select count(*) from lemma", nativeQuery = true)
    int getLemmaCount();
}

