package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.PageEntity;
import java.util.List;
import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<PageEntity, Integer> {
    @Transactional
    boolean existsByPath(String path);

    @Transactional
    long deleteByPath(String path);

    @Query(value = "select count(*) from page", nativeQuery = true)
    int getPageCount();

    @Query(value = "select count(*) " +
            "from page p " +
            "where p.site_id=?1", nativeQuery = true)
    Optional<Integer> getPageCountWhereSiteId(int siteId);

    @Query(value = "select count(*) " +
            "from page p " +
            "join site s on p.site_id = s.id " +
            "where s.status = 'INDEXED'", nativeQuery = true)
    Optional<Integer> getPageCountWhereSiteIndexed();

    @Query(value = "select p.id, p.code, p.content, p.path, p.site_id " +
            "from page p, index_table i " +
            "where i.lemma_id=?1 and p.site_id=?2", nativeQuery = true)
    Optional<List<PageEntity>> findByLemmaIdSiteId(int lemmaId, int siteId);

    @Query(value = "select p.id, p.code, p.content, p.path, p.site_id " +
            "from page p, lemma l, index_table i " +
            "where l.lemma=?1 and i.lemma_id = l.id and p.id = i.page_id", nativeQuery = true)
    Optional<List<PageEntity>> findAllPageWhereLemma(String lemma);
}
