package uk.gov.hmcts.reform.sscs.evidenceshare.service;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.docmosis.domain.DocumentHolder;
import uk.gov.hmcts.reform.sscs.docmosis.service.DocumentManagementService;
import uk.gov.hmcts.reform.sscs.evidenceshare.exception.PdfStoreException;
import uk.gov.hmcts.reform.sscs.evidenceshare.exception.UnableToContactThirdPartyException;
import uk.gov.hmcts.reform.sscs.exception.PdfGenerationException;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@Slf4j
@Service
public class DocumentManagementServiceWrapper {

    private final DocumentManagementService documentManagementService;
    private final CcdService ccdService;
    private final Integer maxRetryAttempts;

    @Autowired
    public DocumentManagementServiceWrapper(DocumentManagementService documentManagementService,
                                            CcdService ccdService,
                                            @Value("${send-letter.maxRetryAttempts}") Integer maxRetryAttempts) {
        this.documentManagementService = documentManagementService;
        this.ccdService = ccdService;
        this.maxRetryAttempts = maxRetryAttempts;
    }

    public void generateDocumentAndAddToCcd(DocumentHolder holder, SscsCaseData caseData, IdamTokens idamTokens) {
        generateDocumentAndAddToCcdWithRetry(holder, caseData, 1, idamTokens);
    }

    private void generateDocumentAndAddToCcdWithRetry(DocumentHolder holder, SscsCaseData caseData,
                                                      Integer reTryNumber, IdamTokens idamTokens) {
        try {
            SscsCaseDetails caseDetails = ccdService.getByCaseId(Long.valueOf(caseData.getCcdCaseId()), idamTokens);
            if (caseDetails == null || caseDetails.getData().getSscsDocument() == null
                || !checkIfDlDocumentAlreadyExists(caseDetails.getData().getSscsDocument())) {
                documentManagementService.generateDocumentAndAddToCcd(holder, caseData);
            }
        } catch (PdfGenerationException | ResourceAccessException e) {
            throw new UnableToContactThirdPartyException("docmosis", e);
        } catch (Exception e) {
            if (reTryNumber > maxRetryAttempts) {
                throw new PdfStoreException(e.getMessage(), e);
            }
            log.info(String.format("Caught recoverable error %s, retrying %s out of %s",
                e.getMessage(), reTryNumber, maxRetryAttempts));
            generateDocumentAndAddToCcdWithRetry(holder, caseData, reTryNumber + 1, idamTokens);
        }
    }

    public Boolean checkIfDlDocumentAlreadyExists(List<SscsDocument> sscsDocuments) {
        for (SscsDocument sscsDocument : sscsDocuments) {
            if (sscsDocument.getValue().getDocumentType() != null
                && (sscsDocument.getValue().getDocumentType().equalsIgnoreCase("dl6")
                || sscsDocument.getValue().getDocumentType().equalsIgnoreCase("dl16"))) {
                return true;
            }
        }
        return false;
    }

}
