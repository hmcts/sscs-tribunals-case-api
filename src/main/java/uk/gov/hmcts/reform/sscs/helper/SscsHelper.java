package uk.gov.hmcts.reform.sscs.helper;

import static uk.gov.hmcts.reform.sscs.ccd.domain.State.*;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.dwpuploadresponse.DwpUploadResponseAboutToSubmitHandler.NEW_OTHER_PARTY_RESPONSE_DUE_DAYS;
import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtil.isValidBenefitTypeForConfidentiality;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.util.DateTimeUtils;

public class SscsHelper {

    private static final List<State> PRE_VALID_STATES = new ArrayList<>(Arrays.asList(INCOMPLETE_APPLICATION, INCOMPLETE_APPLICATION_INFORMATION_REQUESTED, INTERLOCUTORY_REVIEW_STATE));

    private SscsHelper() {
    }

    public static List<State> getPreValidStates() {
        return PRE_VALID_STATES;
    }

    public static void updateDirectionDueDateByAnAmountOfDays(SscsCaseData sscsCaseData) {
        if (isValidBenefitTypeForConfidentiality(sscsCaseData)
                && (sscsCaseData.getDirectionDueDate() == null || !sscsCaseData.getDirectionDueDate().equals(DateTimeUtils.generateDwpResponseDueDate(NEW_OTHER_PARTY_RESPONSE_DUE_DAYS)))) {
            sscsCaseData.setDirectionDueDate(DateTimeUtils.generateDwpResponseDueDate(NEW_OTHER_PARTY_RESPONSE_DUE_DAYS));
        }
    }
}
