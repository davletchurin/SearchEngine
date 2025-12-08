package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;

import java.util.List;
import java.util.Optional;

public interface IndexRepository extends JpaRepository<IndexEntity, Long> {
    List<IndexEntity> findAllByLemmaId(LemmaEntity lemmaEntity);
    List<IndexEntity> findAllByPageId(PageEntity pageEntity);
    Optional<IndexEntity> findByPageId(PageEntity pageEntity);
    Optional<IndexEntity> findByPageIdAndLemmaId(PageEntity pageEntity, LemmaEntity lemmaEntity);
    void deleteAllByPageId(PageEntity pageEntity);
}
