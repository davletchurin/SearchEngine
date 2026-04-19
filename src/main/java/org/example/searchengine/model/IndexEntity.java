package org.example.searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "`index`")
@Getter
@Setter
public class IndexEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "page_id", nullable = false)
    private PageEntity page;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lemma_id", nullable = false)
    private LemmaEntity lemma;
    @Column(name = "`rank`", columnDefinition = "FLOAT", nullable = false)
    private Float rank;
}
