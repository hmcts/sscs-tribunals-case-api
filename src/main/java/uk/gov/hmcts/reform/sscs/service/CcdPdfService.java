package uk.gov.hmcts.reform.sscs.service;

import static java.util.Collections.singletonList;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.UPLOAD_DOCUMENT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.ScannedDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.ScannedDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentTranslationStatus;
import uk.gov.hmcts.reform.sscs.ccd.exception.CcdException;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.domain.UpdateDocParams;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@Service
@Slf4j
public class CcdPdfService {

    private static final String APPELLANT_STATEMENT = "Appellant statement ";
    private static final String REPRESENTATIVE_STATEMENT = "Representative statement ";
    private static final String OTHER_PARTY_STATEMENT = "Other party statement ";
    private static final String OTHER_PARTY_REPRESENTATIVE_STATEMENT = "Other party representative statement ";
    private static final String UPLOADED_DOCUMENT_INTO_SSCS = "Uploaded document into SSCS";
    public static final String YES = "Yes";

    private PdfStoreService pdfStoreService;

    private CcdService ccdService;

    @Autowired
    public CcdPdfService(PdfStoreService pdfStoreService, CcdService ccdService) {
        this.pdfStoreService = pdfStoreService;
        this.ccdService = ccdService;
    }

    // can be removed once COR team decide what to pass into the documentType field
    public SscsCaseData mergeDocIntoCcd(String fileName, byte[] pdf, Long caseId, SscsCaseData caseData,
                                        IdamTokens idamTokens) {
        return updateAndMerge(fileName, pdf, caseId, caseData, idamTokens, UPLOADED_DOCUMENT_INTO_SSCS,
                null);
    }

    public SscsCaseData mergeDocIntoCcd(UpdateDocParams updateDocParams, IdamTokens idamTokens) {
        updateDoc(updateDocParams);
        return updateCaseInCcd(updateDocParams.getCaseData(), updateDocParams.getCaseId(), UPLOAD_DOCUMENT.getCcdType(), idamTokens, UPLOADED_DOCUMENT_INTO_SSCS).getData();
    }

    public SscsCaseData mergeDocIntoCcd(String fileName, byte[] pdf, Long caseId, SscsCaseData caseData,
                                        IdamTokens idamTokens, String documentType) {
        return updateAndMerge(fileName, pdf, caseId, caseData, idamTokens, UPLOADED_DOCUMENT_INTO_SSCS,
                documentType);
    }

    public SscsCaseData mergeDocIntoCcd(String fileName, byte[] pdf, Long caseId, SscsCaseData caseData,
                                        IdamTokens idamTokens, String description, String documentType) {
        return updateAndMerge(fileName, pdf, caseId, caseData, idamTokens, description, documentType);
    }

    private SscsCaseData updateAndMerge(String fileName, byte[] pdf, Long caseId, SscsCaseData caseData,
                                        IdamTokens idamTokens, String description, String documentType) {
        updateDoc(fileName, pdf, caseId, caseData, documentType);
        return updateCaseInCcd(caseData, caseId, UPLOAD_DOCUMENT.getCcdType(), idamTokens, description).getData();
    }

    public SscsCaseData updateDoc(String fileName, byte[] pdf, Long caseId, SscsCaseData caseData, String documentType) {
        return this.updateDoc(fileName, pdf, caseId, caseData, documentType, null);
    }

    public SscsCaseData updateDoc(String fileName, byte[] pdf, Long caseId, SscsCaseData caseData, String documentType, SscsDocumentTranslationStatus documentTranslationStatus) {
        return this.updateDoc(UpdateDocParams.builder().fileName(fileName)
                .pdf(pdf)
                .caseId(caseId)
                .caseData(caseData)
                .documentType(documentType)
                .documentTranslationStatus(documentTranslationStatus)
                .build());
    }

    public SscsCaseData updateDoc(UpdateDocParams updateDocParams) {
        SscsDocument pdfDocuments = pdfStoreService.storeDocument(updateDocParams);

        if (pdfDocuments == null) {
            log.info("Case {} PDF stored in DM for benefit type {}", updateDocParams.getCaseId(),
                    updateDocParams.getCaseData().getAppeal().getBenefitType().getCode());
        }

        if (updateDocParams.getCaseId() == null) {
            log.info("caseId is empty - skipping step to update CCD with PDF");
            return updateDocParams.getCaseData();
        }
        updateCaseDataWithNewDoc(updateDocParams.getFileName(), updateDocParams.getCaseData(), pdfDocuments == null ? null : singletonList(pdfDocuments));
        if (SscsDocumentTranslationStatus.TRANSLATION_REQUIRED.equals(updateDocParams.getDocumentTranslationStatus())) {
            updateDocParams.getCaseData().setTranslationWorkOutstanding(YES);
        }
        return updateDocParams.getCaseData();
    }

    private void updateCaseDataWithNewDoc(String fileName, SscsCaseData caseData, List<SscsDocument> pdfDocuments) {
        if (fileName.startsWith(APPELLANT_STATEMENT) || fileName.startsWith(REPRESENTATIVE_STATEMENT) ||  fileName.startsWith(OTHER_PARTY_STATEMENT) || fileName.startsWith(OTHER_PARTY_REPRESENTATIVE_STATEMENT)) {
            caseData.setScannedDocuments(ListUtils.union(emptyIfNull(caseData.getScannedDocuments()),
                    buildScannedDocListFromSscsDoc(pdfDocuments)));
            caseData.setEvidenceHandled(NO.getValue());
        } else {
            caseData.setSscsDocument(ListUtils.union(emptyIfNull(caseData.getSscsDocument()),
                    emptyIfNull(pdfDocuments)));
        }
    }

    private List<ScannedDocument> buildScannedDocListFromSscsDoc(List<SscsDocument> pdfDocuments) {
        if (CollectionUtils.isEmpty(pdfDocuments)) {
            return Collections.emptyList();
        }
        SscsDocumentDetails pdfDocDetails = pdfDocuments.get(0).getValue();

        final String dateAdded;
        if (pdfDocDetails.getDocumentDateAdded() != null) {
            dateAdded = getDocumentDateAdded(pdfDocDetails);
        } else {
            dateAdded = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
        }

        ScannedDocument scannedDoc = ScannedDocument.builder()
                .value(ScannedDocumentDetails.builder()
                        .fileName(pdfDocDetails.getDocumentFileName())
                        .url(pdfDocDetails.getDocumentLink())
                        .scannedDate(dateAdded)
                        .originalSenderOtherPartyId(pdfDocDetails.getOriginalSenderOtherPartyId())
                        .originalSenderOtherPartyName(pdfDocDetails.getOriginalSenderOtherPartyName())
                        .type("other")
                        .build())
                .build();
        return singletonList(scannedDoc);
    }

    private String getDocumentDateAdded(SscsDocumentDetails pdfDocDetails) {
        if (LocalDate.parse(pdfDocDetails.getDocumentDateAdded()).isEqual(LocalDate.now())) {
            return LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
        } else {
            return LocalDate.parse(pdfDocDetails.getDocumentDateAdded()).atStartOfDay().format(DateTimeFormatter.ISO_DATE_TIME);
        }
    }

    private SscsCaseDetails updateCaseInCcd(SscsCaseData caseData, Long caseId, String eventId, IdamTokens idamTokens,
                                            String description) {
        try {
            return ccdService.updateCase(caseData, caseId, eventId, "SSCS - upload document event",
                    description, idamTokens);
        } catch (CcdException ccdEx) {
            log.error("Failed to update ccd case but carrying on [" + caseId + "] ["
                    + caseData.getCaseReference() + "] with event [" + eventId + "]", ccdEx);
            return SscsCaseDetails.builder().build();
        }
    }

}
