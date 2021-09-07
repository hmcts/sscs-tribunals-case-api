package uk.gov.hmcts.reform.sscs.ccd.presubmit.validsendtointerloc;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.UploadParty.REP;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.model.PartyItemList.REPRESENTATIVE;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReferralReason;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.SelectWhoReviewsCase;

@Component
@Slf4j
public class ValidSendToInterlocAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
            && (callback.getEvent() == EventType.VALID_SEND_TO_INTERLOC
                || callback.getEvent() == EventType.ADMIN_SEND_TO_INTERLOCUTORY_REVIEW_STATE);
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        if (sscsCaseData.getSelectWhoReviewsCase() == null || sscsCaseData.getSelectWhoReviewsCase().getValue() == null
                || sscsCaseData.getSelectWhoReviewsCase().getValue().getCode() == null) {
            preSubmitCallbackResponse.addError("Must select who reviews the appeal.");
            return preSubmitCallbackResponse;
        }

        if (SelectWhoReviewsCase.POSTPONEMENT_REQUEST_INTERLOC_SEND_TO_TCW.getId().equals(sscsCaseData.getSelectWhoReviewsCase().getValue().getCode())) {
            if (sscsCaseData.getOriginalSender() == null || sscsCaseData.getOriginalSender().getValue() == null
                    || sscsCaseData.getOriginalSender().getValue().getCode() == null) {
                preSubmitCallbackResponse.addError("Must select original sender");
                return preSubmitCallbackResponse;
            }
            processPostponementRequest(sscsCaseData);
        } else {
            final String code = sscsCaseData.getSelectWhoReviewsCase().getValue().getCode();
            sscsCaseData.setInterlocReviewState(code);
        }
        sscsCaseData.setSelectWhoReviewsCase(null);
        log.info("Setting interloc referral date to {}  for caseId {}", LocalDate.now(), sscsCaseData.getCcdCaseId());
        sscsCaseData.setInterlocReferralDate(LocalDate.now().toString());
        sscsCaseData.setDirectionDueDate(null);
        return preSubmitCallbackResponse;
    }

    private void processPostponementRequest(SscsCaseData sscsCaseData) {
        ensureSscsDocumentsIsNotNull(sscsCaseData);
        final SscsDocument sscsDocument = buildNewSscsDocumentFromPostponementRequest(sscsCaseData);
        addToSscsDocuments(sscsCaseData, sscsDocument);
        sscsCaseData.setInterlocReviewState(InterlocReviewState.REVIEW_BY_TCW.getId());
        sscsCaseData.setInterlocReferralReason(InterlocReferralReason.REVIEW_POSTPONEMENT_REQUEST.getId());
        sscsCaseData.getPostponementRequest().setUnprocessedPostponementRequest(YES);
        clearTransientFields(sscsCaseData);
    }

    private void clearTransientFields(SscsCaseData sscsCaseData) {
        sscsCaseData.getPostponementRequest().setPostponementRequestDetails(null);
        sscsCaseData.getPostponementRequest().setPostponementPreviewDocument(null);
    }

    private void addToSscsDocuments(SscsCaseData sscsCaseData, SscsDocument sscsDocument) {
        sscsCaseData.getSscsDocument().add(sscsDocument);
    }

    private SscsDocument buildNewSscsDocumentFromPostponementRequest(SscsCaseData sscsCaseData) {

        UploadParty uploadParty = REPRESENTATIVE.getCode().equals(sscsCaseData.getOriginalSender().getValue().getCode())
                ? REP : UploadParty.fromValue(sscsCaseData.getOriginalSender().getValue().getCode());

        return SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentLink(sscsCaseData.getPostponementRequest().getPostponementPreviewDocument())
                .documentFileName(sscsCaseData.getPostponementRequest().getPostponementPreviewDocument().getDocumentFilename())
                .documentType(DocumentType.POSTPONEMENT_REQUEST.getValue())
                .documentDateAdded(LocalDate.now().format(DateTimeFormatter.ISO_DATE))
                .partyUploaded(uploadParty)
                .build()).build();
    }

    private void ensureSscsDocumentsIsNotNull(SscsCaseData sscsCaseData) {
        final List<SscsDocument> sscsDocuments = (sscsCaseData.getSscsDocument() == null) ? new ArrayList<>() : sscsCaseData.getSscsDocument();
        sscsCaseData.setSscsDocument(sscsDocuments);
    }

}
