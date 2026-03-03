package uk.gov.hmcts.reform.sscs.service.metadataprovider;

import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.CHILD_SUPPORT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.UC;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.AWAIT_OTHER_PARTY_DATA;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.WITH_DWP;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.ccd.client.model.metadatafields.CaseViewField;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;

@Component
public class EnableAddOtherPartyDataMetadataFieldProvider implements MetadataFieldProvider {

    private static final String FIELD_ID = "[INJECTED_DATA.ENABLE_ADD_OTHER_PARTY_DATA]";

    private final boolean cmConfidentialityEnabled;
    private final boolean ucConfidentialityEnabled;

    public EnableAddOtherPartyDataMetadataFieldProvider(
        @Value("${feature.cm-other-party-confidentiality.enabled}") final boolean cmConfidentialityEnabled,
        @Value("${feature.uc-other-party-confidentiality.enabled}") final boolean ucConfidentialityEnabled) {
        this.ucConfidentialityEnabled = ucConfidentialityEnabled;
        this.cmConfidentialityEnabled = cmConfidentialityEnabled;
    }

    @Override
    public Optional<CaseViewField> provide(final Callback<SscsCaseData> callback) {
        final YesNo value = isApplicable(callback) ? YesNo.YES : YesNo.NO;
        return Optional.of(MetadataFieldProvider.createTextField(FIELD_ID, value));
    }

    private boolean isApplicable(final Callback<SscsCaseData> callback) {
        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        return (cmConfidentialityEnabled
            && caseDetails.getCaseData().isBenefitType(CHILD_SUPPORT)
            && caseDetails.getState() == AWAIT_OTHER_PARTY_DATA) || (ucConfidentialityEnabled && caseDetails.getCaseData()
            .isBenefitType(UC) && caseDetails.getState() == WITH_DWP);
    }
}
