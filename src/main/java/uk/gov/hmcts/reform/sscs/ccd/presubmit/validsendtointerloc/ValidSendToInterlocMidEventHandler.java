package uk.gov.hmcts.reform.sscs.ccd.presubmit.validsendtointerloc;

import static java.util.Objects.requireNonNull;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.SelectWhoReviewsCase;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.util.PdfRequestUtil;

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
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.MID_EVENT)
                && (callback.getEvent() == EventType.VALID_SEND_TO_INTERLOC
                || callback.getEvent() == EventType.ADMIN_SEND_TO_INTERLOCUTORY_REVIEW_STATE);
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        final SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();
        final PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(sscsCaseData);

        if (SelectWhoReviewsCase.POSTPONEMENT_REQUEST_INTERLOC_SEND_TO_TCW.getId().equals(sscsCaseData.getSelectWhoReviewsCase().getValue().getCode())) {
            PdfRequestUtil.processRequestPdfAndSetPreviewDocument(PdfRequestUtil.PdfType.POSTPONEMENT, userAuthorisation, sscsCaseData, response,
                generateFile, templateId);
        }

        validatePostponementRequests(sscsCaseData, response);
        return response;
    }

    private void validatePostponementRequests(SscsCaseData sscsCaseData,
                                              PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse) {
        if (sscsCaseData.getRegionalProcessingCenter() != null
                && HearingRoute.GAPS.equals(sscsCaseData.getRegionalProcessingCenter().getHearingRoute())
                && SelectWhoReviewsCase.POSTPONEMENT_REQUEST_INTERLOC_SEND_TO_TCW.getId().equals(sscsCaseData.getSelectWhoReviewsCase().getValue().getCode())
                && sscsCaseData.getPostponementRequest().getPostponementRequestDetails() != null) {
            preSubmitCallbackResponse.addError(POSTPONEMENTS_NOT_POSSIBLE_GAPS);
        }
    }
}
