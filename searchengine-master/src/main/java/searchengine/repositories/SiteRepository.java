package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.SiteEntity;
import java.util.List;
import java.util.Optional;

@Repository
public interface SiteRepository extends JpaRepository<SiteEntity, Integer> {

    @Transactional
    void deleteByUrl(String url);

    @Transactional
    Optional<SiteEntity> findByUrl(String url);

    @Query(value = "select count(*) from site", nativeQuery = true)
    int getSiteCount();

    @Query(value = "select * from site s where s.status = 'INDEXED'", nativeQuery = true)
    Optional<List<SiteEntity>> findAllSiteWhereStatusIndexed();
}
