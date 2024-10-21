package uk.gov.hmcts.reform.sscs.helper.mapping;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.BooleanUtils.isTrue;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;
import static uk.gov.hmcts.reform.sscs.helper.mapping.HearingsDetailsMapping.isCaseUrgent;
import static uk.gov.hmcts.reform.sscs.model.service.hearingvalues.PartyFlagsMap.DISABLED_ACCESS;
import static uk.gov.hmcts.reform.sscs.model.service.hearingvalues.PartyFlagsMap.DWP_PHME;
import static uk.gov.hmcts.reform.sscs.model.service.hearingvalues.PartyFlagsMap.DWP_UCB;
import static uk.gov.hmcts.reform.sscs.model.service.hearingvalues.PartyFlagsMap.HEARING_LOOP;
import static uk.gov.hmcts.reform.sscs.model.service.hearingvalues.PartyFlagsMap.IS_CONFIDENTIAL_CASE;
import static uk.gov.hmcts.reform.sscs.model.service.hearingvalues.PartyFlagsMap.LANGUAGE_INTERPRETER_FLAG;
import static uk.gov.hmcts.reform.sscs.model.service.hearingvalues.PartyFlagsMap.SIGN_LANGUAGE_TYPE;
import static uk.gov.hmcts.reform.sscs.model.service.hearingvalues.PartyFlagsMap.URGENT_CASE;
import static uk.gov.hmcts.reform.sscs.utility.HearingChannelUtil.isInterpreterRequired;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.model.service.hearingvalues.CaseFlags;
import uk.gov.hmcts.reform.sscs.model.service.hearingvalues.PartyFlags;

public final class PartyFlagsMapping {

    private PartyFlagsMapping() {

    }

    public static List<PartyFlags> getPartyFlags(SscsCaseData caseData) {
        return Stream.of(
                signLanguage(caseData),
                disabledAccess(caseData),
                hearingLoop(caseData),
                confidentialCase(caseData),
                dwpUcb(caseData),
                dwpPhme(caseData),
                urgentCase(caseData),
                getLanguageInterpreterFlag(caseData)
        ).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public static PartyFlags signLanguage(SscsCaseData caseData) {
        String signLanguageType = Optional
                .ofNullable(caseData.getAppeal())
                .map(Appeal::getHearingOptions)
                .map(HearingOptions::getSignLanguageType).orElse(null);
        PartyFlags partyFlagsSignLanguage = null;
        if (isNotBlank(signLanguageType)) {
            partyFlagsSignLanguage = PartyFlags.builder()
                    .flagId(SIGN_LANGUAGE_TYPE.getFlagId())
                    .flagDescription(SIGN_LANGUAGE_TYPE.getFlagDescription())
                    .flagParentId(SIGN_LANGUAGE_TYPE.getParentId())
                    .build();
        }
        return partyFlagsSignLanguage;
    }

    public static PartyFlags disabledAccess(SscsCaseData caseData) {
        HearingOptions options = Optional
                .ofNullable(caseData.getAppeal())
                .map(Appeal::getHearingOptions).orElse(null);
        PartyFlags partyFlagsDisabledAccess = null;

        if (nonNull(options) && isTrue(options.wantsAccessibleHearingRoom())) {
            partyFlagsDisabledAccess = PartyFlags.builder()
                    .flagId(DISABLED_ACCESS.getFlagId())
                    .flagDescription(DISABLED_ACCESS.getFlagDescription())
                    .flagParentId(DISABLED_ACCESS.getParentId()).build();
        }
        return partyFlagsDisabledAccess;
    }

    public static PartyFlags hearingLoop(SscsCaseData caseData) {
        HearingOptions options = Optional
                .ofNullable(caseData.getAppeal())
                .map(Appeal::getHearingOptions).orElse(null);
        PartyFlags hearingLoop = null;
        if (nonNull(options) && isTrue(options.wantsHearingLoop())) {
            hearingLoop = PartyFlags.builder()
                    .flagId(HEARING_LOOP.getFlagId())
                    .flagDescription(HEARING_LOOP.getFlagDescription())
                    .flagParentId(HEARING_LOOP.getParentId()).build();
        }
        return hearingLoop;
    }

    public static PartyFlags confidentialCase(SscsCaseData caseData) {
        PartyFlags confidentialCaseFlag = null;
        if (isYes(caseData.getIsConfidentialCase())) {
            confidentialCaseFlag = PartyFlags.builder()
                    .flagId(IS_CONFIDENTIAL_CASE.getFlagId())
                    .flagDescription(IS_CONFIDENTIAL_CASE.getFlagDescription())
                    .flagParentId(IS_CONFIDENTIAL_CASE.getParentId())
                    .build();
        }
        return confidentialCaseFlag;
    }

    public static PartyFlags dwpUcb(SscsCaseData caseData) {
        PartyFlags dwpUcbPartyFlag = null;
        if (isNotBlank(caseData.getDwpUcb())) {
            dwpUcbPartyFlag = PartyFlags.builder()
                    .flagId(DWP_UCB.getFlagId())
                    .flagDescription(DWP_UCB.getFlagDescription())
                    .flagParentId(DWP_UCB.getParentId()).build();
        }
        return dwpUcbPartyFlag;
    }

    public static PartyFlags dwpPhme(SscsCaseData caseData) {
        PartyFlags dwpPhmePartyFlag = null;
        if (isNotBlank(caseData.getDwpPhme())) {
            dwpPhmePartyFlag = PartyFlags.builder()
                    .flagId(DWP_PHME.getFlagId())
                    .flagDescription(DWP_PHME.getFlagDescription())
                    .flagParentId(DWP_PHME.getParentId()).build();
        }
        return dwpPhmePartyFlag;
    }

    public static PartyFlags urgentCase(SscsCaseData caseData) {
        PartyFlags urgentCasePartyFlag = null;
        if (isCaseUrgent(caseData)) {
            urgentCasePartyFlag = PartyFlags.builder()
                    .flagId(URGENT_CASE.getFlagId())
                    .flagDescription(URGENT_CASE.getFlagDescription())
                    .flagParentId(URGENT_CASE.getParentId()).build();
        }
        return urgentCasePartyFlag;
    }

    public static PartyFlags getLanguageInterpreterFlag(SscsCaseData caseData) {
        PartyFlags adjournCasePartyFlag = null;

        if (isInterpreterRequired(caseData)) {
            adjournCasePartyFlag = PartyFlags.builder()
                    .flagId(LANGUAGE_INTERPRETER_FLAG.getFlagId())
                    .flagDescription(LANGUAGE_INTERPRETER_FLAG.getFlagDescription())
                    .flagParentId(LANGUAGE_INTERPRETER_FLAG.getParentId()).build();
        }
        return adjournCasePartyFlag;
    }

    public static CaseFlags getCaseFlags(SscsCaseData sscsCaseData) {
        return CaseFlags.builder()
                .flags(PartyFlagsMapping.getPartyFlags(sscsCaseData).stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()))
                .flagAmendUrl("") //TODO Implement when present
                .build();
    }
}
