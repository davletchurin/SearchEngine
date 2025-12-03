package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;

import java.util.List;

public interface IndexRepository extends JpaRepository<IndexEntity, Long> {
    List<IndexEntity> findAllByLemmaId(LemmaEntity lemmaEntity);
    List<IndexEntity> findAllByPageId(PageEntity pageEntity);
}
