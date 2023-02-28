package uk.gov.hmcts.reform.sscs.ccd.presubmit.issuegenericletter;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ISSUE_GENERIC_LETTER;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.VALID_APPEAL;
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
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
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

        var result = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertNotNull(result);
        assertTrue(isEmpty(result.getErrors()));
    }

    private List<CcdValue<OtherParty>> buildOtherParties() {
        var list = new ArrayList<CcdValue<OtherParty>>();

        list.add(buildOtherParty());
        list.add(buildOtherParty());

        return list;
    }

    private CcdValue<OtherParty> buildOtherParty() {
        Address address = Address.builder()
                .postcode("AP12 4PA")
                .line1("42 Appointed Mews")
                .line2("Apford")
                .town("Apton")
                .county("Appshire")
                .build();

        Name name = Name.builder()
                .lastName("LastName")
                .firstName("FirstName")
                .build();

        OtherParty party =  OtherParty.builder()
                .id(UUID.randomUUID().toString())
                .address(address)
                .name(name)
                .build();

        return new CcdValue<>(party);
    }
}
