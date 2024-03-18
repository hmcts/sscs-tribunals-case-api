package uk.gov.hmcts.reform.sscs.evidenceshare.service;

import static java.lang.String.format;
import static java.util.Base64.getEncoder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import uk.gov.hmcts.reform.sendletter.api.LetterWithPdfsRequest;
import uk.gov.hmcts.reform.sendletter.api.SendLetterApi;
import uk.gov.hmcts.reform.sendletter.api.SendLetterResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;
import uk.gov.hmcts.reform.sscs.evidenceshare.domain.FurtherEvidenceLetterType;
import uk.gov.hmcts.reform.sscs.evidenceshare.exception.BulkPrintException;
import uk.gov.hmcts.reform.sscs.evidenceshare.exception.NonPdfBulkPrintException;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

@Service
@Slf4j
@ConditionalOnProperty(prefix = "send-letter", name = "url")
public class BulkPrintService implements PrintService {

    private static final String XEROX_TYPE_PARAMETER = "SSCS001";
    private static final String CASE_IDENTIFIER = "caseIdentifier";
    private static final String LETTER_TYPE_KEY = "letterType";
    private static final String APPELLANT_NAME = "appellantName";
    public static final String RECIPIENTS = "recipients";

    private final SendLetterApi sendLetterApi;
    private final IdamService idamService;
    private final boolean sendLetterEnabled;
    private final Integer maxRetryAttempts;
    private final BulkPrintServiceHelper bulkPrintServiceHelper;
    private final CcdNotificationService ccdNotificationService;

    @Autowired
    public BulkPrintService(SendLetterApi sendLetterApi,
                            IdamService idamService,
                            BulkPrintServiceHelper bulkPrintServiceHelper,
                            @Value("${send-letter.enabled}") boolean sendLetterEnabled,
                            @Value("${send-letter.maxRetryAttempts}") Integer maxRetryAttempts, CcdNotificationService ccdNotificationService) {
        this.idamService = idamService;
        this.bulkPrintServiceHelper = bulkPrintServiceHelper;
        this.sendLetterApi = sendLetterApi;
        this.sendLetterEnabled = sendLetterEnabled;
        this.maxRetryAttempts = maxRetryAttempts;
        this.ccdNotificationService = ccdNotificationService;
    }

    public Optional<UUID> sendToBulkPrint(List<Pdf> pdfs, final SscsCaseData sscsCaseData, FurtherEvidenceLetterType letterType, EventType event, String recipient) {
        if (bulkPrintServiceHelper.sendForReasonableAdjustment(sscsCaseData, letterType)) {
            log.info("Sending to bulk print service {} reasonable adjustments", sscsCaseData.getCcdCaseId());
            bulkPrintServiceHelper.saveAsReasonableAdjustment(sscsCaseData, pdfs, letterType);
        } else {
            return sendToBulkPrint(pdfs, sscsCaseData, recipient);
        }

        return Optional.empty();
    }

    public Optional<UUID> sendToBulkPrint(List<Pdf> pdfs, final SscsCaseData sscsCaseData, String recipient)
        throws BulkPrintException {
        if (sendLetterEnabled) {
            List<String> encodedData = new ArrayList<>();
            for (Pdf pdf : pdfs) {
                encodedData.add(getEncoder().encodeToString(pdf.getContent()));
            }
            final String authToken = idamService.generateServiceAuthorization();
            return sendLetterWithRetry(authToken, sscsCaseData, encodedData, 1, recipient);
        }
        return Optional.empty();
    }

    public Optional<UUID> sendToBulkPrint(long caseId, SscsCaseData caseData, List<Pdf> pdfs, EventType eventType, String recipient) {
        Optional<UUID> id = sendToBulkPrint(pdfs, caseData, recipient);
        Pdf letter = pdfs.get(0);

        if (id.isPresent()) {
            ccdNotificationService.storeNotificationLetterIntoCcd(eventType, letter.getContent(), caseId, recipient);
            log.info("Letter was sent for event {} and case {}, send-letter-service id {}", eventType.getCcdType(), caseId, id.get());
        } else {
            log.error("Failed to send to bulk print for case {}. No print id returned", caseId);
        }

        return id;
    }

    public byte[] buildBundledLetter(byte[] coverSheet, byte[] letter) {
        if (coverSheet != null) {
            PDDocument bundledLetter;

            try {
                bundledLetter = PDDocument.load(letter);
                PDDocument loadDoc = PDDocument.load(coverSheet);

                final PDFMergerUtility merger = new PDFMergerUtility();
                merger.appendDocument(bundledLetter, loadDoc);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bundledLetter.save(baos);
                bundledLetter.close();

                return baos.toByteArray();
            } catch (IOException e) {
                log.info("Failed to merge letter and coversheet with exception {}", e.getMessage());
            }
        }

        return letter;
    }

    private Optional<UUID> sendLetterWithRetry(String authToken, SscsCaseData sscsCaseData, List<String> encodedData,
                                               Integer reTryNumber, String recipient) {
        try {
            return sendLetter(authToken, sscsCaseData, encodedData, recipient);
        } catch (HttpClientErrorException e) {
            log.info(format("Failed to send to bulk print for case %s with error %s. Non-pdf's/broken pdf's seen in list of documents, please correct.",
                sscsCaseData.getCcdCaseId(), e.getMessage()));
            throw new NonPdfBulkPrintException(e);

        } catch (Exception e) {
            if (reTryNumber > maxRetryAttempts) {
                String message = format("Failed to send to bulk print for case %s with error %s.",
                    sscsCaseData.getCcdCaseId(), e.getMessage());
                throw new BulkPrintException(message, e);
            }
            log.info(String.format("Caught recoverable error %s, retrying %s out of %s",
                e.getMessage(), reTryNumber, maxRetryAttempts));
            return sendLetterWithRetry(authToken, sscsCaseData, encodedData, reTryNumber + 1, recipient);
        }
    }

    private Optional<UUID> sendLetter(String authToken, SscsCaseData sscsCaseData, List<String> encodedData, String recipient) {
        SendLetterResponse sendLetterResponse = sendLetterApi.sendLetter(
            authToken,
            new LetterWithPdfsRequest(
                encodedData,
                XEROX_TYPE_PARAMETER,
                getAdditionalData(sscsCaseData, recipient)
            )
        );
        log.info("Letter service produced the following letter Id {} for case {}",
            sendLetterResponse.letterId, sscsCaseData.getCcdCaseId());

        return Optional.of(sendLetterResponse.letterId);
    }

    private static Map<String, Object> getAdditionalData(final SscsCaseData sscsCaseData, String recipient) {
        Map<String, Object> additionalData = new HashMap<>();
        additionalData.put(LETTER_TYPE_KEY, "sscs-data-pack");
        additionalData.put(CASE_IDENTIFIER, sscsCaseData.getCcdCaseId());
        additionalData.put(APPELLANT_NAME, sscsCaseData.getAppeal().getAppellant().getName().getFullNameNoTitle());
        additionalData.put(RECIPIENTS, getRecipients(recipient));
        return additionalData;
    }

    private static List<String> getRecipients(String recipient) {
        List<String> parties = new ArrayList<>();
        parties.add(recipient);
        return parties;
    }
}
