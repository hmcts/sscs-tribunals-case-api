package uk.gov.hmcts.reform.sscs.ccd.presubmit.readytolist;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.FileReader;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.service.RegionalProcessingCenterService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

@Service
@Slf4j
public class ReadyToListAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Value("${feature.scheduling-and-listing.enabled}")
    private boolean schedulingAndListingFeature;
    private RegionalProcessingCenterService regionalProcessingCenterService;

    private enum STATE{
        HEARING_CREATED
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && callback.getEvent() == EventType.READY_TO_LIST;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback.");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData(); //Maybe handle in each method instead of HEAD

        if(schedulingAndListingFeature){
            boolean listAssist = checkIfListAssist();
            if(listAssist){
                //Build response to be enium value and case ID
                JSONObject buildJson = new JSONObject();
                buildJson.put("STATE", STATE.HEARING_CREATED);
                buildJson.put("CaseID", sscsCaseData.getCcdCaseId());
                PreSubmitCallbackResponse<SscsCaseData> callbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData); //Placeholder
                return callbackResponse;
            }else{
                return handleCallbackResponse(sscsCaseData); //Return back to old method
            }
        }else{
            return handleCallbackResponse(sscsCaseData); //Return back to old method
        }
    }

    private boolean checkIfListAssist(){
        //Read file rpc-data from JSON
        //Map out JSON object
        //filter and then return if true or false below on listAssist
        //boolean isListAssist = object.getListAssist : true : false
        return true;
    }

    private PreSubmitCallbackResponse<SscsCaseData> handleCallbackResponse(SscsCaseData sscsCaseData){
        PreSubmitCallbackResponse<uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData> callbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);
        log.info(String.format("createdInGapsFrom is %s for caseId %s", sscsCaseData.getCreatedInGapsFrom(), sscsCaseData.getCcdCaseId()));
        if (sscsCaseData.getCreatedInGapsFrom() == null
            || StringUtils.equalsIgnoreCase(sscsCaseData.getCreatedInGapsFrom(), State.VALID_APPEAL.getId())) {
            callbackResponse.addError("Case already created in GAPS at valid appeal.");
            log.warn(String.format("Case already created in GAPS at valid appeal for caseId %s.", sscsCaseData.getCcdCaseId()));
        }
        return callbackResponse;

    }
}
