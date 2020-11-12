package uk.gov.hmcts.reform.sscs.ccd.presubmit.caseupdated;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.AssociatedCaseLinkHelper;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.ResponseEventsAboutToSubmit;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.isscottish.IsScottishHandler;
import uk.gov.hmcts.reform.sscs.service.RegionalProcessingCenterService;

@Component
@Slf4j
public class CaseUpdatedAboutToSubmitHandler extends ResponseEventsAboutToSubmit implements PreSubmitCallbackHandler<SscsCaseData> {

    private final RegionalProcessingCenterService regionalProcessingCenterService;
    private final AssociatedCaseLinkHelper associatedCaseLinkHelper;

    @Autowired
    CaseUpdatedAboutToSubmitHandler(RegionalProcessingCenterService regionalProcessingCenterService,
                                    AssociatedCaseLinkHelper associatedCaseLinkHelper) {
        this.regionalProcessingCenterService = regionalProcessingCenterService;
        this.associatedCaseLinkHelper = associatedCaseLinkHelper;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && callback.getEvent() == EventType.CASE_UPDATED;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final Optional<CaseDetails<SscsCaseData>> caseDetailsBefore = callback.getCaseDetailsBefore();
        final SscsCaseData sscsCaseData = associatedCaseLinkHelper.linkCaseByNino(caseDetails.getCaseData(), caseDetailsBefore);

        setCaseCode(sscsCaseData);

        if (sscsCaseData.getAppeal().getAppellant() != null
                && sscsCaseData.getAppeal().getAppellant().getAddress() != null
                && sscsCaseData.getAppeal().getAppellant().getAddress().getPostcode() != null) {

            RegionalProcessingCenter newRpc =
                    regionalProcessingCenterService.getByPostcode(sscsCaseData.getAppeal().getAppellant().getAddress().getPostcode());

            maybeChangeIsScottish(sscsCaseData.getRegionalProcessingCenter(), newRpc, sscsCaseData);

            sscsCaseData.setRegionalProcessingCenter(newRpc);

            if (newRpc != null) {
                sscsCaseData.setRegion(newRpc.getName());
            }

        }

        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        return checkFirstCharacterForEachAddressField(sscsCaseData, preSubmitCallbackResponse);
    }

    public void maybeChangeIsScottish(RegionalProcessingCenter oldRpc, RegionalProcessingCenter newRpc, SscsCaseData caseData) {
        if (oldRpc != newRpc) {
            String isScottishCase = IsScottishHandler.isScottishCase(newRpc, caseData);
            caseData.setIsScottishCase(isScottishCase);
        }
    }

    private PreSubmitCallbackResponse<SscsCaseData> checkFirstCharacterForEachAddressField(SscsCaseData sscsCaseData, PreSubmitCallbackResponse<SscsCaseData> response) {

        Appeal appeal = sscsCaseData.getAppeal();

        if (appeal.getAppellant() != null && appeal.getAppellant().getAddress() != null
                && isInvalidAddress(appeal.getAppellant().getAddress())) {
            return addAddressError(response);
        }
        if (appeal.getRep() != null && appeal.getRep().getAddress() != null && isInvalidAddress(appeal.getRep().getAddress())) {
            return addAddressError(response);
        }
        if (appeal.getAppellant().getAppointee() != null && appeal.getAppellant().getAppointee().getAddress() != null
                && isInvalidAddress(appeal.getAppellant().getAppointee().getAddress())) {
            return addAddressError(response);
        }
        if (sscsCaseData.getJointPartyAddress() != null && isInvalidAddress(sscsCaseData.getJointPartyAddress())) {
            return addAddressError(response);
        }
        return response;
    }

    private boolean isInvalidAddress(Address address) {
        Pattern p = Pattern.compile("^[a-zA-ZÀ-ž0-9]{1}[a-zA-ZÀ-ž0-9 \\r\\n.“”\",’?![\\]()/£:\\\\_+\\-%&;]]{1,}$");
        if (address.getLine1() != null && !address.getLine1().isEmpty() && !p.matcher(address.getLine1()).find()) {
            return true;
        } else if (address.getLine2() != null && !address.getLine2().isEmpty() && !p.matcher(address.getLine2()).find()) {
            return true;
        } else if (address.getTown() != null && !address.getTown().isEmpty() && !p.matcher(address.getTown()).find()) {
            return true;
        } else {
            return address.getCounty() != null && !address.getCounty().isEmpty() && !p.matcher(address.getCounty()).find();
        }
    }

    private PreSubmitCallbackResponse<SscsCaseData> addAddressError(PreSubmitCallbackResponse<SscsCaseData> response) {
        response.addError("Invalid characters are being used at the beginning of address fields, please correct");
        return response;
    }
}
