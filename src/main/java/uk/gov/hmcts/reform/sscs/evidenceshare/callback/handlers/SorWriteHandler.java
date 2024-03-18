package uk.gov.hmcts.reform.sscs.evidenceshare.callback.handlers;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.callback.CallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;
import uk.gov.hmcts.reform.sscs.evidenceshare.config.DocmosisTemplateConfig;
import uk.gov.hmcts.reform.sscs.evidenceshare.domain.FurtherEvidenceLetterType;
import uk.gov.hmcts.reform.sscs.evidenceshare.service.BulkPrintService;
import uk.gov.hmcts.reform.sscs.evidenceshare.service.CoverLetterService;
import uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.SorPlaceholderService;
import uk.gov.hmcts.reform.sscs.service.PdfStoreService;

@Slf4j
@Service
public class SorWriteHandler implements CallbackHandler<SscsCaseData> {
    public static final String POST_HEARING_APP_SOR_WRITTEN = "postHearingAppSorWritten";
    private String docmosisTemplate;

    private String docmosisCoverSheetTemplate;

    private final DocmosisTemplateConfig docmosisTemplateConfig;

    private final SorPlaceholderService sorPlaceholderService;

    private final BulkPrintService bulkPrintService;

    private final CoverLetterService coverLetterService;

    private final PdfStoreService pdfStoreService;

    @Autowired
    public SorWriteHandler(DocmosisTemplateConfig docmosisTemplateConfig, SorPlaceholderService sorPlaceholderService,
                           BulkPrintService bulkPrintService, CoverLetterService coverLetterService, PdfStoreService pdfStoreService) {
        this.docmosisTemplateConfig = docmosisTemplateConfig;
        this.sorPlaceholderService = sorPlaceholderService;
        this.bulkPrintService = bulkPrintService;
        this.coverLetterService = coverLetterService;
        this.pdfStoreService = pdfStoreService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbackType must not be null");

        return callbackType.equals(CallbackType.SUBMITTED)
            && callback.getEvent() == EventType.POST_HEARING_APP_SOR_WRITTEN;
    }

    @Override
    public void handle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        if (!canHandle(callbackType, callback)) {
            log.info("Cannot handle this event for case id: {}", callback.getCaseDetails().getId());
            throw new IllegalStateException("Cannot handle callback");
        }

        sendLetters(callback.getCaseDetails().getCaseData());
    }

    private void sendLetters(SscsCaseData caseData) {
        var template = docmosisTemplateConfig.getTemplate();
        LanguagePreference languagePreference = caseData.getLanguagePreference();
        docmosisTemplate = template.get(languagePreference).get(POST_HEARING_APP_SOR_WRITTEN).get("name");
        docmosisCoverSheetTemplate = template.get(languagePreference).get(POST_HEARING_APP_SOR_WRITTEN).get("cover");

        LinkedHashMap<Entity, FurtherEvidenceLetterType> parties = getParties(caseData);
        for (Map.Entry<Entity, FurtherEvidenceLetterType> entry : parties.entrySet()) {
            Entity party = entry.getKey();
            var partyId = party instanceof OtherParty || entry.getValue() == FurtherEvidenceLetterType.OTHER_PARTY_REP_LETTER ? party.getId() : null;
            var placeholders = sorPlaceholderService.populatePlaceholders(caseData,
                entry.getValue(),
                party.getClass().getSimpleName(),
                partyId);

            String letterName = String.format(LETTER_NAME, placeholders.get(ADDRESS_NAME), LocalDateTime.now());

            var generatedPdf = coverLetterService.generateCoverLetterRetry(entry.getValue(),
                docmosisTemplate, letterName, placeholders, 1);

            var coverSheet = coverLetterService.generateCoverSheet(docmosisCoverSheetTemplate,
                "coversheet", placeholders);

            final byte[] caseDocument = downloadDocument(caseData);

            var bundledLetter = bulkPrintService.buildBundledLetter(caseDocument, generatedPdf);
            bundledLetter = bulkPrintService.buildBundledLetter(coverSheet, bundledLetter);

            Pdf pdf = new Pdf(bundledLetter, letterName);
            List<Pdf> letter = new ArrayList<>();
            letter.add(pdf);

            String recipient = (String) placeholders.get(NAME);
            log.info("Party {} {}", party, party.getName());
            log.info("Sending letter to {}", recipient);
            log.info("Appellant name {} entity type {} name {}",
                placeholders.get(APPELLANT_NAME),
                placeholders.get(ENTITY_TYPE),
                placeholders.get(NAME));
            bulkPrintService.sendToBulkPrint(Long.parseLong(caseData.getCcdCaseId()), caseData, letter,
                EventType.POST_HEARING_APP_SOR_WRITTEN, recipient);
        }
    }


    private LinkedHashMap<Entity, FurtherEvidenceLetterType> getParties(SscsCaseData caseData) {
        var parties = new LinkedHashMap<Entity, FurtherEvidenceLetterType>();

        Appeal appeal = caseData.getAppeal();
        if (isYes(appeal.getAppellant().getIsAppointee())) {
            parties.put(appeal.getAppellant().getAppointee(), FurtherEvidenceLetterType.APPELLANT_LETTER);
        } else {
            parties.put(appeal.getAppellant(), FurtherEvidenceLetterType.APPELLANT_LETTER);
        }

        if (caseData.isThereAJointParty()) {
            parties.put(caseData.getJointParty(), FurtherEvidenceLetterType.JOINT_PARTY_LETTER);
        }

        if (!isNull(appeal.getRep()) && isYes(appeal.getRep().getHasRepresentative())) {
            parties.put(appeal.getRep(), FurtherEvidenceLetterType.REPRESENTATIVE_LETTER);
        }

        if (!isNull(caseData.getOtherParties()) && !caseData.getOtherParties().isEmpty()) {
            caseData.getOtherParties()
                .stream()
                .filter(p -> !isNull(p.getValue()))
                .map(CcdValue::getValue)
                .forEach(party -> {
                    if (party.hasAppointee()) {
                        parties.put(party.getAppointee(), FurtherEvidenceLetterType.APPELLANT_LETTER);
                    } else {
                        parties.put(party, FurtherEvidenceLetterType.OTHER_PARTY_LETTER);
                    }

                    if (party.hasRepresentative()) {
                        parties.put(party.getRep(), FurtherEvidenceLetterType.OTHER_PARTY_REP_LETTER);
                    }
                });
        }

        return parties;
    }

    private byte[] downloadDocument(SscsCaseData caseData) {
        byte[] caseDocument = null;
        SscsDocument document = caseData.getLatestDocumentForDocumentType(DocumentType.STATEMENT_OF_REASONS);

        String documentUrl = document.getValue()
            .getDocumentLink().getDocumentUrl();

        if (null != documentUrl) {
            caseDocument = pdfStoreService.download(documentUrl);
        }
        return caseDocument;
    }

    @Override
    public DispatchPriority getPriority() {
        return DispatchPriority.LATEST;
    }
}
