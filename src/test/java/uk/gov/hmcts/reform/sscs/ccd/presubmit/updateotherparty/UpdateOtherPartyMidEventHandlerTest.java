package uk.gov.hmcts.reform.sscs.ccd.presubmit.updateotherparty;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.model.AppConstants.IBCA_BENEFIT_CODE;

import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.Representative;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@RunWith(JUnitParamsRunner.class)
public class UpdateOtherPartyMidEventHandlerTest {
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

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private UpdateOtherPartyMidEventHandler midEventHandler;

    @Before
    public void setUp() throws Exception {
        openMocks(this);
        midEventHandler = new UpdateOtherPartyMidEventHandler();
    }

    @Test
    public void shouldReturnTrueForCanHandle() {
        OtherParty party = OtherParty.builder()
                .address(Address.builder().line1("42 new road").build())
                .build();

        CcdValue<OtherParty> expectedOtherParty = new CcdValue<>(party);
        List<CcdValue<OtherParty>> expectedOtherPartyList = List.of(expectedOtherParty);

        SscsCaseData caseData = SscsCaseData.builder()
                .benefitCode(IBCA_BENEFIT_CODE)
                .otherParties(expectedOtherPartyList)
                .build();

        when(callback.getEvent()).thenReturn(EventType.UPDATE_OTHER_PARTY_DATA);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        assertTrue(midEventHandler.canHandle(MID_EVENT, callback));
    }

    @Test
    public void shouldReturnFalseForCanHandleNonIbaCase() {
        OtherParty party = OtherParty.builder()
            .address(Address.builder().line1("42 new road").build())
            .build();

        CcdValue<OtherParty> expectedOtherParty = new CcdValue<>(party);
        List<CcdValue<OtherParty>> expectedOtherPartyList = List.of(expectedOtherParty);

        SscsCaseData caseData = SscsCaseData.builder()
            .benefitCode("053")
            .otherParties(expectedOtherPartyList)
            .build();

        when(callback.getEvent()).thenReturn(EventType.UPDATE_OTHER_PARTY_DATA);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        assertFalse(midEventHandler.canHandle(MID_EVENT, callback));
    }

    @Test
    @Parameters({
        "EVENTS_UPDATES",
        "APPEAL_RECEIVED",
        "CASE_UPDATED"
    })
    public void shouldReturnFalseForCanHandleWhenNotEventUpdateOtherPartyData(EventType eventType) {
        OtherParty party = OtherParty.builder()
                .address(Address.builder().line1("42 new road").build())
                .build();

        CcdValue<OtherParty> expectedOtherParty = new CcdValue<>(party);
        List<CcdValue<OtherParty>> expectedOtherPartyList = List.of(expectedOtherParty);

        SscsCaseData caseData = SscsCaseData.builder()
                .benefitCode(IBCA_BENEFIT_CODE)
                .otherParties(expectedOtherPartyList)
                .build();

        when(callback.getEvent()).thenReturn(eventType);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        assertFalse(midEventHandler.canHandle(MID_EVENT, callback));
    }

    @Test
    public void shouldReturnNoErrorsOnMidEventForUkNoRep() {
        OtherParty party = OtherParty.builder()
                .address(Address.builder()
                        .line1("42 new road")
                        .inMainlandUk(YES)
                        .postcode("SWA 1AA")
                        .build())
                .build();

        CcdValue<OtherParty> expectedOtherParty = new CcdValue<>(party);
        List<CcdValue<OtherParty>> expectedOtherPartyList = List.of(expectedOtherParty);

        SscsCaseData caseData = SscsCaseData.builder()
                .benefitCode(IBCA_BENEFIT_CODE)
                .otherParties(expectedOtherPartyList)
                .build();

        when(callback.getEvent()).thenReturn(EventType.UPDATE_OTHER_PARTY_DATA);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        PreSubmitCallbackResponse<SscsCaseData> response = midEventHandler.handle(MID_EVENT, callback,USER_AUTHORISATION);

        assertTrue(response.getErrors().isEmpty());
    }

    @Test
    public void shouldReturnNoErrorsOnMidEventForNonUkNoRep() {
        OtherParty party = OtherParty.builder()
                .address(Address.builder()
                        .line1("42 new road")
                        .inMainlandUk(NO)
                        .country("America")
                        .build())
                .build();

        CcdValue<OtherParty> expectedOtherParty = new CcdValue<>(party);
        List<CcdValue<OtherParty>> expectedOtherPartyList = List.of(expectedOtherParty);

        SscsCaseData caseData = SscsCaseData.builder()
                .benefitCode(IBCA_BENEFIT_CODE)
                .otherParties(expectedOtherPartyList)
                .build();

        when(callback.getEvent()).thenReturn(EventType.UPDATE_OTHER_PARTY_DATA);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        PreSubmitCallbackResponse<SscsCaseData> response = midEventHandler.handle(MID_EVENT, callback,USER_AUTHORISATION);

        assertTrue(response.getErrors().isEmpty());
    }

    @Test
    public void shouldReturnErrorsOnMidEventForAllBlankUkNoRep() {
        OtherParty party = OtherParty.builder()
                .address(Address.builder()
                        .build())
                .build();

        CcdValue<OtherParty> expectedOtherParty = new CcdValue<>(party);
        List<CcdValue<OtherParty>> expectedOtherPartyList = List.of(expectedOtherParty);

        SscsCaseData caseData = SscsCaseData.builder()
                .benefitCode(IBCA_BENEFIT_CODE)
                .otherParties(expectedOtherPartyList)
                .build();

        when(callback.getEvent()).thenReturn(EventType.UPDATE_OTHER_PARTY_DATA);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        PreSubmitCallbackResponse<SscsCaseData> response = midEventHandler.handle(MID_EVENT, callback,USER_AUTHORISATION);

        assertEquals(2, response.getErrors().size());
        assertTrue(response.getErrors().contains(ERROR_ADDRESS_LINE_1_OTHER_PARTY));
        assertTrue(response.getErrors().contains(ERROR_MAINLAND_SELECTION_OTHER_PARTY));
    }

    @Test
    public void shouldReturnErrorsOnMidEventForFirstLineUkNoRep() {
        OtherParty party = OtherParty.builder()
                .address(Address.builder()
                        .line1("11 test rd")
                        .build())
                .build();

        CcdValue<OtherParty> expectedOtherParty = new CcdValue<>(party);
        List<CcdValue<OtherParty>> expectedOtherPartyList = List.of(expectedOtherParty);

        SscsCaseData caseData = SscsCaseData.builder()
                .benefitCode(IBCA_BENEFIT_CODE)
                .otherParties(expectedOtherPartyList)
                .build();

        when(callback.getEvent()).thenReturn(EventType.UPDATE_OTHER_PARTY_DATA);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        PreSubmitCallbackResponse<SscsCaseData> response = midEventHandler.handle(MID_EVENT, callback,USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());
        assertTrue(response.getErrors().contains(ERROR_MAINLAND_SELECTION_OTHER_PARTY));
    }

    @Test
    public void shouldReturnErrorsOnMidEventForNeedsPostcodeUkNoRep() {
        OtherParty party = OtherParty.builder()
                .address(Address.builder()
                        .line1("11 test rd")
                        .inMainlandUk(YES)
                        .build())
                .build();

        CcdValue<OtherParty> expectedOtherParty = new CcdValue<>(party);
        List<CcdValue<OtherParty>> expectedOtherPartyList = List.of(expectedOtherParty);

        SscsCaseData caseData = SscsCaseData.builder()
                .benefitCode(IBCA_BENEFIT_CODE)
                .otherParties(expectedOtherPartyList)
                .build();

        when(callback.getEvent()).thenReturn(EventType.UPDATE_OTHER_PARTY_DATA);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        PreSubmitCallbackResponse<SscsCaseData> response = midEventHandler.handle(MID_EVENT, callback,USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());
        assertTrue(response.getErrors().contains(ERROR_POSTCODE_OTHER_PARTY));
    }

    @Test
    public void shouldReturnErrorsOnMidEventForMainlandOnlyNotUkNoRep() {
        OtherParty party = OtherParty.builder()
                .address(Address.builder()
                        .inMainlandUk(NO)
                        .build())
                .build();

        CcdValue<OtherParty> expectedOtherParty = new CcdValue<>(party);
        List<CcdValue<OtherParty>> expectedOtherPartyList = List.of(expectedOtherParty);

        SscsCaseData caseData = SscsCaseData.builder()
                .benefitCode(IBCA_BENEFIT_CODE)
                .otherParties(expectedOtherPartyList)
                .build();

        when(callback.getEvent()).thenReturn(EventType.UPDATE_OTHER_PARTY_DATA);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        PreSubmitCallbackResponse<SscsCaseData> response = midEventHandler.handle(MID_EVENT, callback,USER_AUTHORISATION);

        assertEquals(2, response.getErrors().size());
        assertTrue(response.getErrors().contains(ERROR_ADDRESS_LINE_1_OTHER_PARTY));
        assertTrue(response.getErrors().contains(ERROR_COUNTRY_OTHER_PARTY));
    }

    @Test
    public void shouldReturnErrorsOnMidEventForFirstLineNotUkNoRep() {
        OtherParty party = OtherParty.builder()
                .address(Address.builder()
                        .line1("11 test rd")
                        .inMainlandUk(NO)
                        .build())
                .build();

        CcdValue<OtherParty> expectedOtherParty = new CcdValue<>(party);
        List<CcdValue<OtherParty>> expectedOtherPartyList = List.of(expectedOtherParty);

        SscsCaseData caseData = SscsCaseData.builder()
                .benefitCode(IBCA_BENEFIT_CODE)
                .otherParties(expectedOtherPartyList)
                .build();

        when(callback.getEvent()).thenReturn(EventType.UPDATE_OTHER_PARTY_DATA);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        PreSubmitCallbackResponse<SscsCaseData> response = midEventHandler.handle(MID_EVENT, callback,USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());
        assertTrue(response.getErrors().contains(ERROR_COUNTRY_OTHER_PARTY));
    }

    @Test
    public void shouldReturnNoErrorsOnMidEventForUkHasUkRep() {
        Representative expectedRep = new Representative("YES");
        expectedRep.setAddress(Address.builder()
                .line1("333 London Rd")
                .inMainlandUk(YES)
                .postcode("SWA 1AA")
                .build());

        OtherParty party = OtherParty.builder()
                .address(Address.builder()
                        .line1("42 new road")
                        .inMainlandUk(YES)
                        .postcode("SWA 1AA")
                        .build())
                .rep(expectedRep)
                .build();

        CcdValue<OtherParty> expectedOtherParty = new CcdValue<>(party);
        List<CcdValue<OtherParty>> expectedOtherPartyList = List.of(expectedOtherParty);

        SscsCaseData caseData = SscsCaseData.builder()
                .benefitCode(IBCA_BENEFIT_CODE)
                .otherParties(expectedOtherPartyList)
                .build();

        when(callback.getEvent()).thenReturn(EventType.UPDATE_OTHER_PARTY_DATA);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        PreSubmitCallbackResponse<SscsCaseData> response = midEventHandler.handle(MID_EVENT, callback,USER_AUTHORISATION);

        assertTrue(response.getErrors().isEmpty());
    }

    @Test
    public void shouldReturnNoErrorsOnMidEventForNonUkHasRepInUk() {
        Representative expectedRep = new Representative("YES");
        expectedRep.setAddress(Address.builder()
                .line1("333 London Rd")
                .inMainlandUk(YES)
                .postcode("SWA 1AA")
                .build());

        OtherParty party = OtherParty.builder()
                .address(Address.builder()
                        .line1("42 new road")
                        .inMainlandUk(NO)
                        .country("America")
                        .build())
                .rep(expectedRep)
                .build();

        CcdValue<OtherParty> expectedOtherParty = new CcdValue<>(party);
        List<CcdValue<OtherParty>> expectedOtherPartyList = List.of(expectedOtherParty);

        SscsCaseData caseData = SscsCaseData.builder()
                .benefitCode(IBCA_BENEFIT_CODE)
                .otherParties(expectedOtherPartyList)
                .build();

        when(callback.getEvent()).thenReturn(EventType.UPDATE_OTHER_PARTY_DATA);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        PreSubmitCallbackResponse<SscsCaseData> response = midEventHandler.handle(MID_EVENT, callback,USER_AUTHORISATION);

        assertTrue(response.getErrors().isEmpty());
    }

    @Test
    public void shouldReturnErrorsOnMidEventForUkHasAllBlankRep() {
        Representative expectedRep = new Representative("YES");
        OtherParty party = OtherParty.builder()
                .address(Address.builder()
                        .line1("69 zoo lane")
                        .inMainlandUk(YES)
                        .postcode("swa 1aa")
                        .build())
                .rep(expectedRep)
                .build();

        CcdValue<OtherParty> expectedOtherParty = new CcdValue<>(party);
        List<CcdValue<OtherParty>> expectedOtherPartyList = List.of(expectedOtherParty);

        SscsCaseData caseData = SscsCaseData.builder()
                .benefitCode(IBCA_BENEFIT_CODE)
                .otherParties(expectedOtherPartyList)
                .build();

        when(callback.getEvent()).thenReturn(EventType.UPDATE_OTHER_PARTY_DATA);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        PreSubmitCallbackResponse<SscsCaseData> response = midEventHandler.handle(MID_EVENT, callback,USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());
        assertTrue(response.getErrors().contains(ERROR_ADDRESS_MISSING_OTHER_PARTY_REP));
    }

    @Test
    public void shouldReturnErrorsOnMidEventForNonUkPartyAndRep() {
        Representative expectedRep = new Representative("YES");
        expectedRep.setAddress(Address.builder().inMainlandUk(NO).build());

        OtherParty party = OtherParty.builder()
                .address(Address.builder()
                        .line1("69 zoo lane")
                        .inMainlandUk(NO)
                        .postcode("01210")
                        .country("America")
                        .build())
                .rep(expectedRep)
                .build();

        CcdValue<OtherParty> expectedOtherParty = new CcdValue<>(party);
        List<CcdValue<OtherParty>> expectedOtherPartyList = List.of(expectedOtherParty);

        SscsCaseData caseData = SscsCaseData.builder()
                .benefitCode(IBCA_BENEFIT_CODE)
                .otherParties(expectedOtherPartyList)
                .build();

        when(callback.getEvent()).thenReturn(EventType.UPDATE_OTHER_PARTY_DATA);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        PreSubmitCallbackResponse<SscsCaseData> response = midEventHandler.handle(MID_EVENT, callback,USER_AUTHORISATION);

        assertEquals(2, response.getErrors().size());
        assertTrue(response.getErrors().contains(ERROR_ADDRESS_LINE_1_OTHER_PARTY_REP));
        assertTrue(response.getErrors().contains(ERROR_COUNTRY_OTHER_PARTY_REP));
    }

    @Test
    public void shouldReturnErrorsOnMidEventForUkPartyAndUkRep() {
        Representative expectedRep = new Representative("YES");
        expectedRep.setAddress(Address.builder().inMainlandUk(YES).build());

        OtherParty party = OtherParty.builder()
                .address(Address.builder()
                        .line1("69 zoo lane")
                        .inMainlandUk(NO)
                        .postcode("01210")
                        .country("America")
                        .build())
                .rep(expectedRep)
                .build();

        CcdValue<OtherParty> expectedOtherParty = new CcdValue<>(party);
        List<CcdValue<OtherParty>> expectedOtherPartyList = List.of(expectedOtherParty);

        SscsCaseData caseData = SscsCaseData.builder()
                .benefitCode(IBCA_BENEFIT_CODE)
                .otherParties(expectedOtherPartyList)
                .build();

        when(callback.getEvent()).thenReturn(EventType.UPDATE_OTHER_PARTY_DATA);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        PreSubmitCallbackResponse<SscsCaseData> response = midEventHandler.handle(MID_EVENT, callback,USER_AUTHORISATION);

        assertEquals(2, response.getErrors().size());
        assertTrue(response.getErrors().contains(ERROR_ADDRESS_LINE_1_OTHER_PARTY_REP));
        assertTrue(response.getErrors().contains(ERROR_POSTCODE_OTHER_PARTY_REP));
    }

    @Test
    public void shouldReturnErrorsOnMidEventForNonUkPartyAndNoMainlandRep() {
        Representative expectedRep = new Representative("YES");
        expectedRep.setAddress(Address.builder().line1("22 OnlyLine Lane").build());

        OtherParty party = OtherParty.builder()
                .address(Address.builder()
                        .line1("69 zoo lane")
                        .inMainlandUk(NO)
                        .postcode("01210")
                        .country("America")
                        .build())
                .rep(expectedRep)
                .build();

        CcdValue<OtherParty> expectedOtherParty = new CcdValue<>(party);
        List<CcdValue<OtherParty>> expectedOtherPartyList = List.of(expectedOtherParty);

        SscsCaseData caseData = SscsCaseData.builder()
                .benefitCode(IBCA_BENEFIT_CODE)
                .otherParties(expectedOtherPartyList)
                .build();

        when(callback.getEvent()).thenReturn(EventType.UPDATE_OTHER_PARTY_DATA);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        PreSubmitCallbackResponse<SscsCaseData> response = midEventHandler.handle(MID_EVENT, callback,USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());
        assertTrue(response.getErrors().contains(ERROR_MAINLAND_SELECTION_OTHER_PARTY_REP));
    }

    @Test
    public void shouldReturnErrorsOnMidEventForPartyAndRepIncomplete() {
        Representative expectedRep = new Representative("YES");
        expectedRep.setAddress(Address.builder().line1("22 OnlyLine Lane").build());

        OtherParty party = OtherParty.builder()
                .address(Address.builder()
                        .inMainlandUk(NO)
                        .postcode("01210")
                        .build())
                .rep(expectedRep)
                .build();

        CcdValue<OtherParty> expectedOtherParty = new CcdValue<>(party);
        List<CcdValue<OtherParty>> expectedOtherPartyList = List.of(expectedOtherParty);

        SscsCaseData caseData = SscsCaseData.builder()
                .benefitCode(IBCA_BENEFIT_CODE)
                .otherParties(expectedOtherPartyList)
                .build();

        when(callback.getEvent()).thenReturn(EventType.UPDATE_OTHER_PARTY_DATA);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        PreSubmitCallbackResponse<SscsCaseData> response = midEventHandler.handle(MID_EVENT, callback,USER_AUTHORISATION);

        assertEquals(3, response.getErrors().size());
        assertTrue(response.getErrors().contains(ERROR_ADDRESS_LINE_1_OTHER_PARTY));
        assertTrue(response.getErrors().contains(ERROR_COUNTRY_OTHER_PARTY));
        assertTrue(response.getErrors().contains(ERROR_MAINLAND_SELECTION_OTHER_PARTY_REP));
    }
}
