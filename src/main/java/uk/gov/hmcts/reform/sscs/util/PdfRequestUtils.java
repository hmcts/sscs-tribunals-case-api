package uk.gov.hmcts.reform.sscs.util;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.model.docassembly.GenerateFileParams;
import uk.gov.hmcts.reform.sscs.model.docassembly.PdfRequestTemplateBody;

@Slf4j
public class PdfRequestUtils {
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static String requestDetails;
    private static String title;
    private static final StringBuilder ADDITIONAL_REQUEST_DETAILS = new StringBuilder();

    private PdfRequestUtils() {
        //
    }

    @AllArgsConstructor
    private enum PdfType {
        POSTPONEMENT("Postponement Request"),
        POST_HEARING("Post hearing application");

        final String name;

        @Override
        public String toString() {
            return this.name;
        }
    }

    public static PreSubmitCallbackResponse<SscsCaseData> processPostponementRequestPdfAndSetPreviewDocument(
        String userAuthorisation,
        SscsCaseData sscsCaseData,
        PreSubmitCallbackResponse<SscsCaseData> response,
        GenerateFile generateFile,
        String templateId
    ) {
        return processRequestPdfAndSetPreviewDocument(PdfType.POSTPONEMENT, userAuthorisation, sscsCaseData, response, generateFile, templateId);
    }

    public static PreSubmitCallbackResponse<SscsCaseData> processPostHearingRequestPdfAndSetPreviewDocument(
        String userAuthorisation,
        SscsCaseData sscsCaseData,
        PreSubmitCallbackResponse<SscsCaseData> response,
        GenerateFile generateFile,
        String templateId
    ) {
        return processRequestPdfAndSetPreviewDocument(PdfType.POST_HEARING, userAuthorisation, sscsCaseData, response, generateFile, templateId);
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

        ADDITIONAL_REQUEST_DETAILS.append("Date request received: ").append(LocalDate.now().format(DATE_TIME_FORMATTER)).append("\n");

        switch (pdfType) {
            case POST_HEARING:
                handlePostHearing(sscsCaseData);
                break;
            case POSTPONEMENT:
                handlePostponement(sscsCaseData);
                break;
            default:
                throw new IllegalArgumentException("Unsupported event type for processRequestPdfAndSetPreviewDocument: " + pdfType);
        }

        if (isBlank(requestDetails)) {
            response.addError(String.format("Please enter request details to generate a %s document", pdfType.toString().toLowerCase()));
            return response;
        }

        DocumentLink previewDocument = getPreviewDocument(pdfType, userAuthorisation, generateFile, templateId, title);

        switch (pdfType) {
            case POST_HEARING:
                sscsCaseData.getPostHearing().setPreviewDocument(previewDocument);
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
        requestDetails = sscsCaseData.getPostponementRequest().getPostponementRequestDetails();
        String hearingVenue = sscsCaseData.getPostponementRequest().getPostponementRequestHearingVenue();
        LocalDate hearingDate = LocalDateTime.parse(sscsCaseData.getPostponementRequest().getPostponementRequestHearingDateAndTime()).toLocalDate();
        ADDITIONAL_REQUEST_DETAILS.append("Date of Hearing: ").append(hearingDate.format(DATE_TIME_FORMATTER)).append("\n");
        ADDITIONAL_REQUEST_DETAILS.append("Hearing Venue: ").append(hearingVenue).append("\n");
        ADDITIONAL_REQUEST_DETAILS.append("Reason for Postponement Request: ").append(requestDetails).append("\n");
        title = "Postponement Request from FTA";
    }

    private static void handlePostHearing(SscsCaseData sscsCaseData) {
        requestDetails = sscsCaseData.getPostHearing().getRequestReason();
        LocalDate issueFinalDecisionDate = sscsCaseData.getIssueFinalDecisionDate();
        if (issueFinalDecisionDate == null) {
            throw new IllegalArgumentException("issueFinalDecisionDate unexpectedly null for caseId: " + sscsCaseData.getCcdCaseId());
        }
        ADDITIONAL_REQUEST_DETAILS.append("Date of decision: ").append(issueFinalDecisionDate.format(DATE_TIME_FORMATTER)).append("\n");
        String postHearingRequestType = sscsCaseData.getPostHearing().getRequestType().getDescriptionEn();
        ADDITIONAL_REQUEST_DETAILS.append("Reason for ").append(postHearingRequestType).append(" request: ").append(requestDetails).append("\n");
        title = String.format("%s Application from FTA", postHearingRequestType);
    }

    private static DocumentLink getPreviewDocument(
        PdfType pdfType,
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
                .text(ADDITIONAL_REQUEST_DETAILS.toString())
                .build())
            .userAuthentication(userAuthorisation)
            .build();
        final String generatedFileUrl = generateFile.assemble(params);

        return DocumentLink.builder()
            .documentFilename(pdfType + ".pdf")
            .documentBinaryUrl(generatedFileUrl + "/binary")
            .documentUrl(generatedFileUrl)
            .build();
    }

}
