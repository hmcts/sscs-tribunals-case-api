package uk.gov.hmcts.reform.sscs.util;

import static java.util.Collections.sort;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;


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

    public static boolean haveOtherPartiesChanged(List<CcdValue<OtherParty>> before, List<CcdValue<OtherParty>> after) {
        if (before == null && after == null) {
            return false;
        }
        if (before == null ^ after == null) {
            return true;
        }
        if (before.size() != after.size()) {
            return true;
        }
        List<String> beforeIds = before.stream().map(ccdValue -> ccdValue.getValue().getId()).collect(Collectors.toList());
        List<String> afterIds = after.stream().map(ccdValue -> ccdValue.getValue().getId()).collect(Collectors.toList());
        sort(beforeIds);
        sort(afterIds);
        for (int i = 0; i < beforeIds.size(); i++) {
            if (!beforeIds.get(i).equals(afterIds.get(i))) {
                return true;
            }
        }
        return false;
    }
}
