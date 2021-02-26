package uk.gov.hmcts.reform.sscs.ccd.presubmit.caseupdated;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
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
import uk.gov.hmcts.reform.sscs.service.AirLookupService;
import uk.gov.hmcts.reform.sscs.service.RegionalProcessingCenterService;

@Component
@Slf4j
public class CaseUpdatedAboutToSubmitHandler extends ResponseEventsAboutToSubmit implements PreSubmitCallbackHandler<SscsCaseData> {

    private final RegionalProcessingCenterService regionalProcessingCenterService;
    private final AssociatedCaseLinkHelper associatedCaseLinkHelper;
    private final AirLookupService airLookupService;

    @Autowired
    CaseUpdatedAboutToSubmitHandler(RegionalProcessingCenterService regionalProcessingCenterService,
                                    AssociatedCaseLinkHelper associatedCaseLinkHelper,
                                    AirLookupService airLookupService) {
        this.regionalProcessingCenterService = regionalProcessingCenterService;
        this.associatedCaseLinkHelper = associatedCaseLinkHelper;
        this.airLookupService = airLookupService;
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

        setCaseCode(sscsCaseData, callback.getEvent());

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

            updateProcessingVenueIfRequired(caseDetails);

        }

        /* FIXME: commented out as case loader is failing on this validation checks, we need to do another data exercise to clean the data
        return checkFirstCharacterForEachAddressField(sscsCaseData, new PreSubmitCallbackResponse<>(sscsCaseData));*/
        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }

    /*private PreSubmitCallbackResponse<SscsCaseData> checkFirstCharacterForEachAddressField(SscsCaseData sscsCaseData, PreSubmitCallbackResponse<SscsCaseData> response) {

        Appeal appeal = sscsCaseData.getAppeal();
        String caseId = sscsCaseData.getCcdCaseId();

        if (appeal.getAppellant() != null && appeal.getAppellant().getAddress() != null
                && isInvalidAddress(appeal.getAppellant().getAddress())) {
            return addAddressError("appellant", caseId, response);
        }
        if (appeal.getRep() != null && appeal.getRep().getAddress() != null && isInvalidAddress(appeal.getRep().getAddress())) {
            return addAddressError("representative", caseId, response);
        }
        if (appeal.getAppellant().getAppointee() != null && appeal.getAppellant().getAppointee().getAddress() != null
                && isInvalidAddress(appeal.getAppellant().getAppointee().getAddress())) {
            return addAddressError("appointee", caseId, response);
        }
        if (sscsCaseData.getJointPartyAddress() != null && isInvalidAddress(sscsCaseData.getJointPartyAddress())) {
            return addAddressError("joint party", caseId, response);
        }
        return response;
    }

    private boolean isInvalidAddress(Address address) {
        Pattern p = Pattern.compile("^\\.$|^[a-zA-ZÀ-ž0-9 .,]{1}.{1,}$");
        if (address.getLine1() != null && !StringUtils.isEmpty(StringUtils.trimWhitespace(address.getLine1())) && !p.matcher(address.getLine1()).find()) {
            return true;
        } else if (address.getLine2() != null && !StringUtils.isEmpty(StringUtils.trimWhitespace(address.getLine2())) && !p.matcher(address.getLine2()).find()) {
            return true;
        } else if (address.getTown() != null && !StringUtils.isEmpty(StringUtils.trimWhitespace(address.getTown())) && !p.matcher(address.getTown()).find()) {
            return true;
        } else {
            return address.getCounty() != null && !StringUtils.isEmpty(StringUtils.trimWhitespace(address.getCounty())) && !p.matcher(address.getCounty()).find();
        }
    }

    private PreSubmitCallbackResponse<SscsCaseData> addAddressError(String party, String caseId, PreSubmitCallbackResponse<SscsCaseData> response) {
        log.error(format("caseUpdated event failed for case id %s: Invalid characters are being used at the beginning of %s address fields", caseId, party));
        response.addError("Invalid characters are being used at the beginning of address fields, please correct");
        return response;
    }*/

    public void maybeChangeIsScottish(RegionalProcessingCenter oldRpc, RegionalProcessingCenter newRpc, SscsCaseData caseData) {
        if (oldRpc != newRpc) {
            String isScottishCase = IsScottishHandler.isScottishCase(newRpc, caseData);
            caseData.setIsScottishCase(isScottishCase);
        }
    }

    private void updateProcessingVenueIfRequired(CaseDetails<SscsCaseData> caseDetails) {

        SscsCaseData sscsCaseData = caseDetails.getCaseData();

        String postCode = "yes".equalsIgnoreCase(sscsCaseData.getAppeal().getAppellant().getIsAppointee())
            && null != sscsCaseData.getAppeal().getAppellant().getAppointee()
            && null != sscsCaseData.getAppeal().getAppellant().getAppointee().getAddress()
            && null != sscsCaseData.getAppeal().getAppellant().getAppointee().getAddress().getPostcode()
                ? sscsCaseData.getAppeal().getAppellant().getAppointee().getAddress().getPostcode()
                : sscsCaseData.getAppeal().getAppellant().getAddress().getPostcode();

        String venue = airLookupService.lookupAirVenueNameByPostCode(postCode, sscsCaseData.getAppeal().getBenefitType());

        if (venue != null && !venue.equalsIgnoreCase(sscsCaseData.getProcessingVenue())) {
            log.info("Case id: {} - setting venue name to {} from {}", caseDetails.getId(), venue, sscsCaseData.getProcessingVenue());

            sscsCaseData.setProcessingVenue(venue);
        }

    }

}
