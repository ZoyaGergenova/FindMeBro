package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.IndexEntity;
import java.util.Optional;

public interface IndexRepository extends JpaRepository<IndexEntity, Integer> {

    @Transactional
    boolean existsByLemma_idAndPage_id(int lemmaId, int pageId);

    @Query(value = "select i.id, i.rank_index, i.lemma_id, i.page_id " +
            "from index_table i, lemma l " +
            "where l.lemma=?1 and i.lemma_id = l.id and i.page_id=?2", nativeQuery = true)
    Optional<IndexEntity> findByPageLemma(String lemma, int pageId);

    @Query(value = "select i.rank_index " +
            "from index_table i " +
            "where i.lemma_id=?1 and i.page_id=?2", nativeQuery = true)
    Float getRank(int lemmaId, int pageId);

    @Query(value = "select i.rank_index " +
            "from index_table i, lemma l " +
            "where l.lemma=?1 and i.lemma_id = l.id and i.page_id=?2", nativeQuery = true)
    Float getRankAllSite(String lemma, int pageId);
}
