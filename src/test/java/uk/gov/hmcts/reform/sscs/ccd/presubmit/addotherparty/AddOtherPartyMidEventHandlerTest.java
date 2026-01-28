package uk.gov.hmcts.reform.sscs.ccd.presubmit.addotherparty;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
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

    private SscsCaseData sscsCaseData;

    @BeforeEach
    public void setUp() {
        handler = new AddOtherPartyMidEventHandler();

        sscsCaseData = SscsCaseData.builder()
            .ccdCaseId("ccdId").build();

        Appeal appeal = Appeal.builder()
            .benefitType(BenefitType.builder().code(Benefit.CHILD_SUPPORT.getShortName()).build()).build();

        sscsCaseData.setAppeal(appeal);
    }

    @Nested
    class CanHandleTests {

        @Test
        public void givenANonAddOtherPartyEvent_thenReturnFalse() {
            sscsCaseData.setOtherParties(Collections.singletonList(buildOtherParty(ID_1)));
            when(callback.getEvent()).thenReturn(APPEAL_RECEIVED);

            assertThat(handler.canHandle(MID_EVENT, callback)).isFalse();
        }

        @Test
        public void givenAddOtherPartyEvent_thenReturnTrue() {
            when(callback.getEvent()).thenReturn(EventType.ADD_OTHER_PARTY_DATA);
            when(callback.getCaseDetails()).thenReturn(caseDetails);
            when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

            sscsCaseData.setOtherParties(Collections.singletonList(buildOtherParty(ID_1)));

            assertThat(handler.canHandle(MID_EVENT, callback)).isTrue();
        }

        @Test
        public void givenAnAddOtherPartyEventWithNullOtherPartiesList_thenReturnFalse() {
            when(callback.getEvent()).thenReturn(EventType.ADD_OTHER_PARTY_DATA);
            when(callback.getCaseDetails()).thenReturn(caseDetails);
            when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

            assertThat(handler.canHandle(MID_EVENT, callback)).isFalse();
        }

        @ParameterizedTest
        @EnumSource(value = CallbackType.class, names = {"MID_EVENT"}, mode = EnumSource.Mode.EXCLUDE)
        void shouldReturnFalseForCanHandleWhenNotMidEvent(CallbackType callbackType) {
            assertThat(handler.canHandle(callbackType, callback)).isFalse();
        }

        @ParameterizedTest
        @EnumSource(value = EventType.class, names = {"ADD_OTHER_PARTY_DATA"}, mode = EnumSource.Mode.EXCLUDE)
        void shouldReturnFalseForCanHandleWhenNotAddOtherPartyData(EventType eventType) {
            when(callback.getEvent()).thenReturn(eventType);
            assertThat(handler.canHandle(MID_EVENT, callback)).isFalse();
        }

        @Test
        void shouldThrowIllegalStateExceptionIfCannotHandle() {
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION));

            assertThat(ex).hasMessage("Cannot handle callback");
        }
    }

    @Nested
    class HandleTests {
        @Test
        public void throwsExceptionIfItCannotHandleTheAppeal() {
            when(callback.getEvent()).thenReturn(APPEAL_RECEIVED);

            assertThatThrownBy(() ->
                handler.handle(MID_EVENT, callback, USER_AUTHORISATION))
                .isInstanceOf(IllegalStateException.class);
        }

        @Test
        public void givenAddOtherPartyEventWithMultipleOtherParties_thenReturnError() {
            when(callback.getEvent()).thenReturn(EventType.ADD_OTHER_PARTY_DATA);
            when(callback.getCaseDetails()).thenReturn(caseDetails);
            when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

            sscsCaseData.setOtherParties(List.of(buildOtherParty(ID_1), buildOtherParty(ID_2)));

            var response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

            assertThat(response.getErrors()).contains("Only one other party data can be added using this event!");
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
}