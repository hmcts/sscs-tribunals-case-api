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
import uk.gov.hmcts.reform.sscs.ccd.domain.PostHearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.PostponementRequest;
import uk.gov.hmcts.reform.sscs.ccd.domain.SchedulingAndListingFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.model.docassembly.GenerateFileParams;
import uk.gov.hmcts.reform.sscs.model.docassembly.PostHearingRequestTemplateBody;
import uk.gov.hmcts.reform.sscs.model.docassembly.PostponeRequestTemplateBody;

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

        log.debug("Executing processPostponementRequestPdfAndSetPreviewDocument for caseId: {}", sscsCaseData.getCcdCaseId());
        PostponementRequest postponementRequest = sscsCaseData.getPostponementRequest();
        final String requestDetails = postponementRequest.getPostponementRequestDetails();

        if (isBlank(requestDetails)) {
            response.addError("Please enter request details to generate a postponement request document");
            return response;
        }

        String hearingVenue = postponementRequest.getPostponementRequestHearingVenue();
        LocalDate hearingDate = LocalDateTime.parse(postponementRequest.getPostponementRequestHearingDateAndTime()).toLocalDate();

        StringBuilder additionalRequestDetails = new StringBuilder();
        additionalRequestDetails.append("Date request received: ").append(LocalDate.now().format(DATE_TIME_FORMATTER)).append("\n");
        additionalRequestDetails.append("Date of Hearing: ").append(hearingDate.format(DATE_TIME_FORMATTER)).append("\n");
        additionalRequestDetails.append("Hearing Venue: ").append(hearingVenue).append("\n");
        additionalRequestDetails.append("Reason for Postponement Request: ").append(requestDetails).append("\n");

        GenerateFileParams params = GenerateFileParams.builder()
                .renditionOutputLocation(null)
                .templateId(templateId)
                .formPayload(PostponeRequestTemplateBody.builder()
                    .title("Postponement Request from FTA")
                    .text(additionalRequestDetails.toString())
                    .build())
                .userAuthentication(userAuthorisation)
                .build();
        final String generatedFileUrl = generateFile.assemble(params);
        postponementRequest.setPostponementPreviewDocument(DocumentLink.builder()
                .documentFilename("Postponement Request.pdf")
                .documentBinaryUrl(generatedFileUrl + "/binary")
                .documentUrl(generatedFileUrl)
                .build());

        return response;
    }

    public static PreSubmitCallbackResponse<SscsCaseData> processPostHearingRequestPdfAndSetPreviewDocument(String userAuthorisation,
                                                                                                            SscsCaseData sscsCaseData,
                                                                                                            PreSubmitCallbackResponse<SscsCaseData> response,
                                                                                                            GenerateFile generateFile,
                                                                                                            String templateId) {
        final String caseId = sscsCaseData.getCcdCaseId();
        log.debug("Executing processPostHearingRequestPdfAndSetPreviewDocument for caseId: {}", caseId);
        final PostHearing postHearing = sscsCaseData.getPostHearing();
        final String requestDetails = postHearing.getRequestReason();
        if (isBlank(requestDetails)) {
            response.addError("Please enter request details to generate a post hearing request document");
            return response;
        }

        Event latestIssueFinalDecision = getLatestEventOfSpecifiedTypes(sscsCaseData, ISSUE_FINAL_DECISION, ISSUE_FINAL_DECISION_WELSH)
            .orElseThrow(() -> {
                log.error("latestIssueFinalDecision unexpectedly null for caseId: {}", caseId);
                throw new IllegalArgumentException("latestIssueFinalDecision unexpectedly null for caseId: " + caseId);
            });

        final LocalDate dateOfFinalDecision = latestIssueFinalDecision.getValue().getDateTime().toLocalDate();
        final String postHearingRequestType = postHearing.getRequestType().getDescriptionEn();
        final StringBuilder additionalRequestDetails = new StringBuilder();
        additionalRequestDetails.append("Date request received: ").append(LocalDate.now().format(DATE_TIME_FORMATTER)).append("\n");
        additionalRequestDetails.append("Date of decision: ").append(dateOfFinalDecision.format(DATE_TIME_FORMATTER)).append("\n");
        additionalRequestDetails.append("Reason for ").append(postHearingRequestType).append(" request: ").append(requestDetails).append("\n");

        GenerateFileParams params = GenerateFileParams.builder()
                .renditionOutputLocation(null)
                .templateId(templateId)
                .formPayload(
                    PostHearingRequestTemplateBody.builder()
                        .title(String.format("%s Application from FTA", postHearingRequestType))
                        .text(additionalRequestDetails.toString())
                        .build())
                .userAuthentication(userAuthorisation)
                .build();
        final String generatedFileUrl = generateFile.assemble(params);
        postHearing.setPreviewDocument(DocumentLink.builder()
                .documentFilename("Post Hearing Request.pdf")
                .documentBinaryUrl(generatedFileUrl + "/binary")
                .documentUrl(generatedFileUrl)
                .build());

        return response;
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
