package uk.gov.hmcts.reform.sscs.ccd.presubmit.directionissued;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
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
import uk.gov.hmcts.reform.sscs.ccd.presubmit.DwpState;
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

        if (caseData.getDirectionType() == null) {
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

        if (DirectionType.PROVIDE_INFORMATION.equals(caseData.getDirectionType())) {
            caseData.setInterlocReviewState("awaitingInformation");
            caseData.setDirectionType(null);
        } else if (DirectionType.APPEAL_TO_PROCEED.equals(caseData.getDirectionType())) {
            caseData.setState(State.VALID_APPEAL);
            caseData.setDirectionType(null);
        }

        createFooter(url, caseData);
        clearTransientFields(caseData);
        setDwpState(caseData);

        PreSubmitCallbackResponse<SscsCaseData> sscsCaseDataPreSubmitCallbackResponse = new PreSubmitCallbackResponse<>(caseData);
        log.info("Saved the new interloc direction document for case id: " + caseData.getCcdCaseId());

        return sscsCaseDataPreSubmitCallbackResponse;
    }

    private void setDwpState(SscsCaseData caseData) {
        caseData.setDwpState(DwpState.DIRECTION_ACTION_REQUIRED.getId());
    }

    private void createFooter(DocumentLink url, SscsCaseData caseData) {
        if (url != null) {
            log.info("Direction issued adding footer appendix document link: {} and caseId {}", url, caseData.getCcdCaseId());

            String bundleAddition = footerService.getNextBundleAddition(caseData.getSscsDocument());

            String bundleFileName = footerService.buildBundleAdditionFileName(bundleAddition, "Direction notice issued on "
                    + Optional.ofNullable(caseData.getDateAdded()).orElse(LocalDate.now()).format(DateTimeFormatter.ofPattern("dd-MM-YYYY")));

            SscsDocument sscsDocument = footerService.createFooterDocument(url, "Direction notice", bundleAddition, bundleFileName,
                    caseData.getDateAdded(), DocumentType.DIRECTION_NOTICE);

            List<SscsDocument> documents = new ArrayList<>();
            documents.add(sscsDocument);

            if (caseData.getSscsDocument() != null) {
                documents.addAll(caseData.getSscsDocument());
            }
            caseData.setSscsDocument(documents);
        } else {
            log.info("Could not find direction issued document for caseId {} so skipping generating footer", caseData.getCcdCaseId());
        }
    }
}
