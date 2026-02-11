package uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.updateotherparty;

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
import uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.updateotherparty.UpdateOtherPartyMidEventHandler;
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

    private static final String ERROR_ADDRESS_LINE_1_OTHER_PARTY = "You must enter address line 1 for the other party";
    private static final String ERROR_ADDRESS_LINE_1_OTHER_PARTY_REP = "You must enter address line 1 for the other party representative";
    private static final String ERROR_COUNTRY_OTHER_PARTY = "You must enter a valid country for the other party";
    private static final String ERROR_COUNTRY_OTHER_PARTY_REP = "You must enter a valid country for the other party representative";
    private static final String ERROR_POSTCODE_OTHER_PARTY = "You must enter a valid UK postcode for the other party";
    private static final String ERROR_POSTCODE_OTHER_PARTY_REP = "You must enter a valid UK postcode for the other party representative";
    private static final String ERROR_MAINLAND_SELECTION_OTHER_PARTY = "You must select whether the address is in mainland UK for the other party";
    private static final String ERROR_MAINLAND_SELECTION_OTHER_PARTY_REP = "You must select whether the address is in mainland UK for the other party representative";
    private static final String ADDRESS_DETAILS_ARE_MISSING_FOR_THE_OTHER_PARTY = "Address details are missing for the other party";
    private static final String ERROR_LINE1_OTHER_PARTY_APPOINTEE = "You must enter address line 1 for the other party appointee";
    private static final String ERROR_POSTCODE_OTHER_PARTY_APPOINTEE = "You must enter a valid UK postcode for the other party appointee";

    private static final String USER_AUTHORISATION = "Bearer token";
    private static final String DUFFTOWN = "DUFFTOWN";
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

    @BeforeEach
    void setUp() {
        handler = new UpdateOtherPartyMidEventHandler();
    }

    @Test
    void shouldReturnTrueForCanHandle() {
        OtherParty party = OtherParty.builder()
            .address(Address.builder().line1(LINE_1).postcode(POSTCODE).build())
            .build();
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

    private static Representative repWithAddress(Address address) {
        return Representative.builder().hasRepresentative(YES.getValue()).address(address).build();
    }

    private static CcdValue<OtherParty> ccd(OtherParty otherParty) {
        return CcdValue.<OtherParty>builder().value(otherParty).build();
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

    @Nested
    class ValidationUkOnly {

        @ParameterizedTest(name = "{0}")
        @MethodSource("casesWithValidationError")
        void shouldReturnExactlyOneError(String description, List<CcdValue<OtherParty>> otherParties, String expectedError) {
            SscsCaseData caseData = caseDataWithBenefitAndParties(Benefit.CHILD_SUPPORT.name(), otherParties);

            PreSubmitCallbackResponse<SscsCaseData> response = runMidEvent(caseData);

            assertThat(response.getErrors()).containsExactly(expectedError);
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("casesWithNoValidationError")
        void shouldReturnNoErrorsForValidUkOtherParty(String description, OtherParty party) {
            SscsCaseData caseData = caseDataWithBenefitAndParties(Benefit.CHILD_SUPPORT.name(), List.of(ccd(party)));

            PreSubmitCallbackResponse<SscsCaseData> response = runMidEvent(caseData);

            assertThat(response.getErrors()).isEmpty();
        }

        private static Stream<Arguments> casesWithValidationError() {
            return Stream.of(

                Arguments.of("Other party address details (Address line 1)",
                    List.of(ccd(OtherParty.builder().address(addressMissingFirstLine()).build())),
                    ERROR_ADDRESS_LINE_1_OTHER_PARTY),

                Arguments.of("Other party address details (No postcode)",
                    List.of(ccd(OtherParty.builder().address(addressMissingPostcode()).build())),
                    ERROR_POSTCODE_OTHER_PARTY),

                Arguments.of("Other party address details (Invalid Postcode format)",
                    List.of(ccd(OtherParty.builder().address(addressWithInvalidPostcode()).build())),
                    ERROR_POSTCODE_OTHER_PARTY),

                Arguments.of("Other Party Representative address details (Address line 1)", List.of(ccd(OtherParty.builder()
                    .address(validAddress())
                    .rep(repWithAddress(addressMissingFirstLine()))
                    .build())), ERROR_ADDRESS_LINE_1_OTHER_PARTY_REP),

                Arguments.of("Other Party Representative address details (No postcode)", List.of(ccd(OtherParty.builder()
                    .address(validAddress())
                    .rep(repWithAddress(addressMissingPostcode()))
                    .build())), ERROR_POSTCODE_OTHER_PARTY_REP),

                Arguments.of("Other Party Representative address details (Invalid Postcode format)", List.of(ccd(OtherParty.builder()
                    .address(validAddress())
                    .rep(repWithAddress(addressWithInvalidPostcode()))
                    .build())), ERROR_POSTCODE_OTHER_PARTY_REP),

                Arguments.of("Other Party Appointee address details (Address line 1)", List.of(ccd(OtherParty.builder()
                    .address(validAddress())
                    .isAppointee(YES.getValue())
                    .appointee(appointeeWithAddress(addressMissingFirstLine()))
                    .build())), ERROR_LINE1_OTHER_PARTY_APPOINTEE),

                Arguments.of("Other Party Appointee address details (No postcode)", List.of(ccd(OtherParty.builder()
                    .address(validAddress())
                    .isAppointee(YES.getValue())
                    .appointee(appointeeWithAddress(addressMissingPostcode()))
                    .build())), ERROR_POSTCODE_OTHER_PARTY_APPOINTEE),

                Arguments.of("Other Party Appointee address details (Invalid Postcode format)", List.of(ccd(OtherParty.builder()
                    .address(validAddress())
                    .isAppointee(YES.getValue())
                    .appointee(appointeeWithAddress(addressWithInvalidPostcode()))
                    .build())), ERROR_POSTCODE_OTHER_PARTY_APPOINTEE));
        }

        private static Stream<Arguments> casesWithNoValidationError() {
            return Stream.of(

                Arguments.of("Other party address missing", OtherParty.builder().address(null).build()),

                Arguments.of("Other party address present but no first line of address or postcode",
                    OtherParty.builder().address(ukAddressMissingPostCodeAndFirstLine()).build()),

                Arguments.of("Other party address present with first line of address and postcode",
                    OtherParty.builder().address(validAddress()).build()),

                Arguments.of("Representative present but no address provided",
                    OtherParty.builder().address(validAddress()).rep(repWithAddress(null)).build()),

                Arguments.of("Representative present but no first line of address of postcode", OtherParty.builder()
                    .address(validAddress())
                    .rep(repWithAddress(ukAddressMissingPostCodeAndFirstLine()))
                    .build()),

                Arguments.of("Representative present with first line of address and postcode",
                    OtherParty.builder().address(validAddress()).rep(repWithAddress(validAddress())).build()),

                Arguments.of("Appointee present but no address", OtherParty.builder()
                    .address(validAddress())
                    .isAppointee(YES.getValue())
                    .appointee(appointeeWithAddress(null))
                    .build()),

                Arguments.of("Appointee present but no first line of address or postcode", OtherParty.builder()
                    .address(validAddress())
                    .appointee(appointeeWithAddress(ukAddressMissingPostCodeAndFirstLine()))
                    .build()),

                Arguments.of("Appointee present with first line of address of postcode", OtherParty.builder()
                    .address(validAddress())
                    .appointee(appointeeWithAddress(validAddress()))
                    .build()));

        }

        private static Address validAddress() {
            return Address.builder().line1(LINE_1).postcode(POSTCODE).build();
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

        private static Address ukAddressMissingPostCodeAndFirstLine() {
            return Address.builder().town(DUFFTOWN).build();
        }

        private static Appointee appointeeWithAddress(Address address) {
            return Appointee.builder().address(address).build();
        }
    }

    @Nested
    class ValidationUkAndInternational {

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

        private static Stream<Arguments> casesWithErrors() {
            return Stream.of(

                Arguments.of("Other party empty address null", OtherParty.builder().address(null).build(),
                    Set.of(ADDRESS_DETAILS_ARE_MISSING_FOR_THE_OTHER_PARTY)),

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
                    .address(validUkAddress())
                    .rep(repWithAddress(ukAddressWithNoAddressDetails()))
                    .build(), Set.of(ERROR_ADDRESS_LINE_1_OTHER_PARTY_REP, ERROR_POSTCODE_OTHER_PARTY_REP)),

                Arguments.of("International other party with rep address missing mainland selection",
                    OtherParty.builder()
                        .address(anInternationalAddress())
                        .rep(repWithAddress(addressMissingMainland()))
                        .build(), Set.of(ERROR_MAINLAND_SELECTION_OTHER_PARTY_REP)),

                Arguments.of("International other party missing line1+country, rep UK missing postcode",
                    OtherParty.builder()
                        .address(anInternationalAddressWithNoFirstLineAddressOrCountry())
                        .rep(repWithAddress(addressMissingPostcodeAndMainland()))
                        .build(), Set.of(ERROR_ADDRESS_LINE_1_OTHER_PARTY, ERROR_COUNTRY_OTHER_PARTY,
                        ERROR_MAINLAND_SELECTION_OTHER_PARTY_REP)));
        }

        private static Stream<Arguments> casesWithNoErrors() {
            return Stream.of(
                Arguments.of("UK other party, no rep", OtherParty.builder().address(validUkAddress()).build()),

                Arguments.of("UK other party, UK rep",
                    OtherParty.builder().address(validUkAddress()).rep(repWithAddress(validUkAddress())).build()),

                Arguments.of("UK other party, international rep", OtherParty.builder()
                    .address(validUkAddress())
                    .rep(repWithAddress(anInternationalAddress()))
                    .build()),

                Arguments.of("International other party, no rep",
                    OtherParty.builder().address(anInternationalAddress()).build()),

                Arguments.of("International other party, UK rep", OtherParty.builder()
                    .address(anInternationalAddress())
                    .rep(repWithAddress(validUkAddress()))
                    .build()),

                Arguments.of("International other party, international rep", OtherParty.builder()
                    .address(anInternationalAddress())
                    .rep(repWithAddress(anInternationalAddress()))
                    .build()));
        }

        private static Address validUkAddress() {
            return Address.builder().line1(LINE_1).postcode(POSTCODE).inMainlandUk(YES).build();
        }

        private static Address addressMissingMainland() {
            return Address.builder().line1(LINE_1).postcode(POSTCODE).build();
        }

        private static Address anEmptyAddress() {
            return Address.builder().build();
        }

        private static Address addressMissingPostcodeAndMainland() {
            return Address.builder().line1(LINE_1).build();
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

    }
}
