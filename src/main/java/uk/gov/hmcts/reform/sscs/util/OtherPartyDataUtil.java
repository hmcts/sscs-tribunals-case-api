package uk.gov.hmcts.reform.sscs.util;

import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;


public class OtherPartyDataUtil {

    private OtherPartyDataUtil() {
    }

    public static void updateOtherPartyUcb(SscsCaseData sscsCaseData) {
        if (sscsCaseData.getOtherParties() != null) {
            sscsCaseData.setOtherPartyUcb(sscsCaseData.getOtherParties().stream()
                    .filter(o -> isYes(o.getValue().getUnacceptableCustomerBehaviour()))
                    .map(o -> o.getValue().getUnacceptableCustomerBehaviour().getValue())
                    .findAny()
                    .orElse(NO.getValue()));
        }
    }

    public static void assignOtherPartyId(List<CcdValue<OtherParty>> otherParties) {
        int maxId = getMaxId(otherParties);
        for (CcdValue<OtherParty> ccdOtherParty : otherParties) {
            OtherParty otherParty = ccdOtherParty.getValue();
            if (otherParty.getId() == null) {
                otherParty.setId(Integer.toString(++maxId));
            }
            if (otherParty.getAppointee() != null && isYes(otherParty.getIsAppointee()) && otherParty.getAppointee().getId() == null) {
                otherParty.getAppointee().setId(Integer.toString(++maxId));
            }
            if (otherParty.getRep() != null && isYes(otherParty.getRep().getHasRepresentative()) && otherParty.getRep().getId() == null) {
                otherParty.getRep().setId(Integer.toString(++maxId));
            }
        }
    }

    public static void processCaseState(SscsCaseData sscsCaseData) {
        if (sscsCaseData.getIsFqpmRequired() == null
                || hasDueDateSetAndOtherPartyWithoutHearingOption(sscsCaseData)) {
            sscsCaseData.setState(State.NOT_LISTABLE);
        } else {
            sscsCaseData.setState(State.READY_TO_LIST);
        }
    }

    private static boolean hasDueDateSetAndOtherPartyWithoutHearingOption(SscsCaseData sscsCaseData) {
        return StringUtils.isNotBlank(sscsCaseData.getDwpDueDate())
                && !everyOtherPartyHasAtLeastOneHearingOption(sscsCaseData);
    }

    private static boolean everyOtherPartyHasAtLeastOneHearingOption(SscsCaseData sscsCaseData) {
        if (sscsCaseData.getOtherParties() != null) {
            return sscsCaseData.getOtherParties().stream().noneMatch(otherParty -> otherParty.getValue().getHearingOptions() == null);
        } else {
            return false;
        }
    }

    @NotNull
    private static int getMaxId(List<CcdValue<OtherParty>> otherParties) {
        List<Integer> currentIds = new ArrayList<>();
        otherParties.stream().forEach(o -> {
            OtherParty otherParty = o.getValue();
            if (otherParty.getId() != null) {
                currentIds.add(Integer.parseInt(otherParty.getId()));
            }
            if (otherParty.getAppointee() != null && otherParty.getAppointee().getId() != null) {
                currentIds.add(Integer.parseInt(otherParty.getAppointee().getId()));
            }
            if (otherParty.getRep() != null && otherParty.getRep().getId() != null) {
                currentIds.add(Integer.parseInt(otherParty.getRep().getId()));
            }
        });
        return currentIds.stream().max(Comparator.naturalOrder()).orElse(0);
    }
}
