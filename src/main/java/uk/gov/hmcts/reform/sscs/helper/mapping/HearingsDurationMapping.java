package uk.gov.hmcts.reform.sscs.helper.mapping;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDurationType.NON_STANDARD;
import static uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDurationType.STANDARD;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;
import static uk.gov.hmcts.reform.sscs.utility.HearingChannelUtil.isInterpreterRequired;

import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDurationType;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDurationUnits;
import uk.gov.hmcts.reform.sscs.ccd.domain.Adjournment;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.reference.data.service.HearingDurationsService;
import uk.gov.hmcts.reform.sscs.service.holder.ReferenceDataServiceHolder;

@Slf4j
public final class HearingsDurationMapping {
    public static final int DURATION_SESSIONS_MULTIPLIER = 165;
    public static final int DURATION_DEFAULT = 60;
    public static final int MIN_HEARING_DURATION = 30;
    public static final int MIN_HEARING_SESSION_DURATION = 1;

    private HearingsDurationMapping() {
    }

    public static int getHearingDuration(SscsCaseData caseData, ReferenceDataServiceHolder refData) {
        Integer duration;
        HearingDurationsService hearingDurationsService = refData.getHearingDurations();
        String caseId = caseData.getCcdCaseId();
        boolean adjournmentInProgress = refData.isAdjournmentFlagEnabled() && isYes(caseData.getAdjournment().getAdjournmentInProgress());
        // adjournment values take precedence over override fields if adjournment in progress
        if (adjournmentInProgress) {
            duration = getHearingDurationAdjournment(caseData, hearingDurationsService);
            if (nonNull(duration)) {
                log.info("Hearing Duration for Case ID {} set as Adjournment value {}", caseId, duration);
                return duration;
            }
        }
        Integer overrideDuration = OverridesMapping.getOverrideFields(caseData).getDuration();
        // if no adjournment in progress, we first try to set the override value if present
        if (nonNull(overrideDuration) && overrideDuration >= MIN_HEARING_DURATION) {
            log.info("Hearing Duration for Case ID {} set as existing Override Field value {}", caseId, overrideDuration);
            return handleStandardDuration(caseData, overrideDuration);
        }
        Integer defaultListingDuration = OverridesMapping.getDefaultListingValues(caseData).getDuration();
        // or we set based on existing S&L default listing value for duration if present
        if (nonNull(defaultListingDuration) && defaultListingDuration >= MIN_HEARING_DURATION) {
            log.info("Hearing Duration for Case ID {} set as existing defaultListingDuration value {}", caseId, defaultListingDuration);
            return handleStandardDuration(caseData, defaultListingDuration);
        }
        // otherwise we set duration based on existing duration values ref data json
        duration = hearingDurationsService.getHearingDurationBenefitIssueCodes(caseData);
        if (nonNull(duration)) {
            log.debug("Hearing Duration for Case ID {} set as Benefit Code value {}", caseId, duration);
            return duration;
        }
        // else return default value (60)
        log.debug("Hearing Duration for Case ID {} set as default value {}", caseId, DURATION_DEFAULT);
        return DURATION_DEFAULT;
    }

    public static Integer getHearingDurationAdjournment(SscsCaseData caseData, HearingDurationsService hearingDurationsService) {
        AdjournCaseNextHearingDurationType durationType = caseData.getAdjournment().getNextHearingListingDurationType();

        Integer existingDuration = OverridesMapping.getDefaultListingValues(caseData).getDuration();
        if (nonNull(existingDuration) && durationType == STANDARD) {
            log.debug("existingDuration with STANDARD for caseId={}", caseData.getCcdCaseId());
            existingDuration = handleAdjournmentHearingType(caseData, hearingDurationsService, existingDuration);
            return handleStandardDuration(caseData, existingDuration);
        }

        Integer nextDuration = caseData.getAdjournment().getNextHearingListingDuration();
        if (nonNull(nextDuration) && durationType == NON_STANDARD) {
            log.debug("existingDuration with NON_STANDARD for caseId={}", caseData.getCcdCaseId());
            return handleNonStandardDuration(caseData, nextDuration);
        }

        log.debug("getHearingDurationBenefitIssueCodes for caseId={}", caseData.getCcdCaseId());
        return hearingDurationsService.getHearingDurationBenefitIssueCodes(caseData);
    }

    private static Integer handleAdjournmentHearingType(SscsCaseData caseData, HearingDurationsService durationsService, Integer duration) {
        Adjournment adjournment = caseData.getAdjournment();
        if (!adjournment.getTypeOfHearing().equals(adjournment.getTypeOfNextHearing())) {
            // update override value here otherwise it will not be correctly amended when change of hearing type
            Integer newDuration = durationsService.getHearingDurationBenefitIssueCodes(caseData);
            caseData.getSchedulingAndListingFields().getOverrideFields().setDuration(newDuration);
            return newDuration;
        }
        return duration;
    }

    private static Integer handleStandardDuration(SscsCaseData caseData, Integer duration) {
        if (isYes(caseData.getAppeal().getHearingOptions().getWantsToAttend())
                && isInterpreterRequired(caseData)) {
            // if interpreter, add 30 minutes to existing duration
            return duration + MIN_HEARING_DURATION;
        }
        return duration;
    }

    private static Integer handleNonStandardDuration(SscsCaseData caseData, Integer duration) {
        AdjournCaseNextHearingDurationUnits units = caseData.getAdjournment().getNextHearingListingDurationUnits();
        if (units == AdjournCaseNextHearingDurationUnits.SESSIONS && duration >= MIN_HEARING_SESSION_DURATION) {
            return duration * DURATION_SESSIONS_MULTIPLIER;
        } else if (units == AdjournCaseNextHearingDurationUnits.MINUTES && duration >= MIN_HEARING_DURATION) {
            return duration;
        }

        return DURATION_DEFAULT;
    }

    public static List<String> getNonStandardHearingDurationReasons() {
        // TODO Future Work
        return Collections.emptyList();
    }
}
