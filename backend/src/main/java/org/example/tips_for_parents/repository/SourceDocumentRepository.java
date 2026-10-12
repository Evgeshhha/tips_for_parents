package org.example.tips_for_parents.repository;

import org.example.tips_for_parents.model.entity.SourceDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SourceDocumentRepository extends JpaRepository<SourceDocument, Long> {
    Optional<SourceDocument> findByUrlAndContentHash(String url, String contentHash);
}
