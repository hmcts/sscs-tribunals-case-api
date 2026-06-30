package uk.gov.hmcts.reform.sscs.helper.mapping;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.model.service.hearingvalues.PartyFlagsMap.DISABLED_ACCESS;
import static uk.gov.hmcts.reform.sscs.model.service.hearingvalues.PartyFlagsMap.DWP_PHME;
import static uk.gov.hmcts.reform.sscs.model.service.hearingvalues.PartyFlagsMap.DWP_UCB;
import static uk.gov.hmcts.reform.sscs.model.service.hearingvalues.PartyFlagsMap.HEARING_LOOP;
import static uk.gov.hmcts.reform.sscs.model.service.hearingvalues.PartyFlagsMap.IS_CONFIDENTIAL_CASE;
import static uk.gov.hmcts.reform.sscs.model.service.hearingvalues.PartyFlagsMap.SIGN_LANGUAGE_TYPE;
import static uk.gov.hmcts.reform.sscs.model.service.hearingvalues.PartyFlagsMap.URGENT_CASE;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import uk.gov.hmcts.reform.sscs.ccd.domain.Adjournment;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.model.service.hearingvalues.CaseFlags;
import uk.gov.hmcts.reform.sscs.model.service.hearingvalues.PartyFlags;

class PartyFlagsMappingTest extends HearingsMappingBase {

    @DisplayName("Should Add The Mappings Given The Values Are Not Null")
    @Test
    void shouldAddTheMappingsGivenTheValuesAreNotNull() {

        SscsCaseData caseData = SscsCaseData.builder()
            .dwpPhme("dwpPHME")
            .dwpUcb("dwpUCB")
            .urgentCase(YES.toString())
            .adjournment(Adjournment.builder()
                .interpreterLanguage(new DynamicList("adjournCaseInterpreterLanguage"))
                .build())
            .isConfidentialCase(YES)
            .appeal(Appeal.builder().hearingOptions(
                HearingOptions.builder()
                    .signLanguageType("signLanguageType")
                    .arrangements(List.of("disabledAccess", "hearingLoop"))
                    .build()).build())
            .build();

        List<PartyFlags> actual = PartyFlagsMapping.getPartyFlags(caseData);

        assertThat(actual)
            .extracting("flagId")
            .contains(
                DISABLED_ACCESS.getFlagId(),
                SIGN_LANGUAGE_TYPE.getFlagId(),
                HEARING_LOOP.getFlagId(),
                IS_CONFIDENTIAL_CASE.getFlagId(),
                DWP_UCB.getFlagId(),
                DWP_PHME.getFlagId(),
                URGENT_CASE.getFlagId()
            );
    }

    @DisplayName("Should Not Throw Null Pointer When Chained Values In Case Data Is Null")
    @Test
    void shouldNotThrowNullPointerWhenChainedValuesInCaseDataIsNull() {
        SscsCaseData caseData = SscsCaseData.builder()
            .dwpPhme(null)
            .dwpUcb(null)
            .urgentCase(null)
            .adjournment(Adjournment.builder()
                .interpreterLanguage(null)
                .build())
            .isConfidentialCase(null)
            .appeal(Appeal.builder().hearingOptions(
                HearingOptions.builder()
                    .signLanguageType(null)
                    .arrangements(
                        List.of("", ""))
                    .build()).build())
            .build();
        NullPointerException npe = null;
        try {
            PartyFlagsMapping.getPartyFlags(caseData);
        } catch (NullPointerException ex) {
            npe = ex;
        }
        assertThat(npe).isNull();
    }

    @DisplayName("mapSignLanguageType returns flagParameterised Tests")
    @ParameterizedTest
    @ValueSource(strings = { "British Sign Language (BSL)", "New Zealand Sign Language (NZSL)"})
    void mapSignLanguageType(String signLanguageType) {
        SscsCaseData caseData = SscsCaseData.builder()
            .appeal(Appeal.builder().hearingOptions(
                HearingOptions.builder()
                    .signLanguageType(signLanguageType)
                    .build()).build()).build();
        PartyFlags result = PartyFlagsMapping.signLanguage(caseData);

        assertThat(result).isEqualTo(PartyFlags.builder()
            .flagId("44")
            .flagParentId("10")
            .flagDescription("Sign Language Interpreter")
            .build());
    }

    @DisplayName("mapSignLanguageType returns null Parameterised Tests")
    @ParameterizedTest
    @NullAndEmptySource
    void mapSignLanguageTypeNull(String signLanguageType) {
        SscsCaseData caseData = SscsCaseData.builder()
            .appeal(Appeal.builder().hearingOptions(
                HearingOptions.builder()
                    .signLanguageType(signLanguageType)
                    .build()).build()).build();
        PartyFlags result = PartyFlagsMapping.signLanguage(caseData);

        assertThat(result).isNull();
    }

    @DisplayName("disabledAccess when flag is returnedParameterised Tests")
    @ParameterizedTest
    @ValueSource(strings = {"disabledAccess|hearingLoop", "disabledAccess"})
    void disabledAccessReturnsFlag(String arrangements) {
        HearingOptions hearingOptions = HearingOptions.builder()
            .arrangements(nonNull(arrangements) ? splitCsvParamArray(arrangements) : null)
            .build();

        SscsCaseData caseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .hearingOptions(hearingOptions)
                .build())
            .build();

        PartyFlags result = PartyFlagsMapping.disabledAccess(caseData);

        assertThat(result).isEqualTo(PartyFlags.builder()
            .flagId("21")
            .flagParentId("6")
            .flagDescription("Step free / wheelchair access")
            .build());
    }

    @DisplayName("disabledAccess when Null is returned Parameterised Tests")
    @ParameterizedTest
    @ValueSource(strings = {"hearingLoop"})
    @NullAndEmptySource
    void disabledAccessReturnsNull(String arrangements) {
        HearingOptions hearingOptions = HearingOptions.builder()
            .arrangements(nonNull(arrangements) ? splitCsvParamArray(arrangements) : null)
            .build();

        SscsCaseData caseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .hearingOptions(hearingOptions)
                .build())
            .build();

        PartyFlags result = PartyFlagsMapping.disabledAccess(caseData);

        assertThat(result).isNull();
    }

    @DisplayName("disabledAccess when no hearing options Parameterised Tests")
    @Test
    void disabledAccessNoHearingOptions() {
        SscsCaseData caseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .hearingOptions(null)
                .build())
            .build();

        PartyFlags result = PartyFlagsMapping.disabledAccess(caseData);

        assertThat(result).isNull();
    }

    @DisplayName("hearingLoop when flag is returnedParameterised Tests")
    @ParameterizedTest
    @ValueSource(strings = {"hearingLoop|disabledAccess", "hearingLoop"})
    void hearingLoopReturnsFlag(String arrangements) {
        HearingOptions hearingOptions = HearingOptions.builder()
            .arrangements(nonNull(arrangements) ? splitCsvParamArray(arrangements) : null)
            .build();

        SscsCaseData caseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .hearingOptions(hearingOptions)
                .build())
            .build();

        PartyFlags result = PartyFlagsMapping.hearingLoop(caseData);

        assertThat(result).isEqualTo(PartyFlags.builder()
            .flagId("45")
            .flagParentId("11")
            .flagDescription("Hearing loop (hearing enhancement system)")
            .build());
    }

    @DisplayName("hearingLoop when Null is returned Parameterised Tests")
    @ParameterizedTest
    @ValueSource(strings = {"disabledAccess"})
    @NullAndEmptySource
    void hearingLoopReturnsNull(String arrangements) {
        HearingOptions hearingOptions = HearingOptions.builder()
            .arrangements(nonNull(arrangements) ? splitCsvParamArray(arrangements) : null)
            .build();

        SscsCaseData caseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .hearingOptions(hearingOptions)
                .build())
            .build();

        PartyFlags result = PartyFlagsMapping.hearingLoop(caseData);

        assertThat(result).isNull();
    }

    @DisplayName("hearingLoop when no hearing options Parameterised Tests")
    @Test
    void hearingLoopNoHearingOptions() {
        SscsCaseData caseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .hearingOptions(null)
                .build())
            .build();

        PartyFlags result = PartyFlagsMapping.hearingLoop(caseData);

        assertThat(result).isNull();
    }

    @DisplayName("confidentialCase return Flag")
    @Test
    void confidentialCase() {
        SscsCaseData caseData = SscsCaseData.builder()
            .isConfidentialCase(YES)
            .build();

        PartyFlags result = PartyFlagsMapping.confidentialCase(caseData);

        assertThat(result).isEqualTo(PartyFlags.builder()
            .flagId("53")
            .flagParentId("2")
            .flagDescription("Confidential address")
            .build());
    }

    @DisplayName("confidentialCase returns null Parameterised Tests")
    @ParameterizedTest
    @EnumSource(value = YesNo.class, names = {"NO"})
    @NullSource
    void confidentialCase(YesNo isConfidentialCase) {
        SscsCaseData caseData = SscsCaseData.builder()
            .isConfidentialCase(isConfidentialCase)
            .build();

        PartyFlags result = PartyFlagsMapping.confidentialCase(caseData);

        assertThat(result).isNull();
    }

    @DisplayName("dwpUcb return Flag")
    @Test
    void dwpUcb() {
        SscsCaseData caseData = SscsCaseData.builder()
            .dwpUcb("dwpUcb")
            .build();

        PartyFlags result = PartyFlagsMapping.dwpUcb(caseData);

        assertThat(result).isEqualTo(PartyFlags.builder()
            .flagId("56")
            .flagParentId("2")
            .flagDescription("Unacceptable customer behaviour")
            .build());
    }

    @DisplayName("dwpUcb returns null Parameterised Tests")
    @ParameterizedTest
    @NullAndEmptySource
    void dwpUcb(String dwpUcb) {
        SscsCaseData caseData = SscsCaseData.builder()
            .dwpUcb(dwpUcb)
            .build();

        PartyFlags result = PartyFlagsMapping.dwpUcb(caseData);

        assertThat(result).isNull();
    }

    @DisplayName("dwpPhme returns Flag")
    @Test
    void dwpPhme() {
        SscsCaseData caseData = SscsCaseData.builder()
            .dwpPhme("dwpPhme")
            .build();

        PartyFlags result = PartyFlagsMapping.dwpPhme(caseData);

        assertThat(result).isEqualTo(PartyFlags.builder()
            .flagId("63")
            .flagParentId("1")
            .flagDescription("Potentially harmful medical evidence")
            .build());
    }

    @DisplayName("dwpPhme return null Parameterised Tests")
    @ParameterizedTest
    @NullAndEmptySource
    void dwpPhme(String dwpPhme) {
        SscsCaseData caseData = SscsCaseData.builder()
            .dwpPhme(dwpPhme)
            .build();

        PartyFlags result = PartyFlagsMapping.dwpPhme(caseData);

        assertThat(result).isNull();
    }

    @DisplayName("urgentCase returns Flag")
    @Test
    void urgentCase() {
        SscsCaseData caseData = SscsCaseData.builder()
            .urgentCase("Yes")
            .build();

        PartyFlags result = PartyFlagsMapping.urgentCase(caseData);

        assertThat(result).isEqualTo(PartyFlags.builder()
            .flagId("67")
            .flagParentId("1")
            .flagDescription("Urgent flag")
            .build());
    }

    @DisplayName("urgentCase returns null Parameterised Tests")
    @ParameterizedTest
    @ValueSource(strings = {"No"})
    @NullAndEmptySource
    void urgentCase(String urgentCase) {
        SscsCaseData caseData = SscsCaseData.builder()
            .urgentCase(urgentCase)
            .build();

        PartyFlags result = PartyFlagsMapping.urgentCase(caseData);

        assertThat(result).isNull();
    }


    @DisplayName("adjournCaseInterpreterLanguage should return party flag mapping if interpreter required")
    @Test
    void partyFlagIfAdjournCaseInterpreterLanguageGiven() {
        SscsCaseData caseData = SscsCaseData.builder()
            .adjournment(Adjournment.builder()
                    .interpreterRequired(YES)
                .build())
            .appeal(Appeal.builder().hearingOptions(
                HearingOptions.builder()
                    .build())
                .build())
            .build();

        PartyFlags result = PartyFlagsMapping.getLanguageInterpreterFlag(caseData);

        assertThat(result).isEqualTo(PartyFlags.builder()
            .flagId("70")
            .flagParentId("2")
            .flagDescription("Language Interpreter")
            .build());
    }

    @DisplayName("adjournCaseInterpreterLanguage should return party flag as null if no interpreter required")
    @Test
    void partyFlagNullIfNoAdjournCaseInterpreterLanguageGiven() {
        SscsCaseData caseData = SscsCaseData.builder()
            .adjournment(Adjournment.builder()
                             .interpreterRequired(NO)
                             .build())
            .appeal(Appeal.builder().hearingOptions(
                    HearingOptions.builder()
                        .build())
                        .build())
            .build();

        PartyFlags result = PartyFlagsMapping.getLanguageInterpreterFlag(caseData);

        assertThat(result).isNull();
    }

    @DisplayName("noAdjournCaseInterpreterLanguage should not return party flag mapping")
    void adjournCaseInterpreterLanguageNull() {
        SscsCaseData caseData = SscsCaseData.builder()
            .adjournment(Adjournment.builder()
                             .interpreterRequired(null)
                             .build())
            .appeal(Appeal.builder().hearingOptions(
                    HearingOptions.builder()
                        .build())
                        .build())
            .build();

        PartyFlags result = PartyFlagsMapping.getLanguageInterpreterFlag(caseData);

        assertThat(result).isNull();
    }

    @DisplayName("get Case Flags")
    @Test
    void shouldGetCaseFlags() {
        // given
        SscsCaseData sscsCaseData = Mockito.mock(SscsCaseData.class);
        Appeal appeal = Mockito.mock(Appeal.class);
        HearingOptions hearingOptions = Mockito.mock(HearingOptions.class);
        // when
        Mockito.when(hearingOptions.getSignLanguageType()).thenReturn("British Sign Language (BSL)");
        Mockito.when(appeal.getHearingOptions()).thenReturn(hearingOptions);
        Mockito.when(sscsCaseData.getAppeal()).thenReturn(appeal);
        Mockito.when(sscsCaseData.getAdjournment()).thenReturn(Adjournment.builder().build());
        // then
        CaseFlags caseFlags = PartyFlagsMapping.getCaseFlags(sscsCaseData);
        assertEquals("", caseFlags.getFlagAmendUrl());
        assertEquals(1, caseFlags.getFlags().size());
        assertEquals(SIGN_LANGUAGE_TYPE.getFlagId(), caseFlags.getFlags().stream().findFirst().orElseThrow().getFlagId());
    }

}
