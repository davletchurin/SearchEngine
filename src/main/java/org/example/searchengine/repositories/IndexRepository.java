package org.example.searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.example.searchengine.model.IndexEntity;
import org.example.searchengine.model.LemmaEntity;
import org.example.searchengine.model.PageEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.Modifying;

import java.util.Collection;
import java.util.List;

public interface IndexRepository extends JpaRepository<IndexEntity, Long> {

    List<IndexEntity> findAllByLemma(LemmaEntity lemmaEntity);

    List<IndexEntity> findAllByPage(PageEntity pageEntity);

    @Transactional
    @Modifying
    void deleteAllByPageIn(List<PageEntity> pages);

    List<IndexEntity> findAllByPageAndLemmaIn(PageEntity page, Collection<LemmaEntity> lemmas);

    @Modifying
    @Transactional
    @Query(value = "DELETE i FROM `index` i JOIN page p ON i.page_id = p.id WHERE p.site_id = :siteId", nativeQuery = true)
    void deleteBySiteId(@Param("siteId") Long siteId);

    List<IndexEntity> findAllByPageInAndLemmaIn(Collection<PageEntity> pages, Collection<LemmaEntity> lemmas);
}
