package uk.gov.hmcts.reform.sscs.ccd.presubmit.directionissued;

import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState.AWAITING_ADMIN_ACTION;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState.AWAITING_INFORMATION;
import static uk.gov.hmcts.reform.sscs.helper.SscsHelper.getPreValidStates;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DirectionType;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpState;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.ExtensionNextEvent;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentTranslationStatus;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsWelshDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.IssueDocumentHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.service.FooterService;
import uk.gov.hmcts.reform.sscs.service.ServiceRequestExecutor;

@Service
@Slf4j
public class DirectionIssuedAboutToSubmitHandler extends IssueDocumentHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final FooterService footerService;
    private final ServiceRequestExecutor serviceRequestExecutor;
    private String bulkScanEndpoint;

    @Autowired
    public DirectionIssuedAboutToSubmitHandler(FooterService footerService, ServiceRequestExecutor serviceRequestExecutor,
                                               @Value("${bulk_scan.url}") String bulkScanUrl,
                                               @Value("${bulk_scan.validateEndpoint}") String validateEndpoint) {
        this.footerService = footerService;
        this.serviceRequestExecutor = serviceRequestExecutor;
        this.bulkScanEndpoint = String.format("%s%s", trimToEmpty(bulkScanUrl), trimToEmpty(validateEndpoint));
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType == CallbackType.ABOUT_TO_SUBMIT
                && (callback.getEvent() == EventType.DIRECTION_ISSUED
                || callback.getEvent() == EventType.DIRECTION_ISSUED_WELSH)
                && Objects.nonNull(callback.getCaseDetails())
                && Objects.nonNull(callback.getCaseDetails().getCaseData());
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {

        CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        SscsCaseData caseData = caseDetails.getCaseData();

        if (caseData.getDirectionTypeDl() == null || caseData.getDirectionTypeDl().getValue() == null) {
            PreSubmitCallbackResponse<SscsCaseData> errorResponse = new PreSubmitCallbackResponse<>(caseData);
            errorResponse.addError("Direction Type cannot be empty");
            return errorResponse;
        }

        DocumentLink url = null;
        if (Objects.nonNull(caseData.getPreviewDocument()) && callback.getEvent() == EventType.DIRECTION_ISSUED) {
            url = caseData.getPreviewDocument();
        } else if (caseData.getSscsInterlocDirectionDocument() != null && callback.getEvent() == EventType.DIRECTION_ISSUED) {
            url = caseData.getSscsInterlocDirectionDocument().getDocumentLink();
            caseData.setDateAdded(caseData.getSscsInterlocDirectionDocument().getDocumentDateAdded());
        } else if (callback.getEvent() == EventType.DIRECTION_ISSUED_WELSH) {
            Optional<SscsWelshDocument> document = caseData.getLatestWelshDocumentForDocumentType(DocumentType.DIRECTION_NOTICE);
            url = document.isPresent() ? document.get().getValue().getDocumentLink() : null;
        }

        SscsDocumentTranslationStatus documentTranslationStatus = caseData.isLanguagePreferenceWelsh() && callback.getEvent() == EventType.DIRECTION_ISSUED ? SscsDocumentTranslationStatus.TRANSLATION_REQUIRED : null;

        if (!SscsDocumentTranslationStatus.TRANSLATION_REQUIRED.equals(documentTranslationStatus)) {
            if (DirectionType.PROVIDE_INFORMATION.toString().equals(caseData.getDirectionTypeDl().getValue().getCode())) {
                caseData.setInterlocReviewState(AWAITING_INFORMATION.getId());
            } else if (getPreValidStates().contains(caseDetails.getState()) && DirectionType.APPEAL_TO_PROCEED.toString().equals(caseData.getDirectionTypeDl().getValue().getCode())) {
                caseData.setDateSentToDwp(LocalDate.now().toString());
                caseData.setInterlocReviewState(AWAITING_ADMIN_ACTION.getId());
            } else if (DirectionType.REFUSE_EXTENSION.toString().equals(caseData.getDirectionTypeDl().getValue().getCode())
                    && ExtensionNextEvent.SEND_TO_LISTING.toString().equals(caseData.getExtensionNextEventDl().getValue().getCode())) {
                updateCaseAfterExtensionRefused(caseData, AWAITING_ADMIN_ACTION.getId(), State.RESPONSE_RECEIVED);
            } else if (DirectionType.REFUSE_EXTENSION.toString().equals(caseData.getDirectionTypeDl().getValue().getCode())
                    && ExtensionNextEvent.SEND_TO_VALID_APPEAL.toString().equals(caseData.getExtensionNextEventDl().getValue().getCode())) {
                updateCaseAfterExtensionRefused(caseData, null, State.WITH_DWP);
            } else {
                caseData.setInterlocReviewState(null);
            }
        }
        PreSubmitCallbackResponse<SscsCaseData> sscsCaseDataPreSubmitCallbackResponse = new PreSubmitCallbackResponse<>(caseData);

        footerService.createFooterAndAddDocToCase(url, caseData, DocumentType.DIRECTION_NOTICE,
                    Optional.ofNullable(caseData.getDateAdded()).orElse(LocalDate.now()).format(DateTimeFormatter.ofPattern("dd-MM-YYYY")),
                    caseData.getDateAdded(), null, documentTranslationStatus);

        if (!SscsDocumentTranslationStatus.TRANSLATION_REQUIRED.equals(documentTranslationStatus)) {
            State beforeState = callback.getCaseDetailsBefore().map(e -> e.getState()).orElse(null);
            clearTransientFields(caseData, beforeState);
            caseData.setDwpState(DwpState.DIRECTION_ACTION_REQUIRED.getId());
            caseData.setTimeExtensionRequested("No");
            if (caseDetails.getState().equals(State.INTERLOCUTORY_REVIEW_STATE) && caseData.getDirectionTypeDl() != null && StringUtils.equals(DirectionType.APPEAL_TO_PROCEED.toString(), caseData.getDirectionTypeDl().getValue().getCode())) {
                PreSubmitCallbackResponse<SscsCaseData> response = serviceRequestExecutor.post(callback, bulkScanEndpoint);
                sscsCaseDataPreSubmitCallbackResponse.addErrors(response.getErrors());
            }
        } else {
            caseData.setInterlocReviewState(InterlocReviewState.WELSH_TRANSLATION.getId());
            caseData.setTranslationWorkOutstanding("Yes");
            clearBasicTransientFields(caseData);

        }
        log.info("Saved the new interloc direction document for case id: " + caseData.getCcdCaseId());

        return sscsCaseDataPreSubmitCallbackResponse;
    }

    private void updateCaseAfterExtensionRefused(SscsCaseData caseData, String interlocReviewState, State state) {
        caseData.setHmctsDwpState("sentToDwp");
        caseData.setDateSentToDwp(LocalDate.now().toString());
        caseData.setInterlocReviewState(interlocReviewState);
        caseData.setState(state);
    }
}
