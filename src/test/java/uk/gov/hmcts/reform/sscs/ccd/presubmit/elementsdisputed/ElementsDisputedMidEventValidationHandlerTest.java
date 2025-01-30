package uk.gov.hmcts.reform.sscs.ccd.presubmit.elementsdisputed;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import javax.validation.Validation;
import javax.validation.Validator;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

@RunWith(JUnitParamsRunner.class)
public class ElementsDisputedMidEventValidationHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private ElementsDisputedMidEventValidationHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private IdamClient idamClient;

    private SscsCaseData sscsCaseData;

    protected static Validator validator = Validation.byDefaultProvider()
            .configure()
            .messageInterpolator(new ParameterMessageInterpolator())
            .buildValidatorFactory()
            .getValidator();

    @Before
    public void setUp() throws IOException {
        openMocks(this);
        handler = new ElementsDisputedMidEventValidationHandler(validator);

        when(callback.getEvent()).thenReturn(EventType.DWP_UPLOAD_RESPONSE);
        when(callback.getCaseDetails()).thenReturn(caseDetails);

        sscsCaseData = SscsCaseData.builder()
            .ccdCaseId("ccdId")
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("UC").build()).build()).build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    @Parameters({"DWP_UPLOAD_RESPONSE", "HMCTS_RESPONSE_REVIEWED", "AMEND_ELEMENTS_ISSUES"})
    public void givenAnElementsDisputedEventType_thenReturnTrue(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);

        assertTrue(handler.canHandle(MID_EVENT, callback));
    }

    @Test
    public void givenANonElementsDisputedEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.WRITE_FINAL_DECISION);
        assertFalse(handler.canHandle(MID_EVENT, callback));
    }

    @Test
    @Parameters({"ABOUT_TO_START", "ABOUT_TO_SUBMIT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    public void givenElementsDisputedGeneralPopulatedWithDuplicateIssues_thenDisplayError() {

        sscsCaseData.setElementsDisputedGeneral(List.of(
                ElementDisputed.builder().value(ElementDisputedDetails.builder().issueCode("AA").build()).build(),
                ElementDisputed.builder().value(ElementDisputedDetails.builder().issueCode("AA").build()).build()));

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("General element contains duplicate issue codes", error);
    }

    @Test
    public void givenElementsDisputedSanctionsPopulatedWithDuplicateIssues_thenDisplayError() {

        sscsCaseData.setElementsDisputedSanctions(List.of(
                ElementDisputed.builder().value(ElementDisputedDetails.builder().issueCode("AA").build()).build(),
                ElementDisputed.builder().value(ElementDisputedDetails.builder().issueCode("AA").build()).build()));

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Standard allowance - sanctions element contains duplicate issue codes", error);
    }

    @Test
    public void givenElementsDisputedOverpaymentPopulatedWithDuplicateIssues_thenDisplayError() {

        sscsCaseData.setElementsDisputedOverpayment(List.of(
                ElementDisputed.builder().value(ElementDisputedDetails.builder().issueCode("AA").build()).build(),
                ElementDisputed.builder().value(ElementDisputedDetails.builder().issueCode("AA").build()).build()));

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Standard allowance - overpayment element contains duplicate issue codes", error);
    }

    @Test
    public void givenElementsDisputedHousingPopulatedWithDuplicateIssues_thenDisplayError() {

        sscsCaseData.setElementsDisputedHousing(List.of(
                ElementDisputed.builder().value(ElementDisputedDetails.builder().issueCode("AA").build()).build(),
                ElementDisputed.builder().value(ElementDisputedDetails.builder().issueCode("AA").build()).build()));

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Housing element contains duplicate issue codes", error);
    }

    @Test
    public void givenElementsDisputedChildcarePopulatedWithDuplicateIssues_thenDisplayError() {

        sscsCaseData.setElementsDisputedChildCare(List.of(
                ElementDisputed.builder().value(ElementDisputedDetails.builder().issueCode("AA").build()).build(),
                ElementDisputed.builder().value(ElementDisputedDetails.builder().issueCode("AA").build()).build()));

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Childcare element contains duplicate issue codes", error);
    }

    @Test
    public void givenElementsDisputedCarePopulatedWithDuplicateIssues_thenDisplayError() {

        sscsCaseData.setElementsDisputedCare(List.of(
                ElementDisputed.builder().value(ElementDisputedDetails.builder().issueCode("AA").build()).build(),
                ElementDisputed.builder().value(ElementDisputedDetails.builder().issueCode("AA").build()).build()));

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Carer element contains duplicate issue codes", error);
    }

    @Test
    public void givenElementsDisputedChildElementPopulatedWithDuplicateIssues_thenDisplayError() {

        sscsCaseData.setElementsDisputedChildElement(List.of(
                ElementDisputed.builder().value(ElementDisputedDetails.builder().issueCode("AA").build()).build(),
                ElementDisputed.builder().value(ElementDisputedDetails.builder().issueCode("AA").build()).build()));

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Child element contains duplicate issue codes", error);
    }

    @Test
    public void givenElementsDisputedChildDisabledPopulatedWithDuplicateIssues_thenDisplayError() {

        sscsCaseData.setElementsDisputedChildDisabled(List.of(
                ElementDisputed.builder().value(ElementDisputedDetails.builder().issueCode("AA").build()).build(),
                ElementDisputed.builder().value(ElementDisputedDetails.builder().issueCode("AA").build()).build()));

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Disabled child addition element contains duplicate issue codes", error);
    }

    @Test
    public void givenElementsDisputedLimitedWorkPopulatedWithDuplicateIssues_thenDisplayError() {

        sscsCaseData.setElementsDisputedLimitedWork((List.of(
                ElementDisputed.builder().value(ElementDisputedDetails.builder().issueCode("AA").build()).build(),
                ElementDisputed.builder().value(ElementDisputedDetails.builder().issueCode("AA").build()).build())));

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Limited Capability for Work (WCA) element contains duplicate issue codes", error);
    }

    @Test
    public void givenElementsDisputedSelectedWithNoDuplicates_thenDoNotDisplayError() {

        sscsCaseData.setElementsDisputedChildDisabled(List.of(
                ElementDisputed.builder().value(ElementDisputedDetails.builder().issueCode("AA").build()).build()));

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertTrue(response.getErrors().isEmpty());
    }

    @Test
    public void givenElementsDisputedGeneralPopulatedWithDuplicateIssueCodeButDifferentOutcomes_thenDoShowError() {
        sscsCaseData.setElementsDisputedGeneral(List.of(
                ElementDisputed.builder().value(ElementDisputedDetails.builder().issueCode("AA").outcome("Test").build()).build(),
                ElementDisputed.builder().value(ElementDisputedDetails.builder().issueCode("AA").outcome("Different").build()).build()));

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("General element contains duplicate issue codes", error);
    }

    @Test
    public void givenNoJointPartyDob_thenDoNotDisplayError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertTrue(response.getErrors().isEmpty());
    }

    @Test
    public void givenDobWithYearInPast_thenDoNotDisplayError() {

        String dateToTest = LocalDate.now().minusYears(1).toString();

        sscsCaseData.getJointParty().setIdentity(Identity.builder().dob(dateToTest).build());

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertTrue(response.getErrors().isEmpty());
    }

    @Test
    public void givenDobWithYearOfThisYear_thenDoShowError() {

        String dateToTest = LocalDate.now().toString();

        sscsCaseData.getJointParty().setIdentity(Identity.builder().dob(dateToTest).build());

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("You’ve entered an invalid date of birth", error);
    }

    @Test
    public void givenDobWithYearOfFutureYear_thenDoShowError() {

        String dateToTest = LocalDate.now().plusYears(1).toString();

        sscsCaseData.getJointParty().setIdentity(Identity.builder().dob(dateToTest).build());

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("You’ve entered an invalid date of birth", error);
    }

    @Test
    public void givenNoJointPartyNino_thenDoNotDisplayError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertTrue(response.getErrors().isEmpty());
    }

    @Test
    public void givenValidJointPartyNinoNoSpaces_thenDoNotDisplayError() {

        sscsCaseData.getJointParty().setIdentity(Identity.builder().nino("BB000000B").build());

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertTrue(response.getErrors().isEmpty());
    }

    @Test
    public void givenValidJointPartyNinoWithSpaces_thenDoNotDisplayError() {

        sscsCaseData.getJointParty().setIdentity(Identity.builder().nino("BB 00 00 00 B").build());

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);


        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertTrue(response.getErrors().isEmpty());
    }

    @Test
    public void givenInvalidJointPartyNino_thenDoDisplayError() {

        sscsCaseData.getJointParty().setIdentity(Identity.builder().nino("blah").build());

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Invalid National Insurance number", error);
    }

    @Test
    public void givenInvalidJointPartyValidAddress_thenDoNoDisplayError() {

        Address jointPartyAddress = Address.builder()
            .line1("some text")
            .line2("some text")
            .town("some text")
            .county("some text")
            .postcode("w1p 4df").build();

        sscsCaseData.getJointParty().setAddress(jointPartyAddress);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertTrue(response.getErrors().isEmpty());
    }

    @Test
    public void givenInvalidJointPartyAddressInvalidCharacterInLine1_thenDoDisplayError() {

        Address jointPartyAddress = Address.builder().line1("some $ text").build();

        sscsCaseData.getJointParty().setAddress(jointPartyAddress);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Line 1 must not contain special characters", error);
    }

    @Test
    public void givenInvalidJointPartyAddressInvalidCharacterInLine2_thenDoDisplayError() {

        Address jointPartyAddress = Address.builder().line2("some $ text").build();

        sscsCaseData.getJointParty().setAddress(jointPartyAddress);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Line 2 must not contain special characters", error);
    }

    @Test
    public void givenInvalidJointPartyAddressInvalidCharacterInTown_thenDoDisplayError() {

        Address jointPartyAddress = Address.builder().town("some $ text").build();

        sscsCaseData.getJointParty().setAddress(jointPartyAddress);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Town must not contain special characters", error);
    }

    @Test
    public void givenInvalidJointPartyAddressInvalidCharacterInCounty_thenDoDisplayError() {

        Address jointPartyAddress = Address.builder().county("some $ text").build();

        sscsCaseData.getJointParty().setAddress(jointPartyAddress);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("County must not contain special characters", error);
    }

    @Test
    public void givenInvalidJointPartyAddressInvalidPostcode_thenDoDisplayError() {

        Address jointPartyAddress = Address.builder().postcode("invalid postcode").build();

        sscsCaseData.getJointParty().setAddress(jointPartyAddress);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Please enter a valid postcode", error);
    }

    @Test
    public void givenUniversalCreditCaseWithFurtherInfoSetToYesAndNoAT38Document_thenShowError() {
        sscsCaseData.getAppeal().getBenefitType().setCode("UC");
        sscsCaseData.setDwpFurtherInfo("Yes");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("AT38 document is missing", error);
    }

    @Test
    public void givenUniversalCreditCaseWithFurtherInfoSetToYesAndNoAT38DocumentWithCollection_thenShowError() {
        sscsCaseData.getAppeal().getBenefitType().setCode("UC");
        sscsCaseData.setDwpFurtherInfo("Yes");
        sscsCaseData.setDwpDocuments(Collections.singletonList(DwpDocument.builder().value(
                DwpDocumentDetails.builder().documentType(DocumentType.DWP_RESPONSE.getValue()).build()
        ).build()));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("AT38 document is missing", error);
    }

    @Test
    public void givenUniversalCreditCaseWithFurtherInfoSetToYesAndNoAT38DocumentWithCollectionFieldSet_thenShowError() {
        sscsCaseData.getAppeal().getBenefitType().setCode("UC");
        sscsCaseData.setDwpFurtherInfo("Yes");
        sscsCaseData.setDwpDocuments(Collections.singletonList(DwpDocument.builder().value(
                DwpDocumentDetails.builder().documentType(DocumentType.DWP_RESPONSE.getValue()).build()
        ).build()));
        sscsCaseData.setDwpAT38Document(DwpResponseDocument.builder().build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertTrue(response.getErrors().isEmpty());
    }

    @Test
    public void givenUniversalCreditCaseWithFurtherInfoSetToYesAndNoAT38DocumentOld_thenShowError() {
        sscsCaseData.getAppeal().getBenefitType().setCode("UC");
        sscsCaseData.setDwpFurtherInfo("Yes");
        sscsCaseData.setDwpAT38Document(null);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("AT38 document is missing", error);
    }

    @Test
    public void givenUniversalCreditCaseWithFurtherInfoSetToNoAndAT38DocumentExists_thenDoNotShowError() {
        sscsCaseData.getAppeal().getBenefitType().setCode("UC");
        sscsCaseData.setDwpFurtherInfo("No");
        sscsCaseData.setDwpAT38Document(DwpResponseDocument.builder().build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertTrue(response.getErrors().isEmpty());
    }

    @Test
    public void givenNonUniversalCreditCaseWithNoAT38Document_thenShowError() {
        sscsCaseData.getAppeal().getBenefitType().setCode("PIP");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("AT38 document is missing", error);
    }

    @Test
    public void givenNonUniversalCreditCaseWithNoAT38DocumentInCollection_thenShowError() {
        sscsCaseData.getAppeal().getBenefitType().setCode("PIP");
        sscsCaseData.setDwpDocuments(Collections.singletonList(DwpDocument.builder().value(
                DwpDocumentDetails.builder().documentType(DocumentType.DWP_RESPONSE.getValue()).build()
        ).build()));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("AT38 document is missing", error);
    }

    @Test
    public void givenNonUniversalCreditCaseWithNoAT38DocumentOld_thenShowError() {
        sscsCaseData.getAppeal().getBenefitType().setCode("PIP");
        sscsCaseData.setDwpAT38Document(null);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("AT38 document is missing", error);
    }

    @Test
    public void givenNonUniversalCreditCaseWithAT38Document_thenDoNotShowError() {
        when(callback.getEvent()).thenReturn(EventType.AMEND_ELEMENTS_ISSUES);

        sscsCaseData.getAppeal().getBenefitType().setCode("PIP");
        sscsCaseData.setDwpDocuments(Collections.singletonList(DwpDocument.builder().value(
                DwpDocumentDetails.builder().documentType(DocumentType.AT38.getValue()).build()
        ).build()));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertTrue(response.getErrors().isEmpty());
    }

    @Test
    public void givenNonUniversalCreditCaseWithAT38DocumentOld_thenDoNotShowError() {
        sscsCaseData.getAppeal().getBenefitType().setCode("PIP");
        sscsCaseData.setDwpAT38Document(DwpResponseDocument.builder().build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertTrue(response.getErrors().isEmpty());
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheEvent() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
    }
}
