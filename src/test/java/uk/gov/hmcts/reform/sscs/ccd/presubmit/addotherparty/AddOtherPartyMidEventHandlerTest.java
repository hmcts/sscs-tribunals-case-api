package uk.gov.hmcts.reform.sscs.ccd.presubmit.addotherparty;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.CHILD_SUPPORT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtilTest.ID_1;
import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtilTest.ID_2;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.Role;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;

@ExtendWith(MockitoExtension.class)
public class AddOtherPartyMidEventHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    private AddOtherPartyMidEventHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Nested
    class CanHandleTests {

        @Nested
        class CmOtherPartyConfidentialityEnabled {

            @BeforeEach
            public void setUp() {
                handler = new AddOtherPartyMidEventHandler(true);
            }

            @ParameterizedTest
            @EnumSource(value = CallbackType.class, names = {"MID_EVENT"}, mode = EnumSource.Mode.EXCLUDE)
            void givenNonMidEvent_thenReturnFalse(CallbackType callbackType) {
                assertThat(handler.canHandle(callbackType, callback)).isFalse();
            }

            @ParameterizedTest
            @EnumSource(value = Benefit.class, mode = EnumSource.Mode.EXCLUDE, names = {
                "CHILD_SUPPORT"
            })
            void givenNonChildSupportBenefit_thenReturnFalse(Benefit benefit) {
                when(callback.getEvent()).thenReturn(EventType.ADD_OTHER_PARTY_DATA);
                when(callback.getCaseDetails()).thenReturn(caseDetails);
                when(caseDetails.getCaseData()).thenReturn(caseDataWithBenefit(benefit.getShortName()));

                assertThat(handler.canHandle(MID_EVENT, callback)).isFalse();
            }

            @ParameterizedTest
            @EnumSource(value = EventType.class, mode = EnumSource.Mode.EXCLUDE, names = {"ADD_OTHER_PARTY_DATA"})
            public void givenNonAddOtherPartyEvent_thenReturnFalse(EventType eventType) {
                when(callback.getEvent()).thenReturn(eventType);

                assertThat(handler.canHandle(MID_EVENT, callback)).isFalse();
            }

            @Test
            public void givenAddOtherPartyEventAndChildSupportBenefitWithoutOtherPartyData_thenReturnFalse() {
                var sscsCaseData = caseDataWithBenefit(CHILD_SUPPORT.getShortName());

                when(callback.getEvent()).thenReturn(EventType.ADD_OTHER_PARTY_DATA);
                when(callback.getCaseDetails()).thenReturn(caseDetails);
                when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

                assertThat(handler.canHandle(MID_EVENT, callback)).isFalse();
            }

            @Test
            public void givenAddOtherPartyEventAndChildSupportBenefit_thenReturnTrue() {
                var sscsCaseData = caseDataWithBenefit(CHILD_SUPPORT.getShortName());

                when(callback.getEvent()).thenReturn(EventType.ADD_OTHER_PARTY_DATA);
                when(callback.getCaseDetails()).thenReturn(caseDetails);
                when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

                sscsCaseData.setOtherParties(Collections.singletonList(buildOtherParty(ID_1)));

                assertThat(handler.canHandle(MID_EVENT, callback)).isTrue();
            }
        }

        @Nested
        class CmOtherPartyConfidentialityDisabled {
            @BeforeEach
            public void setUp() {
                handler = new AddOtherPartyMidEventHandler(false);
            }

            @Test
            void givenCmOtherPArtyConfidentialityFlagIsDisabled_thenReturnFalse() {
                assertThat(handler.canHandle(MID_EVENT, callback)).isFalse();
            }
        }
    }

    @Nested
    class HandleTests {

        @Nested
        class CmOtherPartyConfidentialityEnabled {

            @BeforeEach
            public void setUp() {
                handler = new AddOtherPartyMidEventHandler(true);
            }

            @Test
            public void givenAddOtherPartyEventWithSingleOtherPartyData_thenRunSuccessfully() {
                var sscsCaseData = caseDataWithBenefit(CHILD_SUPPORT.getShortName());

                when(callback.getEvent()).thenReturn(EventType.ADD_OTHER_PARTY_DATA);
                when(callback.getCaseDetails()).thenReturn(caseDetails);
                when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

                sscsCaseData.setOtherParties(List.of(buildOtherParty(ID_1)));

                var response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

                assertThat(response.getErrors()).isEmpty();

            }

            @Test
            void throwIllegalStateExceptionIfNoAnyOtherPartyProvided() {
                var sscsCaseData = caseDataWithBenefit(CHILD_SUPPORT.getShortName());

                when(callback.getEvent()).thenReturn(EventType.ADD_OTHER_PARTY_DATA);
                when(callback.getCaseDetails()).thenReturn(caseDetails);
                when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

                IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> handler.handle(MID_EVENT, callback, USER_AUTHORISATION));

                assertThat(ex).hasMessage("Cannot handle callback");
            }

            @Test
            void throwIllegalStateExceptionWhenCallbackTypeIsNotMidEvent() {

                IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION));

                assertThat(ex).hasMessage("Cannot handle callback");
            }

            @Test
            public void throwsExceptionIfItCannotHandleTheAppeal() {
                when(callback.getEvent()).thenReturn(APPEAL_RECEIVED);

                assertThatThrownBy(() ->
                    handler.handle(MID_EVENT, callback, USER_AUTHORISATION))
                    .isInstanceOf(IllegalStateException.class);
            }

            @Test
            public void givenAddOtherPartyEventWithoutOtherPartyData_thenReturnError() {
                var sscsCaseData = caseDataWithBenefit(CHILD_SUPPORT.getShortName());

                when(callback.getEvent()).thenReturn(EventType.ADD_OTHER_PARTY_DATA);
                when(callback.getCaseDetails()).thenReturn(caseDetails);
                when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

                sscsCaseData.setOtherParties(Collections.emptyList());

                var response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

                assertThat(response.getErrors()).contains("Other party data must be added to submit this event.");
            }

            @Test
            public void givenAddOtherPartyEventWithMultipleOtherParties_thenReturnError() {
                var sscsCaseData = caseDataWithBenefit(CHILD_SUPPORT.getShortName());

                when(callback.getEvent()).thenReturn(EventType.ADD_OTHER_PARTY_DATA);
                when(callback.getCaseDetails()).thenReturn(caseDetails);
                when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

                sscsCaseData.setOtherParties(List.of(buildOtherParty(ID_1), buildOtherParty(ID_2)));

                var response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

                assertThat(response.getErrors()).contains("Only one other party data can be added using this event.");
            }
        }

        @Nested
        class CmOtherPartyConfidentialityDisabled {
            @BeforeEach
            public void setUp() {
                handler = new AddOtherPartyMidEventHandler(false);
            }

            @Test
            void givenCmOtherPartyConfidentialityDisabled_throwIllegalStateException() {
                IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION));

                assertThat(ex).hasMessage("Cannot handle callback");
            }
        }
    }

    private CcdValue<OtherParty> buildOtherParty(String id) {
        return CcdValue.<OtherParty>builder()
            .value(OtherParty.builder()
                .id(id)
                .unacceptableCustomerBehaviour(YesNo.YES)
                .role(Role.builder().name("PayingParent").build())
                .build())
            .build();
    }

    private static SscsCaseData caseDataWithBenefit(String benefitCode) {
        return SscsCaseData.builder()
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code(benefitCode).build()).build())
            .build();
    }
}