package uk.gov.hmcts.reform.sscs.util;

import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static uk.gov.hmcts.reform.sscs.model.PartyItemList.*;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@Slf4j
public class SscsUtil {

    private SscsUtil() {
        //
    }

    public static List<DynamicListItem> getPartiesOnCase(SscsCaseData sscsCaseData, boolean includeDwp, boolean includeHmcts) {
        List<DynamicListItem> listOptions = new ArrayList<>();

        listOptions.add(new DynamicListItem(APPELLANT.getCode(), APPELLANT.getLabel()));
        if (includeDwp) {
            listOptions.add(new DynamicListItem(DWP.getCode(), DWP.getLabel()));
        }
        if (sscsCaseData.isThereAJointParty()) {
            listOptions.add(new DynamicListItem(JOINT_PARTY.getCode(), JOINT_PARTY.getLabel()));
        }

        if (sscsCaseData.getAppeal().getRep() != null
                && equalsIgnoreCase(sscsCaseData.getAppeal().getRep().getHasRepresentative(), "yes")) {
            listOptions.add(new DynamicListItem(REPRESENTATIVE.getCode(), REPRESENTATIVE.getLabel()));
        }
        if (includeHmcts) {
            listOptions.add(new DynamicListItem(HMCTS.getCode(), HMCTS.getLabel()));
        }

        return listOptions;
    }


}
