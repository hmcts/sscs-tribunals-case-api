package uk.gov.hmcts.reform.sscs.ccd.presubmit.adjourncase;

import java.util.Comparator;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;

public class DynamicListItemComparator implements Comparator<DynamicListItem> {

    @Override
    public int compare(DynamicListItem o1, DynamicListItem o2) {
        return o1.getLabel().compareTo(o2.getLabel());
    }
}
