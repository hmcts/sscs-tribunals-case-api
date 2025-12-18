package uk.gov.hmcts.reform.sscs.ccd.presubmit.updateotherparty;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.model.AppConstants.IBCA_BENEFIT_CODE;

import java.util.List;
import java.util.Set;
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

    private static final String ADDRESS_DETAILS_MISSING_OTHER_PARTY = "Address details are missing for the other party";
    private static final String ERROR_POSTCODE_OTHER_PARTY_APPOINTEE = "You must enter a valid UK postcode for the other party appointee";
    private static final String ERROR_LINE1_OTHER_PARTY_APPOINTEE = "You must enter address line 1 for the other party appointee";
    private static final String ADDRESS_DETAILS_MISSING_OTHER_PARTY_APPOINTEE = "Address details are missing for the other party appointee";

    private static final String ZIPCODE = "01210";
    private static final String POSTCODE = "SWA 1AA";
    private static final String LINE_1 = "22 OnlyLine Lane";
    private static final String AMERICA = "America";
    private static final String INVALID_POSTCODE = "ZZZ";

    @Mock
    private Callback<SscsCaseData> callback;
    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private UpdateOtherPartyMidEventHandler handler;

    private static CcdValue<OtherParty> ccd(OtherParty otherParty) {
        return CcdValue.<OtherParty>builder().value(otherParty).build();
    }

    private static Address validAddress() {
        return Address.builder().line1(LINE_1).postcode(POSTCODE).build();
    }

    private static Address anEmptyAddress() {
        return Address.builder().build();
    }

    private static Address addressMissingPostcode() {
        return Address.builder().line1(LINE_1).build();
    }

    private static Address addressWithInvalidPostcode() {
        return Address.builder().line1(LINE_1).postcode(INVALID_POSTCODE).build();
    }

    private static Address addressMissingFirstLine() {
        return Address.builder().postcode(POSTCODE).build();
    }

    private static Address ukAddress() {
        return Address.builder().line1(LINE_1).inMainlandUk(YES).postcode(POSTCODE).build();
    }

    private static Address ukAddressWithNoPostcode() {
        return Address.builder().line1(LINE_1).inMainlandUk(YES).build();
    }

    private static Address ukAddressWithInvalidPostcode() {
        return Address.builder().line1(LINE_1).inMainlandUk(YES).postcode(INVALID_POSTCODE).build();
    }

    private static Address ukAddressWithNoFirstLine() {
        return Address.builder().postcode(POSTCODE).inMainlandUk(YES).build();
    }

    private static Address ukAddressWithNoAddressDetails() {
        return Address.builder().inMainlandUk(YES).build();
    }

    private static Address anInternationalAddress() {
        return Address.builder().line1(LINE_1).inMainlandUk(NO).postcode(ZIPCODE).country(AMERICA).build();
    }

    private static Address anEmptyInternationalAddress() {
        return Address.builder().inMainlandUk(NO).build();
    }

    private static Address anInternationalAddressNoCountry() {
        return Address.builder().line1(LINE_1).postcode(ZIPCODE).inMainlandUk(NO).build();
    }

    private static Address anInternationalAddressWithNoFirstLineAddressOrCountry() {
        return Address.builder().inMainlandUk(NO).postcode(ZIPCODE).build();
    }

    private static Representative hasRepButNoAddress() {
        return Representative.builder().hasRepresentative(YES.getValue()).build();
    }

    private static Representative repWithAddress(Address address) {
        return Representative.builder().hasRepresentative(YES.getValue()).address(address).build();
    }

    private SscsCaseData ibcCaseWith(OtherParty party) {
        return caseDataWithBenefitAndParties(IBCA_BENEFIT_CODE, List.of(ccd(party)));
    }

    private void stubCallbackWith(SscsCaseData caseData) {
        when(callback.getEvent()).thenReturn(EventType.UPDATE_OTHER_PARTY_DATA);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);
    }

    private SscsCaseData caseDataWithBenefitAndParties(String benefitCode, List<CcdValue<OtherParty>> otherParties) {
        return SscsCaseData.builder().benefitCode(benefitCode).otherParties(otherParties).build();
    }

    private PreSubmitCallbackResponse<SscsCaseData> runMidEvent(SscsCaseData caseData) {
        stubCallbackWith(caseData);
        return handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
    }

    @BeforeEach
    void setUp() {
        handler = new UpdateOtherPartyMidEventHandler();
    }

    @Test
    void shouldReturnTrueForCanHandle() {
        OtherParty party = OtherParty.builder().address(ukAddress()).build();
        SscsCaseData caseData = caseDataWithBenefitAndParties(IBCA_BENEFIT_CODE, List.of(ccd(party)));

        stubCallbackWith(caseData);

        assertThat(handler.canHandle(MID_EVENT, callback)).isTrue();
    }

    @Test
    void shouldReturnFalseForCanHandleWhenNoOtherParties() {
        stubCallbackWith(SscsCaseData.builder().build());
        assertThat(handler.canHandle(MID_EVENT, callback)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = CallbackType.class, names = {"MID_EVENT"}, mode = EnumSource.Mode.EXCLUDE)
    void shouldReturnFalseForCanHandleWhenNotMidEvent(CallbackType callbackType) {
        assertThat(handler.canHandle(callbackType, callback)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = EventType.class, names = {"UPDATE_OTHER_PARTY_DATA"}, mode = EnumSource.Mode.EXCLUDE)
    void shouldReturnFalseForCanHandleWhenNotUpdateOtherPartyData(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);
        assertThat(handler.canHandle(MID_EVENT, callback)).isFalse();
    }

    @Test
    void shouldThrowIllegalStateExceptionIfCannotHandle() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION));

        assertThat(ex).hasMessage("Cannot handle callback");
    }

    @Nested
    class ValidationUkOnly {

        private static Stream<Arguments> casesWithValidationError() {
            return Stream.of(
                Arguments.of("Other party address missing", List.of(ccd(OtherParty.builder().address(null).build())),
                    ADDRESS_DETAILS_MISSING_OTHER_PARTY),

                Arguments.of("Other party address missing first line",
                    List.of(ccd(OtherParty.builder().address(addressMissingFirstLine()).build())),
                    ERROR_ADDRESS_LINE_1_OTHER_PARTY),

                Arguments.of("Other party address missing postcode",
                    List.of(ccd(OtherParty.builder().address(addressMissingPostcode()).build())),
                    ERROR_POSTCODE_OTHER_PARTY),

                Arguments.of("Other party address invalid postcode",
                    List.of(ccd(OtherParty.builder().address(addressWithInvalidPostcode()).build())),
                    ERROR_POSTCODE_OTHER_PARTY),

                Arguments.of("Representative present but no address provided",
                    List.of(ccd(OtherParty.builder().address(validAddress()).rep(hasRepButNoAddress()).build())),
                    ERROR_ADDRESS_MISSING_OTHER_PARTY_REP),

                Arguments.of("Representative missing first line", List.of(ccd(OtherParty.builder()
                    .address(validAddress())
                    .rep(repWithAddress(addressMissingFirstLine()))
                    .build())), ERROR_ADDRESS_LINE_1_OTHER_PARTY_REP),

                Arguments.of("Representative missing postcode", List.of(ccd(OtherParty.builder()
                    .address(validAddress())
                    .rep(repWithAddress(addressMissingPostcode()))
                    .build())), ERROR_POSTCODE_OTHER_PARTY_REP),

                Arguments.of("Appointee present but no address", List.of(ccd(OtherParty.builder()
                    .address(validAddress())
                    .isAppointee(YES.getValue())
                    .appointee(Appointee.builder().build())
                    .build())), ADDRESS_DETAILS_MISSING_OTHER_PARTY_APPOINTEE),

                Arguments.of("Appointee missing first line", List.of(ccd(OtherParty.builder()
                    .address(validAddress())
                    .isAppointee(YES.getValue())
                    .appointee(Appointee.builder().address(addressMissingFirstLine()).build())
                    .build())), ERROR_LINE1_OTHER_PARTY_APPOINTEE),

                Arguments.of("Appointee missing postcode", List.of(ccd(OtherParty.builder()
                    .address(validAddress())
                    .isAppointee(YES.getValue())
                    .appointee(Appointee.builder().address(addressMissingPostcode()).build())
                    .build())), ERROR_POSTCODE_OTHER_PARTY_APPOINTEE));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("casesWithValidationError")
        void shouldReturnExactlyOneError(String description, List<CcdValue<OtherParty>> otherParties, String expectedError) {
            SscsCaseData caseData = caseDataWithBenefitAndParties(Benefit.CHILD_SUPPORT.name(), otherParties);

            PreSubmitCallbackResponse<SscsCaseData> response = runMidEvent(caseData);

            assertThat(response.getErrors()).containsExactly(expectedError);
        }

        @Test
        void shouldReturnNoErrorsForValidUkOtherPartyWithNoRepresentative() {
            OtherParty party = OtherParty.builder().address(ukAddress()).build();
            SscsCaseData caseData = caseDataWithBenefitAndParties(Benefit.CHILD_SUPPORT.name(), List.of(ccd(party)));

            PreSubmitCallbackResponse<SscsCaseData> response = runMidEvent(caseData);

            assertThat(response.getErrors()).isEmpty();
        }
    }

    @Nested
    class ValidationUkAndInternational {

        private static Stream<Arguments> casesWithErrors() {
            return Stream.of(
                Arguments.of("Other party empty address", OtherParty.builder().address(anEmptyAddress()).build(),
                    Set.of(ERROR_ADDRESS_LINE_1_OTHER_PARTY, ERROR_MAINLAND_SELECTION_OTHER_PARTY)),

                Arguments.of("Other party empty UK address details",
                    OtherParty.builder().address(ukAddressWithNoAddressDetails()).build(),
                    Set.of(ERROR_ADDRESS_LINE_1_OTHER_PARTY, ERROR_POSTCODE_OTHER_PARTY)),

                Arguments.of("Other party UK address with no line1",
                    OtherParty.builder().address(ukAddressWithNoFirstLine()).build(),
                    Set.of(ERROR_ADDRESS_LINE_1_OTHER_PARTY)),

                Arguments.of("Other party UK address with no postcode",
                    OtherParty.builder().address(ukAddressWithNoPostcode()).build(),
                    Set.of(ERROR_POSTCODE_OTHER_PARTY)),

                Arguments.of("Other party UK address with invalid postcode",
                    OtherParty.builder().address(ukAddressWithInvalidPostcode()).build(),
                    Set.of(ERROR_POSTCODE_OTHER_PARTY)),

                Arguments.of("Other party empty international address",
                    OtherParty.builder().address(anEmptyInternationalAddress()).build(),
                    Set.of(ERROR_ADDRESS_LINE_1_OTHER_PARTY, ERROR_COUNTRY_OTHER_PARTY)),

                Arguments.of("Other party international address with no country",
                    OtherParty.builder().address(anInternationalAddressNoCountry()).build(),
                    Set.of(ERROR_COUNTRY_OTHER_PARTY)),

                Arguments.of("International other party with rep empty international address", OtherParty.builder()
                    .address(anInternationalAddress())
                    .rep(repWithAddress(anEmptyInternationalAddress()))
                    .build(), Set.of(ERROR_ADDRESS_LINE_1_OTHER_PARTY_REP, ERROR_COUNTRY_OTHER_PARTY_REP)),

                Arguments.of("UK other party with rep empty UK address details", OtherParty.builder()
                    .address(ukAddress())
                    .rep(repWithAddress(ukAddressWithNoAddressDetails()))
                    .build(), Set.of(ERROR_ADDRESS_LINE_1_OTHER_PARTY_REP, ERROR_POSTCODE_OTHER_PARTY_REP)),

                Arguments.of("International other party with rep address missing mainland selection",
                    OtherParty.builder().address(anInternationalAddress()).rep(repWithAddress(validAddress())).build(),
                    Set.of(ERROR_MAINLAND_SELECTION_OTHER_PARTY_REP)),

                Arguments.of("International other party missing line1+country, rep UK missing postcode",
                    OtherParty.builder()
                        .address(anInternationalAddressWithNoFirstLineAddressOrCountry())
                        .rep(repWithAddress(addressMissingPostcode()))
                        .build(), Set.of(ERROR_ADDRESS_LINE_1_OTHER_PARTY, ERROR_COUNTRY_OTHER_PARTY,
                        ERROR_MAINLAND_SELECTION_OTHER_PARTY_REP)));
        }

        private static Stream<Arguments> casesWithNoErrors() {
            return Stream.of(Arguments.of("UK other party, no rep", OtherParty.builder().address(ukAddress()).build()),

                Arguments.of("UK other party, UK rep",
                    OtherParty.builder().address(ukAddress()).rep(repWithAddress(ukAddress())).build()),

                Arguments.of("UK other party, international rep",
                    OtherParty.builder().address(ukAddress()).rep(repWithAddress(anInternationalAddress())).build()),

                Arguments.of("International other party, no rep",
                    OtherParty.builder().address(anInternationalAddress()).build()),

                Arguments.of("International other party, UK rep",
                    OtherParty.builder().address(anInternationalAddress()).rep(repWithAddress(ukAddress())).build()),

                Arguments.of("International other party, international rep", OtherParty.builder()
                    .address(anInternationalAddress())
                    .rep(repWithAddress(anInternationalAddress()))
                    .build()));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("casesWithErrors")
        void shouldReturnErrors(String description, OtherParty party, Set<String> expectedErrors) {
            PreSubmitCallbackResponse<SscsCaseData> response = runMidEvent(ibcCaseWith(party));

            assertThat(response.getErrors()).containsExactlyInAnyOrderElementsOf(expectedErrors);
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("casesWithNoErrors")
        void shouldReturnNoErrors(String description, OtherParty party) {
            PreSubmitCallbackResponse<SscsCaseData> response = runMidEvent(ibcCaseWith(party));

            assertThat(response.getErrors()).isEmpty();
        }
    }
}
