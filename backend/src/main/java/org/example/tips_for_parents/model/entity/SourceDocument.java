package org.example.tips_for_parents.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name = "source_document", uniqueConstraints = {@UniqueConstraint(columnNames = {"url", "content_hash"})})
public class SourceDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, columnDefinition = "text")
    private String url;
    @Column(name = "content_hash", nullable = false)
    private String contentHash;
    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt = Instant.now();
    @Column(name = "raw_text", nullable = false, columnDefinition = "text")
    private String rawText;
}
