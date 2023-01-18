package uk.gov.hmcts.reform.sscs.util;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ISSUE_FINAL_DECISION;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ISSUE_FINAL_DECISION_WELSH;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.LIST_ASSIST;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentGeneration;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentStaging;
import uk.gov.hmcts.reform.sscs.ccd.domain.Event;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SchedulingAndListingFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.model.docassembly.GenerateFileParams;
import uk.gov.hmcts.reform.sscs.model.docassembly.PdfRequestTemplateBody;

@Slf4j
public class SscsUtil {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private SscsUtil() {
        //
    }

    public static <T> List<T> mutableEmptyListIfNull(List<T> list) {
        return Optional.ofNullable(list).orElse(new ArrayList<>());
    }

    public static PreSubmitCallbackResponse<SscsCaseData> processPostponementRequestPdfAndSetPreviewDocument(String userAuthorisation,
        SscsCaseData sscsCaseData,
        PreSubmitCallbackResponse<SscsCaseData> response,
        GenerateFile generateFile,
        String templateId) {

        return processRequestPdfAndSetPreviewDocument(RequestPdfType.POSTPONEMENT, userAuthorisation, sscsCaseData, response, generateFile, templateId);
    }

    public static PreSubmitCallbackResponse<SscsCaseData> processPostHearingRequestPdfAndSetPreviewDocument(String userAuthorisation,
        SscsCaseData sscsCaseData,
        PreSubmitCallbackResponse<SscsCaseData> response,
        GenerateFile generateFile,
        String templateId) {

        return processRequestPdfAndSetPreviewDocument(RequestPdfType.POST_HEARING, userAuthorisation, sscsCaseData, response, generateFile, templateId);
    }

    private enum RequestPdfType {
        POSTPONEMENT("Postponement"),
        POST_HEARING("Post Hearing");

        RequestPdfType(String name) {
            this.name = name;
        }

        final String name;

        @Override
        public String toString() {
            return this.name;
        }
    }

    public static PreSubmitCallbackResponse<SscsCaseData> processRequestPdfAndSetPreviewDocument(
        RequestPdfType requestPdfType,
        String userAuthorisation,
        SscsCaseData sscsCaseData,
        PreSubmitCallbackResponse<SscsCaseData> response,
        GenerateFile generateFile,
        String templateId
    ) {
        final String caseId = sscsCaseData.getCcdCaseId();
        log.debug("Executing processRequestPdfAndSetPreviewDocument for caseId: {}", caseId);

        String requestDetails;
        String title;
        final StringBuilder additionalRequestDetails = new StringBuilder();
        additionalRequestDetails.append("Date request received: ").append(LocalDate.now().format(DATE_TIME_FORMATTER)).append("\n");

        switch (requestPdfType) {
            case POST_HEARING:
                requestDetails = sscsCaseData.getPostHearing().getRequestReason();
                Event latestIssueFinalDecision = getLatestEventOfSpecifiedTypes(sscsCaseData, ISSUE_FINAL_DECISION, ISSUE_FINAL_DECISION_WELSH)
                    .orElseThrow(() -> {
                        log.error("latestIssueFinalDecision unexpectedly null for caseId: {}", caseId);
                        throw new IllegalArgumentException("latestIssueFinalDecision unexpectedly null for caseId: " + caseId);
                    });
                final LocalDate dateOfFinalDecision = latestIssueFinalDecision.getValue().getDateTime().toLocalDate();
                final String postHearingRequestType = sscsCaseData.getPostHearing().getRequestType().getDescriptionEn();
                additionalRequestDetails.append("Date of decision: ").append(dateOfFinalDecision.format(DATE_TIME_FORMATTER)).append("\n");
                additionalRequestDetails.append("Reason for ").append(postHearingRequestType).append(" request: ").append(requestDetails).append("\n");
                title = String.format("%s Application from FTA", postHearingRequestType);
                break;
            case POSTPONEMENT:
                requestDetails = sscsCaseData.getPostponementRequest().getPostponementRequestDetails();
                String hearingVenue = sscsCaseData.getPostponementRequest().getPostponementRequestHearingVenue();
                LocalDate hearingDate = LocalDateTime.parse(sscsCaseData.getPostponementRequest().getPostponementRequestHearingDateAndTime()).toLocalDate();
                additionalRequestDetails.append("Date of Hearing: ").append(hearingDate.format(DATE_TIME_FORMATTER)).append("\n");
                additionalRequestDetails.append("Hearing Venue: ").append(hearingVenue).append("\n");
                additionalRequestDetails.append("Reason for Postponement Request: ").append(requestDetails).append("\n");
                title = "Postponement Request from FTA";
                break;
            default:
                throw new IllegalArgumentException("Unsupported event type for processRequestPdfAndSetPreviewDocument: " + requestPdfType);
        }

        if (isBlank(requestDetails)) {
            response.addError(String.format("Please enter request details to generate a %s request document", requestPdfType.toString().toLowerCase()));
            return response;
        }

        DocumentLink previewDocument = getPreviewDocument(requestPdfType, userAuthorisation, generateFile, templateId, title, additionalRequestDetails);

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

    private static DocumentLink getPreviewDocument(
        RequestPdfType requestPdfType,
        String userAuthorisation,
        GenerateFile generateFile,
        String templateId,
        String title,
        StringBuilder additionalRequestDetails
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

    public static boolean isSAndLCase(SscsCaseData sscsCaseData) {
        return LIST_ASSIST == Optional.of(sscsCaseData)
            .map(SscsCaseData::getSchedulingAndListingFields)
            .map(SchedulingAndListingFields::getHearingRoute)
            .orElse(null);
    }

    public static boolean isValidCaseState(State state, List<State> allowedStates) {
        return allowedStates.contains(state);
    }

    public static void clearDocumentTransientFields(SscsCaseData caseData) {
        caseData.setDocumentGeneration(DocumentGeneration.builder().build());
        caseData.setDocumentStaging(DocumentStaging.builder().build());
    }

    public static Optional<Event> getLatestEventOfSpecifiedTypes(SscsCaseData caseData, EventType specifiedType, EventType... specifiedTypes) {
        List<EventType> targets = Stream.concat(Stream.of(specifiedType), Arrays.stream(specifiedTypes)).collect(Collectors.toList());

        return caseData.getEvents().stream()
            .filter(event -> targets.contains(event.getValue().getEventType()))
            .max(Event::compareTo);
    }
}
