package uk.gov.hmcts.reform.sscs.ccd.presubmit.confidentiality;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConfidentialityTabAboutToSubmitHandlerTest {

    private ConfidentialityTabAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private CaseDetails<SscsCaseData> caseDetailsBefore;

    private SscsCaseData sscsCaseData;

    @BeforeEach
    void setUp() {
        handler = new ConfidentialityTabAboutToSubmitHandler(true);
        sscsCaseData = SscsCaseData.builder()
                                   .appeal(
                                       Appeal.builder().benefitType(BenefitType.builder().code("childSupport").build()).build())
                                   .build();
        when(callback.getEvent()).thenReturn(EventType.CASE_UPDATED);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(callback.getCaseDetailsBefore()).thenReturn(Optional.empty());
    }

    @ParameterizedTest
    @EnumSource(value = EventType.class, names = {
        "DWP_UPLOAD_RESPONSE",
        "UPDATE_OTHER_PARTY_DATA",
        "INCOMPLETE_APPLICATION_RECEIVED",
        "CASE_UPDATED",
        "ACTION_HEARING_RECORDING_REQUEST"
    })
    void canHandleReturnsTrueForEachSupportedEventType(final EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);
        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isTrue();
    }

    @Test
    void canHandleReturnsFalseForUnsupportedEventType() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isFalse();
    }

    @Test
    void canHandleReturnsFalseForNonAboutToSubmit() {
        assertThat(handler.canHandle(MID_EVENT, callback)).isFalse();
    }

    @Test
    void canHandleReturnsFalseWhenFeatureDisabled() {
        ConfidentialityTabAboutToSubmitHandler disabledHandler = new ConfidentialityTabAboutToSubmitHandler(false);

        assertThat(disabledHandler.canHandle(ABOUT_TO_SUBMIT, callback)).isFalse();
    }

    @Test
    void canHandleReturnsFalseForNonChildSupportBenefit() {
        sscsCaseData.setAppeal(
            Appeal.builder().benefitType(BenefitType.builder().code(Benefit.PIP.getShortName()).build()).build());

        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isFalse();
    }

    @Test
    void handleThrowsIllegalStateWhenNotChildSupport() {
        sscsCaseData.setAppeal(
            Appeal.builder().benefitType(BenefitType.builder().code(Benefit.PIP.getShortName()).build()).build());

        assertThatThrownBy(() -> handler.handle(ABOUT_TO_SUBMIT, callback, "Bearer token")).isInstanceOf(
            IllegalStateException.class);
    }

    @Test
    void handleUpdatesAppellantConfidentialityRequiredChangedDateWhenConfidentialityChanges() {
        final LocalDateTime originalDate = LocalDateTime.now().minusHours(1);
        final Appellant appellant = Appellant.builder()
                                             .confidentialityRequired(YES)
                                             .confidentialityRequiredChangedDate(originalDate)
                                             .build();
        sscsCaseData.setAppeal(Appeal.builder()
                                     .benefitType(BenefitType.builder().code(Benefit.CHILD_SUPPORT.getShortName()).build())
                                     .appellant(appellant)
                                     .build());

        final SscsCaseData beforeCaseData = SscsCaseData.builder()
                                                        .appeal(Appeal.builder()
                                                                      .appellant(
                                                                          Appellant.builder().confidentialityRequired(NO).build())
                                                                      .build())
                                                        .build();

        when(caseDetailsBefore.getCaseData()).thenReturn(beforeCaseData);
        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetailsBefore));

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, "Bearer token");

        assertThat(response.getData().getAppeal().getAppellant().getConfidentialityRequiredChangedDate()).isAfter(originalDate);
    }

    @Test
    void handleDoesNotUpdateAppellantConfidentialityRequiredChangedDateWhenConfidentialityUnchanged() {
        final LocalDateTime originalDate = LocalDateTime.now().minusHours(1);
        final Appellant appellant = Appellant.builder()
                                             .confidentialityRequired(YES)
                                             .confidentialityRequiredChangedDate(originalDate)
                                             .build();
        sscsCaseData.setAppeal(Appeal.builder()
                                     .benefitType(BenefitType.builder().code(Benefit.CHILD_SUPPORT.getShortName()).build())
                                     .appellant(appellant)
                                     .build());

        final SscsCaseData beforeCaseData = SscsCaseData.builder()
                                                        .appeal(Appeal.builder()
                                                                      .appellant(Appellant.builder()
                                                                                          .confidentialityRequired(YES)
                                                                                          .build())
                                                                      .build())
                                                        .build();
        when(caseDetailsBefore.getCaseData()).thenReturn(beforeCaseData);
        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetailsBefore));

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, "Bearer token");

        assertThat(response.getData().getAppeal().getAppellant().getConfidentialityRequiredChangedDate()).isEqualTo(originalDate);
    }

    @Test
    void handleUpdatesOtherPartyConfidentialityRequiredChangedDateWhenConfidentialityChanges() {
        final LocalDateTime originalDate = LocalDateTime.now().minusHours(1);
        final OtherParty otherParty = OtherParty.builder()
                                                .id("op1")
                                                .confidentialityRequired(NO)
                                                .confidentialityRequiredChangedDate(originalDate)
                                                .build();
        sscsCaseData.setOtherParties(singletonList(CcdValue.<OtherParty>builder().value(otherParty).build()));
        sscsCaseData.setAppeal(
            Appeal.builder().benefitType(BenefitType.builder().code(Benefit.CHILD_SUPPORT.getShortName()).build()).build());

        final OtherParty beforeOtherParty = OtherParty.builder().id("op1").confidentialityRequired(YES).build();
        final SscsCaseData beforeCaseData = SscsCaseData.builder()
                                                        .otherParties(singletonList(
                                                            CcdValue.<OtherParty>builder().value(beforeOtherParty).build()))
                                                        .build();
        when(caseDetailsBefore.getCaseData()).thenReturn(beforeCaseData);
        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetailsBefore));

        handler.handle(ABOUT_TO_SUBMIT, callback, "Bearer token");

        assertThat(otherParty.getConfidentialityRequiredChangedDate()).isAfter(originalDate);
    }

    @Test
    void handleDoesNotUpdateOtherPartyConfidentialityRequiredChangedDateWhenConfidentialityUnchanged() {
        final LocalDateTime originalDate = LocalDateTime.now().minusHours(1);
        final OtherParty otherParty = OtherParty.builder()
                                                .id("op1")
                                                .confidentialityRequired(YES)
                                                .confidentialityRequiredChangedDate(originalDate)
                                                .build();
        sscsCaseData.setOtherParties(singletonList(CcdValue.<OtherParty>builder().value(otherParty).build()));
        sscsCaseData.setAppeal(
            Appeal.builder().benefitType(BenefitType.builder().code(Benefit.CHILD_SUPPORT.getShortName()).build()).build());

        final OtherParty beforeOtherParty = OtherParty.builder().id("op1").confidentialityRequired(YES).build();
        final SscsCaseData beforeCaseData = SscsCaseData.builder()
                                                        .otherParties(singletonList(
                                                            CcdValue.<OtherParty>builder().value(beforeOtherParty).build()))
                                                        .build();
        when(caseDetailsBefore.getCaseData()).thenReturn(beforeCaseData);
        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetailsBefore));

        handler.handle(ABOUT_TO_SUBMIT, callback, "Bearer token");

        assertThat(otherParty.getConfidentialityRequiredChangedDate()).isEqualTo(originalDate);
    }

    @Test
    void handleDoesNotUpdateOtherPartyConfidentialityRequiredChangedDateWhenCurrentConfidentialityRequiredIsNull() {
        final OtherParty otherParty = OtherParty.builder().id("op1").confidentialityRequired(null).build();
        sscsCaseData.setOtherParties(singletonList(CcdValue.<OtherParty>builder().value(otherParty).build()));
        sscsCaseData.setAppeal(
            Appeal.builder().benefitType(BenefitType.builder().code(Benefit.CHILD_SUPPORT.getShortName()).build()).build());

        final OtherParty beforeOtherParty = OtherParty.builder().id("op1").confidentialityRequired(NO).build();
        final SscsCaseData beforeCaseData = SscsCaseData.builder()
                                                        .otherParties(singletonList(
                                                            CcdValue.<OtherParty>builder().value(beforeOtherParty).build()))
                                                        .build();
        when(caseDetailsBefore.getCaseData()).thenReturn(beforeCaseData);
        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetailsBefore));

        handler.handle(ABOUT_TO_SUBMIT, callback, "Bearer token");

        assertThat(otherParty.getConfidentialityRequiredChangedDate()).isNull();
    }

    @Test
    void handleDoesNotUpdateAppellantConfidentialityRequiredChangedDateWhenConfidentialityIsNull() {
        final Appellant appellant = Appellant.builder()
                                             .confidentialityRequired(null)
                                             .confidentialityRequiredChangedDate(null)
                                             .build();
        sscsCaseData.setAppeal(Appeal.builder()
                                     .benefitType(BenefitType.builder().code(Benefit.CHILD_SUPPORT.getShortName()).build())
                                     .appellant(appellant)
                                     .build());

        final SscsCaseData beforeCaseData = SscsCaseData.builder()
                                                        .appeal(Appeal.builder()
                                                                      .appellant(Appellant.builder()
                                                                                          .confidentialityRequired(YES)
                                                                                          .build())
                                                                      .build())
                                                        .build();
        when(caseDetailsBefore.getCaseData()).thenReturn(beforeCaseData);
        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetailsBefore));

        handler.handle(ABOUT_TO_SUBMIT, callback, "Bearer token");

        assertThat(appellant.getConfidentialityRequiredChangedDate()).isNull();
    }

    @Test
    void handleUpdatesAppellantConfidentialityRequiredChangedDateWhenNoPreviousCaseData() {
        final LocalDateTime originalDate = LocalDateTime.now().minusHours(1);
        final Appellant appellant = Appellant.builder()
                                             .confidentialityRequired(YES)
                                             .confidentialityRequiredChangedDate(originalDate)
                                             .build();
        sscsCaseData.setAppeal(Appeal.builder()
                                     .benefitType(BenefitType.builder().code(Benefit.CHILD_SUPPORT.getShortName()).build())
                                     .appellant(appellant)
                                     .build());

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, "Bearer token");

        assertThat(response.getData().getAppeal().getAppellant().getConfidentialityRequiredChangedDate()).isAfter(originalDate);
    }

    @Test
    void canHandleReturnsTrueForUcBenefit() {
        sscsCaseData.setAppeal(
            Appeal.builder().benefitType(BenefitType.builder().code(Benefit.UC.getShortName()).build()).build());

        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isTrue();
    }

    @Test
    void handleSetsShowConfidentialityTabForChildSupportWithNoOtherParties() {
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, "Bearer token");

        assertThat(response.getData().getExtendedSscsCaseData().getShowConfidentialityTab()).isEqualTo(YES);
    }

    @Test
    void handleSetsShowConfidentialityTabForUcWithOtherParties() {
        final OtherParty otherParty = OtherParty.builder()
                                                .name(Name.builder().firstName("Other").lastName("Party").build())
                                                .confidentialityRequired(YES)
                                                .build();
        sscsCaseData.setOtherParties(singletonList(CcdValue.<OtherParty>builder().value(otherParty).build()));
        sscsCaseData.setAppeal(
            Appeal.builder().benefitType(BenefitType.builder().code(Benefit.UC.getShortName()).build()).build());

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, "Bearer token");

        assertThat(response.getData().getExtendedSscsCaseData().getShowConfidentialityTab()).isEqualTo(YES);
    }

    @Test
    void handleDoesNotSetShowConfidentialityTabForUcWithNoOtherParties() {
        sscsCaseData.setAppeal(
            Appeal.builder().benefitType(BenefitType.builder().code(Benefit.UC.getShortName()).build()).build());

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, "Bearer token");

        assertThat(response.getData().getExtendedSscsCaseData().getShowConfidentialityTab()).isNotEqualTo(YES);
    }

}