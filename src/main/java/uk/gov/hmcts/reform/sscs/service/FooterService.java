package uk.gov.hmcts.reform.sscs.service;

import static java.util.Collections.singletonList;
import static org.springframework.http.MediaType.APPLICATION_PDF;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ComparatorUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.document.domain.UploadResponse;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentTranslationStatus;
import uk.gov.hmcts.reform.sscs.domain.pdf.ByteArrayMultipartFile;
import uk.gov.hmcts.reform.sscs.pdf.PdfWatermarker;

@Component
@Slf4j
public class FooterService {

    private static final String DM_STORE_USER_ID = "sscs";
    private final EvidenceManagementService evidenceManagementService;
    private final PdfWatermarker alter;

    @Autowired
    public FooterService(EvidenceManagementService evidenceManagementService, PdfWatermarker alter) {
        this.evidenceManagementService = evidenceManagementService;
        this.alter = alter;
    }

    public void createFooterAndAddDocToCase(DocumentLink url, SscsCaseData caseData, DocumentType documentType, String dateIssued, LocalDate dateAdded, String overrideFileName) {
        this.createFooterAndAddDocToCase(url,caseData, documentType, dateIssued, dateAdded, overrideFileName, null); ;
    }

    public void createFooterAndAddDocToCase(DocumentLink url, SscsCaseData caseData, DocumentType documentType, String dateIssued, LocalDate dateAdded, String overrideFileName, SscsDocumentTranslationStatus documentTranslationStatus) {
        String label = documentType.getLabel() != null ? documentType.getLabel() : documentType.getValue();
        if (url != null) {
            log.info(label + " adding footer appendix document link: {} and caseId {}", url, caseData.getCcdCaseId());

            String bundleAddition = getNextBundleAddition(caseData.getSscsDocument());

            String bundleFileName = overrideFileName != null ? overrideFileName : buildBundleAdditionFileName(bundleAddition, label + " issued on " + dateIssued);

            SscsDocument sscsDocument = createFooterDocument(url, label, bundleAddition, bundleFileName,
                    dateAdded, documentType, documentTranslationStatus);

            List<SscsDocument> documents = new ArrayList<>();
            documents.add(sscsDocument);

            if (caseData.getSscsDocument() != null) {
                documents.addAll(caseData.getSscsDocument());
            }
            caseData.setSscsDocument(documents);
        } else {
            log.info("Could not find {} document for caseId {} so skipping generating footer", label, caseData.getCcdCaseId());
        }
    }

    private SscsDocument createFooterDocument(DocumentLink url, String leftText, String bundleAddition, String documentFileName,
                                                LocalDate dateAdded, DocumentType documentType, SscsDocumentTranslationStatus documentTranslationStatus) {
        url = addFooter(url, leftText, bundleAddition);

        return SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentFileName(documentFileName)
                .documentLink(url)
                .bundleAddition(bundleAddition)
                .documentDateAdded(Optional.ofNullable(dateAdded).orElse(LocalDate.now()).format(DateTimeFormatter.ISO_DATE))
                .documentType(documentType.getValue())
                .documentTranslationStatus(documentTranslationStatus)
                .build())
                .build();
    }

    public DocumentLink addFooter(DocumentLink url, String leftText, String rightText) {

        byte[] oldContent = toBytes(url.getDocumentUrl());
        byte[] newContent;

        try {
            newContent = alter.shrinkAndWatermarkPdf(oldContent, leftText, String.format("Addition %s", rightText));
        } catch (Exception e) {
            log.error("Caught exception :" + e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }

        ByteArrayMultipartFile file = ByteArrayMultipartFile.builder()
                .content(newContent)
                .name(url.getDocumentFilename())
                .contentType(APPLICATION_PDF).build();

        UploadResponse uploadResponse = evidenceManagementService.upload(singletonList(file), DM_STORE_USER_ID);

        if (uploadResponse != null) {
            String location = uploadResponse.getEmbedded().getDocuments().get(0).links.self.href;
            return url.toBuilder().documentUrl(location).documentBinaryUrl(location + "/binary").build();
        } else {
            return null;
        }
    }

    public String getNextBundleAddition(List<SscsDocument> sscsDocument) {
        if (sscsDocument == null) {
            sscsDocument = new ArrayList<>();
        }
        String[] appendixArray = sscsDocument.stream().filter(s -> StringUtils.isNotEmpty(s.getValue().getBundleAddition())).map(s -> StringUtils.stripToEmpty(s.getValue().getBundleAddition())).toArray(String[]::new);
        Arrays.sort(appendixArray, (o1, o2) -> {
            if (StringUtils.isNotEmpty(o1) && StringUtils.isNotEmpty(o2) && o1.length() > 1 && o2.length() > 1) {
                Integer n1 = NumberUtils.isCreatable(o1.substring(1)) ? Integer.parseInt(o1.substring(1)) : 0;
                Integer n2 = NumberUtils.isCreatable(o2.substring(1)) ? Integer.parseInt(o2.substring(1)) : 0;
                return ComparatorUtils.<Integer>naturalComparator().compare(n1, n2);
            }
            return ComparatorUtils.<String>naturalComparator().compare(o1, o2);
        });
        if (appendixArray.length >  0) {
            String lastAppendix = appendixArray[appendixArray.length - 1];
            char nextChar =  (char) (StringUtils.upperCase(lastAppendix).charAt(0) + 1);
            if (nextChar > 'Z') {
                if (lastAppendix.length() == 1) {
                    return "Z1";
                } else if (NumberUtils.isCreatable(lastAppendix.substring(1))) {
                    return "Z" + (Integer.valueOf(lastAppendix.substring(1)) + 1);
                }
            }
            return String.valueOf(nextChar);
        }

        return "A";
    }

    protected String buildBundleAdditionFileName(String bundleAddition, String rightText) {
        return "Addition " + bundleAddition + " - " + rightText + ".pdf";
    }

    private byte[] toBytes(String documentUrl) {
        return evidenceManagementService.download(
                URI.create(documentUrl),
                DM_STORE_USER_ID
        );
    }
}
