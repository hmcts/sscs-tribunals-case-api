package uk.gov.hmcts.reform.sscs.ccd.presubmit.dwpuploadresponse;

import static java.util.Objects.requireNonNull;


import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Service
public class DwpUploadResponseAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_START)
            && callback.getEvent() == EventType.DWP_UPLOAD_RESPONSE;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        setBenefitTypeDropdown(sscsCaseData);

        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }

    private void setBenefitTypeDropdown(SscsCaseData sscsCaseData) {
        List<DynamicListItem> listOptions = new ArrayList<>();

        if (sscsCaseData.getAppeal() != null && sscsCaseData.getAppeal().getBenefitType() != null) {
            Optional<BenefitTypeDynamicListItems> item = BenefitTypeDynamicListItems.findByBenefitType(sscsCaseData.getAppeal().getBenefitType());
            if (item.isPresent()) {
                populateListWithItems(listOptions, BenefitTypeDynamicListItems.UC);
            }
        }
        if (!listOptions.isEmpty()) {
            sscsCaseData.setDwpUploadResponseDynamicBenefitType(new DynamicList(listOptions.get(0), listOptions));
        }
    }

    private void populateListWithItems(List<DynamicListItem> listOptions,
        BenefitTypeDynamicListItems... items) {
        for (BenefitTypeDynamicListItems item : items) {
            listOptions.add(new DynamicListItem(item.getCode(), item.getLabel()));
        }
    }
}
