package uk.gov.hmcts.reform.sscs.helper;

import static uk.gov.hmcts.reform.sscs.ccd.domain.State.*;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.dwpuploadresponse.DwpUploadResponseAboutToSubmitHandler.NEW_OTHER_PARTY_RESPONSE_DUE_DAYS;
import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtil.isValidBenefitTypeForConfidentiality;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.util.DateTimeUtils;

public class SscsHelper {

    private static final List<State> PRE_VALID_STATES = new ArrayList<>(Arrays.asList(INCOMPLETE_APPLICATION, INCOMPLETE_APPLICATION_INFORMATION_REQUESTED, INTERLOCUTORY_REVIEW_STATE));

    private SscsHelper() {
    }

    public static List<State> getPreValidStates() {
        return PRE_VALID_STATES;
    }

    private static boolean hasNewOtherPartyEntryAdded(SscsCaseData sscsCaseData) {
        Optional<CcdValue<OtherParty>> otherParty = Optional.ofNullable(sscsCaseData.getOtherParties()).orElse(Collections.emptyList()).stream().filter(
                op -> YesNo.isYes(op.getValue().getSendNewOtherPartyNotification())
        ).findFirst();
        return otherParty.isPresent();
    }

    public static void updateDirectionDueDateByAnAmountOfDays(SscsCaseData sscsCaseData) {
        if (!isValidBenefitTypeForConfidentiality(sscsCaseData)) {
            return;
        }

        if (sscsCaseData.getDirectionDueDate() == null && hasNewOtherPartyEntryAdded(sscsCaseData)) {
            sscsCaseData.setDirectionDueDate(DateTimeUtils.generateDwpResponseDueDate(NEW_OTHER_PARTY_RESPONSE_DUE_DAYS));
        } else if (sscsCaseData.getDirectionDueDate() != null) {
            Optional<LocalDate> directionDueDate = DateTimeUtils.getLocalDate(sscsCaseData.getDirectionDueDate());
            long dueDateLength = ChronoUnit.DAYS.between(LocalDate.now(), directionDueDate.get());
            if (dueDateLength <= 14) {
                sscsCaseData.setDirectionDueDate(DateTimeUtils.generateDwpResponseDueDate(NEW_OTHER_PARTY_RESPONSE_DUE_DAYS));
            }
        }
    }
}
