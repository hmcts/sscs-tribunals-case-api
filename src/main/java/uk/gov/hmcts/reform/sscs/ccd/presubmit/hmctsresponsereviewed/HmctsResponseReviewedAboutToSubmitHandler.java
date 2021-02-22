package uk.gov.hmcts.reform.sscs.ccd.presubmit.hmctsresponsereviewed;

import static java.util.Objects.requireNonNull;

import lombok.extern.slf4j.Slf4j;
import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.*;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.ResponseEventsAboutToSubmit;
import uk.gov.hmcts.reform.sscs.service.DwpDocumentService;


@Component
@Slf4j
public class HmctsResponseReviewedAboutToSubmitHandler extends ResponseEventsAboutToSubmit implements PreSubmitCallbackHandler<SscsCaseData> {
    private DwpDocumentService dwpDocumentService;

    @Autowired
    public HmctsResponseReviewedAboutToSubmitHandler(DwpDocumentService dwpDocumentService) {
        this.dwpDocumentService = dwpDocumentService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
            && callback.getEvent() == EventType.HMCTS_RESPONSE_REVIEWED;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        setCaseCode(sscsCaseData, callback.getEvent());
        checkMandatoryFields(preSubmitCallbackResponse, sscsCaseData);
        clearDwpDocuments(sscsCaseData);

        if (sscsCaseData.getDwpResponseDate() == null) {
            sscsCaseData.setDwpResponseDate(LocalDate.now().toString());
        }

        return preSubmitCallbackResponse;
    }

    private void clearDwpDocuments(SscsCaseData sscsCaseData) {
        sscsCaseData.setDwpResponseDocument(null);
        sscsCaseData.setDwpAT38Document(null);
        sscsCaseData.setDwpEvidenceBundleDocument(null);
        sscsCaseData.setDwpEditedEvidenceBundleDocument(null);
        sscsCaseData.setDwpEditedResponseDocument(null);
        sscsCaseData.setAppendix12Doc(null);
        sscsCaseData.setDwpUcbEvidenceDocument(null);
    }

}
