package uk.gov.hmcts.reform.sscs.service;

import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.CHILD_SUPPORT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.UC;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.AWAIT_OTHER_PARTY_DATA;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.WITH_DWP;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.GetCaseCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.metadatafields.CaseViewField;
import uk.gov.hmcts.reform.ccd.client.model.metadatafields.definition.FieldTypeDefinition;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@Service
public class GetCaseCallbackService {

    private static final String ENABLE_ADD_OTHER_PARTY_DATA_FIELD_ID = "[INJECTED_DATA.ENABLE_ADD_OTHER_PARTY_DATA]";
    private static final String TEXT_FIELD_TYPE_ID = "Text";
    private static final String LABEL_FIELD_TYPE = "Label";
    private static final String YES_VALUE = "y";
    private static final String NO_VALUE = "n";
    private final boolean ucOtherPartyConfidentialityEnabled;

    public GetCaseCallbackService(
        @Value("${feature.uc-other-party-confidentiality.enabled}") boolean ucOtherPartyConfidentialityEnabled) {
        this.ucOtherPartyConfidentialityEnabled = ucOtherPartyConfidentialityEnabled;
    }

    public GetCaseCallbackResponse buildResponse(final Callback<SscsCaseData> callback) {
        final GetCaseCallbackResponse response = new GetCaseCallbackResponse();
        if (ucOtherPartyConfidentialityEnabled) {
            response.setMetadataFields(List.of(createEnableAddOtherPartyDataMetadataField(callback)));
        }
        return response;
    }

    private CaseViewField createTextMetadataField(final String fieldId, final String value) {
        final CaseViewField field = new CaseViewField();
        field.setId(fieldId);
        field.setValue(value);
        field.setMetadata(true);
        final FieldTypeDefinition fieldTypeDefinition = new FieldTypeDefinition();
        fieldTypeDefinition.setId(TEXT_FIELD_TYPE_ID);
        fieldTypeDefinition.setType(LABEL_FIELD_TYPE);
        field.setFieldTypeDefinition(fieldTypeDefinition);
        return field;
    }

    private boolean isEnableAddOtherPartyData(final Callback<SscsCaseData> callback) {
        CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        return ((caseDetails.getCaseData().isBenefitType(CHILD_SUPPORT) && caseDetails.getState() == AWAIT_OTHER_PARTY_DATA) || (
            caseDetails.getCaseData().isBenefitType(UC)
                && caseDetails.getState() == WITH_DWP));
    }

    private CaseViewField createEnableAddOtherPartyDataMetadataField(final Callback<SscsCaseData> callback) {
        final String fieldValue = isEnableAddOtherPartyData(callback) ? YES_VALUE : NO_VALUE;
        return createTextMetadataField(ENABLE_ADD_OTHER_PARTY_DATA_FIELD_ID, fieldValue);
    }
}
