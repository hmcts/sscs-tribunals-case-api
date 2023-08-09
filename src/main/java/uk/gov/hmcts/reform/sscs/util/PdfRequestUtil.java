package uk.gov.hmcts.reform.sscs.util;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.model.docassembly.GenerateFileParams;
import uk.gov.hmcts.reform.sscs.model.docassembly.NoticeIssuedTemplateBody;
import uk.gov.hmcts.reform.sscs.model.docassembly.PdfRequestTemplateBody;

@Slf4j
public class PdfRequestUtil {
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final String POST_HEARING_REQUEST_FILE_SUFFIX = " Application from FTA.pdf";
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
        String templateId,
        boolean isPostHearingsEnabled) {
        log.debug("Executing processRequestPdfAndSetPreviewDocument for caseId: {}", sscsCaseData.getCcdCaseId());

        additionalRequestDetails = new StringBuilder();
        additionalRequestDetails.append("Date request received: ").append(LocalDate.now().format(DATE_TIME_FORMATTER)).append("\n");
        StringBuilder pdfUrlBuilder = new StringBuilder();
        switch (pdfType) {
            case POST_HEARING:
                if (!isPostHearingsEnabled) {
                    response.addError("Post hearings is not currently enabled");
                    return response;
                }
                String postHearingDocumentTypeLabel = getPostHearingDocumentType(sscsCaseData.getPostHearing().getRequestType()).getLabel();
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
                // handled in first switch
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
        requestDetails = getRequestDetailsForPostHearingType(sscsCaseData);
        LocalDate issueFinalDecisionDate = sscsCaseData.getIssueFinalDecisionDate();
        if (isNull(issueFinalDecisionDate)) {
            throw new IllegalArgumentException("issueFinalDecisionDate unexpectedly null for caseId: " + sscsCaseData.getCcdCaseId());
        }

        String requestTypeDescriptionEn = sscsCaseData.getPostHearing().getRequestType().getDescriptionEn();
        additionalRequestDetails.append("Date of decision issued: ")
            .append(issueFinalDecisionDate.format(DATE_TIME_FORMATTER))
            .append("\n")
            .append("Reason for ")
            .append(requestTypeDescriptionEn)
            .append(" application: ")
            .append(requestDetails)
            .append("\n");
        title = String.format("%s Application from %s", requestTypeDescriptionEn, "FTA");
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

    public static DocumentType getPostHearingDocumentType(PostHearingRequestType postHearingRequestType) {
        switch (postHearingRequestType) {
            case SET_ASIDE:
                return DocumentType.SET_ASIDE_APPLICATION;
            case CORRECTION:
                return DocumentType.CORRECTION_APPLICATION;
            case STATEMENT_OF_REASONS:
                return DocumentType.STATEMENT_OF_REASONS_APPLICATION;
            case LIBERTY_TO_APPLY:
                return DocumentType.LIBERTY_TO_APPLY_APPLICATION;
            case PERMISSION_TO_APPEAL:
                return DocumentType.PERMISSION_TO_APPEAL_APPLICATION;
            default:
                throw new IllegalArgumentException("Unexpected request type: " + postHearingRequestType);
        }
    }

    public static NoticeIssuedTemplateBody populateNoticeBodySignedByAndSignedRole(SscsCaseData caseData, NoticeIssuedTemplateBody formPayload, boolean isPostHearingsEnabled, boolean isPostHearingsBEnabled) {
        NoticeIssuedTemplateBody.NoticeIssuedTemplateBodyBuilder formPayloadBuilder = formPayload.toBuilder();
        DocumentGeneration documentGeneration = caseData.getDocumentGeneration();
        if (isPostHearingsEnabled) {
            PostHearingReviewType postHearingReviewType = caseData.getPostHearing().getReviewType();
          
            if (nonNull(postHearingReviewType)) {
                switch (postHearingReviewType) {
                    case SET_ASIDE:
                        formPayloadBuilder.noticeBody(documentGeneration.getBodyContent());
                        formPayloadBuilder.userName(documentGeneration.getSignedBy());
                        formPayloadBuilder.userRole(documentGeneration.getSignedRole());
                        return formPayloadBuilder.build();
                    case CORRECTION:
                        formPayloadBuilder.noticeBody(documentGeneration.getCorrectionBodyContent());
                        formPayloadBuilder.userName(documentGeneration.getCorrectionSignedBy());
                        formPayloadBuilder.userRole(documentGeneration.getCorrectionSignedRole());
                        return formPayloadBuilder.build();
                    case STATEMENT_OF_REASONS:
                        formPayloadBuilder.noticeBody(documentGeneration.getStatementOfReasonsBodyContent());
                        formPayloadBuilder.userName(documentGeneration.getStatementOfReasonsSignedBy());
                        formPayloadBuilder.userRole(documentGeneration.getStatementOfReasonsSignedRole());
                        return formPayloadBuilder.build();
                    case LIBERTY_TO_APPLY:
                        if (isPostHearingsBEnabled) {
                            formPayloadBuilder.noticeBody(documentGeneration.getLibertyToApplyBodyContent());
                            formPayloadBuilder.userName(documentGeneration.getLibertyToApplySignedBy());
                            formPayloadBuilder.userRole(documentGeneration.getLibertyToApplySignedRole());
                            return formPayloadBuilder.build();
                        }
                        throw new IllegalArgumentException("isPostHearingsBEnabled is false - Liberty to Apply is not available");
                    case PERMISSION_TO_APPEAL:
                        if (isPostHearingsBEnabled) {
                            return caseData.getDocumentGeneration().getPermissionToAppealBodyContent();
                        }
                        throw new IllegalArgumentException("isPostHearingsBEnabled is false - Permission to Appeal is not available");
                    default:
                        throw new IllegalArgumentException("getNoticeBody has unexpected postHearingReviewType: "
                            + postHearingReviewType.getDescriptionEn());
                }
            }
        }
      
        formPayloadBuilder.noticeBody(Optional.ofNullable(documentGeneration.getBodyContent())
                .orElse(documentGeneration.getDirectionNoticeContent()));
        formPayloadBuilder.userName(documentGeneration.getSignedBy());
        formPayloadBuilder.userRole(documentGeneration.getSignedRole());
        return formPayloadBuilder.build();
    }

    protected static String getRequestDetailsForPostHearingType(SscsCaseData sscsCaseData) {
        PostHearingRequestType postHearingRequestType = sscsCaseData.getPostHearing().getRequestType();

        switch (postHearingRequestType) {
            case SET_ASIDE:
                return sscsCaseData.getDocumentGeneration().getBodyContent();
            case CORRECTION:
                return sscsCaseData.getDocumentGeneration().getCorrectionBodyContent();
            case STATEMENT_OF_REASONS:
                return sscsCaseData.getDocumentGeneration().getStatementOfReasonsBodyContent();
            case LIBERTY_TO_APPLY:
                return sscsCaseData.getDocumentGeneration().getLibertyToApplyBodyContent();
            case PERMISSION_TO_APPEAL:
                return sscsCaseData.getDocumentGeneration().getPermissionToAppealBodyContent();
            default:
                throw new IllegalArgumentException("getRequestDetailsForPostHearingType has unexpected postHearingRequestType: " + postHearingRequestType);
        }
    }

    public static YesNo getGenerateNotice(SscsCaseData caseData, boolean isPostHearingsEnabled, boolean isPostHearingsBEnabled) {
        PostHearingReviewType postHearingReviewType = caseData.getPostHearing().getReviewType();
        if (isPostHearingsEnabled && nonNull(postHearingReviewType)) {
            switch (postHearingReviewType) {
                case SET_ASIDE:
                    return caseData.getDocumentGeneration().getGenerateNotice();
                case CORRECTION:
                    return caseData.getDocumentGeneration().getCorrectionGenerateNotice();
                case STATEMENT_OF_REASONS:
                    return caseData.getDocumentGeneration().getStatementOfReasonsGenerateNotice();
                case LIBERTY_TO_APPLY:
                    if (isPostHearingsBEnabled) {
                        return caseData.getDocumentGeneration().getLibertyToApplyGenerateNotice();
                    }
                    throw new IllegalArgumentException("isPostHearingsBEnabled is false - Liberty to Apply is not available");
                case PERMISSION_TO_APPEAL:
                    if (isPostHearingsBEnabled) {
                        return caseData.getDocumentGeneration().getPermissionToAppealGenerateNotice();
                    }
                    throw new IllegalArgumentException("isPostHearingsBEnabled is false - Permission to Appeal is not available");
                default:
                    throw new IllegalArgumentException("getGenerateNotice has unexpected PostHearingReviewType: " + postHearingReviewType);
            }
        }

        return caseData.getDocumentGeneration().getGenerateNotice();
    }
}
