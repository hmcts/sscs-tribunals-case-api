package uk.gov.hmcts.reform.sscs.util;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ISSUE_FINAL_DECISION;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ISSUE_FINAL_DECISION_WELSH;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.Event;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.model.docassembly.GenerateFileParams;
import uk.gov.hmcts.reform.sscs.model.docassembly.PdfRequestTemplateBody;

@Slf4j
public class PdfRequestUtils {
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static String requestDetails;
    private static String title;
    private static final StringBuilder additionalRequestDetails = new StringBuilder();

    private PdfRequestUtils() {
        //
    }

    @AllArgsConstructor
    private enum RequestPdfType {
        POSTPONEMENT("Postponement"),
        POST_HEARING("Post Hearing");

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
        return processRequestPdfAndSetPreviewDocument(RequestPdfType.POSTPONEMENT, userAuthorisation, sscsCaseData, response, generateFile, templateId);
    }

    public static PreSubmitCallbackResponse<SscsCaseData> processPostHearingRequestPdfAndSetPreviewDocument(
        String userAuthorisation,
        SscsCaseData sscsCaseData,
        PreSubmitCallbackResponse<SscsCaseData> response,
        GenerateFile generateFile,
        String templateId
    ) {
        return processRequestPdfAndSetPreviewDocument(RequestPdfType.POST_HEARING, userAuthorisation, sscsCaseData, response, generateFile, templateId);
    }

    public static PreSubmitCallbackResponse<SscsCaseData> processRequestPdfAndSetPreviewDocument(
        RequestPdfType requestPdfType,
        String userAuthorisation,
        SscsCaseData sscsCaseData,
        PreSubmitCallbackResponse<SscsCaseData> response,
        GenerateFile generateFile,
        String templateId
    ) {
        log.debug("Executing processRequestPdfAndSetPreviewDocument for caseId: {}", sscsCaseData.getCcdCaseId());

        additionalRequestDetails.append("Date request received: ").append(LocalDate.now().format(DATE_TIME_FORMATTER)).append("\n");

        switch (requestPdfType) {
            case POST_HEARING:
                handlePostHearing(sscsCaseData);
                break;
            case POSTPONEMENT:
                handlePostponement(sscsCaseData);
                break;
            default:
                throw new IllegalArgumentException("Unsupported event type for processRequestPdfAndSetPreviewDocument: " + requestPdfType);
        }

        if (isBlank(requestDetails)) {
            response.addError(String.format("Please enter request details to generate a %s request document", requestPdfType.toString().toLowerCase()));
            return response;
        }

        DocumentLink previewDocument = getPreviewDocument(requestPdfType, userAuthorisation, generateFile, templateId, title);

        switch (requestPdfType) {
            case POST_HEARING:
                sscsCaseData.getPostHearing().setPreviewDocument(previewDocument);
                break;
            case POSTPONEMENT:
                sscsCaseData.getPostponementRequest().setPostponementPreviewDocument(previewDocument);
                break;
            default:
                throw new IllegalArgumentException("Unsupported event type for processRequestPdfAndSetPreviewDocument: " + requestPdfType);
        }

        return response;
    }

    private static void handlePostponement(SscsCaseData sscsCaseData) {
        requestDetails = sscsCaseData.getPostponementRequest().getPostponementRequestDetails();
        String hearingVenue = sscsCaseData.getPostponementRequest().getPostponementRequestHearingVenue();
        LocalDate hearingDate = LocalDateTime.parse(sscsCaseData.getPostponementRequest().getPostponementRequestHearingDateAndTime()).toLocalDate();
        additionalRequestDetails.append("Date of Hearing: ").append(hearingDate.format(DATE_TIME_FORMATTER)).append("\n");
        additionalRequestDetails.append("Hearing Venue: ").append(hearingVenue).append("\n");
        additionalRequestDetails.append("Reason for Postponement Request: ").append(requestDetails).append("\n");
        title = "Postponement Request from FTA";
    }

    private static void handlePostHearing(SscsCaseData sscsCaseData) {
        requestDetails = sscsCaseData.getPostHearing().getRequestReason();
        Event latestIssueFinalDecision = SscsUtil.getLatestEventOfSpecifiedTypes(sscsCaseData, ISSUE_FINAL_DECISION, ISSUE_FINAL_DECISION_WELSH)
            .orElseThrow(() -> {
                log.error("latestIssueFinalDecision unexpectedly null for caseId: {}", sscsCaseData.getCcdCaseId());
                throw new IllegalArgumentException("latestIssueFinalDecision unexpectedly null for caseId: " + sscsCaseData.getCcdCaseId());
            });
        final LocalDate dateOfFinalDecision = latestIssueFinalDecision.getValue().getDateTime().toLocalDate();
        final String postHearingRequestType = sscsCaseData.getPostHearing().getRequestType().getDescriptionEn();
        additionalRequestDetails.append("Date of decision: ").append(dateOfFinalDecision.format(DATE_TIME_FORMATTER)).append("\n");
        additionalRequestDetails.append("Reason for ").append(postHearingRequestType).append(" request: ").append(requestDetails).append("\n");
        title = String.format("%s Application from FTA", postHearingRequestType);
    }

    private static DocumentLink getPreviewDocument(
        RequestPdfType requestPdfType,
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
            .documentFilename(requestPdfType + " Request.pdf")
            .documentBinaryUrl(generatedFileUrl + "/binary")
            .documentUrl(generatedFileUrl)
            .build();
    }

}
