package org.example.searchengine.repositories;

import org.example.searchengine.model.SiteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.example.searchengine.model.LemmaEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface LemmaRepository extends JpaRepository<LemmaEntity, Long> {

    Optional<LemmaEntity> findBySiteAndLemma(SiteEntity siteEntity, String lemma);

    int countBySite(SiteEntity siteEntity);

    @Transactional
    @Modifying
    void deleteAllBySite(SiteEntity siteEntity);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO lemma (site_id, lemma, frequency) " +
            "VALUES (:siteId, :lemma, 1) " +
            "ON DUPLICATE KEY UPDATE frequency = frequency + 1",
            nativeQuery = true)
    void upsertLemma(@Param("siteId") Long siteId, @Param("lemma") String lemma);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM lemma WHERE site_id = :siteId", nativeQuery = true)
    void deleteBySiteId(@Param("siteId") Long siteId);
}
