package uk.gov.hmcts.reform.sscs.ccd.presubmit.createcase;

import static java.util.List.of;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.CREATE_APPEAL_PDF;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.DRAFT_TO_INCOMPLETE_APPLICATION;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.DRAFT_TO_NON_COMPLIANT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.DRAFT_TO_VALID_APPEAL_CREATED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.INCOMPLETE_APPLICATION_RECEIVED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.NON_COMPLIANT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.VALID_APPEAL_CREATED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.helper.SscsHelper.isScottishCase;
import static uk.gov.hmcts.reform.sscs.model.AppConstants.IBCA_BENEFIT_CODE;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.generateUniqueIbcaId;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.getSscsType;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.handleBenefitType;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.handleIbcaCase;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.pdf.service.client.exception.PDFServiceClientException;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appointee;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
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
    @Value("${feature.work-allocation.enabled}")
    private final boolean workAllocationFeature;

    private static final List<EventType> NON_PAPER_EVENTS = of(VALID_APPEAL_CREATED, DRAFT_TO_VALID_APPEAL_CREATED,
            NON_COMPLIANT, DRAFT_TO_NON_COMPLIANT, INCOMPLETE_APPLICATION_RECEIVED, DRAFT_TO_INCOMPLETE_APPLICATION);

    private final VerbalLanguagesService verbalLanguagesService;

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        boolean nonPaperCase =
                !"Paper".equalsIgnoreCase(callback.getCaseDetails().getCaseData().getAppeal().getReceivedVia());
        return callbackType == CallbackType.ABOUT_TO_SUBMIT
                && (callback.getEvent() == CREATE_APPEAL_PDF
                || (nonPaperCase && NON_PAPER_EVENTS.contains(callback.getEvent()))
            );
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        caseData.setPoAttendanceConfirmed(YesNo.NO);
        caseData.setTribunalDirectPoToAttend(YesNo.NO);

        handleBenefitType(caseData);
        if (IBCA_BENEFIT_CODE.equals(caseData.getBenefitCode())) {
            handleIbcaCase(caseData);
        }
        updateLanguage(caseData);

        if (isNull(caseData.getDwpIsOfficerAttending())) {
            caseData.setDwpIsOfficerAttending(NO.toString());
        }

        if (nonNull(caseData.getBenefitCode())) {
            if (isNull(caseData.getIssueCode())) {
                caseData.setIssueCode("DD");
            }
            caseData.setCaseCode(caseData.getBenefitCode() + caseData.getIssueCode());
        }

        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(caseData);
        if (isNull(caseData.getCaseCreated())) {
            preSubmitCallbackResponse.addError("The Case Created Date must be set to generate the " + getSscsType(caseData));
        } else {
            createAppealPdf(caseData);
        }

        if (!CREATE_APPEAL_PDF.equals(callback.getEvent())) {
            caseData.setPreWorkAllocation(workAllocationFeature ? YesNo.NO : YesNo.YES);
            caseData.setIsScottishCase(isScottishCase(caseData.getRegionalProcessingCenter()));
            log.info("Setting isScottishCase field to {} for case {}",
                    caseData.getIsScottishCase(), callback.getCaseDetails().getId());
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
        final String fileName = IBCA_BENEFIT_CODE.equals(caseData.getBenefitCode())
                ? generateUniqueIbcaId(caseData.getAppeal().getAppellant()) + ".pdf"
                : emailHelper.generateUniqueEmailId(caseData.getAppeal().getAppellant()) + ".pdf";
        final String documentType = IBCA_BENEFIT_CODE.equals(caseData.getBenefitCode()) ? "sscs8" : "sscs1";
        final boolean hasPdf = hasPdfDocument(caseData, fileName);

        log.info("Does case have {} pdf {} for caseId {}", documentType, hasPdf, caseData.getCcdCaseId());
        if (!hasPdf) {
            log.info("Existing pdf document not found, start generating pdf for caseId {}", caseData.getCcdCaseId());

            try {
                updateAppointeeNullIfNotPresent(caseData);
                caseData.setEvidencePresent(hasEvidence(caseData, fileName));
                sscsPdfService.generatePdf(caseData, Long.parseLong(caseData.getCcdCaseId()), documentType, fileName);

            } catch (PDFServiceClientException pdfServiceClientException) {
                log.error("{} form could not be generated for caseId {} for exception ", documentType, caseData.getCcdCaseId(), pdfServiceClientException);
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
