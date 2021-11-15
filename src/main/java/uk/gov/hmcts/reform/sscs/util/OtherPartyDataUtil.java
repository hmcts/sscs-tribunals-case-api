package uk.gov.hmcts.reform.sscs.util;

import java.util.Comparator;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;

public class OtherPartyDataUtil {

    private OtherPartyDataUtil() {
        //not called
    }

    public static void assignOtherPartyId(List<CcdValue<OtherParty>> otherParties) {
        int maxId = 0;
        for (CcdValue<OtherParty> otherParty : otherParties) {
            if (otherParty.getValue().getId() != null) {
                maxId = Integer.parseInt(otherParty.getValue().getId());
            } else {
                otherParty.getValue().setId(Integer.toString(++maxId));
            }
        }
    }

    @NotNull
    public static Comparator<CcdValue<OtherParty>> getIdComparator() {
        return new Comparator<CcdValue<OtherParty>>() {
            @Override
            public int compare(CcdValue<OtherParty> a, CcdValue<OtherParty> b) {
                if (a.getValue().getId() == null) {
                    return b.getValue().getId() == null ? 0 : 1;
                } else if (b.getValue().getId() == null) {
                    return -1;
                } else {
                    return a.getValue().getId().compareTo(b.getValue().getId());
                }
            }
        };
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
        before.sort(getIdComparator());
        after.sort(getIdComparator());
        for (int i = 0; i < before.size(); i++) {
            if (!before.get(i).getValue().getId().equals(after.get(i).getValue().getId())) {
                return true;
            }
        }
        return false;
    }
}
