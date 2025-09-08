package uk.gov.hmcts.reform.sscs.helper.mapping;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDurationType.NON_STANDARD;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.getDurationForAdjournment;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.hasChannelChangedForAdjournment;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.hasInterpreterChangedForAdjournment;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDurationType;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDurationUnits;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.exception.ListingException;
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

    public static Integer getHearingDuration(SscsCaseData caseData, ReferenceDataServiceHolder refData) throws ListingException {
        Integer duration;
        HearingDurationsService hearingDurationsService = refData.getHearingDurations();
        String caseId = caseData.getCcdCaseId();
        boolean adjournmentInProgress = isYes(caseData.getAdjournment().getAdjournmentInProgress());
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
            return overrideDuration;
        }
        Integer defaultListingDuration = OverridesMapping.getDefaultListingValues(caseData).getDuration();
        // or we set based on existing S&L default listing value for duration if present
        if (nonNull(defaultListingDuration) && defaultListingDuration >= MIN_HEARING_DURATION) {
            log.info("Hearing Duration for Case ID {} set as existing defaultListingDuration value {}", caseId, defaultListingDuration);
            return defaultListingDuration;
        }
        if (caseData.isIbcCase()) {
            throw new ListingException("Hearing duration is required to list case");
        }
        // otherwise we set duration based on existing duration values ref data json
        duration = hearingDurationsService.getHearingDurationBenefitIssueCodes(caseData);
        if (nonNull(duration)) {
            log.info("Hearing Duration for Case ID {} set as Benefit Code value {}", caseId, duration);
            return duration;
        }
        log.info("Hearing Duration cannot be mapped for case ID {}", caseId);
        throw new ListingException("Hearing duration is required to list case");
    }

    public static Integer getHearingDurationAdjournment(SscsCaseData caseData, HearingDurationsService hearingDurationsService) throws ListingException {
        AdjournCaseNextHearingDurationType durationType = caseData.getAdjournment().getNextHearingListingDurationType();
        if (!NON_STANDARD.equals(durationType)) {
            Integer duration = getDurationForAdjournment(caseData, hearingDurationsService);
            HearingOptions hearingOptions = Optional.ofNullable(caseData.getAppeal().getHearingOptions()).orElse(HearingOptions.builder().build());
            String hearingType = caseData.getAppeal().getHearingType();
            boolean hasInterpreterChannelChanged = hasChannelChangedForAdjournment(caseData, hearingType) || hasInterpreterChangedForAdjournment(caseData, hearingOptions);
            if (hasInterpreterChannelChanged && isNull(duration)) {
                throw new ListingException("Hearing duration is required to list case");
            } else if (!hasInterpreterChannelChanged) {
                Integer overrideDuration = OverridesMapping.getOverrideFields(caseData).getDuration();
                if (nonNull(overrideDuration) && overrideDuration >= MIN_HEARING_DURATION) {
                    return overrideDuration;
                }
            }
            return duration;
        }
        Integer nextDuration = caseData.getAdjournment().getNextHearingListingDuration();
        if (nonNull(nextDuration)) {
            log.debug("existingDuration with NON_STANDARD for caseId={}", caseData.getCcdCaseId());
            return handleNonStandardDuration(caseData, nextDuration);
        }
        if (caseData.isIbcCase()) {
            throw new ListingException("Hearing duration is required to list case");
        }
        log.debug("getHearingDurationBenefitIssueCodes for caseId={}", caseData.getCcdCaseId());
        return hearingDurationsService.getHearingDurationBenefitIssueCodes(caseData);
    }

    private static Integer handleNonStandardDuration(SscsCaseData caseData, Integer duration) {
        if (duration == null) {
            return null;
        }
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
