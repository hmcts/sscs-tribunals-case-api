package uk.gov.hmcts.reform.sscs.ccd.presubmit.confidentiality;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appointee;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.awaitotherpartydata.AwaitOtherPartyDataAboutToSubmitHandler;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AwaitOtherPartyDataAboutToSubmitHandlerTest {

    private AwaitOtherPartyDataAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    @BeforeEach
    void setUp() {
        handler = new AwaitOtherPartyDataAboutToSubmitHandler(true);
        sscsCaseData = SscsCaseData.builder()
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("childSupport").build()).build()).build();
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(callback.getCaseDetailsBefore()).thenReturn(Optional.empty());
    }

    @Test
    void canHandleReturnsTrue() {
        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isTrue();
    }

    @Test
    void canHandleReturnsFalseForNonAboutToSubmit() {
        assertThat(handler.canHandle(MID_EVENT, callback)).isFalse();
    }

    @Test
    void canHandleReturnsFalseWhenFeatureDisabled() {
        AwaitOtherPartyDataAboutToSubmitHandler disabledHandler = new AwaitOtherPartyDataAboutToSubmitHandler(false);

        assertThat(disabledHandler.canHandle(ABOUT_TO_SUBMIT, callback)).isFalse();
    }

    @Test
    void handleSetsConfidentialityTabToNullWhenNotChildSupport() {
        sscsCaseData.setAppeal(
            Appeal.builder().benefitType(BenefitType.builder().code(Benefit.PIP.getShortName()).build()).build());

        assertThrows(IllegalStateException.class, () -> handler.handle(ABOUT_TO_SUBMIT, callback, "Bearer token"));

    }

    @Test
    void handleBuildsConfidentialityTabForChildSupportWithAppellantAppointeeAndOtherParties() {
        LocalDateTime appellantDate = LocalDateTime.of(2020, 2, 3, 16, 5, 6);
        LocalDateTime otherPartyDate = LocalDateTime.of(2020, 2, 4, 9, 10, 11);

        Appellant appellant = Appellant.builder().name(Name.builder().firstName("John").lastName("Smith").build())
            .confidentialityRequired(YES).confidentialityRequiredChangedDate(appellantDate).isAppointee("Yes")
            .appointee(Appointee.builder().name(Name.builder().firstName("Jane").lastName("Doe").build()).build()).build();

        OtherParty otherParty1 = OtherParty.builder().id("op1").name(Name.builder().firstName("Other").lastName("One").build())
            .confidentialityRequired(NO).confidentialityRequiredChangedDate(otherPartyDate).build();

        OtherParty otherParty2 = OtherParty.builder().name(Name.builder().firstName("Other").lastName("Two").build())
            .confidentialityRequired(null).confidentialityRequiredChangedDate(null).build();

        List<CcdValue<OtherParty>> otherParties = Arrays.asList(CcdValue.<OtherParty>builder().value(otherParty1).build(), null,
            CcdValue.<OtherParty>builder().value(otherParty2).build());

        sscsCaseData.setOtherParties(otherParties);
        sscsCaseData.setAppeal(
            Appeal.builder().benefitType(BenefitType.builder().code(Benefit.CHILD_SUPPORT.getShortName()).build())
                .appellant(appellant).build());

        final OtherParty beforeOtherParty1 = OtherParty.builder().id("op1").confidentialityRequired(NO).build();
        final SscsCaseData beforeCaseData = SscsCaseData.builder()
            .appeal(Appeal.builder().appellant(Appellant.builder().confidentialityRequired(YES).build()).build())
            .otherParties(singletonList(CcdValue.<OtherParty>builder().value(beforeOtherParty1).build())).build();
        final CaseDetails<SscsCaseData> caseDetailsBefore = mock(CaseDetails.class);
        when(caseDetailsBefore.getCaseData()).thenReturn(beforeCaseData);
        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetailsBefore));

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, "Bearer token");

        final String tab = response.getData().getExtendedSscsCaseData().getConfidentialityTab();
        assertThat(tab).isEqualToNormalizingWhitespace("""
            Party | Name | Confidentiality Status | Confidentiality Status Confirmed
            -|-|-|-
            Appellant | John Smith | Yes | 3 Feb 2020, 4:05:06 pm
            Appointee | Jane Doe | Yes | 3 Feb 2020, 4:05:06 pm
            Other Party 1 | Other One | No | 4 Feb 2020, 9:10:11 am
            Other Party 2 | Other Two | Undetermined |
            """);
    }

    @ParameterizedTest
    @MethodSource("appointeeNotIncludedTestCases")
    void handleDoesNotIncludeAppointeeWhenNotMarkedAsAppointee(String isAppointee, Appointee appointee) {
        Appellant appellant = Appellant.builder().name(Name.builder().firstName("John").lastName("Smith").build())
            .confidentialityRequired(YES).isAppointee(isAppointee).appointee(appointee).build();

        sscsCaseData.setAppeal(
            Appeal.builder().benefitType(BenefitType.builder().code(Benefit.CHILD_SUPPORT.getShortName()).build())
                .appellant(appellant).build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, "Bearer token");

        String tab = response.getData().getExtendedSscsCaseData().getConfidentialityTab();
        assertThat(tab).contains("Appellant | John Smith | Yes");
        assertThat(tab).doesNotContain("Appointee |");
    }

    @Test
    void handleDoesNotIncludeAppellantOrAppointeeWhenAppellantMissing() {
        OtherParty otherParty = OtherParty.builder().name(Name.builder().firstName("Other").lastName("Party").build())
            .confidentialityRequired(YES).build();

        sscsCaseData.setOtherParties(singletonList(CcdValue.<OtherParty>builder().value(otherParty).build()));
        sscsCaseData.setAppeal(
            Appeal.builder().benefitType(BenefitType.builder().code(Benefit.CHILD_SUPPORT.getShortName()).build()).appellant(null)
                .build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, "Bearer token");

        String tab = response.getData().getExtendedSscsCaseData().getConfidentialityTab();
        assertThat(tab).contains("Other Party 1 | Other Party | Yes");
        assertThat(tab).doesNotContain("Appellant |");
        assertThat(tab).doesNotContain("Appointee |");
    }

    @Test
    void handleUsesEmptyNamesWhenNameMissing() {
        final Appellant appellant = Appellant.builder().name(null).confidentialityRequired(YES).build();

        final OtherParty otherParty = OtherParty.builder().id("op1").name(null).confidentialityRequired(NO).build();

        sscsCaseData.setOtherParties(singletonList(CcdValue.<OtherParty>builder().value(otherParty).build()));
        sscsCaseData.setAppeal(
            Appeal.builder().benefitType(BenefitType.builder().code(Benefit.CHILD_SUPPORT.getShortName()).build())
                .appellant(appellant).build());

        final OtherParty beforeOtherParty = OtherParty.builder().id("op1").confidentialityRequired(NO).build();
        final SscsCaseData beforeCaseData = SscsCaseData.builder()
            .appeal(Appeal.builder().appellant(Appellant.builder().confidentialityRequired(YES).build()).build())
            .otherParties(singletonList(CcdValue.<OtherParty>builder().value(beforeOtherParty).build())).build();
        final CaseDetails<SscsCaseData> caseDetailsBefore = mock(CaseDetails.class);
        when(caseDetailsBefore.getCaseData()).thenReturn(beforeCaseData);
        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetailsBefore));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, "Bearer token");

        String tab = response.getData().getExtendedSscsCaseData().getConfidentialityTab();
        assertThat(tab).isEqualToNormalizingPunctuationAndWhitespace("""
            Party | Name | Confidentiality Status | Confidentiality Status Confirmed
            -|-|-|-
            Appellant |  | Yes |
            Other Party 1 |  | No |
            """);
    }

    @Test
    void handleUpdatesAppellantConfidentialityRequiredChangedDateWhenConfidentialityChanges() {
        final LocalDateTime originalDate = LocalDateTime.now().minusHours(1);
        final Appellant appellant = Appellant.builder().confidentialityRequired(YES)
            .confidentialityRequiredChangedDate(originalDate).build();
        sscsCaseData.setAppeal(
            Appeal.builder().benefitType(BenefitType.builder().code(Benefit.CHILD_SUPPORT.getShortName()).build())
                .appellant(appellant).build());

        final SscsCaseData beforeCaseData = SscsCaseData.builder()
            .appeal(Appeal.builder().appellant(Appellant.builder().confidentialityRequired(NO).build()).build()).build();
        final CaseDetails<SscsCaseData> caseDetailsBefore = mock(CaseDetails.class);
        when(caseDetailsBefore.getCaseData()).thenReturn(beforeCaseData);
        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetailsBefore));

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, "Bearer token");

        assertThat(response.getData().getAppeal().getAppellant().getConfidentialityRequiredChangedDate()).isAfter(originalDate);
    }

    @Test
    void handleDoesNotUpdateAppellantConfidentialityRequiredChangedDateWhenConfidentialityUnchanged() {
        final LocalDateTime originalDate = LocalDateTime.now().minusHours(1);
        final Appellant appellant = Appellant.builder().confidentialityRequired(YES)
            .confidentialityRequiredChangedDate(originalDate).build();
        sscsCaseData.setAppeal(
            Appeal.builder().benefitType(BenefitType.builder().code(Benefit.CHILD_SUPPORT.getShortName()).build())
                .appellant(appellant).build());

        final SscsCaseData beforeCaseData = SscsCaseData.builder()
            .appeal(Appeal.builder().appellant(Appellant.builder().confidentialityRequired(YES).build()).build()).build();
        final CaseDetails<SscsCaseData> caseDetailsBefore = mock(CaseDetails.class);
        when(caseDetailsBefore.getCaseData()).thenReturn(beforeCaseData);
        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetailsBefore));

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, "Bearer token");

        assertThat(response.getData().getAppeal().getAppellant().getConfidentialityRequiredChangedDate()).isEqualTo(originalDate);
    }

    @Test
    void handleUpdatesOtherPartyConfidentialityRequiredChangedDateWhenConfidentialityChanges() {
        final LocalDateTime originalDate = LocalDateTime.now().minusHours(1);
        final OtherParty otherParty = OtherParty.builder().id("op1").confidentialityRequired(NO)
            .confidentialityRequiredChangedDate(originalDate).build();
        sscsCaseData.setOtherParties(singletonList(CcdValue.<OtherParty>builder().value(otherParty).build()));
        sscsCaseData.setAppeal(
            Appeal.builder().benefitType(BenefitType.builder().code(Benefit.CHILD_SUPPORT.getShortName()).build()).build());

        final OtherParty beforeOtherParty = OtherParty.builder().id("op1").confidentialityRequired(YES).build();
        final SscsCaseData beforeCaseData = SscsCaseData.builder()
            .otherParties(singletonList(CcdValue.<OtherParty>builder().value(beforeOtherParty).build())).build();
        final CaseDetails<SscsCaseData> caseDetailsBefore = mock(CaseDetails.class);
        when(caseDetailsBefore.getCaseData()).thenReturn(beforeCaseData);
        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetailsBefore));

        handler.handle(ABOUT_TO_SUBMIT, callback, "Bearer token");

        assertThat(otherParty.getConfidentialityRequiredChangedDate()).isAfter(originalDate);
    }

    @Test
    void handleDoesNotUpdateOtherPartyConfidentialityRequiredChangedDateWhenConfidentialityUnchanged() {
        final LocalDateTime originalDate = LocalDateTime.now().minusHours(1);
        final OtherParty otherParty = OtherParty.builder().id("op1").confidentialityRequired(YES)
            .confidentialityRequiredChangedDate(originalDate).build();
        sscsCaseData.setOtherParties(singletonList(CcdValue.<OtherParty>builder().value(otherParty).build()));
        sscsCaseData.setAppeal(
            Appeal.builder().benefitType(BenefitType.builder().code(Benefit.CHILD_SUPPORT.getShortName()).build()).build());

        final OtherParty beforeOtherParty = OtherParty.builder().id("op1").confidentialityRequired(YES).build();
        final SscsCaseData beforeCaseData = SscsCaseData.builder()
            .otherParties(singletonList(CcdValue.<OtherParty>builder().value(beforeOtherParty).build())).build();
        final CaseDetails<SscsCaseData> caseDetailsBefore = mock(CaseDetails.class);
        when(caseDetailsBefore.getCaseData()).thenReturn(beforeCaseData);
        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetailsBefore));

        handler.handle(ABOUT_TO_SUBMIT, callback, "Bearer token");

        assertThat(otherParty.getConfidentialityRequiredChangedDate()).isEqualTo(originalDate);
    }

    @Test
    void handleDoesNotUpdateOtherPartyConfidentialityRequiredChangedDateWhenCurrentConfidentialityRequiredIsNull() {
        final OtherParty otherParty = OtherParty.builder().id("op1")
            .confidentialityRequired(null)
            .build();
        sscsCaseData.setOtherParties(singletonList(CcdValue.<OtherParty>builder().value(otherParty).build()));
        sscsCaseData.setAppeal(
            Appeal.builder().benefitType(BenefitType.builder().code(Benefit.CHILD_SUPPORT.getShortName()).build()).build());

        final OtherParty beforeOtherParty = OtherParty.builder().id("op1").confidentialityRequired(NO).build();
        final SscsCaseData beforeCaseData = SscsCaseData.builder()
            .otherParties(singletonList(CcdValue.<OtherParty>builder().value(beforeOtherParty).build())).build();
        final CaseDetails<SscsCaseData> caseDetailsBefore = mock(CaseDetails.class);
        when(caseDetailsBefore.getCaseData()).thenReturn(beforeCaseData);
        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetailsBefore));

        handler.handle(ABOUT_TO_SUBMIT, callback, "Bearer token");

        assertThat(otherParty.getConfidentialityRequiredChangedDate()).isNull();
    }

    private static Stream<Arguments> appointeeNotIncludedTestCases() {
        return Stream.of(org.junit.jupiter.params.provider.Arguments.of("No",
                Appointee.builder().name(Name.builder().firstName("Jane").lastName("Doe").build()).build()),
            org.junit.jupiter.params.provider.Arguments.of("Yes", null));
    }

}