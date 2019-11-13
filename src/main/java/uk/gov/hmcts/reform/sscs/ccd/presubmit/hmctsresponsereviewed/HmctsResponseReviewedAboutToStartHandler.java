package uk.gov.hmcts.reform.sscs.ccd.presubmit.hmctsresponsereviewed;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.model.dwp.OfficeMapping;
import uk.gov.hmcts.reform.sscs.service.DwpAddressLookupService;

@Service
public class HmctsResponseReviewedAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private DwpAddressLookupService service;

    public HmctsResponseReviewedAboutToStartHandler(DwpAddressLookupService service) {
        this.service = service;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_START)
            && callback.getEvent() == EventType.HMCTS_RESPONSE_REVIEWED;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        setOfficeDropdowns(sscsCaseData);
        setDefaultFieldValues(sscsCaseData);

        if (sscsCaseData.getCreatedInGapsFrom() == null || !sscsCaseData.getCreatedInGapsFrom().equals("readyToList")) {
            preSubmitCallbackResponse.addError("This event cannot be run for cases created in GAPS at valid appeal");
        }

        return preSubmitCallbackResponse;
    }

    public void setOfficeDropdowns(SscsCaseData sscsCaseData) {
        List<DynamicListItem> listOptions = new ArrayList<>();

        Optional<OfficeMapping> selectedOfficeMapping = sscsCaseData.getAppeal().getMrnDetails() != null && sscsCaseData.getAppeal().getMrnDetails().getDwpIssuingOffice() != null && sscsCaseData.getAppeal().getBenefitType() != null
                ? service.getDwpMappingByOffice(sscsCaseData.getAppeal().getBenefitType().getCode(), sscsCaseData.getAppeal().getMrnDetails().getDwpIssuingOffice()) : Optional.empty();

        OfficeMapping[] offices = service.allDwpBenefitOffices();
        int defaultSelectedIndex = -1;

        for (int i = 0; i < offices.length; i++) {
            OfficeMapping office = offices[i];
            listOptions.add(new DynamicListItem(office.getMapping().getCcd(), office.getMapping().getCcd()));

            if (selectedOfficeMapping.isPresent() && selectedOfficeMapping.get() == office) {
                defaultSelectedIndex = i;
            }
        }

        DynamicListItem selectedDynamicListItem = defaultSelectedIndex == -1 ? new DynamicListItem(null, null) : listOptions.get(defaultSelectedIndex);

        if (sscsCaseData.getDwpOriginatingOffice() == null) {
            sscsCaseData.setDwpOriginatingOffice(new DynamicList(selectedDynamicListItem, listOptions));
        }
        if (sscsCaseData.getDwpPresentingOffice() == null) {
            sscsCaseData.setDwpPresentingOffice(new DynamicList(selectedDynamicListItem, listOptions));
        }
    }

    public void setDefaultFieldValues(SscsCaseData sscsCaseData) {
        if (sscsCaseData.getDwpIsOfficerAttending() == null) {
            sscsCaseData.setDwpIsOfficerAttending("No");
        }
        if (sscsCaseData.getDwpUcb() == null) {
            sscsCaseData.setDwpUcb("No");
        }
        if (sscsCaseData.getDwpPhme() == null) {
            sscsCaseData.setDwpPhme("No");
        }
        if (sscsCaseData.getDwpComplexAppeal() == null) {
            sscsCaseData.setDwpComplexAppeal("No");
        }
    }
}
