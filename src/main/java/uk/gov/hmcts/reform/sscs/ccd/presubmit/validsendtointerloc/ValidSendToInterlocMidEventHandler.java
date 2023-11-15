package uk.gov.hmcts.reform.sscs.ccd.presubmit.validsendtointerloc;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.postponementrequest.PostponementRequestAboutToStartHandler.NOT_LIST_ASSIST_CASE_ERROR;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.SelectWhoReviewsCase;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.util.PdfRequestUtil;
import uk.gov.hmcts.reform.sscs.util.SscsUtil;

@Slf4j
@Service
public class ValidSendToInterlocMidEventHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    public static final String POSTPONEMENTS_NOT_POSSIBLE_GAPS = "Postponement requests cannot be made for hearings listed in GAPS";
    private final GenerateFile generateFile;
    private final String templateId;

    public ValidSendToInterlocMidEventHandler(GenerateFile generateFile, @Value("${doc_assembly.postponementrequest}") String templateId) {
        this.generateFile = generateFile;
        this.templateId = templateId;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbackType must not be null");

        return callbackType.equals(CallbackType.MID_EVENT)
                && (callback.getEvent() == EventType.VALID_SEND_TO_INTERLOC
                || callback.getEvent() == EventType.ADMIN_SEND_TO_INTERLOCUTORY_REVIEW_STATE);
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        final SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();
        final PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(sscsCaseData);

        if (SelectWhoReviewsCase.POSTPONEMENT_REQUEST_INTERLOC_SEND_TO_TCW.getId().equals(sscsCaseData.getSelectWhoReviewsCase().getValue().getCode())) {
            validatePostponementRequest(sscsCaseData, response);

            if (response.getErrors().isEmpty()) {
                PdfRequestUtil.processRequestPdfAndSetPreviewDocument(PdfRequestUtil.PdfType.POSTPONEMENT, userAuthorisation, sscsCaseData, response,
                        generateFile, templateId, false);
            }
        }

        return response;
    }

    private void validatePostponementRequest(SscsCaseData sscsCaseData,
                                             PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {
        if (sscsCaseData.getRegionalProcessingCenter() != null
                && HearingRoute.GAPS.equals(sscsCaseData.getRegionalProcessingCenter().getHearingRoute())
                && sscsCaseData.getPostponementRequest().getPostponementRequestDetails() != null) {
            preSubmitCallbackResponse.addError(POSTPONEMENTS_NOT_POSSIBLE_GAPS);
            return;
        }

        if (!SscsUtil.isSAndLCase(sscsCaseData)) {
            preSubmitCallbackResponse.addError(NOT_LIST_ASSIST_CASE_ERROR);
            return;
        }

        Hearing hearing = sscsCaseData.getLatestHearing();

        if (isNull(hearing)) {
            preSubmitCallbackResponse.addError("There is no current hearing to postpone on the case");
            return;
        }

        HearingStatus hearingStatus = Optional.ofNullable(hearing.getValue())
                .map(HearingDetails::getHearingStatus)
                .orElse(null);

        if (!HearingStatus.LISTED.equals(hearingStatus)) {
            preSubmitCallbackResponse.addError("There is no listed hearing to postpone on the case");
            return;
        }

        setPostponementRequest(hearing, sscsCaseData);
    }

    private void setPostponementRequest(Hearing hearing, SscsCaseData sscsCaseData) {
        sscsCaseData.getPostponementRequest().setPostponementRequestHearingDateAndTime(hearing.getValue().getStart().toString());
        sscsCaseData.getPostponementRequest().setPostponementRequestHearingVenue(hearing.getValue().getVenue().getName());
    }
}
