package uk.gov.hmcts.reform.sscs.ccd.presubmit.directionissued;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState.AWAITING_ADMIN_ACTION;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState.AWAITING_INFORMATION;
import static uk.gov.hmcts.reform.sscs.helper.SscsHelper.getPreValidStates;
import static uk.gov.hmcts.reform.sscs.util.DocumentUtil.isFileAPdf;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
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
    private final String bulkScanEndpoint;

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
                && nonNull(callback.getCaseDetails())
                && nonNull(callback.getCaseDetails().getCaseData());
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

        final PreSubmitCallbackResponse<SscsCaseData> sscsCaseDataPreSubmitCallbackResponse = new PreSubmitCallbackResponse<>(caseData);
        DocumentLink url = null;
        if (nonNull(caseData.getPreviewDocument()) && callback.getEvent() == EventType.DIRECTION_ISSUED) {
            url = caseData.getPreviewDocument();
        } else if (caseData.getSscsInterlocDirectionDocument() != null && callback.getEvent() == EventType.DIRECTION_ISSUED) {
            url = caseData.getSscsInterlocDirectionDocument().getDocumentLink();
            caseData.setDateAdded(caseData.getSscsInterlocDirectionDocument().getDocumentDateAdded());
            if (!isFileAPdf(caseData.getSscsInterlocDirectionDocument().getDocumentLink())) {
                sscsCaseDataPreSubmitCallbackResponse.addError("You need to upload PDF documents only");
                return sscsCaseDataPreSubmitCallbackResponse;
            }
        }
        if (isNull(url) && callback.getEvent() != EventType.DIRECTION_ISSUED_WELSH) {
            sscsCaseDataPreSubmitCallbackResponse.addError("You need to upload a PDF document");
            return sscsCaseDataPreSubmitCallbackResponse;
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

        if (callback.getEvent() == EventType.DIRECTION_ISSUED) {
            footerService.createFooterAndAddDocToCase(url, caseData, DocumentType.DIRECTION_NOTICE,
                Optional.ofNullable(caseData.getDateAdded()).orElse(LocalDate.now())
                    .format(DateTimeFormatter.ofPattern("dd-MM-YYYY")),
                caseData.getDateAdded(), null, documentTranslationStatus);
        }

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
