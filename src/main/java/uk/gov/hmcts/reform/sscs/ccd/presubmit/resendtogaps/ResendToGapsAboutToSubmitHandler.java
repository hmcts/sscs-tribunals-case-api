package uk.gov.hmcts.reform.sscs.ccd.presubmit.resendtogaps;

import static java.util.Objects.requireNonNull;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
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

@Component
@Slf4j
public class ResendToGapsAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse;

    private RoboticsJsonMapper roboticsJsonMapper;

    private RoboticsJsonValidator roboticsJsonValidator;

    @Autowired
    public ResendToGapsAboutToSubmitHandler(RoboticsJsonMapper roboticsMapper, RoboticsJsonValidator jsonValidator) {
        this.roboticsJsonMapper = roboticsMapper;
        this.roboticsJsonValidator = jsonValidator;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

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

        preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        try {
            isValid(sscsCaseData, caseDetails.getId(), caseDetails.getState());
        } catch (RoboticsValidationException ve) {
            preSubmitCallbackResponse.addError("Unable to validate robotics json for case id"
                    + sscsCaseData.getCcdCaseId() + " with error: " + ve.getMessage());
        }

        return preSubmitCallbackResponse;
    }

    public void isValid(SscsCaseData caseData, Long caseId, State caseState) throws RoboticsValidationException {

        RoboticsWrapper roboticsWrapper = RoboticsWrapper
                .builder()
                .sscsCaseData(caseData)
                .ccdCaseId(caseId)
                .evidencePresent(caseData.getEvidencePresent())
                .state(caseState).build();

        JSONObject roboticsJson = toJsonObject(roboticsWrapper);
        roboticsJsonValidator.validate(roboticsJson);
    }

    public JSONObject toJsonObject(RoboticsWrapper roboticsWrapper) throws RoboticsValidationException  {
        JSONObject roboticsJson;
        try {
            roboticsJson = roboticsJsonMapper.map(roboticsWrapper);
        } catch  (NullPointerException e) {
            log.error("Json Mapper throws NPE", e);
            throw new RoboticsValidationException(new Exception("Json Mapper Unable to build robotics json due to missing fields", e));
        }
        return roboticsJson;
    }
}
