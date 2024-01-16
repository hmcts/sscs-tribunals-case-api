package uk.gov.hmcts.reform.sscs.ccd.presubmit.createwelshnotice;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.*;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.service.FooterDetails;
import uk.gov.hmcts.reform.sscs.service.PdfStoreService;
import uk.gov.hmcts.reform.sscs.service.WelshFooterService;
import uk.gov.hmcts.reform.sscs.service.conversion.LocalDateToWelshStringConverter;
import uk.gov.hmcts.reform.sscs.thirdparty.pdfservice.DocmosisPdfService;

@Service
@Slf4j
public class CreateWelshNoticeAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private static final String DM_STORE_USER_ID = "sscs";
    private final String directionTemplatePath;
    private final DocmosisPdfService docmosisPdfService;
    private final PdfStoreService pdfStoreService;
    private final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
    private final WelshFooterService welshFooterService;
    private static final Map<String, String> NEXT_EVENT_MAP = new HashMap<>();

    static {
        NEXT_EVENT_MAP.put(DECISION_NOTICE.getValue(), DECISION_ISSUED_WELSH.getCcdType());
        NEXT_EVENT_MAP.put(DIRECTION_NOTICE.getValue(), DIRECTION_ISSUED_WELSH.getCcdType());
        NEXT_EVENT_MAP.put(AUDIO_VIDEO_EVIDENCE_DIRECTION_NOTICE.getValue(), PROCESS_AUDIO_VIDEO_WELSH.getCcdType());
        NEXT_EVENT_MAP.put(ADJOURNMENT_NOTICE.getValue(), ISSUE_ADJOURNMENT_NOTICE_WELSH.getCcdType());
        NEXT_EVENT_MAP.put(POSTPONEMENT_REQUEST_DIRECTION_NOTICE.getValue(), ACTION_POSTPONEMENT_REQUEST_WELSH.getCcdType());
    }

    @Autowired
    public CreateWelshNoticeAboutToSubmitHandler(DocmosisPdfService docmosisPdfService,
                                                 PdfStoreService pdfStoreService,
                                                 WelshFooterService welshFooterService,
                                                 @Value("${document.bilingual.notice.template}") String template) {
        this.docmosisPdfService = docmosisPdfService;
        this.pdfStoreService = pdfStoreService;
        this.welshFooterService = welshFooterService;
        this.directionTemplatePath  = template;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbackType must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && callback.getEvent().equals(CREATE_WELSH_NOTICE);
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        createNoticeAndUpload(callback, caseData);
        clearFields(caseData);
        return new PreSubmitCallbackResponse<>(caseData);
    }

    private void createNoticeAndUpload(Callback<SscsCaseData> callback, SscsCaseData caseData) {

        Map<String, Object> placeholderMap = caseDataMap(callback.getCaseDetails().getCaseData());
        LocalDate dateAdded =
            Optional.ofNullable(caseData.getDocumentStaging().getDateAdded()).orElse(LocalDate.now());
        final String filename = String.format("%s on %s.pdf", caseData.getDocumentTypes().getValue().getCode(),
                dateAdded.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
        byte[] content = docmosisPdfService.createPdf(placeholderMap, directionTemplatePath);

        DocumentLink newDocLink = null;

        SscsDocument sscsDocument = pdfStoreService.storeDocument(content, filename);

        markOriginalDocumentsAsTranslationComplete(caseData);

        if (sscsDocument != null) {
            String location = sscsDocument.getValue().getDocumentLink().getDocumentUrl();
            newDocLink = DocumentLink.builder().documentFilename(filename).documentUrl(location).documentBinaryUrl(location + "/binary").build();
            final FooterDetails footerDetails = welshFooterService.addFooterToExistingToContentAndCreateNewUrl(newDocLink, caseData.getSscsWelshDocuments(), fromValue(caseData.getDocumentTypes().getValue().getCode()), null, LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));

            SscsWelshDocumentDetails sscsWelshDocumentDetails = SscsWelshDocumentDetails.builder()
                    .documentType(caseData.getDocumentTypes().getValue().getCode())
                    .documentFileName(footerDetails.getBundleFileName())
                    .bundleAddition(footerDetails.getBundleAddition())
                    .documentLink(footerDetails.getUrl())
                    .originalDocumentFileName(caseData.getOriginalNoticeDocuments().getValue().getLabel())
                    .build();

            if (caseData.getSscsWelshDocuments() == null) {
                List<SscsWelshDocument> sscsWelshDocumentsList = new ArrayList<>();
                caseData.setSscsWelshDocuments(sscsWelshDocumentsList);
            }
            caseData.getSscsWelshDocuments().add(SscsWelshDocument.builder()
                    .value(sscsWelshDocumentDetails)
                    .build());
        }

        String nextEvent = getNextEvent(caseData.getDocumentTypes().getValue().getCode());
        log.info("Setting next event to {}", nextEvent);
        caseData.setSscsWelshPreviewNextEvent(nextEvent);
        caseData.updateTranslationWorkOutstandingFlag();
    }


    private void markOriginalDocumentsAsTranslationComplete(SscsCaseData caseData) {
        for (SscsDocument sscsDocument : caseData.getSscsDocument()) {
            SscsDocumentDetails sscsDocumentDetails = sscsDocument.getValue();
            if (SscsDocumentTranslationStatus.TRANSLATION_REQUESTED.equals(sscsDocumentDetails.getDocumentTranslationStatus())
                    && sscsDocumentDetails.getDocumentType().equals(caseData.getDocumentTypes().getValue().getCode())) {
                sscsDocumentDetails.setDocumentTranslationStatus(SscsDocumentTranslationStatus.TRANSLATION_COMPLETE);
            }
        }
    }

    private Map<String, Object> caseDataMap(SscsCaseData caseData) {
        Map<String, Object> dataMap = new HashMap<>();
        LocalDate dateAdded = Optional.ofNullable(caseData.getDocumentStaging().getDateAdded()).orElse(LocalDate.now());
        String documentTypeLabel = getEnglishNoticeType(caseData.getDocumentTypes().getValue().getLabel() != null ? caseData.getDocumentTypes().getValue().getLabel() : caseData.getDocumentTypes().getValue().getCode());

        dataMap.put("appellant_full_name", buildFullName(caseData));
        dataMap.put("case_id", caseData.getCcdCaseId());
        dataMap.put("nino", caseData.getAppeal().getAppellant().getIdentity().getNino());
        dataMap.put("en_notice_type", documentTypeLabel.toUpperCase());
        dataMap.put("cy_notice_type", getWelshNoticeType(documentTypeLabel));
        dataMap.put("en_notice_body", caseData.getEnglishBodyContent());
        dataMap.put("cy_notice_body", caseData.getWelshBodyContent());
        dataMap.put("user_name", caseData.getDocumentGeneration().getSignedBy());
        dataMap.put("user_role", caseData.getDocumentGeneration().getSignedRole());
        dataMap.put("date_added", dateAdded.toString());
        dataMap.put("generated_date", formatter.format(new Date()));
        dataMap.put("welsh_date_added", LocalDateToWelshStringConverter.convert(dateAdded));
        dataMap.put("welsh_generated_date", LocalDateToWelshStringConverter.convert(LocalDate.now()));
        dataMap.put("should_hide_nino", isBenefitTypeValidToHideNino(caseData.getBenefitType()));

        return dataMap;
    }

    protected boolean isBenefitTypeValidToHideNino(Optional<Benefit> benefitType) {
        return benefitType.filter(benefit -> SscsType.SSCS2.equals(benefit.getSscsType())
                || SscsType.SSCS5.equals(benefit.getSscsType())).isPresent();
    }

    private String buildFullName(SscsCaseData caseData) {
        StringBuilder fullNameText = new StringBuilder();
        if (caseData.getAppeal().getAppellant().getIsAppointee() != null && caseData.getAppeal().getAppellant().getIsAppointee().equalsIgnoreCase("Yes") && caseData.getAppeal().getAppellant().getAppointee().getName() != null) {
            fullNameText.append(caseData.getAppeal().getAppellant().getAppointee().getName().getFullNameNoTitle());
            fullNameText.append(", appointee for ");
        }

        fullNameText.append(caseData.getAppeal().getAppellant().getName().getFullNameNoTitle());
        return fullNameText.toString();
    }

    private String getNextEvent(String documentType) {
        return NEXT_EVENT_MAP.get(documentType);
    }

    private String getEnglishNoticeType(String noticeType) {
        if (AUDIO_VIDEO_EVIDENCE_DIRECTION_NOTICE.getLabel().equals(noticeType)
            || POSTPONEMENT_REQUEST_DIRECTION_NOTICE.getLabel().equals(noticeType)) {
            return DIRECTION_NOTICE.getLabel();
        }
        return noticeType;
    }

    private String getWelshNoticeType(String noticeType) {
        if (noticeType.equalsIgnoreCase(DECISION_NOTICE.getLabel())) {
            return "Hysbysiad o Benderfyniad".toUpperCase();
        }
        if (noticeType.equalsIgnoreCase(DIRECTION_NOTICE.getLabel())) {
            return "Hysbysiad Cyfarwyddiadau".toUpperCase();
        }
        return noticeType.toUpperCase();
    }

    private void clearFields(SscsCaseData caseData) {
        caseData.setWelshBodyContent(null);
        caseData.setEnglishBodyContent(null);
    }
}
