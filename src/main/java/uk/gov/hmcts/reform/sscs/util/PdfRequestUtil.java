package uk.gov.hmcts.reform.sscs.util;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.PostHearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.PostponementRequest;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.model.docassembly.GenerateFileParams;
import uk.gov.hmcts.reform.sscs.model.docassembly.PdfRequestTemplateBody;

@Slf4j
public class PdfRequestUtil {
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static String requestDetails;
    private static String title;
    private static StringBuilder additionalRequestDetails;

    private PdfRequestUtil() {
        //
    }

    @AllArgsConstructor
    public enum PdfType {
        POSTPONEMENT("Postponement Request"),
        POST_HEARING("Post Hearing Application");

        final String name;

        @Override
        public String toString() {
            return this.name;
        }
    }

    public static PreSubmitCallbackResponse<SscsCaseData> processRequestPdfAndSetPreviewDocument(
        PdfType pdfType,
        String userAuthorisation,
        SscsCaseData sscsCaseData,
        PreSubmitCallbackResponse<SscsCaseData> response,
        GenerateFile generateFile,
        String templateId
    ) {
        log.debug("Executing processRequestPdfAndSetPreviewDocument for caseId: {}", sscsCaseData.getCcdCaseId());

        additionalRequestDetails = new StringBuilder();
        additionalRequestDetails.append("Date request received: ").append(LocalDate.now().format(DATE_TIME_FORMATTER)).append("\n");
        StringBuilder pdfUrlBuilder = new StringBuilder();
        switch (pdfType) {
            case POST_HEARING:
                String postHearingDocumentTypeLabel = getPostHearingDocumentType(sscsCaseData).getLabel();
                pdfUrlBuilder.append(postHearingDocumentTypeLabel)
                    .append(" from FTA");
                handlePostHearing(sscsCaseData);
                break;
            case POSTPONEMENT:
                pdfUrlBuilder.append("Postponement Request");
                handlePostponement(sscsCaseData);
                break;
            default:
                throw new IllegalArgumentException("Unsupported pdf type for processRequestPdfAndSetPreviewDocument: " + pdfType);
        }
        pdfUrlBuilder.append(".pdf");

        if (isBlank(requestDetails)) {
            String responseErrorMsg = String.format("Please enter request details to generate a %s document", pdfType.toString().toLowerCase());
            response.addError(responseErrorMsg);
            return response;
        }

        DocumentLink previewDocument = getPreviewDocument(pdfUrlBuilder.toString(), userAuthorisation, generateFile, templateId, title);

        switch (pdfType) {
            case POST_HEARING:
                sscsCaseData.getDocumentStaging().setPreviewDocument(previewDocument);
                break;
            case POSTPONEMENT:
                sscsCaseData.getPostponementRequest().setPostponementPreviewDocument(previewDocument);
                break;
            default:
                throw new IllegalArgumentException("Unsupported event type for processRequestPdfAndSetPreviewDocument: " + pdfType);
        }

        return response;
    }

    private static void handlePostponement(SscsCaseData sscsCaseData) {
        PostponementRequest postponementRequest = sscsCaseData.getPostponementRequest();
        requestDetails = postponementRequest.getPostponementRequestDetails();
        String hearingVenue = postponementRequest.getPostponementRequestHearingVenue();
        LocalDate hearingDate = LocalDateTime.parse(postponementRequest.getPostponementRequestHearingDateAndTime()).toLocalDate();
        additionalRequestDetails.append("Date of Hearing: ")
            .append(hearingDate.format(DATE_TIME_FORMATTER))
            .append("\n")
            .append("Hearing Venue: ")
            .append(hearingVenue)
            .append("\n")
            .append("Reason for Postponement Request: ")
            .append(requestDetails)
            .append("\n");
        title = "Postponement Request from FTA";
    }

    private static void handlePostHearing(SscsCaseData sscsCaseData) {
        requestDetails = sscsCaseData.getDocumentGeneration().getBodyContent();
        LocalDate issueFinalDecisionDate = sscsCaseData.getIssueFinalDecisionDate();
        if (issueFinalDecisionDate == null) {
            throw new IllegalArgumentException("issueFinalDecisionDate unexpectedly null for caseId: " + sscsCaseData.getCcdCaseId());
        }
        String postHearingRequestType = sscsCaseData.getPostHearing().getRequestType().getDescriptionEn();
        additionalRequestDetails.append("Date of decision issued: ")
            .append(issueFinalDecisionDate.format(DATE_TIME_FORMATTER))
            .append("\n")
            .append("Reason for ")
            .append(postHearingRequestType)
            .append(" application: ")
            .append(requestDetails)
            .append("\n");
        title = String.format("%s Application from %s", postHearingRequestType, "FTA");
    }

    private static DocumentLink getPreviewDocument(
        String pdfUrl,
        String userAuthorisation,
        GenerateFile generateFile,
        String templateId,
        String title
    ) {
        GenerateFileParams params = GenerateFileParams.builder()
            .renditionOutputLocation(null)
            .templateId(templateId)
            .formPayload(PdfRequestTemplateBody.builder()
                .title(title)
                .text(additionalRequestDetails.toString())
                .build())
            .userAuthentication(userAuthorisation)
            .build();
        final String generatedFileUrl = generateFile.assemble(params);

        return DocumentLink.builder()
            .documentFilename(pdfUrl)
            .documentBinaryUrl(generatedFileUrl + "/binary")
            .documentUrl(generatedFileUrl)
            .build();
    }

    public static DocumentType getPostHearingDocumentType(SscsCaseData sscsCaseData) {
        PostHearing postHearing = sscsCaseData.getPostHearing();
        DocumentType documentType;

        switch (postHearing.getRequestType()) {
            case SET_ASIDE:
                documentType = DocumentType.SET_ASIDE_APPLICATION;
                break;
            default:
                throw new IllegalArgumentException("Unexpected request type: " + postHearing.getRequestType());
        }
        return documentType;
    }

}
