package uk.gov.hmcts.reform.sscs.ccd.presubmit.resendtogaps;

import static java.util.Objects.requireNonNull;

import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.robotics.RoboticsJsonMapper;
import uk.gov.hmcts.reform.sscs.robotics.RoboticsJsonValidator;
import uk.gov.hmcts.reform.sscs.robotics.RoboticsValidationException;
import uk.gov.hmcts.reform.sscs.robotics.RoboticsWrapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResendToGapsAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final RoboticsJsonMapper roboticsJsonMapper;
    private final RoboticsJsonValidator roboticsJsonValidator;
    private final ResendToGapsMessageHandler messageHandler;

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbackType must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
            && callback.getEvent() == EventType.RESEND_CASE_TO_GAPS2;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {

        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        final PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        try {
            Set<String> errorSet = isValid(sscsCaseData, caseDetails.getId(), caseDetails.getState());
            if (!CollectionUtils.isEmpty(errorSet)) {
                preSubmitCallbackResponse.addErrors(errorSet);
            } else {
                sscsCaseData.setHmctsDwpState("sentToRobotics");
                messageHandler.sendMessage(sscsCaseData.getCcdCaseId());
            }
        } catch (RoboticsValidationException roboticsValidationException) {
            preSubmitCallbackResponse.addError(roboticsValidationException.getMessage());
        }

        return preSubmitCallbackResponse;
    }

    public Set<String> isValid(SscsCaseData caseData, Long caseId, State caseState) throws RoboticsValidationException {

        RoboticsWrapper roboticsWrapper = RoboticsWrapper
                .builder()
                .sscsCaseData(caseData)
                .ccdCaseId(caseId)
                .evidencePresent(caseData.getEvidencePresent())
                .state(caseState).build();

        JSONObject roboticsJson = toJsonObject(roboticsWrapper);
        return roboticsJsonValidator.validate(roboticsJson, String.valueOf(caseId));
    }

    public JSONObject toJsonObject(RoboticsWrapper roboticsWrapper) throws RoboticsValidationException  {
        JSONObject roboticsJson;
        try {
            roboticsJson = roboticsJsonMapper.map(roboticsWrapper);
        } catch (NullPointerException e) {
            log.error("Json Mapper throws NPE", e);
            throw new RoboticsValidationException("Json Mapper unable to build robotics json due to missing fields", null);
        }
        return roboticsJson;
    }
}
