package org.example.tips_for_parents.model.personal;

import org.example.tips_for_parents.model.enums.ReactionAction;
import org.example.tips_for_parents.model.enums.Symptom;
import org.example.tips_for_parents.model.enums.Urgency;

import java.util.List;

public record ReactionAnalysis(String productCode,
                               List<Symptom> symptoms,
                               Integer onsetMinutes,
                               Urgency urgency,
                               ReactionAction suggestedAction,
                               String clarifyingQuestion,
                               String parentSummary,
                               boolean llmUsed) {

    public ReactionAnalysis withUrgency(Urgency u) {
        return new ReactionAnalysis(productCode, symptoms, onsetMinutes, u, suggestedAction,
                clarifyingQuestion, parentSummary, llmUsed);
    }
}
