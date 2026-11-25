package org.example.tips_for_parents.model.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.tips_for_parents.model.enums.ReactionAction;
import org.example.tips_for_parents.model.enums.ReactionStatus;
import org.example.tips_for_parents.model.enums.Urgency;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/** Сообщение родителя о реакции + разбор (ИИ или словарный) + решение родителя. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "reaction_report")
public class ReactionReport {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "child_id")
    private Child child;

    @Column(name = "product_code", length = 200)
    private String productCode;

    @Column(name = "raw_text", nullable = false, length = 4000)
    private String rawText;

    /** Полный разбор (ReactionAnalysis) в JSON. */
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode analysis;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Urgency urgency;

    @Enumerated(EnumType.STRING)
    @Column(name = "suggested_action", nullable = false, length = 24)
    private ReactionAction suggestedAction;

    @Column(name = "llm_used", nullable = false)
    private boolean llmUsed;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ReactionStatus status = ReactionStatus.PROPOSED;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    public ReactionReport(Child child, String productCode, String rawText, JsonNode analysis,
                          Urgency urgency, ReactionAction suggestedAction, boolean llmUsed) {
        this.child = child;
        this.productCode = productCode;
        this.rawText = rawText;
        this.analysis = analysis;
        this.urgency = urgency;
        this.suggestedAction = suggestedAction;
        this.llmUsed = llmUsed;
    }

    public void resolve(ReactionStatus status) {
        this.status = status;
        this.resolvedAt = Instant.now();
    }
}
