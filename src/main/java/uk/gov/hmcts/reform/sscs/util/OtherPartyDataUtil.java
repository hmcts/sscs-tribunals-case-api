package uk.gov.hmcts.reform.sscs.util;

import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;


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

    public static void checkConfidentiality(SscsCaseData sscsCaseData) {
        if (sscsCaseData.getAppeal().getBenefitType() != null
                && Benefit.CHILD_SUPPORT.getShortName().equals(sscsCaseData.getAppeal().getBenefitType().getCode())) {
            if ((sscsCaseData.getAppeal().getAppellant() != null
                    && sscsCaseData.getAppeal().getAppellant().getConfidentialityRequired() != null
                    && YesNo.isYes(sscsCaseData.getAppeal().getAppellant().getConfidentialityRequired()))
                    || otherPartyHasConfidentiality(sscsCaseData)) {
                sscsCaseData.setIsConfidentialCase(YesNo.YES);
            } else {
                sscsCaseData.setIsConfidentialCase(null);
            }
        }
    }

    public static boolean isOtherPartyPresent(SscsCaseData sscsCaseData) {
        return sscsCaseData.getOtherParties() != null && sscsCaseData.getOtherParties().size() > 0;
    }

    private static boolean otherPartyHasConfidentiality(SscsCaseData sscsCaseData) {
        if (sscsCaseData.getOtherParties() != null) {
            Optional otherParty = sscsCaseData.getOtherParties().stream().filter(op -> YesNo.isYes(op.getValue().getConfidentialityRequired())).findAny();
            if (otherParty.isPresent()) {
                return true;
            }
        }
        return false;
    }
}
