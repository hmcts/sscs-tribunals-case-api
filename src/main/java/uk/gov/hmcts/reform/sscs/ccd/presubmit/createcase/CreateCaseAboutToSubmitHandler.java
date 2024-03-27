package uk.gov.hmcts.reform.sscs.ccd.presubmit.createcase;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.handleBenefitType;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.pdf.service.client.exception.PDFServiceClientException;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.helper.EmailHelper;
import uk.gov.hmcts.reform.sscs.reference.data.model.Language;
import uk.gov.hmcts.reform.sscs.reference.data.service.VerbalLanguagesService;
import uk.gov.hmcts.reform.sscs.service.SscsPdfService;

@Component
@Slf4j
@AllArgsConstructor
public class CreateCaseAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final SscsPdfService sscsPdfService;
    private final EmailHelper emailHelper;

    private final VerbalLanguagesService verbalLanguagesService;

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType == CallbackType.ABOUT_TO_SUBMIT
                && (!"Paper".equalsIgnoreCase(callback.getCaseDetails().getCaseData().getAppeal().getReceivedVia())
                && (callback.getEvent() == EventType.VALID_APPEAL_CREATED
                || callback.getEvent() == EventType.DRAFT_TO_VALID_APPEAL_CREATED
                || callback.getEvent() == EventType.NON_COMPLIANT
                || callback.getEvent() == EventType.DRAFT_TO_NON_COMPLIANT
                || callback.getEvent() == EventType.INCOMPLETE_APPLICATION_RECEIVED
                || callback.getEvent() == EventType.DRAFT_TO_INCOMPLETE_APPLICATION)
                || callback.getEvent() == EventType.CREATE_APPEAL_PDF);
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        SscsCaseData caseData = caseDetails.getCaseData();

        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(caseData);

        caseData.setPoAttendanceConfirmed(YesNo.NO);
        caseData.setTribunalDirectPoToAttend(YesNo.NO);

        handleBenefitType(caseData);
        updateLanguage(caseData);

        if (isNull(caseData.getDwpIsOfficerAttending())) {
            caseData.setDwpIsOfficerAttending(NO.toString());
        }

        if (!isNull(caseData.getBenefitCode())) {
            if (isNull(caseData.getIssueCode())) {
                caseData.setIssueCode("DD");
            }

            caseData.setCaseCode(caseData.getBenefitCode() + caseData.getIssueCode());
        }

        if (caseData.getCaseCreated() == null) {
            preSubmitCallbackResponse.addError("The Case Created Date must be set to generate the SSCS1");
        } else {
            createAppealPdf(caseData);
        }

        return preSubmitCallbackResponse;
    }

    private void updateLanguage(SscsCaseData caseData) {
        Appeal appeal = caseData.getAppeal();
        if (nonNull(appeal)) {
            HearingOptions hearingOptions = appeal.getHearingOptions();

            if (nonNull(hearingOptions)) {
                String syaSelectedLanguage = hearingOptions.getLanguages();
                Language language = verbalLanguagesService.getVerbalLanguage(syaSelectedLanguage);

                if (nonNull(language)) {
                    hearingOptions.setLanguages(language.getNameEn());
                }
            }
        }
    }

    private void createAppealPdf(SscsCaseData caseData) {
        String fileName = emailHelper.generateUniqueEmailId(caseData.getAppeal().getAppellant()) + ".pdf";

        boolean hasPdf = hasPdfDocument(caseData, fileName);

        log.info("Does case have sscs1 pdf {} for caseId {}", hasPdf, caseData.getCcdCaseId());
        if (!hasPdf) {
            log.info("Existing pdf document not found, start generating pdf for caseId {}", caseData.getCcdCaseId());

            try {
                updateAppointeeNullIfNotPresent(caseData);
                caseData.setEvidencePresent(hasEvidence(caseData, fileName));
                sscsPdfService.generatePdf(caseData, Long.parseLong(caseData.getCcdCaseId()), "sscs1", fileName);

            } catch (PDFServiceClientException pdfServiceClientException) {
                log.error("Sscs1 form could not be generated for caseId {} for exception ", caseData.getCcdCaseId(), pdfServiceClientException);
            }
        }
    }

    private boolean hasPdfDocument(SscsCaseData caseData, String fileName) {
        log.info("Case does have document {} and Pdf file name to check {} for caseId {}",
                !CollectionUtils.isEmpty(caseData.getSscsDocument()), fileName, caseData.getCcdCaseId());

        if (caseData.getSscsDocument() != null) {
            for (SscsDocument document : caseData.getSscsDocument()) {
                log.info("Existing document {} for case {} ",
                        document != null ? document.getValue().getDocumentFileName() : null,
                        caseData.getCcdCaseId());
                if (document != null && fileName.equalsIgnoreCase(document.getValue().getDocumentFileName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void updateAppointeeNullIfNotPresent(SscsCaseData caseData) {
        if (caseData != null && caseData.getAppeal() != null && caseData.getAppeal().getAppellant() != null) {
            Appointee appointee = caseData.getAppeal().getAppellant().getAppointee();
            if (appointee != null && appointee.getName() == null) {
                caseData.getAppeal().getAppellant().setAppointee(null);
            }
        }
    }

    private String hasEvidence(SscsCaseData caseData, String fileName) {
        if (caseData.getSscsDocument() != null) {
            for (SscsDocument document : caseData.getSscsDocument()) {
                if (!fileName.equals(document.getValue().getDocumentFileName())) {
                    return YES.getValue();
                }
            }
        }
        return NO.getValue();
    }
}
