package uk.gov.hmcts.reform.sscs.service;

import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ComparatorUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.AbstractDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.domain.wrapper.pdf.PdfState;
import uk.gov.hmcts.reform.sscs.pdf.PdfACompliance;
import uk.gov.hmcts.reform.sscs.pdf.PdfWatermarker;

@Component
@Slf4j
public abstract class AbstractFooterService<D extends AbstractDocument> {

    private static final String DM_STORE_USER_ID = "sscs";
    private final PdfStoreService pdfStoreService;
    private final PdfWatermarker alter;

    public AbstractFooterService(PdfStoreService pdfStoreService, PdfWatermarker alter) {
        this.pdfStoreService = pdfStoreService;
        this.alter = alter;
    }

    public FooterDetails addFooterToExistingToContentAndCreateNewUrl(DocumentLink url, List<D> documents, DocumentType documentType, String overrideFileName, String dateIssued) {
        String label = documentType.getLabel() != null ? documentType.getLabel() : documentType.getValue();
        FooterDetails footerDetails = null;
        if (url != null) {
            log.info(label + " adding footer appendix document link: {}", url);

            String bundleAddition = getNextBundleAddition(documents);

            boolean isPostHearingApp = Set.of(SET_ASIDE_APPLICATION,
                    CORRECTION_APPLICATION,
                    STATEMENT_OF_REASONS_APPLICATION,
                    LIBERTY_TO_APPLY_APPLICATION,
                    UPPER_TRIBUNALS_DECISION_REMADE,
                    UPPER_TRIBUNALS_DECISION_REFUSED,
                    PERMISSION_TO_APPEAL_APPLICATION)
                .contains(documentType);

            String verb = isPostHearingApp ? " received on " : " issued on ";

            String rightText = label + verb + dateIssued;
            String bundleFileName = overrideFileName != null ? overrideFileName : buildBundleAdditionFileName(bundleAddition, rightText);

            url = addFooter(url, label, bundleAddition);

            footerDetails = new FooterDetails(url, bundleAddition, bundleFileName);

        } else {
            log.info("Could not find {} document so skipping generating footer", label);
        }
        return footerDetails;
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

        SscsDocument sscsDocument = pdfStoreService.storeDocument(newContent, url.getDocumentFilename());

        if (sscsDocument != null) {
            String location = sscsDocument.getValue().getDocumentLink().getDocumentUrl();
            return url.toBuilder().documentUrl(location).documentBinaryUrl(location + "/binary")
                    .documentHash(sscsDocument.getValue().getDocumentLink().getDocumentHash()).build();
        } else {
            return null;
        }
    }

    public String getNextBundleAddition(List<D> sscsDocument) {
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
        if (appendixArray.length > 0) {
            String lastAppendix = appendixArray[appendixArray.length - 1];
            char nextChar = (char) (StringUtils.upperCase(lastAppendix).charAt(0) + 1);
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
        return pdfStoreService.download(documentUrl);
    }

    public PdfState isReadablePdf(String documentUrl) {
        try (PDDocument document = PDDocument.load(toBytes(documentUrl))) {

            PdfACompliance p1a = new PdfACompliance();
            p1a.makeCompliant(document);
            return PdfState.OK;

        } catch (InvalidPasswordException ipe) {
            log.error("Error while reading the encrypted PDF with URL:{}, Exception:{}", documentUrl, ipe.getMessage());
            return PdfState.PASSWORD_ENCRYPTED;
        } catch (Exception e) {
            log.error("Error while reading the PDF with URL:{}, Exception:{}", documentUrl, e.getMessage());
            return PdfState.UNREADABLE;
        }
    }
}
