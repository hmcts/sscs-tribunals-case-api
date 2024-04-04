package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import static uk.gov.hmcts.reform.sscs.ccd.domain.DwpState.STRIKE_OUT_ACTIONED;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpState;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@Service
public class ActionStrikeOutHandler extends EventToFieldPreSubmitCallbackHandler {

    @Autowired
    ActionStrikeOutHandler() {
        super(createMappings());
    }

    private static Map<EventType, String> createMappings() {
        Map<EventType, String> eventFieldMappings = new HashMap<>();
        eventFieldMappings.put(EventType.ACTION_STRIKE_OUT, STRIKE_OUT_ACTIONED.getCcdDefinition());
        return eventFieldMappings;
    }

    @Override
    protected SscsCaseData setField(SscsCaseData sscsCaseData, String newValue, EventType eventType) {
        DwpState dwpState = Arrays.stream(DwpState.values())
            .filter(x -> x.getCcdDefinition().equals(newValue))
            .findFirst()
            .orElse(null);
        sscsCaseData.setDwpState(dwpState);
        sscsCaseData.clearPoDetails();
        return sscsCaseData;
    }

}
