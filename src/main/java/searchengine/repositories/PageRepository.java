package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

import java.util.List;

@Repository
public interface PageRepository extends JpaRepository<PageEntity, Long> {
    void deleteBySiteId(Long siteId);
    boolean existsBySiteIdAndPath(SiteEntity siteEntity, String path);
    void deleteByPath(String path);
    List<PageEntity> findAllBySiteId(SiteEntity siteEntity);
    int countBySiteId(SiteEntity siteEntity);
}
