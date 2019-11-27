package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@Service
public class OutcomeServiceHandler extends EventToFieldPreSubmitCallbackHandler {

    @Autowired
    public OutcomeServiceHandler() {
        super(createMappings());
    }

    private static Map<EventType, String> createMappings() {
        Map<EventType, String> eventFieldMappings = new HashMap<>();
        eventFieldMappings.put(EventType.TCW_DECISION_STRIKE_OUT, "nonCompliantAppealStruckout");
        eventFieldMappings.put(EventType.JUDGE_DECISION_STRIKEOUT, "nonCompliantAppealStruckout");
        eventFieldMappings.put(EventType.REINSTATE_APPEAL, "reinstated");
        eventFieldMappings.put(EventType.COH_DECISION_ISSUED, "decisionUpheld");
        return eventFieldMappings;
    }

    protected SscsCaseData setField(SscsCaseData newSscsCaseData, String newValue, EventType eventType) {
        newSscsCaseData.setOutcome(newValue);
        return newSscsCaseData;
    }
}
