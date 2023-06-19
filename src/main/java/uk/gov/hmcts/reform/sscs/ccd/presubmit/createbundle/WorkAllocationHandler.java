package uk.gov.hmcts.reform.sscs.ccd.presubmit.createbundle;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;

import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.reference.data.model.SessionCategoryMap;
import uk.gov.hmcts.reform.sscs.reference.data.service.SessionCategoryMapService;

@Service
@Slf4j
public class WorkAllocationHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    
    private SessionCategoryMapService sessionCategoryMapService;
    
    public WorkAllocationHandler(SessionCategoryMapService sessionCategoryMapService) {
        this.sessionCategoryMapService = sessionCategoryMapService;
    }
    
    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && callback.getEvent() == EventType.CREATE_BUNDLE;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();
        
        updateSessionCategory(sscsCaseData);
        updatePanelCount(sscsCaseData);
        
        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }

    private void updatePanelCount(SscsCaseData caseData) {
        boolean doctorSpecialistSecond = isNotBlank(caseData.getSscsIndustrialInjuriesData().getSecondPanelDoctorSpecialism());
        boolean fqpmRequired = isYes(caseData.getIsFqpmRequired());
        SessionCategoryMap sessionCategory = sessionCategoryMapService.getSessionCategory(caseData.getBenefitCode(), caseData.getIssueCode(),
                        doctorSpecialistSecond, fqpmRequired);

        if (sessionCategory != null) {
            caseData.getWorkAllocationFields().setSessionCategory(sessionCategory.getCategory().getSessionCategoryCode());
        }
    }

    private void updateSessionCategory(SscsCaseData caseData) {
        if (caseData.getPanel() != null) {
            long count = Stream.of(
                            caseData.getPanel().getAssignedTo(),
                            caseData.getPanel().getMedicalMember(),
                            caseData.getPanel().getDisabilityQualifiedMember())
                    .filter(m -> isNotBlank(m))
                    .count();

            if (count > 0) {
                caseData.getWorkAllocationFields().setPanelCount((int) count);
            }
        }
    }
}
