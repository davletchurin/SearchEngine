package org.example.searchengine.repositories;

import org.example.searchengine.model.SiteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.example.searchengine.model.PageEntity;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<PageEntity, Long> {

    @Transactional
    @Modifying
    void deleteAllBySite(SiteEntity siteEntity);

    boolean existsBySiteAndPath(SiteEntity siteEntity, String path);

    List<PageEntity> findAllBySite(SiteEntity siteEntity);

    int countBySite(SiteEntity siteEntity);

    Optional<PageEntity> findBySiteAndPath(SiteEntity siteEntity, String relUrl);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM page WHERE site_id = :siteId", nativeQuery = true)
    void deleteBySiteId(@Param("siteId") Long siteId);
}
