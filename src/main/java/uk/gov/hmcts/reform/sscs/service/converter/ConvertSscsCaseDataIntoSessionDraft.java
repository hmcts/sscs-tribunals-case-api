package uk.gov.hmcts.reform.sscs.service.converter;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.model.draft.SessionBenefitType;
import uk.gov.hmcts.reform.sscs.model.draft.SessionDraft;

@Service
public class ConvertSscsCaseDataIntoSessionDraft implements ConvertAintoBService<SscsCaseData, SessionDraft> {
    @Override
    public SessionDraft convert(SscsCaseData source) {
        return new SessionDraft(new SessionBenefitType("Personal Independence Payment (PIP)"));
    }
}
