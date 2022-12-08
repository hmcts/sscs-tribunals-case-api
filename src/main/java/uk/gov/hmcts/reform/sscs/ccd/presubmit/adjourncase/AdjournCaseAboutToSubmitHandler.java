package uk.gov.hmcts.reform.sscs.ccd.presubmit.adjourncase;

import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_ADJOURNMENT_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isNoOrNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingType.PAPER;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingType.ORAL;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.resendtogaps.ListAssistHearingMessageHelper;
import uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel;
import uk.gov.hmcts.reform.sscs.service.PreviewDocumentService;
import uk.gov.hmcts.reform.sscs.util.SscsUtil;

@Component
@Slf4j
@RequiredArgsConstructor
public class AdjournCaseAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final PreviewDocumentService previewDocumentService;

    private final ListAssistHearingMessageHelper hearingMessageHelper;

    @Value("${feature.snl.adjournment.enabled}")
    private boolean isAdjournmentEnabled; // TODO SSCS-10951

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType == CallbackType.ABOUT_TO_SUBMIT
            && callback.getEvent() == EventType.ADJOURN_CASE
            && Objects.nonNull(callback.getCaseDetails())
            && Objects.nonNull(callback.getCaseDetails().getCaseData());
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();

        previewDocumentService.writePreviewDocumentToSscsDocument(
            sscsCaseData,
            DRAFT_ADJOURNMENT_NOTICE,
            sscsCaseData.getAdjournment().getPreviewDocument());

        if (SscsUtil.isSAndLCase(sscsCaseData)
            && isAdjournmentEnabled // TODO SSCS-10951
            && (isYes(sscsCaseData.getAdjournment().getCanCaseBeListedRightAway())
            || isNoOrNull(sscsCaseData.getAdjournment().getAreDirectionsBeingMadeToParties()))
        ) {
            sscsCaseData.getAdjournment().setAdjournmentInProgress(YES);
            hearingMessageHelper.sendListAssistCreateHearingMessage(sscsCaseData.getCcdCaseId());
        }

        if (sscsCaseData.getAdjournment().getInterpreterRequired() != null) {
            HearingOptions hearingOptions = HearingOptions.builder().build();
            if (sscsCaseData.getAppeal().getHearingOptions() != null) {
                hearingOptions = sscsCaseData.getAppeal().getHearingOptions();
            }
            hearingOptions.setLanguages(sscsCaseData.getAdjournment().getInterpreterLanguage());
            hearingOptions.setLanguageInterpreter(sscsCaseData.getAdjournment().getInterpreterRequired().getValue());

            sscsCaseData.getAppeal().setHearingOptions(hearingOptions);
        }

        if (sscsCaseData.getAdjournment().getGeneratedDate() == null) {
            sscsCaseData.getAdjournment().setGeneratedDate(LocalDate.now());
        }

        updateHearingChannel(sscsCaseData);

        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }

    private static void updateHearingChannel(SscsCaseData sscsCaseData) {

        if (sscsCaseData.getAdjournment().getTypeOfNextHearing() != null) {
            log.info(String.format("Update the hearing channel %s", sscsCaseData.getAdjournment().getTypeOfNextHearing()));
            final Hearing latestHearing = sscsCaseData.getLatestHearing();
            if (latestHearing != null && latestHearing.getValue() != null) {
                final HearingChannel hearingChannel = getNextHearingChannel(sscsCaseData);
                latestHearing.getValue().setHearingChannel(hearingChannel);
                if (hearingChannel.getValueTribunals().equalsIgnoreCase(PAPER.getValue())) {
                    sscsCaseData.getAppeal().setHearingType(PAPER.getValue());
                } else {
                    sscsCaseData.getAppeal().setHearingType(ORAL.getValue());
                }
            }
        }
    }

    private static HearingChannel getNextHearingChannel(SscsCaseData caseData) {
        return Arrays.stream(HearingChannel.values())
                .filter(hearingChannel -> caseData.getAdjournment().getTypeOfNextHearing().getHearingChannel().getValueTribunals().equalsIgnoreCase(
                        hearingChannel.getValueTribunals()))
                .findFirst().orElse(HearingChannel.PAPER);
    }
}
