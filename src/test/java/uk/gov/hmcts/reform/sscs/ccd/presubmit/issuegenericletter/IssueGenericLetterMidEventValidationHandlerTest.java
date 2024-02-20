package uk.gov.hmcts.reform.sscs.ccd.presubmit.issuegenericletter;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ISSUE_GENERIC_LETTER;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.VALID_APPEAL;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.YES;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseData;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.JointParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherPartySelectionDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.Representative;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;

@Slf4j
class IssueGenericLetterMidEventValidationHandlerTest {
    private IssueGenericLetterMidEventValidationHandler handler;

    private Callback<SscsCaseData> callback;

    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData caseData;

    private static final String CASE_ID = "1111111111111111";

    private static final String USER_AUTHORISATION = "Bearer token";

    @BeforeEach
    protected void setUp() {
        caseData = buildCaseData();
        caseDetails = new CaseDetails<>(1L, "SSCS", VALID_APPEAL, caseData,
                LocalDateTime.now(), "Benefit");

        callback = new Callback<>(caseDetails, Optional.empty(), ISSUE_GENERIC_LETTER, false);

        handler = new IssueGenericLetterMidEventValidationHandler();
    }

    @ParameterizedTest
    @EnumSource(value = CallbackType.class, names = {"ABOUT_TO_START", "ABOUT_TO_SUBMIT", "SUBMITTED"})
    void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    void givenANonIssueGenericLetter_MidEvent_thenReturnFalse() {
        callback = new Callback<>(caseDetails, Optional.empty(), APPEAL_RECEIVED, false);
        assertFalse(handler.canHandle(MID_EVENT, callback));
    }

    @Test
    void givenAddressesAreValid_errorsShouldBeEmpty() {
        caseData.getJointParty().setAddress(caseData.getAppeal().getAppellant().getAddress());
        caseData.setSendToAllParties(YesNo.YES);
        caseData.setOtherParties(buildOtherParties());
        caseData.setJointParty(buildJointParty(true));

        var result = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNotNull(result);
        assertTrue(isEmpty(result.getErrors()));
    }

    @Test
    void givenJoinPartyIsSameAsAppellant_andAddressIsValid_errorsShouldBeEmpty() {
        caseData.getJointParty().setJointPartyAddressSameAsAppellant(YesNo.YES);
        caseData.setSendToJointParty(YesNo.YES);

        var result = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNotNull(result);
        assertTrue(isEmpty(result.getErrors()));
    }

    @Test
    void giveAddressIsEmptyForMultipleParties_returnErrors() {
        caseData.getAppeal().getAppellant().setAppointee(null);
        caseData.getAppeal().getAppellant().setAddress(null);
        caseData.getAppeal().getRep().setAddress(null);
        caseData.setOtherParties(List.of(buildOtherParty(false)));
        caseData.setSendToAllParties(YesNo.YES);
        caseData.setJointParty(buildJointParty(false));

        var result = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
        var errors = result.getErrors();

        assertNotNull(result);
        assertFalse(isEmpty(errors));
        assertEquals(4, errors.size());
    }

    @Test
    void giveAppellantAppointeeAddressIsEmpty_returnError() {
        caseData.getAppeal().getAppellant().setIsAppointee(YES);
        caseData.getAppeal().getAppellant().getAppointee().setAddress(null);
        caseData.setSendToAllParties(YesNo.YES);

        var result = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
        var errors = result.getErrors();

        assertNotNull(result);
        assertFalse(isEmpty(errors));
        assertEquals(1, errors.size());
    }

    @Test
    void givenOtherPartyAppointeeHasEmptyAddress_returnError() {
        var otherParty = buildOtherParty(false);
        var appointee = caseData.getAppeal().getAppellant().getAppointee();
        appointee.setAddress(null);
        otherParty.getValue().setAppointee(appointee);
        otherParty.getValue().setIsAppointee(YesNo.YES.getValue());

        var item = new DynamicListItem(appointee.getId(), null);
        var list = new ArrayList<DynamicListItem>();

        List<CcdValue<OtherPartySelectionDetails>> otherPartySelection = List.of(
                new CcdValue<>(new OtherPartySelectionDetails(new DynamicList(item, list)))
        );

        caseData.setSendToAllParties(YesNo.NO);
        caseData.setSendToApellant(YesNo.NO);
        caseData.setSendToOtherParties(YesNo.YES);
        caseData.setOtherParties(List.of(otherParty));
        caseData.setOtherPartySelection(otherPartySelection);

        var result = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
        var errors = result.getErrors();

        assertNotNull(result);
        assertFalse(isEmpty(errors));
        assertEquals(1, errors.size());
    }

    @Test
    void givenOtherPartyRepHasEmptyAddress_returnError() {
        var otherParty = buildOtherParty(false);
        var rep = otherParty.getValue().getRep();
        rep.setAddress(null);

        var item = new DynamicListItem(rep.getId(), null);
        var list = new ArrayList<DynamicListItem>();

        List<CcdValue<OtherPartySelectionDetails>> otherPartySelection = List.of(
                new CcdValue<>(new OtherPartySelectionDetails(new DynamicList(item, list)))
        );

        caseData.setSendToAllParties(YesNo.NO);
        caseData.setSendToApellant(YesNo.NO);
        caseData.setSendToOtherParties(YesNo.YES);
        caseData.setOtherParties(List.of(otherParty));
        caseData.setOtherPartySelection(otherPartySelection);

        var result = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
        var errors = result.getErrors();

        assertNotNull(result);
        assertFalse(isEmpty(errors));
        assertEquals(1, errors.size());
    }

    @Test
    void givenOtherPartyHasEmptyAddress_returnError() {
        var otherParty = buildOtherParty(false);

        var item = new DynamicListItem(otherParty.getValue().getId(), null);
        var list = new ArrayList<DynamicListItem>();

        List<CcdValue<OtherPartySelectionDetails>> otherPartySelection = List.of(
                new CcdValue<>(new OtherPartySelectionDetails(new DynamicList(item, list)))
        );
        caseData.setOtherPartySelection(otherPartySelection);
        caseData.setSendToOtherParties(YesNo.YES);
        caseData.setOtherParties(List.of(otherParty));

        var result = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
        var errors = result.getErrors();

        assertNotNull(result);
        assertFalse(isEmpty(errors));
        assertEquals(1, errors.size());
    }

    @Test
    void givenNoReceiverSelected_returnError() {
        caseData.setSendToAllParties(YesNo.NO);
        caseData.setSendToApellant(YesNo.NO);
        caseData.setSendToOtherParties(YesNo.NO);

        var result = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
        var errors = result.getErrors();

        assertNotNull(result);
        assertFalse(isEmpty(errors));
        assertEquals(1, errors.size());
    }

    private List<CcdValue<OtherParty>> buildOtherParties() {
        var list = new ArrayList<CcdValue<OtherParty>>();

        list.add(buildOtherParty(true));
        list.add(buildOtherParty(true));

        return list;
    }

    private CcdValue<OtherParty> buildOtherParty(boolean addAddress) {
        Address address = addAddress ? buildAddress() : null;

        Name name = Name.builder()
                .lastName("LastName")
                .firstName("FirstName")
                .build();

        Representative rep = Representative.builder()
                .id(UUID.randomUUID().toString())
                .name(name)
                .address(address)
                .hasRepresentative(YesNo.YES.getValue())
                .build();

        OtherParty party =  OtherParty.builder()
                .id(UUID.randomUUID().toString())
                .address(address)
                .name(name)
                .rep(rep)
                .build();

        return new CcdValue<>(party);
    }

    private JointParty buildJointParty(boolean addAddress) {
        Address address = addAddress ? buildAddress() : null;

        Name name = Name.builder()
                .lastName("LastName")
                .firstName("FirstName")
                .build();

        return JointParty.builder()
                .hasJointParty(YesNo.YES)
                .address(address)
                .name(name)
                .build();

    }

    private static Address buildAddress() {
        return Address.builder()
                .postcode("AP12 4PA")
                .line1("42 Appointed Mews")
                .line2("Apford")
                .town("Apton")
                .county("Appshire")
                .build();
    }
}
