package uk.gov.hmcts.sscs.transform.deserialize;

import org.springframework.stereotype.Service;

import uk.gov.hmcts.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.sscs.model.ccd.CaseData;

@Service
public class SubmitYourAppealToCcdCaseDataDeserializer {

    public CaseData convertSyaToCcdCaseData(SyaCaseWrapper syaCaseWrapper) {
        return CaseData.builder().build();
    }
}
