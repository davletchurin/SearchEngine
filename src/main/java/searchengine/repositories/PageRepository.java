package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<PageEntity, Long> {
    void deleteAllBySiteId(SiteEntity siteEntity);
    boolean existsBySiteIdAndPath(SiteEntity siteEntity, String path);
    void deleteByPath(String path);
    List<PageEntity> findAllBySiteId(SiteEntity siteEntity);
    int countBySiteId(SiteEntity siteEntity);
    Optional<PageEntity> findBySiteIdAndPath(SiteEntity siteEntity, String relUrl);
}
