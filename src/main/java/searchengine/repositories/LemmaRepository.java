package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Sort;
import searchengine.model.LemmaEntity;
import searchengine.model.SiteEntity;

import java.util.List;
import java.util.Optional;

public interface LemmaRepository extends JpaRepository<LemmaEntity, Long> {
    Optional<LemmaEntity> findBySiteIdAndLemma(SiteEntity siteEntity, String lemma);
    int countBySiteId(SiteEntity siteEntity);
    Optional<LemmaEntity> findByLemma(String lemma);
    void deleteAllBySiteId(SiteEntity siteEntity);
}
