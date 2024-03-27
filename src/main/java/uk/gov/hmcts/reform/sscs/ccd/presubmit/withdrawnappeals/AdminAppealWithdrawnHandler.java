package uk.gov.hmcts.reform.sscs.ccd.presubmit.withdrawnappeals;

import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.WITHDRAWAL_REQUEST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DwpState.WITHDRAWAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.clearPostponementTransientFields;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.resendtogaps.ListAssistHearingMessageHelper;
import uk.gov.hmcts.reform.sscs.reference.data.model.CancellationReason;
import uk.gov.hmcts.reform.sscs.util.SscsUtil;

@Service
@Slf4j
public class AdminAppealWithdrawnHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final ListAssistHearingMessageHelper hearingMessageHelper;
    private boolean isScheduleListingEnabled;

    public AdminAppealWithdrawnHandler(ListAssistHearingMessageHelper hearingMessageHelper,
        @Value("${feature.snl.enabled}") boolean isScheduleListingEnabled) {
        this.hearingMessageHelper = hearingMessageHelper;
        this.isScheduleListingEnabled = isScheduleListingEnabled;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType != null && callback != null && callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
            && callback.getEvent().equals(EventType.ADMIN_APPEAL_WITHDRAWN);
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        caseData.setDwpState(WITHDRAWAL_RECEIVED);

        caseData.clearPoDetails();

        DocumentLink documentDetails = caseData.getDocumentStaging().getPreviewDocument();
        if (nonNull(documentDetails)) {
            addToSscsDocuments(caseData, documentDetails);
        }
        clearPostponementTransientFields(caseData);
        cancelHearing(callback);
        return new PreSubmitCallbackResponse<>(caseData);
    }

    private void cancelHearing(Callback<SscsCaseData> callback) {
        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();

        if (eligibleForHearingsCancel.test(callback)) {
            log.info("Admin Appeal Withdrawn: HearingRoute ListAssist Case ({}). Sending cancellation message",
                    sscsCaseData.getCcdCaseId());
            hearingMessageHelper.sendListAssistCancelHearingMessage(sscsCaseData.getCcdCaseId(),
                    CancellationReason.WITHDRAWN);
        }
    }

    private final Predicate<Callback<SscsCaseData>> eligibleForHearingsCancel = callback -> isScheduleListingEnabled
        && SscsUtil.isValidCaseState(callback.getCaseDetailsBefore().map(CaseDetails::getState)
            .orElse(State.UNKNOWN), List.of(State.HEARING, State.READY_TO_LIST))
        && SscsUtil.isSAndLCase(callback.getCaseDetails().getCaseData());

    private void addToSscsDocuments(SscsCaseData caseData, DocumentLink documentDetails) {
        SscsDocumentDetails details = SscsDocumentDetails.builder()
                .documentDateAdded(LocalDate.now().toString())
                .documentType(WITHDRAWAL_REQUEST.getValue())
                .documentFileName(documentDetails.getDocumentFilename())
                .documentLink(documentDetails)
                .build();

        List<SscsDocument> allDocuments = new ArrayList<>(ofNullable(caseData.getSscsDocument()).orElse(emptyList()));
        allDocuments.add(SscsDocument.builder().value(details).build());
        caseData.setSscsDocument(allDocuments);
        caseData.getDocumentStaging().setPreviewDocument(null);
    }
}
