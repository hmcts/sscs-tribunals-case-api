package uk.gov.hmcts.reform.sscs.util;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.LIST_ASSIST;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.OverrideFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SchedulingAndListingFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.model.docassembly.GenerateFileParams;
import uk.gov.hmcts.reform.sscs.model.docassembly.PostponeRequestTemplateBody;

@Slf4j
public class SscsUtil {

    private static final String TITLE = "Postponement Request from FTA";
    public static final String FILENAME = "Postponement Request.pdf";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private SscsUtil() {
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
        final String requestDetails = sscsCaseData.getPostponementRequest().getPostponementRequestDetails();

        if (isBlank(requestDetails)) {
            response.addError("Please enter request details to generate a postponement request document");
            return response;
        }

        String hearingVenue = sscsCaseData.getPostponementRequest().getPostponementRequestHearingVenue();
        LocalDate hearingDate = LocalDateTime.parse(sscsCaseData.getPostponementRequest().getPostponementRequestHearingDateAndTime()).toLocalDate();

        String additionalRequestDetails = "Date request received: " + LocalDate.now().format(DATE_TIME_FORMATTER) + "\n"
            + "Date of Hearing: " + hearingDate.format(DATE_TIME_FORMATTER) + "\n"
            + "Hearing Venue: " + hearingVenue + "\n"
            + "Reason for Postponement Request: " + requestDetails + "\n";

        GenerateFileParams params = GenerateFileParams.builder()
                .renditionOutputLocation(null)
                .templateId(templateId)
                .formPayload(PostponeRequestTemplateBody.builder().title(TITLE).text(additionalRequestDetails).build())
                .userAuthentication(userAuthorisation)
                .build();
        final String generatedFileUrl = generateFile.assemble(params);
        sscsCaseData.getPostponementRequest().setPostponementPreviewDocument(DocumentLink.builder()
                .documentFilename(FILENAME)
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

    public static boolean isMissingListingRequirements(SchedulingAndListingFields schedulingAndListingFields) {
        return isMissingListingRequirements(schedulingAndListingFields.getOverrideFields())
            && isMissingListingRequirements(schedulingAndListingFields.getDefaultListingValues());
    }

    private static boolean isMissingListingRequirements(OverrideFields listingRequirements) {
        return isNull(listingRequirements) || isEmpty(listingRequirements.getDuration());
    }
}
