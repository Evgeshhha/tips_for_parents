package org.example.tips_for_parents.model.personal;

import org.example.tips_for_parents.model.entity.ReactionReport;
import org.example.tips_for_parents.model.enums.ReactionAction;
import org.example.tips_for_parents.model.enums.ReactionStatus;
import org.example.tips_for_parents.model.enums.Symptom;
import org.example.tips_for_parents.model.enums.Urgency;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReactionView(UUID id, String productCode, List<Symptom> symptoms, Integer onsetMinutes,
                           Urgency urgency, String safetyNotice, ReactionAction suggestedAction,
                           String clarifyingQuestion, String summary, boolean llmUsed,
                           ReactionStatus status, Instant createdAt) {

    public static ReactionView of(ReactionReport r, ReactionAnalysis a, String notice) {
        return new ReactionView(r.getId(), r.getProductCode(), a.symptoms(), a.onsetMinutes(), r.getUrgency(),
                notice, r.getSuggestedAction(), a.clarifyingQuestion(), a.parentSummary(),
                r.isLlmUsed(), r.getStatus(), r.getCreatedAt());
    }
}
