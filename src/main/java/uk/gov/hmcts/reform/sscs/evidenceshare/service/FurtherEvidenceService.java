package uk.gov.hmcts.reform.sscs.evidenceshare.service;

import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;
import static uk.gov.hmcts.reform.sscs.evidenceshare.domain.FurtherEvidenceLetterType.*;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;
import uk.gov.hmcts.reform.sscs.evidenceshare.config.DocmosisTemplateConfig;
import uk.gov.hmcts.reform.sscs.evidenceshare.domain.FurtherEvidenceLetterType;
import uk.gov.hmcts.reform.sscs.evidenceshare.model.PdfDocument;
import uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderUtility;

@Service
@Slf4j
@SuppressWarnings("squid:S2201")
public class FurtherEvidenceService {

    private DocmosisTemplateConfig docmosisTemplateConfig;

    private CoverLetterService coverLetterService;

    private SscsDocumentService sscsDocumentService;

    private PrintService bulkPrintService;


    public FurtherEvidenceService(@Autowired CoverLetterService coverLetterService,
                                  @Autowired SscsDocumentService sscsDocumentService,
                                  @Autowired PrintService bulkPrintService,
                                  @Autowired DocmosisTemplateConfig docmosisTemplateConfig) {
        this.coverLetterService = coverLetterService;
        this.sscsDocumentService = sscsDocumentService;
        this.bulkPrintService = bulkPrintService;
        this.docmosisTemplateConfig = docmosisTemplateConfig;
    }

    public void issue(List<? extends AbstractDocument> sscsDocuments, SscsCaseData caseData, DocumentType documentType,
                      List<FurtherEvidenceLetterType> allowedLetterTypes, String otherPartyOriginalSenderId) {
        List<PdfDocument> pdfDocument = sscsDocumentService.getPdfsForGivenDocTypeNotIssued(sscsDocuments, documentType, isYes(caseData.getIsConfidentialCase()), otherPartyOriginalSenderId);
        List<PdfDocument> sizeNormalisedPdfDocuments = sscsDocumentService.sizeNormalisePdfs(pdfDocument);
        updateCaseDocuments(sizeNormalisedPdfDocuments.stream().map(PdfDocument::getDocument).collect(Collectors.toList()), caseData, documentType);
        List<Pdf> pdfs = sizeNormalisedPdfDocuments.stream().map(PdfDocument::getPdf).collect(Collectors.toList());

        if (!pdfs.isEmpty()) {
            send609_97_OriginalSender(caseData, documentType, pdfs, allowedLetterTypes, otherPartyOriginalSenderId);
            send609_98_partiesOnCase(caseData, documentType, pdfs, allowedLetterTypes, otherPartyOriginalSenderId);
            log.info("Sending documents to bulk print for ccd Id: {} and document type: {}", caseData.getCcdCaseId(), documentType);
        }
    }

    public void updateCaseDocuments(List<? extends AbstractDocument> documents, SscsCaseData caseData, DocumentType documentType) {
        List<SscsDocument> sscsCaseDocuments = caseData.getSscsDocument();

        for (AbstractDocument<AbstractDocumentDetails> doc : documents) {
            if (doc.getValue() != null
                && documentType.getValue().equals(doc.getValue().getDocumentType())
                && doc.getValue().getResizedDocumentLink() != null) {
                if (doc.getValue().getClass().isAssignableFrom(SscsDocumentDetails.class)) {
                    sscsCaseDocuments
                        .stream()
                        .filter(d -> d.getValue().getDocumentLink().getDocumentBinaryUrl().equals(doc.getValue().getDocumentLink().getDocumentBinaryUrl()))
                        .map(d -> {
                            DocumentLink resizedLink = doc.getValue().getResizedDocumentLink();
                            d.getValue().setResizedDocumentLink(resizedLink);
                            log.info("Sending resized document to bulk print link: DocumentLink(documentUrl= {} , documentFilename= {} and caseId {} )",
                                resizedLink.getDocumentUrl(), resizedLink.getDocumentFilename(), caseData.getCcdCaseId());
                            return d;
                        }).findFirst();
                }
            }
        }
    }


    protected void send609_97_OriginalSender(SscsCaseData caseData, DocumentType documentType, List<Pdf> pdfs,
                                             List<FurtherEvidenceLetterType> allowedLetterTypes, String otherPartyOriginalSenderId) {
        String docName = "609-97-template (original sender)";
        final FurtherEvidenceLetterType letterType = findLetterType(documentType);

        if (allowedLetterTypes.contains(letterType)) {
            byte[] bulkPrintList60997 = buildPdfsFor609_97(caseData, letterType, docName, otherPartyOriginalSenderId);
            String recipient = PlaceholderUtility.getName(caseData, letterType, otherPartyOriginalSenderId);
            bulkPrintService.sendToBulkPrint(buildPdfs(bulkPrintList60997, pdfs, docName), caseData, letterType, EventType.ISSUE_FURTHER_EVIDENCE, recipient);
        }
    }

    protected void send609_98_partiesOnCase(SscsCaseData caseData, DocumentType documentType, List<Pdf> pdfs,
                                            List<FurtherEvidenceLetterType> allowedLetterTypes, String otherPartyOriginalSenderId) {
        Multimap<FurtherEvidenceLetterType, String> otherPartiesMap = buildMapOfPartiesFor609_98(caseData, documentType, otherPartyOriginalSenderId);

        for (Map.Entry<FurtherEvidenceLetterType, String> party : otherPartiesMap.entries()) {
            String docName = party.getKey() == DWP_LETTER ? "609-98-template (FTA)" : "609-98-template (other parties)";

            if (allowedLetterTypes.contains(party.getKey())) {
                byte[] bulkPrintList60998 = buildPdfsFor609_98(caseData, party.getKey(), docName, party.getValue());
                List<Pdf> pdfs60998 = buildPdfs(bulkPrintList60998, pdfs, docName);
                String recipient = PlaceholderUtility.getName(caseData, party.getKey(), party.getValue());
                bulkPrintService.sendToBulkPrint(pdfs60998, caseData, party.getKey(), EventType.ISSUE_FURTHER_EVIDENCE, recipient);
            }
        }
    }

    private Multimap<FurtherEvidenceLetterType, String> buildMapOfPartiesFor609_98(SscsCaseData caseData, DocumentType documentType, String otherPartyOriginalSenderId) {
        Multimap<FurtherEvidenceLetterType, String> partiesMap = LinkedHashMultimap.create();

        if (documentType != APPELLANT_EVIDENCE) {
            partiesMap.put(APPELLANT_LETTER, null);
        }
        if (documentType != REPRESENTATIVE_EVIDENCE && checkRepExists(caseData)) {
            partiesMap.put(REPRESENTATIVE_LETTER, null);
        }
        if (documentType != JOINT_PARTY_EVIDENCE && checkJointPartyExists(caseData)) {
            partiesMap.put(JOINT_PARTY_LETTER, null);
        }

        if (caseData.getOtherParties() != null && caseData.getOtherParties().size() > 0) {
            for (CcdValue<OtherParty> otherParty : caseData.getOtherParties()) {
                addOtherPartyOrAppointeeTo609_98Map(otherPartyOriginalSenderId, otherParty, partiesMap);
                addOtherPartyRepTo609_98Map(otherPartyOriginalSenderId, otherParty, partiesMap);
            }
        }
        return partiesMap;
    }

    private void addOtherPartyOrAppointeeTo609_98Map(String otherPartyOriginalSenderId, CcdValue<OtherParty> otherParty, Multimap<FurtherEvidenceLetterType, String> partiesMap) {
        if ((otherPartyOriginalSenderId == null && !YesNo.isYes(otherParty.getValue().getIsAppointee()))
            || (otherPartyOriginalSenderId != null && !otherPartyOriginalSenderId.equals(otherParty.getValue().getId()) && !YesNo.isYes(otherParty.getValue().getIsAppointee()))) {
            partiesMap.put(OTHER_PARTY_LETTER, otherParty.getValue().getId());
        } else if (otherParty.getValue().getAppointee() != null
            && ((otherPartyOriginalSenderId == null && YesNo.isYes(otherParty.getValue().getIsAppointee())
            || (otherPartyOriginalSenderId != null && !otherPartyOriginalSenderId.equals(otherParty.getValue().getAppointee().getId()) && YesNo.isYes(otherParty.getValue().getIsAppointee()))))) {
            partiesMap.put(OTHER_PARTY_LETTER, otherParty.getValue().getAppointee().getId());
        }
    }

    private void addOtherPartyRepTo609_98Map(String otherPartyOriginalSenderId, CcdValue<OtherParty> otherParty, Multimap<FurtherEvidenceLetterType, String> partiesMap) {
        if (otherParty.getValue().getRep() != null
            && ((otherPartyOriginalSenderId == null && YesNo.isYes(otherParty.getValue().getRep().getHasRepresentative())
            || (otherPartyOriginalSenderId != null && !otherPartyOriginalSenderId.equals(otherParty.getValue().getRep().getId()) && YesNo.isYes(otherParty.getValue().getRep().getHasRepresentative()))))) {
            partiesMap.put(OTHER_PARTY_REP_LETTER, otherParty.getValue().getRep().getId());
        }
    }

    private boolean checkRepExists(SscsCaseData caseData) {
        Representative rep = caseData.getAppeal().getRep();
        return null != rep && "yes".equalsIgnoreCase(rep.getHasRepresentative());
    }

    private boolean checkJointPartyExists(SscsCaseData caseData) {
        return caseData.isThereAJointParty();
    }

    private FurtherEvidenceLetterType findLetterType(DocumentType documentType) {
        if (documentType == APPELLANT_EVIDENCE) {
            return APPELLANT_LETTER;
        } else if (documentType == REPRESENTATIVE_EVIDENCE) {
            return REPRESENTATIVE_LETTER;
        } else if (documentType == JOINT_PARTY_EVIDENCE) {
            return JOINT_PARTY_LETTER;
        } else if (documentType == OTHER_PARTY_EVIDENCE) {
            return OTHER_PARTY_LETTER;
        } else if (documentType == OTHER_PARTY_REPRESENTATIVE_EVIDENCE) {
            return OTHER_PARTY_REP_LETTER;
        } else {
            return DWP_LETTER;
        }
    }

    private List<Pdf> buildPdfs(byte[] coverLetterContent, List<Pdf> pdfsToBulkPrint, String pdfName) {
        List<Pdf> pdfs = new ArrayList<>(pdfsToBulkPrint);
        coverLetterService.appendCoverLetter(coverLetterContent, pdfs, pdfName);
        return pdfs;
    }

    private byte[] buildPdfsFor609_97(SscsCaseData caseData, FurtherEvidenceLetterType letterType, String pdfName, String otherPartyId) {
        return coverLetterService.generateCoverLetter(caseData, letterType,
            getTemplateNameBasedOnLanguagePreference(caseData.getLanguagePreference(), "d609-97"), pdfName, otherPartyId);
    }

    private byte[] buildPdfsFor609_98(SscsCaseData caseData, FurtherEvidenceLetterType letterType, String pdfName, String otherPartyId) {
        return coverLetterService.generateCoverLetter(caseData, letterType,
            getTemplateNameBasedOnLanguagePreference(caseData.getLanguagePreference(), "d609-98"), pdfName, otherPartyId);
    }

    public boolean canHandleAnyDocument(List<SscsDocument> sscsDocumentList) {
        return null != sscsDocumentList && sscsDocumentList.stream()
            .anyMatch(this::canHandleDocument);
    }

    private boolean canHandleDocument(SscsDocument sscsDocument) {
        return sscsDocument != null && sscsDocument.getValue() != null
            && "No".equals(sscsDocument.getValue().getEvidenceIssued())
            && null != sscsDocument.getValue().getDocumentType();
    }

    private String getTemplateNameBasedOnLanguagePreference(LanguagePreference languagePreference, String documentType) {
        return docmosisTemplateConfig.getTemplate().get(languagePreference)
            .get(documentType).get("name");
    }
}
