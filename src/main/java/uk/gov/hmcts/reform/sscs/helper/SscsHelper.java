package uk.gov.hmcts.reform.sscs.helper;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.*;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.dwpuploadresponse.DwpUploadResponseAboutToSubmitHandler.NEW_OTHER_PARTY_RESPONSE_DUE_DAYS;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.util.DateTimeUtils;
import uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtil;

@Slf4j
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
        log.info("entering updateDirectionDueDateByAnAmountOfDays for caseId: {}", sscsCaseData.getCcdCaseId());
        if (!OtherPartyDataUtil.isValidBenefitTypeForConfidentiality(sscsCaseData)) {
            return;
        }

        if (isEmpty(sscsCaseData.getDirectionDueDate()) && hasNewOtherPartyEntryAdded(sscsCaseData)) {
            log.info("getDirectionDueDate is isEmpty and hasNewOtherPartyEntryAdded for caseId: {}", sscsCaseData.getCcdCaseId());
            sscsCaseData.setDirectionDueDate(DateTimeUtils.generateDwpResponseDueDate(NEW_OTHER_PARTY_RESPONSE_DUE_DAYS));
        } else if (!isEmpty(sscsCaseData.getDirectionDueDate())) {
            LocalDate directionDueDate = LocalDate.parse(sscsCaseData.getDirectionDueDate());
            long dueDateLength = ChronoUnit.DAYS.between(LocalDate.now(), directionDueDate);
            log.info("getDirectionDueDate is no isEmpty and dueDateLength value is {} for caseId: {}", dueDateLength, sscsCaseData.getCcdCaseId());
            if (dueDateLength <= 14) {
                sscsCaseData.setDirectionDueDate(DateTimeUtils.generateDwpResponseDueDate(NEW_OTHER_PARTY_RESPONSE_DUE_DAYS));
            }
        }
    }
}
