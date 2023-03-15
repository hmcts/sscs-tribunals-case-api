package uk.gov.hmcts.reform.sscs.ccd.presubmit.issuegenericletter;

import static java.util.Objects.nonNull;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.Entity;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherPartySelectionDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Component
@Slf4j
public class IssueGenericLetterMidEventValidationHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType == CallbackType.MID_EVENT
                && callback.getEvent() == EventType.ISSUE_GENERIC_LETTER
                && nonNull(callback.getCaseDetails())
                && nonNull(callback.getCaseDetails().getCaseData());
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback.");
        }

        CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        SscsCaseData sscsCaseData = caseDetails.getCaseData();

        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        preSubmitCallbackResponse.addErrors(validateAddresses(sscsCaseData));

        if (YesNo.NO.equals(sscsCaseData.getSendToAllParties()) && hasNoPartySelected(sscsCaseData)) {
            preSubmitCallbackResponse.addError("At least one party should be selected");
        }

        return preSubmitCallbackResponse;
    }

    private Set<String> validateAddresses(SscsCaseData caseData) {
        var errors = new HashSet<String>();
        var sendToAllParties = YesNo.isYes(caseData.getSendToAllParties());
        var appellant = caseData.getAppeal().getAppellant();

        if (sendToAllParties || YesNo.isYes(caseData.getSendToApellant())) {
            var appointee = appellant.getAppointee();

            var hasAppointee = nonNull(appointee) && isYes(appellant.getIsAppointee());

            if (hasAppointee && isAddressEmpty(appointee.getAddress())) {
                errors.add("Address is empty for an appellant appointee");
            } else if (isAddressEmpty(appellant.getAddress())) {
                errors.add("Address is empty for an appellant");
            }
        }
        
        if ((sendToAllParties && caseData.isThereAJointParty()) || YesNo.isYes(caseData.getSendToJointParty())) {
            var jointParty = caseData.getJointParty();

            var address = isYes(jointParty.getJointPartyAddressSameAsAppellant()) ? appellant.getAddress() : jointParty.getAddress();

            if (isAddressEmpty(address)) {
                errors.add("Address is empty for a joint party");
            }
        }
        
        if ((sendToAllParties && caseData.isThereARepresentative()) || YesNo.isYes(caseData.getSendToRepresentative())) {
            var representative = caseData.getAppeal().getRep();
            
            if (isAddressEmpty(representative.getAddress())) {
                errors.add("Address is empty for a representative");
            }
        }
        
        if ((sendToAllParties && !isEmpty(caseData.getOtherParties())) || YesNo.isYes(caseData.getSendToOtherParties())) {
            errors.addAll(validateOtherPartiesAddresses(caseData));
        }
        
        return errors;
        
    }

    private static final String ADDRESS_IS_EMPTY = "Address is empty for %s";

    private Set<String> validateOtherPartiesAddresses(SscsCaseData caseData) {
        var errors = new HashSet<String>();
        var selectedOtherParties = caseData.getOtherPartySelection();
        var otherParties = caseData.getOtherParties();

        if (!isEmpty(selectedOtherParties)) {
            errors.addAll(validateSelectedParties(selectedOtherParties, otherParties));
        } else {
            errors.addAll(validateAllOtherParties(otherParties));
        }

        return errors;
    }

    private Set<String> validateAllOtherParties(List<CcdValue<OtherParty>> otherParties) {
        var errors = new HashSet<String>();

        if (!isEmpty(otherParties)) {
            for (CcdValue<OtherParty> otherParty : otherParties) {
                var party = otherParty.getValue();

                if (party.hasAppointee() && isAddressEmpty(party.getAppointee().getAddress())) {
                    errors.add(String.format(ADDRESS_IS_EMPTY, party.getAppointee().getName().getFullNameNoTitle()));
                } else if (isAddressEmpty(party.getAddress())) {
                    errors.add(String.format(ADDRESS_IS_EMPTY, party.getName().getFullNameNoTitle()));
                }

                if (party.hasRepresentative() && isAddressEmpty(party.getRep().getAddress())) {
                    errors.add(String.format(ADDRESS_IS_EMPTY, party.getRep().getName().getFullNameNoTitle()));
                }
            }
        }

        return errors;
    }

    private Set<String> validateSelectedParties(List<CcdValue<OtherPartySelectionDetails>> selectedOtherParties, List<CcdValue<OtherParty>> otherParties) {
        var errors = new HashSet<String>();

        for (var selectedParty : selectedOtherParties) {
            String entityId = selectedParty.getValue().getOtherPartiesList().getValue().getCode();
            var party = getOtherPartyByEntityId(entityId, otherParties);

            if (party != null && isAddressEmpty(party.getAddress())) {
                errors.add(String.format(ADDRESS_IS_EMPTY, party.getName().getFullNameNoTitle()));
            }
        }

        return errors;
    }

    private Entity getOtherPartyByEntityId(String entityId, List<CcdValue<OtherParty>> otherParties) {
        return otherParties.stream()
                .map(CcdValue::getValue)
                .filter(o -> filterByEntityID(entityId, o))
                .findFirst()
                .map(r -> getParty(entityId, r))
                .orElse(null);
    }

    private Entity getParty(String entityId, OtherParty party) {
        if (party.hasAppointee() && entityId.contains(party.getAppointee().getId())) {
            return party.getAppointee();
        } else if (entityId.contains(party.getId())) {
            return party;
        } else {
            return party.getRep();
        }
    }

    private static boolean filterByEntityID(String entityId, OtherParty o) {
        return entityId.contains(o.getId())
                || (o.hasRepresentative() && entityId.contains(o.getRep().getId()))
                || (o.hasAppointee() && entityId.contains(o.getAppointee().getId()));
    }

    private boolean hasNoPartySelected(SscsCaseData sscsCaseData) {
        if (YesNo.isYes(sscsCaseData.getSendToApellant())) {
            return false;
        }
        if (YesNo.isYes(sscsCaseData.getSendToJointParty())) {
            return false;
        }
        if (YesNo.isYes(sscsCaseData.getSendToRepresentative())) {
            return false;
        }

        return !YesNo.isYes(sscsCaseData.getSendToOtherParties()) || isEmpty(sscsCaseData.getOtherPartySelection());
    }

    private boolean isAddressEmpty(Address address) {
        return address == null || address.isAddressEmpty();
    }

}
