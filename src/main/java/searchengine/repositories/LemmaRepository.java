package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.model.LemmaEntity;
import searchengine.model.SiteEntity;

import java.util.Optional;

public interface LemmaRepository extends JpaRepository<LemmaEntity, Long> {
    Optional<LemmaEntity> findBySiteIdAndLemma(SiteEntity siteEntity, String lemma);
}
