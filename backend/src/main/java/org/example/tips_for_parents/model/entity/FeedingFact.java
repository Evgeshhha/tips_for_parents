package org.example.tips_for_parents.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.tips_for_parents.model.enums.ReviewStatus;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name = "feeding_fact")
public class FeedingFact {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "block_id")
    private SourceBlock block;

    @Column(name = "fact_type", nullable = false)
    private String factType;
    @Column(name = "subject")
    private String subject;
    @Column(columnDefinition = "text")
    private String payload;
    @Column(name = "value_text", columnDefinition = "text")
    private String valueText;
    @Column(name = "value_min")
    private Integer valueMin;
    @Column(name = "value_max")
    private Integer valueMax;
    @Column(nullable = false, columnDefinition = "text")
    private String sentence;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false)
    private ReviewStatus reviewStatus = ReviewStatus.PENDING;
    @Column()
    private String confidence;
    @Column(name = "reviewed_by")
    private String reviewedBy;
    @Column(name = "review_note", columnDefinition = "text")
    private String reviewNote;
}
