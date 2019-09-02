package uk.gov.hmcts.reform.sscs.ccd.presubmit.dwpuploadresponse;

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
public class DwpUploadResponseAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private DwpAddressLookupService service;

    public DwpUploadResponseAboutToStartHandler(DwpAddressLookupService service) {
        this.service = service;
    }

    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_START)
            && callback.getEvent() == EventType.DWP_UPLOAD_RESPONSE;
    }

    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        setOfficeDropdowns(sscsCaseData);
        setDefaultFieldValues(sscsCaseData);

        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }

    private void setOfficeDropdowns(SscsCaseData sscsCaseData) {
        List<DynamicListItem> listOptions = new ArrayList<>();

        Optional<OfficeMapping> selectedOfficeMapping = sscsCaseData.getAppeal().getMrnDetails() != null && sscsCaseData.getAppeal().getMrnDetails().getDwpIssuingOffice() != null
                ? service.getDwpMappingByOffice(sscsCaseData.getAppeal().getBenefitType().getCode(), sscsCaseData.getAppeal().getMrnDetails().getDwpIssuingOffice()) : Optional.empty();

        OfficeMapping[] offices = service.allDwpBenefitOffices();
        int defaultSelectedIndex = 0;

        for (int i = 0; i < offices.length; i++) {
            OfficeMapping office = offices[i];
            listOptions.add(new DynamicListItem(office.getMapping().getCcd(), office.getMapping().getCcd()));

            if (selectedOfficeMapping.isPresent() && selectedOfficeMapping.get() == office) {
                defaultSelectedIndex = i;
            }
        }

        sscsCaseData.setDwpOriginatingOffice(new DynamicList(listOptions.get(defaultSelectedIndex), listOptions));
        sscsCaseData.setDwpPresentingOffice(new DynamicList(listOptions.get(defaultSelectedIndex), listOptions));
    }

    private void setDefaultFieldValues(SscsCaseData sscsCaseData) {
        sscsCaseData.setDwpIsOfficerAttending("No");
        sscsCaseData.setDwpUcb("No");
        sscsCaseData.setDwpPhme("No");
        sscsCaseData.setDwpComplexAppeal("No");
    }
}
