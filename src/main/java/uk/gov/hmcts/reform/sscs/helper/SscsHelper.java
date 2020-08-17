package uk.gov.hmcts.reform.sscs.helper;

import static uk.gov.hmcts.reform.sscs.ccd.domain.State.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;

public class SscsHelper {

    private static final List<State> PRE_VALID_STATES = new ArrayList<>(Arrays.asList(INCOMPLETE_APPLICATION, INCOMPLETE_APPLICATION_INFORMATION_REQUESTED, INTERLOCUTORY_REVIEW_STATE));

    private SscsHelper() {
    }

    public static List<State> getPreValidStates() {
        return PRE_VALID_STATES;
    }
}
