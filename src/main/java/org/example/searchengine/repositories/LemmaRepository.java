package org.example.searchengine.repositories;

import org.example.searchengine.model.SiteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.example.searchengine.model.LemmaEntity;

import java.util.Optional;

public interface LemmaRepository extends JpaRepository<LemmaEntity, Long> {
    Optional<LemmaEntity> findBySiteIdAndLemma(SiteEntity siteEntity, String lemma);
    int countBySiteId(SiteEntity siteEntity);
    void deleteAllBySiteId(SiteEntity siteEntity);
}
