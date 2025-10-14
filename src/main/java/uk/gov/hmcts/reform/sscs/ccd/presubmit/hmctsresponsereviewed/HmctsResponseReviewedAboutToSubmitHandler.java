package uk.gov.hmcts.reform.sscs.ccd.presubmit.hmctsresponsereviewed;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.CHILD_SUPPORT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReferralReason.PHE_REQUEST;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DwpDocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpResponseDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReferralReason;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.ResponseEventsAboutToSubmit;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.SelectWhoReviewsCase;
import uk.gov.hmcts.reform.sscs.model.AppConstants;
import uk.gov.hmcts.reform.sscs.reference.data.service.PanelCompositionService;
import uk.gov.hmcts.reform.sscs.service.AddNoteService;
import uk.gov.hmcts.reform.sscs.service.DwpDocumentService;


@Component
@Slf4j
public class HmctsResponseReviewedAboutToSubmitHandler extends ResponseEventsAboutToSubmit
    implements PreSubmitCallbackHandler<SscsCaseData> {

    private final DwpDocumentService dwpDocumentService;
    private final PanelCompositionService panelCompositionService;
    private final AddNoteService addNoteService;

    @Autowired
    public HmctsResponseReviewedAboutToSubmitHandler(DwpDocumentService dwpDocumentService,
                                                     PanelCompositionService panelCompositionService,
                                                     AddNoteService addNoteService) {
        this.dwpDocumentService = dwpDocumentService;
        this.panelCompositionService = panelCompositionService;
        this.addNoteService = addNoteService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
            && callback.getEvent() == EventType.HMCTS_RESPONSE_REVIEWED;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType,
                                                          Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();

        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse =
            new PreSubmitCallbackResponse<>(sscsCaseData);

        if (sscsCaseData.isIbcCase()) {
            sscsCaseData.setBenefitCode(sscsCaseData.getBenefitCodeIbcaOnly());
            sscsCaseData.setBenefitCodeIbcaOnly(null);

            sscsCaseData.setIssueCode(sscsCaseData.getIssueCodeIbcaOnly());
            sscsCaseData.setIssueCodeIbcaOnly(null);
        }

        setCaseCode(preSubmitCallbackResponse, callback);
        checkMandatoryFields(preSubmitCallbackResponse, sscsCaseData);
        setDwpDocuments(sscsCaseData);

        if (isNull(sscsCaseData.getDwpResponseDate())) {
            sscsCaseData.setDwpResponseDate(LocalDate.now().toString());
        }
        validateInterlocReferralReason(sscsCaseData, preSubmitCallbackResponse);

        sscsCaseData.setPanelMemberComposition(panelCompositionService
                .resetPanelCompositionIfStale(sscsCaseData, callback.getCaseDetailsBefore()));

        addNote(sscsCaseData, userAuthorisation);

        // Setting this to yes so that the warning about hearings in exception state on ready to list does not block the event
        sscsCaseData.setIgnoreCallbackWarnings(YesNo.YES);

        return preSubmitCallbackResponse;
    }

    private void validateInterlocReferralReason(SscsCaseData sscsCaseData,
                                                PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {
        if (sscsCaseData.isBenefitType(CHILD_SUPPORT)
            && sscsCaseData.getInterlocReferralReason() == PHE_REQUEST) {
            preSubmitCallbackResponse.addError("PHE request' is not a valid selection for child support cases");
        }
    }

    protected void setDwpDocuments(SscsCaseData sscsCaseData) {
        if (nonNull(sscsCaseData.getDwpDocuments())) {
            for (DwpDocument dwpDocument : sscsCaseData.getDwpDocuments()) {
                if (dwpDocument.getValue().getDocumentType().equals(DwpDocumentType.DWP_RESPONSE.getValue())) {
                    updateDocument(dwpDocument,
                        sscsCaseData.getDwpResponseDocument(),
                        AppConstants.DWP_DOCUMENT_RESPONSE_FILENAME_PREFIX);
                    sscsCaseData.setDwpResponseDocument(null);
                } else if (dwpDocument.getValue().getDocumentType().equals(DwpDocumentType.AT_38.getValue())) {
                    updateDocument(dwpDocument,
                        sscsCaseData.getDwpAT38Document(),
                        AppConstants.DWP_DOCUMENT_AT38_FILENAME_PREFIX);
                    sscsCaseData.setDwpAT38Document(null);
                } else if (dwpDocument.getValue().getDocumentType().equals(
                    DwpDocumentType.DWP_EVIDENCE_BUNDLE.getValue())) {
                    updateDocument(dwpDocument,
                        sscsCaseData.getDwpEvidenceBundleDocument(),
                        AppConstants.DWP_DOCUMENT_EVIDENCE_FILENAME_PREFIX);
                    sscsCaseData.setDwpEvidenceBundleDocument(null);
                }
            }
        }

        if (nonNull(sscsCaseData.getDwpAT38Document())
            || nonNull(sscsCaseData.getDwpEvidenceBundleDocument())
            || nonNull(sscsCaseData.getDwpResponseDocument())) {
            dwpDocumentService.moveDocsToCorrectCollection(sscsCaseData);
        }
    }

    private void updateDocument(DwpDocument dwpDocument,
                                DwpResponseDocument dwpResponseDocument,
                                String documentTypePrefix) {
        String todayDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        if (nonNull(dwpResponseDocument) && nonNull(dwpResponseDocument.getDocumentLink())) {
            String fileExtension = dwpResponseDocument.getDocumentLink().getDocumentFilename()
                    .substring(dwpResponseDocument.getDocumentLink().getDocumentFilename().lastIndexOf("."));
            if (!dwpDocument.getValue().getDocumentLink().getDocumentUrl()
                    .equals(dwpResponseDocument.getDocumentLink().getDocumentUrl())) {
                dwpDocument.getValue().setDocumentLink(DocumentLink.builder()
                        .documentBinaryUrl(dwpResponseDocument.getDocumentLink().getDocumentBinaryUrl())
                        .documentUrl(dwpResponseDocument.getDocumentLink().getDocumentUrl())
                        .documentFilename(documentTypePrefix + " on " + todayDate + fileExtension)
                        .build());
                dwpDocument.getValue().setDocumentDateAdded(LocalDate.now().toString());
                dwpDocument.getValue().setDocumentFileName(documentTypePrefix + " on " + todayDate);
            }
        }
    }

    private void addNote(SscsCaseData caseData, String userAuthorisation) {
        String note = caseData.getTempNoteDetail();
        if (nonNull(caseData.getInterlocReferralReason())
                && caseData.getInterlocReferralReason() != InterlocReferralReason.NONE
                && nonNull(caseData.getSelectWhoReviewsCase())) {

            String reasonLabel = caseData.getInterlocReferralReason().getDescription();

            log.info("Add note details for case id {} - select who reviews case: {}, interloc referral reason: {}",
                    caseData.getCcdCaseId(), caseData.getSelectWhoReviewsCase(), caseData.getInterlocReferralReason());

            String whoReviewsCaseCode = caseData.getSelectWhoReviewsCase().getValue().getCode();

            Optional<String> whoReviewsCaseLabel = Arrays.stream(SelectWhoReviewsCase.values())
                    .filter(selectWhoReviewsCase ->
                            selectWhoReviewsCase.getId().equals(whoReviewsCaseCode))
                    .map(selectWhoReviewsCase -> selectWhoReviewsCase.getLabel().toLowerCase())
                    .findFirst();

            note = String.format("Referred to interloc for %s - %s%s",
                    whoReviewsCaseLabel.orElse(""), reasonLabel, isNotBlank(note) ? " - ".concat(note) : "");
        }
        addNoteService.addNote(userAuthorisation, caseData, note);
    }
}
