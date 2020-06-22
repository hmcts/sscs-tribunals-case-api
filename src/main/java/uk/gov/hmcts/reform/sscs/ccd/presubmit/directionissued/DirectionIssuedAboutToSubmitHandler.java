package uk.gov.hmcts.reform.sscs.ccd.presubmit.directionissued;

import static uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState.AWAITING_ADMIN_ACTION;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState.AWAITING_INFORMATION;
import static uk.gov.hmcts.reform.sscs.helper.SscsHelper.getPreValidStates;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.IssueDocumentHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.service.FooterService;

@Service
@Slf4j
public class DirectionIssuedAboutToSubmitHandler extends IssueDocumentHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final FooterService footerService;

    @Autowired
    public DirectionIssuedAboutToSubmitHandler(FooterService footerService) {
        this.footerService = footerService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType == CallbackType.ABOUT_TO_SUBMIT
                && callback.getEvent() == EventType.DIRECTION_ISSUED
                && Objects.nonNull(callback.getCaseDetails())
                && Objects.nonNull(callback.getCaseDetails().getCaseData());
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();

        if (caseData.getDirectionTypeDl() == null || caseData.getDirectionTypeDl().getValue() == null) {
            PreSubmitCallbackResponse<SscsCaseData> errorResponse = new PreSubmitCallbackResponse<>(caseData);
            errorResponse.addError("Direction Type cannot be empty");
            return errorResponse;
        }

        DocumentLink url = null;
        if (Objects.nonNull(callback.getCaseDetails().getCaseData().getPreviewDocument())) {
            url = caseData.getPreviewDocument();
        } else {
            if (caseData.getSscsInterlocDirectionDocument() != null) {
                url = caseData.getSscsInterlocDirectionDocument().getDocumentLink();
                caseData.setDateAdded(caseData.getSscsInterlocDirectionDocument().getDocumentDateAdded());
            }
        }

        if (DirectionType.PROVIDE_INFORMATION.toString().equals(caseData.getDirectionTypeDl().getValue().getCode())) {
            caseData.setInterlocReviewState(AWAITING_INFORMATION.getId());
        } else if (getPreValidStates().contains(callback.getCaseDetails().getState()) && DirectionType.APPEAL_TO_PROCEED.toString().equals(caseData.getDirectionTypeDl().getValue().getCode())) {
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

        footerService.createFooterAndAddDocToCase(url, caseData, DocumentType.DIRECTION_NOTICE,
                Optional.ofNullable(caseData.getDateAdded()).orElse(LocalDate.now()).format(DateTimeFormatter.ofPattern("dd-MM-YYYY")));

        State beforeState = callback.getCaseDetailsBefore().map(e -> e.getState()).orElse(null);

        clearTransientFields(caseData, beforeState);

        caseData.setDwpState(DwpState.DIRECTION_ACTION_REQUIRED.getId());

        caseData.setTimeExtensionRequested("No");

        PreSubmitCallbackResponse<SscsCaseData> sscsCaseDataPreSubmitCallbackResponse = new PreSubmitCallbackResponse<>(caseData);
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
