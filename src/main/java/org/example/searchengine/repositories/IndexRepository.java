package org.example.searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.example.searchengine.model.IndexEntity;
import org.example.searchengine.model.LemmaEntity;
import org.example.searchengine.model.PageEntity;

import java.util.List;

public interface IndexRepository extends JpaRepository<IndexEntity, Long> {
    List<IndexEntity> findAllByLemmaId(LemmaEntity lemmaEntity);
    List<IndexEntity> findAllByPageId(PageEntity pageEntity);
    void deleteAllByPageId(PageEntity pageEntity);
}
