package uk.gov.hmcts.reform.sscs.ccd.presubmit.updateotherparty;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.UPDATE_OTHER_PARTY_DATA;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.model.AppConstants.IBCA_BENEFIT_CODE;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appointee;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.Representative;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@ExtendWith(MockitoExtension.class)
class UpdateOtherPartyMidEventHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    private static final String ERROR_ADDRESS_LINE_1_OTHER_PARTY = "You must enter address line 1 for the other party";
    private static final String ERROR_ADDRESS_LINE_1_OTHER_PARTY_REP = "You must enter address line 1 for the other party representative";
    private static final String ERROR_COUNTRY_OTHER_PARTY = "You must enter a valid country for the other party";
    private static final String ERROR_COUNTRY_OTHER_PARTY_REP = "You must enter a valid country for the other party representative";
    private static final String ERROR_POSTCODE_OTHER_PARTY = "You must enter a valid UK postcode for the other party";
    private static final String ERROR_POSTCODE_OTHER_PARTY_REP = "You must enter a valid UK postcode for the other party representative";
    private static final String ERROR_MAINLAND_SELECTION_OTHER_PARTY = "You must select whether the address is in mainland UK for the other party";
    private static final String ERROR_MAINLAND_SELECTION_OTHER_PARTY_REP = "You must select whether the address is in mainland UK for the other party representative";
    private static final String ERROR_ADDRESS_MISSING_OTHER_PARTY_REP = "Address details are missing for the other party representative";

    private static final String ADDRESS_DETAILS_ARE_MISSING_FOR_THE_OTHER_PARTY = "Address details are missing for the other party";
    private static final String YOU_MUST_ENTER_A_VALID_UK_POSTCODE_FOR_THE_OTHER_PARTY_APPOINTEE = "You must enter a valid UK postcode for the other party appointee";
    private static final String YOU_MUST_ENTER_ADDRESS_LINE_1_FOR_THE_OTHER_PARTY_APPOINTEE = "You must enter address line 1 for the other party appointee";
    private static final String ADDRESS_DETAILS_ARE_MISSING_FOR_THE_OTHER_PARTY_APPOINTEE = "Address details are missing for the other party appointee";

    @Mock
    private Callback<SscsCaseData> callback;
    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private UpdateOtherPartyMidEventHandler handler;

    private static CcdValue<OtherParty> ccd(OtherParty p) {
        return CcdValue.<OtherParty>builder().value(p).build();
    }

    private static Address addressMissingPostcode() {
        return Address.builder().line1("first address line").build();
    }

    private static Address validUkAddress() {
        return Address.builder().line1("first address line").postcode("SWA 1AA").build();
    }

    private static Address ukAddressMissingFirstLine() {
        return Address.builder().postcode("SWA 1AA").build();
    }

    @BeforeEach
    void setUp() {
        handler = new UpdateOtherPartyMidEventHandler();
    }

    private PreSubmitCallbackResponse<SscsCaseData> runMidEvent(SscsCaseData caseData) {
        when(callback.getEvent()).thenReturn(UPDATE_OTHER_PARTY_DATA);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        return handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
    }

    @Test
    void shouldReturnTrueForCanHandle() {
        OtherParty party = OtherParty.builder().address(Address.builder().line1("42 new road").build()).build();

        SscsCaseData caseData = SscsCaseData.builder()
            .benefitCode(IBCA_BENEFIT_CODE)
            .otherParties(List.of(new CcdValue<>(party)))
            .build();

        when(callback.getEvent()).thenReturn(UPDATE_OTHER_PARTY_DATA);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        assertThat(handler.canHandle(MID_EVENT, callback)).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = CallbackType.class, names = {"MID_EVENT"}, mode = EnumSource.Mode.EXCLUDE)
    void shouldReturnFalseForCanHandleWhenNotAMidEvent(CallbackType callbackType) {
        assertThat(handler.canHandle(callbackType, callback)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = EventType.class, names = {"EVENTS_UPDATES", "APPEAL_RECEIVED", "CASE_UPDATED"})
    void shouldReturnFalseForCanHandleWhenNotEventUpdateOtherPartyData(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);
        assertThat(handler.canHandle(MID_EVENT, callback)).isFalse();
    }

    @Test
    void shouldReturnFalseForCanHandleWhenNoOtherParties() {
        when(callback.getEvent()).thenReturn(UPDATE_OTHER_PARTY_DATA);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(SscsCaseData.builder().build());

        assertThat(handler.canHandle(MID_EVENT, callback)).isFalse();
    }

    @Test
    void shouldThrowAnIllegalStateExceptionIfHandlerCannotHandleCallback() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION));
        assertThat(ex).hasMessage("Cannot handle callback");
    }

    @Test
    void shouldReturnNoErrorsOnMidEventForUkNoRep() {
        OtherParty party = OtherParty.builder()
            .address(Address.builder().line1("42 new road").inMainlandUk(YES).postcode("SWA 1AA").build())
            .build();

        SscsCaseData caseData = SscsCaseData.builder()
            .benefitCode(IBCA_BENEFIT_CODE)
            .otherParties(List.of(ccd(party)))
            .build();

        PreSubmitCallbackResponse<SscsCaseData> response = runMidEvent(caseData);

        assertThat(response.getErrors()).isEmpty();
    }

    @Nested
    class NonInfectedBloodCaseType {

        private static Stream<Arguments> addressAndErrorProvider() {
            return Stream.of(
                Arguments.of("Other party address missing", List.of(ccd(OtherParty.builder().address(null).build())),
                    ADDRESS_DETAILS_ARE_MISSING_FOR_THE_OTHER_PARTY),

                Arguments.of("Other party address missing first line of address",
                    List.of(ccd(OtherParty.builder().address(ukAddressMissingFirstLine()).build())),
                    ERROR_ADDRESS_LINE_1_OTHER_PARTY),

                Arguments.of("Other party address missing postcode",
                    List.of(ccd(OtherParty.builder().address(addressMissingPostcode()).build())),
                    ERROR_POSTCODE_OTHER_PARTY),

                Arguments.of("Other party address postcode invalid", List.of(ccd(OtherParty.builder()
                    .address(Address.builder().line1("line 1").postcode("ZZ").build())
                    .build())), ERROR_POSTCODE_OTHER_PARTY),

                Arguments.of("Other party representative address missing", List.of(ccd(OtherParty.builder()
                    .address(validUkAddress())
                    .rep(Representative.builder().hasRepresentative(YES.getValue()).build())
                    .build())), ERROR_ADDRESS_MISSING_OTHER_PARTY_REP),

                Arguments.of("Other party representative address missing first line of address", List.of(
                    ccd(OtherParty.builder()
                        .address(validUkAddress())
                        .rep(Representative.builder()
                            .hasRepresentative(YES.getValue())
                            .address(ukAddressMissingFirstLine())
                            .build())
                        .build())), ERROR_ADDRESS_LINE_1_OTHER_PARTY_REP),

                Arguments.of("Other party representative address missing postcode", List.of(ccd(OtherParty.builder()
                    .address(validUkAddress())
                    .rep(Representative.builder()
                        .hasRepresentative(YES.getValue())
                        .address(addressMissingPostcode())
                        .build())
                    .build())), ERROR_POSTCODE_OTHER_PARTY_REP),

                Arguments.of("Other party appointee address missing", List.of(ccd(OtherParty.builder()
                    .address(validUkAddress())
                    .isAppointee(YES.getValue())
                    .appointee(Appointee.builder().build())
                    .build())), ADDRESS_DETAILS_ARE_MISSING_FOR_THE_OTHER_PARTY_APPOINTEE),

                Arguments.of("Other party appointee address missing first line of address", List.of(
                    ccd(OtherParty.builder()
                        .address(validUkAddress())
                        .isAppointee(YES.getValue())
                        .appointee(Appointee.builder().address(ukAddressMissingFirstLine()).build())
                        .build())), YOU_MUST_ENTER_ADDRESS_LINE_1_FOR_THE_OTHER_PARTY_APPOINTEE),

                Arguments.of("Other party appointee address missing postcode", List.of(ccd(OtherParty.builder()
                    .address(validUkAddress())
                    .isAppointee(YES.getValue())
                    .appointee(Appointee.builder().address(addressMissingPostcode()).build())
                    .build())), YOU_MUST_ENTER_A_VALID_UK_POSTCODE_FOR_THE_OTHER_PARTY_APPOINTEE));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("addressAndErrorProvider")
        void shouldReturnSingleErrorOnMidEvent(String description, List<CcdValue<OtherParty>> otherParties, String expectedError) {
            SscsCaseData caseData = SscsCaseData.builder()
                .benefitCode(Benefit.CHILD_SUPPORT.name())
                .otherParties(otherParties)
                .build();

            PreSubmitCallbackResponse<SscsCaseData> response = runMidEvent(caseData);

            assertThat(response.getErrors()).containsExactly(expectedError);
        }
    }

    @Nested
    class InfectedBloodCaseType {

        private SscsCaseData ibcCaseWith(OtherParty party) {
            return SscsCaseData.builder().benefitCode(IBCA_BENEFIT_CODE).otherParties(List.of(ccd(party))).build();
        }

        @Test
        void shouldReturnErrorsOnMidEventForAllBlankUkNoRep() {
            OtherParty party = OtherParty.builder().address(Address.builder().build()).build();

            PreSubmitCallbackResponse<SscsCaseData> response = runMidEvent(ibcCaseWith(party));

            assertThat(response.getErrors()).containsExactlyInAnyOrder(ERROR_ADDRESS_LINE_1_OTHER_PARTY,
                ERROR_MAINLAND_SELECTION_OTHER_PARTY);
        }

        @Test
        void shouldReturnErrorsOnMidEventForFirstLineUkNoRep() {
            OtherParty party = OtherParty.builder().address(Address.builder().line1("11 test rd").build()).build();

            PreSubmitCallbackResponse<SscsCaseData> response = runMidEvent(ibcCaseWith(party));

            assertThat(response.getErrors()).containsExactly(ERROR_MAINLAND_SELECTION_OTHER_PARTY);
        }

        @Test
        void shouldReturnErrorsOnMidEventForNeedsPostcodeUkNoRep() {
            OtherParty party = OtherParty.builder()
                .address(Address.builder().line1("11 test rd").inMainlandUk(YES).build())
                .build();

            PreSubmitCallbackResponse<SscsCaseData> response = runMidEvent(ibcCaseWith(party));

            assertThat(response.getErrors()).containsExactly(ERROR_POSTCODE_OTHER_PARTY);
        }

        @Test
        void shouldReturnErrorsOnMidEventForMainlandOnlyNotUkNoRep() {
            OtherParty party = OtherParty.builder().address(Address.builder().inMainlandUk(NO).build()).build();

            PreSubmitCallbackResponse<SscsCaseData> response = runMidEvent(ibcCaseWith(party));

            assertThat(response.getErrors()).containsExactlyInAnyOrder(ERROR_ADDRESS_LINE_1_OTHER_PARTY,
                ERROR_COUNTRY_OTHER_PARTY);
        }

        @Test
        void shouldReturnErrorsOnMidEventForFirstLineNotUkNoRep() {
            OtherParty party = OtherParty.builder()
                .address(Address.builder().line1("11 test rd").inMainlandUk(NO).build())
                .build();

            PreSubmitCallbackResponse<SscsCaseData> response = runMidEvent(ibcCaseWith(party));

            assertThat(response.getErrors()).containsExactly(ERROR_COUNTRY_OTHER_PARTY);
        }

        @Test
        void shouldReturnErrorsOnMidEventForNonUkPartyAndRep() {
            Representative rep = new Representative(YES.getValue());
            rep.setAddress(Address.builder().inMainlandUk(NO).build());

            OtherParty party = OtherParty.builder()
                .address(Address.builder()
                    .line1("69 zoo lane")
                    .inMainlandUk(NO)
                    .postcode("01210")
                    .country("America")
                    .build())
                .rep(rep)
                .build();

            PreSubmitCallbackResponse<SscsCaseData> response = runMidEvent(ibcCaseWith(party));

            assertThat(response.getErrors()).containsExactlyInAnyOrder(ERROR_ADDRESS_LINE_1_OTHER_PARTY_REP,
                ERROR_COUNTRY_OTHER_PARTY_REP);
        }

        @Test
        void shouldReturnErrorsOnMidEventForUkPartyAndUkRep() {
            Representative rep = new Representative(YES.getValue());
            rep.setAddress(Address.builder().inMainlandUk(YES).build());

            OtherParty party = OtherParty.builder()
                .address(Address.builder()
                    .line1("69 zoo lane")
                    .inMainlandUk(NO)
                    .postcode("01210")
                    .country("America")
                    .build())
                .rep(rep)
                .build();

            PreSubmitCallbackResponse<SscsCaseData> response = runMidEvent(ibcCaseWith(party));

            assertThat(response.getErrors()).containsExactlyInAnyOrder(ERROR_ADDRESS_LINE_1_OTHER_PARTY_REP,
                ERROR_POSTCODE_OTHER_PARTY_REP);
        }

        @Test
        void shouldReturnErrorsOnMidEventForNonUkPartyAndNoMainlandRep() {
            Representative rep = new Representative(YES.getValue());
            rep.setAddress(Address.builder().line1("22 OnlyLine Lane").build());

            OtherParty party = OtherParty.builder()
                .address(Address.builder()
                    .line1("69 zoo lane")
                    .inMainlandUk(NO)
                    .postcode("01210")
                    .country("America")
                    .build())
                .rep(rep)
                .build();

            PreSubmitCallbackResponse<SscsCaseData> response = runMidEvent(ibcCaseWith(party));

            assertThat(response.getErrors()).containsExactly(ERROR_MAINLAND_SELECTION_OTHER_PARTY_REP);
        }

        @Test
        void shouldReturnErrorsOnMidEventForPartyAndRepIncomplete() {
            Representative rep = new Representative(YES.getValue());
            rep.setAddress(Address.builder().line1("22 OnlyLine Lane").build());

            OtherParty party = OtherParty.builder()
                .address(Address.builder().inMainlandUk(NO).postcode("01210").build())
                .rep(rep)
                .build();

            PreSubmitCallbackResponse<SscsCaseData> response = runMidEvent(ibcCaseWith(party));

            assertThat(response.getErrors()).containsExactlyInAnyOrder(ERROR_ADDRESS_LINE_1_OTHER_PARTY,
                ERROR_COUNTRY_OTHER_PARTY, ERROR_MAINLAND_SELECTION_OTHER_PARTY_REP);
        }

        @Test
        void shouldReturnNoErrorsOnMidEventForNonUkNoRep() {
            OtherParty party = OtherParty.builder()
                .address(Address.builder().line1("42 new road").inMainlandUk(NO).country("America").build())
                .build();

            PreSubmitCallbackResponse<SscsCaseData> response = runMidEvent(ibcCaseWith(party));

            assertThat(response.getErrors()).isEmpty();
        }

        @Test
        void shouldReturnNoErrorsOnMidEventForUkHasUkRep() {
            Representative rep = new Representative(YES.getValue());
            rep.setAddress(Address.builder().line1("333 London Rd").inMainlandUk(YES).postcode("SWA 1AA").build());

            OtherParty party = OtherParty.builder()
                .address(Address.builder().line1("42 new road").inMainlandUk(YES).postcode("SWA 1AA").build())
                .rep(rep)
                .build();

            PreSubmitCallbackResponse<SscsCaseData> response = runMidEvent(ibcCaseWith(party));

            assertThat(response.getErrors()).isEmpty();
        }

        @Test
        void shouldReturnNoErrorsOnMidEventForNonUkHasRepInUk() {
            Representative rep = new Representative(YES.getValue());
            rep.setAddress(Address.builder().line1("333 London Rd").inMainlandUk(YES).postcode("SWA 1AA").build());

            OtherParty party = OtherParty.builder()
                .address(Address.builder().line1("42 new road").inMainlandUk(NO).country("America").build())
                .rep(rep)
                .build();

            PreSubmitCallbackResponse<SscsCaseData> response = runMidEvent(ibcCaseWith(party));

            assertThat(response.getErrors()).isEmpty();
        }

        @Test
        void shouldReturnErrorsOnMidEventForUkHasAllBlankRep() {
            Representative rep = new Representative(YES.getValue());

            OtherParty party = OtherParty.builder()
                .address(Address.builder().line1("69 zoo lane").inMainlandUk(YES).postcode("swa 1aa").build())
                .rep(rep)
                .build();

            PreSubmitCallbackResponse<SscsCaseData> response = runMidEvent(ibcCaseWith(party));

            assertThat(response.getErrors()).containsExactly(ERROR_ADDRESS_MISSING_OTHER_PARTY_REP);
        }
    }
}
