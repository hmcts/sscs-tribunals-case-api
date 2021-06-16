package uk.gov.hmcts.reform.sscs.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.sscs.domain.wrapper.*;
import uk.gov.hmcts.reform.sscs.exception.PdfGenerationException;
import uk.gov.hmcts.reform.sscs.model.SaveCaseOperation;
import uk.gov.hmcts.reform.sscs.model.SaveCaseResult;
import uk.gov.hmcts.reform.sscs.model.draft.SessionAppellantNino;
import uk.gov.hmcts.reform.sscs.model.draft.SessionAppointee;
import uk.gov.hmcts.reform.sscs.model.draft.SessionBenefitType;
import uk.gov.hmcts.reform.sscs.model.draft.SessionCheckMrn;
import uk.gov.hmcts.reform.sscs.model.draft.SessionContactDetails;
import uk.gov.hmcts.reform.sscs.model.draft.SessionCreateAccount;
import uk.gov.hmcts.reform.sscs.model.draft.SessionDate;
import uk.gov.hmcts.reform.sscs.model.draft.SessionDob;
import uk.gov.hmcts.reform.sscs.model.draft.SessionDraft;
import uk.gov.hmcts.reform.sscs.model.draft.SessionDwpIssuingOffice;
import uk.gov.hmcts.reform.sscs.model.draft.SessionEvidenceProvide;
import uk.gov.hmcts.reform.sscs.model.draft.SessionHaveAMrn;
import uk.gov.hmcts.reform.sscs.model.draft.SessionMrnDate;
import uk.gov.hmcts.reform.sscs.model.draft.SessionMrnOverThirteenMonthsLate;
import uk.gov.hmcts.reform.sscs.model.draft.SessionName;
import uk.gov.hmcts.reform.sscs.model.draft.SessionOtherReasonForAppealing;
import uk.gov.hmcts.reform.sscs.model.draft.SessionPostcodeChecker;
import uk.gov.hmcts.reform.sscs.model.draft.SessionReasonForAppealing;
import uk.gov.hmcts.reform.sscs.model.draft.SessionReasonForAppealingItem;
import uk.gov.hmcts.reform.sscs.model.draft.SessionRepName;
import uk.gov.hmcts.reform.sscs.model.draft.SessionRepresentative;
import uk.gov.hmcts.reform.sscs.model.draft.SessionRepresentativeDetails;
import uk.gov.hmcts.reform.sscs.model.draft.SessionSameAddress;
import uk.gov.hmcts.reform.sscs.model.draft.SessionSendToNumber;
import uk.gov.hmcts.reform.sscs.model.draft.SessionTextReminders;
import uk.gov.hmcts.reform.sscs.model.draft.SessionTheHearing;
import uk.gov.hmcts.reform.sscs.service.SubmitAppealService;

@RunWith(JUnitParamsRunner.class)
public class SyaControllerTest {

    // being: it needed to run springRunner and junitParamsRunner
    @ClassRule
    public static final SpringClassRule SCR = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();
    // end


    private MockMvc mockMvc;

    @MockBean
    private SubmitAppealService submitAppealService;

    private SyaController controller;

    @Before
    public void setUp() {
        controller = new SyaController(submitAppealService);
        mockMvc = standaloneSetup(controller).build();
    }

    @Test
    public void shouldReturnHttpStatusCode201ForTheSubmittedAppeal() throws Exception {
        when(submitAppealService.submitAppeal(any(SyaCaseWrapper.class), any(String.class))).thenReturn(1L);

        String json = getSyaCaseWrapperJson("json/sya.json");

        mockMvc.perform(post("/appeals")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json))
            .andExpect(status().isCreated());
    }

    @Test
    public void shouldReturnHttpStatusCode201ForTheSubmittedDraft() throws Exception {
        when(submitAppealService.submitDraftAppeal(any(), any(), any()))
            .thenReturn(Optional.of(SaveCaseResult.builder()
                .caseDetailsId(1L)
                .saveCaseOperation(SaveCaseOperation.CREATE)
                .build()));

        String json = getSyaCaseWrapperJson("json/sya.json");

        mockMvc.perform(put("/drafts")
            .header("Authorization", "Bearer myToken")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json))
            .andExpect(status().isCreated());
    }

    @Test
    public void shouldReturnHttpStatusCode201ForTheSubmittedDraftWhenForceCreateTrue() throws Exception {
        when(submitAppealService.submitDraftAppeal(any(), any(), any()))
                .thenReturn(Optional.of(SaveCaseResult.builder()
                        .caseDetailsId(1L)
                        .saveCaseOperation(SaveCaseOperation.CREATE)
                        .build()));

        String json = getSyaCaseWrapperJson("json/sya.json");

        mockMvc.perform(put("/drafts?forceCreate=true")
                .header("Authorization", "Bearer myToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated());
    }

    @Test
    public void shouldReturnHttpStatusCode201ForTheSubmittedDraftWhenForceCreateNotTrue() throws Exception {
        when(submitAppealService.submitDraftAppeal(any(), any(), any()))
                .thenReturn(Optional.of(SaveCaseResult.builder()
                        .caseDetailsId(1L)
                        .saveCaseOperation(SaveCaseOperation.CREATE)
                        .build()));

        String json = getSyaCaseWrapperJson("json/sya.json");

        mockMvc.perform(put("/drafts?forceCreate=notTrue")
                .header("Authorization", "Bearer myToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated());
    }

    @Test
    public void shouldReturnHttpStatusCode200ForTheUpdatedDraft() throws Exception {
        when(submitAppealService.updateDraftAppeal(any(), any()))
                .thenReturn(Optional.of(SaveCaseResult.builder()
                        .caseDetailsId(1L)
                        .saveCaseOperation(SaveCaseOperation.UPDATE)
                        .build()));

        String json = getSyaCaseWrapperJson("json/sya_with_ccdId.json");

        mockMvc.perform(post("/drafts")
                .header("Authorization", "Bearer myToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk());
    }

    @Test
    public void shouldReturnHttpStatusCode400ForInvalidAuthDraftUpdate() throws Exception {
        when(submitAppealService.updateDraftAppeal(any(), any()))
                .thenReturn(Optional.of(SaveCaseResult.builder()
                        .caseDetailsId(1L)
                        .saveCaseOperation(SaveCaseOperation.UPDATE)
                        .build()));

        String json = getSyaCaseWrapperJson("json/sya_with_ccdId.json");

        mockMvc.perform(post("/drafts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturnHttpStatusCode204ForInvalidCcdIdDraftUpdate() throws Exception {
        when(submitAppealService.updateDraftAppeal(any(), any()))
                .thenReturn(Optional.of(SaveCaseResult.builder()
                        .caseDetailsId(1L)
                        .saveCaseOperation(SaveCaseOperation.UPDATE)
                        .build()));

        String json = getSyaCaseWrapperJson("json/sya.json");

        mockMvc.perform(post("/drafts")
                .header("Authorization", "Bearer myToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isNoContent());
    }

    @Test
    public void shouldReturnHttpStatusCode200ForArchivedDraft() throws Exception {
        when(submitAppealService.archiveDraftAppeal(any(), any(), any()))
                .thenReturn(Optional.of(SaveCaseResult.builder()
                        .caseDetailsId(1L)
                        .saveCaseOperation(SaveCaseOperation.ARCHIVE)
                        .build()));

        String json = getSyaCaseWrapperJson("json/sya_with_ccdId.json");

        mockMvc.perform(delete("/drafts/1234567890")
                .header("Authorization", "Bearer myToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk());
    }

    @Test
    public void shouldReturnHttpStatusCode400ForInvalidAuthArchivedDraft() throws Exception {
        when(submitAppealService.archiveDraftAppeal(any(), any(), any()))
                .thenReturn(Optional.of(SaveCaseResult.builder()
                        .caseDetailsId(1L)
                        .saveCaseOperation(SaveCaseOperation.ARCHIVE)
                        .build()));

        String json = getSyaCaseWrapperJson("json/sya_with_ccdId.json");

        mockMvc.perform(delete("/drafts/1234567890")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturnHttpStatusCode204ForInvalidDataArchivedDraft() throws Exception {
        when(submitAppealService.archiveDraftAppeal(any(), any(), any()))
                .thenReturn(Optional.of(SaveCaseResult.builder()
                        .caseDetailsId(1L)
                        .saveCaseOperation(SaveCaseOperation.ARCHIVE)
                        .build()));

        String json = getSyaCaseWrapperJson("json/sya.json");

        mockMvc.perform(delete("/drafts/1234567890")
                .header("Authorization", "Bearer myToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isNoContent());
    }

    @Test
    @Parameters(method = "generateInvalidScenariosPut")
    public void givenParameterIsNotValid_whenPutDraftIsCalled_shouldReturn204Response(String payload, String token)
        throws Exception {
        mockMvc.perform(put("/drafts")
            .header("Authorization", token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
            .andExpect(status().isNoContent());
    }

    @Test
    @Parameters(method = "generateInvalidScenariosPost")
    public void givenParameterIsNotValid_whenPostDraftIsCalled_shouldReturn204Response(String payload, String token)
            throws Exception {
        mockMvc.perform(post("/drafts")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isNoContent());
    }

    @Test
    @Parameters(method = "generateInvalidScenariosDelete")
    public void givenParameterIsNotValid_whenArchiveDraftIsCalled_shouldReturn204Response(String payload, String token)
            throws Exception {
        mockMvc.perform(delete("/drafts/555")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isNoContent());
    }

    private Object[] generateInvalidScenariosPut() throws Exception {

        String validPayload = getSyaCaseWrapperJson("json/sya.json");

        String emptyPayload = "{}";
        String noBenefitCode = "{\n"
                + "  \"benefitType\": {\n"
                + "    \"description\": \"Personal Independence Payment\"\n"
                + "  }\n"
                + "}";
        String emptyBenefitCode = "{\n"
                + "  \"benefitType\": {\n"
                + "    \"code\": \"\"\n"
                + "  }\n"
                + "}";

        String validUserToken = "Bearer myToken";
        String invalidUserToken = "";

        return new Object[]{
            new Object[]{emptyPayload, validUserToken},
            new Object[]{noBenefitCode, validUserToken},
            new Object[]{emptyBenefitCode, validUserToken},
            new Object[]{validPayload, invalidUserToken}
        };
    }

    private Object[] generateInvalidScenariosPost() throws Exception {

        String validPayload = getSyaCaseWrapperJson("json/sya_with_ccdId.json");

        String emptyPayload = "{}";
        String noBenefitCode = "{\n"
                + "  \"benefitType\": {\n"
                + "    \"description\": \"Personal Independence Payment\"\n"
                + "  },\n"
                + "  \"ccdId\": 1234 \n"
                + "}";
        String emptyBenefitCode = "{\n"
                + "  \"benefitType\": {\n"
                + "    \"code\": \"\"\n"
                + "  },\n"
                + "  \"ccdId\": 1234 \n"
                + "}";

        String emptyCcdId = "{\n"
                + "  \"benefitType\": {\n"
                + "    \"code\": \"ESA\"\n"
                + "  }\n"
                + "}";

        String validUserToken = "Bearer myToken";
        String invalidUserToken = "";

        return new Object[]{
            new Object[]{emptyPayload, validUserToken},
            new Object[]{noBenefitCode, validUserToken},
            new Object[]{emptyBenefitCode, validUserToken},
            new Object[]{validPayload, invalidUserToken},
            new Object[]{emptyCcdId, validUserToken}
        };
    }

    private Object[] generateInvalidScenariosDelete() throws Exception {

        String validPayload = getSyaCaseWrapperJson("json/sya_with_ccdId.json");

        String emptyPayload = "{}";
        String emptyCcdId = "{\n"
                + "  \"benefitType\": {\n"
                + "    \"code\": \"ESA\"\n"
                + "  }\n"
                + "}";

        String validUserToken = "Bearer myToken";
        String invalidUserToken = "";

        return new Object[]{
            new Object[]{emptyPayload, validUserToken},
            new Object[]{validPayload, invalidUserToken},
            new Object[]{emptyCcdId, validUserToken}
        };
    }

    @Test
    public void shouldHandleErrorWhileSubmitAppeal() throws Exception {
        doThrow(new PdfGenerationException("malformed html template", new Exception()))
            .when(submitAppealService).submitAppeal(any(SyaCaseWrapper.class), any());
        String json = getSyaCaseWrapperJson("json/sya.json");

        mockMvc.perform(post("/appeals")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json)
            .accept(APPLICATION_PDF_VALUE))
            .andExpect(status().is5xxServerError());
    }

    @Test
    public void givenGetDraftIsCalled_shouldReturn200AndTheDraft() throws Exception {
        SessionDraft sessionDraft = SessionDraft.builder()
            .benefitType(new SessionBenefitType("Personal Independence Payment (PIP)"))
            .postcode(new SessionPostcodeChecker("AP1 4NT"))
            .createAccount(new SessionCreateAccount("yes"))
            .haveAMrn(new SessionHaveAMrn("yes"))
            .mrnDate(new SessionMrnDate(new SessionDate("01", "02", "2017")))
            .checkMrn(new SessionCheckMrn("yes"))
            .mrnOverThirteenMonthsLate(new SessionMrnOverThirteenMonthsLate("Just forgot to do it"))
            .dwpIssuingOffice(new SessionDwpIssuingOffice("1"))
            .appointee(new SessionAppointee("no"))
            .appellantName(new SessionName("Mrs.", "Ap", "Pellant"))
            .appellantDob(new SessionDob(new SessionDate("31", "12", "1998")))
            .appellantNino(new SessionAppellantNino("AB123456C"))
            .appellantContactDetails(
                new SessionContactDetails(
                    "line1",
                    "line2",
                    "town-city",
                    "county",
                    "AP1 4NT",
                    "07000000000",
                    "appellant@test.com",
                    null,
                    null,
                    null
                )
            )
            .sameAddress(new SessionSameAddress("no"))
            .textReminders(new SessionTextReminders("yes"))
            .sendToNumber(new SessionSendToNumber("yes"))
            .representative(new SessionRepresentative("no"))
            .reasonForAppealing(
                new SessionReasonForAppealing(
                    Collections.singletonList(
                        new SessionReasonForAppealingItem(
                            "Underpayment",
                            "I think I should get more")
                    )
                )
            )
            .otherReasonForAppealing(new SessionOtherReasonForAppealing("I can't think of anything else"))
            .evidenceProvide(new SessionEvidenceProvide("no"))
            .theHearing(new SessionTheHearing("yes"))
            .build();

        when(submitAppealService.getDraftAppeal(any())).thenReturn(Optional.of(sessionDraft));

        mockMvc.perform(
            get("/drafts")
                .header("Authorization", "Bearer myToken")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.BenefitType.benefitType").value("Personal Independence Payment (PIP)"))
            .andExpect(jsonPath("$.PostcodeChecker.postcode").value("AP1 4NT"))
            .andExpect(jsonPath("$.CreateAccount.createAccount").value("yes"))
            .andExpect(jsonPath("$.HaveAMRN.haveAMRN").value("yes"))
            .andExpect(jsonPath("$.MRNDate.mrnDate.day").value("01"))
            .andExpect(jsonPath("$.MRNDate.mrnDate.month").value("02"))
            .andExpect(jsonPath("$.MRNDate.mrnDate.year").value("2017"))
            .andExpect(jsonPath("$.MRNOverThirteenMonthsLate.reasonForBeingLate").value("Just forgot to do it"))
            .andExpect(jsonPath("$.DWPIssuingOffice.pipNumber").value("1"))
            .andExpect(jsonPath("$.Appointee.isAppointee").value("no"))
            .andExpect(jsonPath("$.AppellantName.title").value("Mrs."))
            .andExpect(jsonPath("$.AppellantName.firstName").value("Ap"))
            .andExpect(jsonPath("$.AppellantName.lastName").value("Pellant"))
            .andExpect(jsonPath("$.AppellantDOB.date.day").value("31"))
            .andExpect(jsonPath("$.AppellantDOB.date.month").value("12"))
            .andExpect(jsonPath("$.AppellantDOB.date.year").value("1998"))
            .andExpect(jsonPath("$.AppellantNINO.nino").value("AB123456C"))
            .andExpect(jsonPath("$.AppellantContactDetails.addressLine1").value("line1"))
            .andExpect(jsonPath("$.AppellantContactDetails.addressLine2").value("line2"))
            .andExpect(jsonPath("$.AppellantContactDetails.townCity").value("town-city"))
            .andExpect(jsonPath("$.AppellantContactDetails.county").value("county"))
            .andExpect(jsonPath("$.AppellantContactDetails.postCode").value("AP1 4NT"))
            .andExpect(jsonPath("$.AppellantContactDetails.phoneNumber").value("07000000000"))
            .andExpect(jsonPath("$.AppellantContactDetails.emailAddress").value("appellant@test.com"))
            .andExpect(jsonPath("$.SameAddress.isAddressSameAsAppointee").value("no"))
            .andExpect(jsonPath("$.TextReminders.doYouWantTextMsgReminders").value("yes"))
            .andExpect(jsonPath("$.SendToNumber.useSameNumber").value("yes"))
            .andExpect(jsonPath("$.Representative.hasRepresentative").value("no"))
            .andExpect(jsonPath("$.ReasonForAppealing.items", hasSize(1)))
            .andExpect(jsonPath("$.ReasonForAppealing.items[0].whatYouDisagreeWith").value("Underpayment"))
            .andExpect(jsonPath("$.ReasonForAppealing.items[0].reasonForAppealing").value("I think I should get more"))
            .andExpect(jsonPath("$.OtherReasonForAppealing.otherReasonForAppealing").value("I can't think of anything else"))
            .andExpect(jsonPath("$.EvidenceProvide.evidenceProvide").value("no"))
            .andExpect(jsonPath("$.TheHearing.attendHearing").value("yes"))
        ;
    }

    @Test
    public void givenGetDraftWithRepIsCalled_shouldReturn200AndTheDraft() throws Exception {
        SessionDraft sessionDraft = SessionDraft.builder()
            .benefitType(new SessionBenefitType("Personal Independence Payment (PIP)"))
            .postcode(new SessionPostcodeChecker("AP1 4NT"))
            .createAccount(new SessionCreateAccount("yes"))
            .haveAMrn(new SessionHaveAMrn("yes"))
            .mrnDate(new SessionMrnDate(new SessionDate("01", "02", "2017")))
            .checkMrn(new SessionCheckMrn("yes"))
            .mrnOverThirteenMonthsLate(new SessionMrnOverThirteenMonthsLate("Just forgot to do it"))
            .dwpIssuingOffice(new SessionDwpIssuingOffice("1"))
            .appointee(new SessionAppointee("no"))
            .appellantName(new SessionName("Mrs.", "Ap", "Pellant"))
            .appellantDob(new SessionDob(new SessionDate("31", "12", "1998")))
            .appellantNino(new SessionAppellantNino("AB123456C"))
            .appellantContactDetails(
                new SessionContactDetails(
                    "line1",
                    "line2",
                    "town-city",
                    "county",
                    "AP1 4NT",
                    "07000000000",
                    "appellant@test.com",
                    null,
                    null,
                    null
                )
            )
            .textReminders(new SessionTextReminders("yes"))
            .sendToNumber(new SessionSendToNumber("yes"))
            .representative(new SessionRepresentative("yes"))
            .representativeDetails(
                new SessionRepresentativeDetails(
                    new SessionRepName(
                        "Mr.",
                        "Re",
                        "Presentative"
                    ),
                    "rep-line1",
                    "rep-line2",
                    "rep-town-city",
                    "rep-county",
                    "RE7 7ES",
                    "07222222222",
                    "representative@test.com",
                    null,
                    null,
                    null
                )
            )
            .build();

        when(submitAppealService.getDraftAppeal(any())).thenReturn(Optional.of(sessionDraft));

        mockMvc.perform(
            get("/drafts")
                .header("Authorization", "Bearer myToken")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.BenefitType.benefitType").value("Personal Independence Payment (PIP)"))
            .andExpect(jsonPath("$.PostcodeChecker.postcode").value("AP1 4NT"))
            .andExpect(jsonPath("$.CreateAccount.createAccount").value("yes"))
            .andExpect(jsonPath("$.HaveAMRN.haveAMRN").value("yes"))
            .andExpect(jsonPath("$.MRNDate.mrnDate.day").value("01"))
            .andExpect(jsonPath("$.MRNDate.mrnDate.month").value("02"))
            .andExpect(jsonPath("$.MRNDate.mrnDate.year").value("2017"))
            .andExpect(jsonPath("$.MRNOverThirteenMonthsLate.reasonForBeingLate").value("Just forgot to do it"))
            .andExpect(jsonPath("$.DWPIssuingOffice.pipNumber").value("1"))
            .andExpect(jsonPath("$.Appointee.isAppointee").value("no"))
            .andExpect(jsonPath("$.AppellantName.title").value("Mrs."))
            .andExpect(jsonPath("$.AppellantName.firstName").value("Ap"))
            .andExpect(jsonPath("$.AppellantName.lastName").value("Pellant"))
            .andExpect(jsonPath("$.AppellantDOB.date.day").value("31"))
            .andExpect(jsonPath("$.AppellantDOB.date.month").value("12"))
            .andExpect(jsonPath("$.AppellantDOB.date.year").value("1998"))
            .andExpect(jsonPath("$.AppellantNINO.nino").value("AB123456C"))
            .andExpect(jsonPath("$.AppellantContactDetails.addressLine1").value("line1"))
            .andExpect(jsonPath("$.AppellantContactDetails.addressLine2").value("line2"))
            .andExpect(jsonPath("$.AppellantContactDetails.townCity").value("town-city"))
            .andExpect(jsonPath("$.AppellantContactDetails.county").value("county"))
            .andExpect(jsonPath("$.AppellantContactDetails.postCode").value("AP1 4NT"))
            .andExpect(jsonPath("$.AppellantContactDetails.phoneNumber").value("07000000000"))
            .andExpect(jsonPath("$.AppellantContactDetails.emailAddress").value("appellant@test.com"))
            .andExpect(jsonPath("$.TextReminders.doYouWantTextMsgReminders").value("yes"))
            .andExpect(jsonPath("$.SendToNumber.useSameNumber").value("yes"))
            .andExpect(jsonPath("$.Representative.hasRepresentative").value("yes"))
            .andExpect(jsonPath("$.RepresentativeDetails.name.title").value("Mr."))
            .andExpect(jsonPath("$.RepresentativeDetails.name.first").value("Re"))
            .andExpect(jsonPath("$.RepresentativeDetails.name.last").value("Presentative"))
            .andExpect(jsonPath("$.RepresentativeDetails.addressLine1").value("rep-line1"))
            .andExpect(jsonPath("$.RepresentativeDetails.addressLine2").value("rep-line2"))
            .andExpect(jsonPath("$.RepresentativeDetails.townCity").value("rep-town-city"))
            .andExpect(jsonPath("$.RepresentativeDetails.county").value("rep-county"))
            .andExpect(jsonPath("$.RepresentativeDetails.postCode").value("RE7 7ES"))
            .andExpect(jsonPath("$.RepresentativeDetails.phoneNumber").value("07222222222"))
            .andExpect(jsonPath("$.RepresentativeDetails.emailAddress").value("representative@test.com"))
        ;
    }

    @Test
    public void givenGetDraftWithAppointeeIsCalled_shouldReturn200AndTheDraft() throws Exception {
        SessionDraft sessionDraft = SessionDraft.builder()
            .benefitType(new SessionBenefitType("Personal Independence Payment (PIP)"))
            .postcode(new SessionPostcodeChecker("AP1 4NT"))
            .createAccount(new SessionCreateAccount("yes"))
            .haveAMrn(new SessionHaveAMrn("yes"))
            .mrnDate(new SessionMrnDate(new SessionDate("01", "02", "2017")))
            .checkMrn(new SessionCheckMrn("yes"))
            .mrnOverThirteenMonthsLate(new SessionMrnOverThirteenMonthsLate("Just forgot to do it"))
            .dwpIssuingOffice(new SessionDwpIssuingOffice("1"))
            .appointee(new SessionAppointee("yes"))
            .appointeeName(new SessionName("Mr.", "Ap", "Pointee"))
            .appointeeDob(new SessionDob(new SessionDate("1", "1", "1999")))
            .appointeeContactDetails(
                new SessionContactDetails(
                    "tee-line1",
                    "tee-line2",
                    "tee-town-city",
                    "tee-county",
                    "AP1 33T",
                    "07111111111",
                    "appointee@test.com",
                    null,
                    null,
                    null
                )
            )
            .sameAddress(new SessionSameAddress("no"))
            .build();

        when(submitAppealService.getDraftAppeal(any())).thenReturn(Optional.of(sessionDraft));

        mockMvc.perform(
            get("/drafts")
                .header("Authorization", "Bearer myToken")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.Appointee.isAppointee").value("yes"))
            .andExpect(jsonPath("$.AppointeeName.title").value("Mr."))
            .andExpect(jsonPath("$.AppointeeName.firstName").value("Ap"))
            .andExpect(jsonPath("$.AppointeeName.lastName").value("Pointee"))
            .andExpect(jsonPath("$.AppointeeDOB.date.day").value("1"))
            .andExpect(jsonPath("$.AppointeeDOB.date.month").value("1"))
            .andExpect(jsonPath("$.AppointeeDOB.date.year").value("1999"))
            .andExpect(jsonPath("$.AppointeeContactDetails.addressLine1").value("tee-line1"))
            .andExpect(jsonPath("$.AppointeeContactDetails.addressLine2").value("tee-line2"))
            .andExpect(jsonPath("$.AppointeeContactDetails.townCity").value("tee-town-city"))
            .andExpect(jsonPath("$.AppointeeContactDetails.county").value("tee-county"))
            .andExpect(jsonPath("$.AppointeeContactDetails.postCode").value("AP1 33T"))
            .andExpect(jsonPath("$.AppointeeContactDetails.phoneNumber").value("07111111111"))
            .andExpect(jsonPath("$.AppointeeContactDetails.emailAddress").value("appointee@test.com"))
            .andExpect(jsonPath("$.SameAddress.isAddressSameAsAppointee").value("no"))
        ;
    }

    @Test
    public void getDraftWillReturn204WhenNoneExistForTheUser() throws Exception {
        when(submitAppealService.getDraftAppeal(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/drafts")
            .header("Authorization", "Bearer myToken")
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());
    }

    @Test
    public void givenGetDraftsIsCalled_shouldReturn200AndTheDraft() throws Exception {

        SessionDraft sessionDraft = SessionDraft.builder()
                .benefitType(new SessionBenefitType("Personal Independence Payment (PIP)"))
                .postcode(new SessionPostcodeChecker("AP1 4NT"))
                .createAccount(new SessionCreateAccount("yes"))
                .build();

        SessionDraft sessionDraft2 = SessionDraft.builder()
                .benefitType(new SessionBenefitType("Personal Independence Payment (PIP)"))
                .postcode(new SessionPostcodeChecker("DD1 8FH"))
                .createAccount(new SessionCreateAccount("no"))
                .build();

        SessionDraft sessionDraft3 = SessionDraft.builder()
                .benefitType(new SessionBenefitType("Personal Independence Payment (PIP)"))
                .postcode(new SessionPostcodeChecker("LF4 9SH"))
                .createAccount(new SessionCreateAccount("yes"))
                .build();

        when(submitAppealService.getDraftAppeals(any())).thenReturn(List.of(sessionDraft, sessionDraft2, sessionDraft3));

        mockMvc.perform(
                get("/drafts/all")
                        .header("Authorization", "Bearer myToken")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].BenefitType.benefitType").value("Personal Independence Payment (PIP)"))
                .andExpect(jsonPath("$[0].PostcodeChecker.postcode").value("AP1 4NT"))
                .andExpect(jsonPath("$[0].CreateAccount.createAccount").value("yes"))
                .andExpect(jsonPath("$[1].BenefitType.benefitType").value("Personal Independence Payment (PIP)"))
                .andExpect(jsonPath("$[1].PostcodeChecker.postcode").value("DD1 8FH"))
                .andExpect(jsonPath("$[1].CreateAccount.createAccount").value("no"))
                .andExpect(jsonPath("$[2].BenefitType.benefitType").value("Personal Independence Payment (PIP)"))
                .andExpect(jsonPath("$[2].PostcodeChecker.postcode").value("LF4 9SH"))
                .andExpect(jsonPath("$[2].CreateAccount.createAccount").value("yes"))
        ;
    }

    @Test
    public void getDraftsWillReturn204WhenNoneExistForTheUser() throws Exception {
        when(submitAppealService.getDraftAppeals(any())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/drafts/all")
                .header("Authorization", "Bearer myToken")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test(expected = NullPointerException.class)
    public void testLoggingMethodNullBenefitType() throws Exception {
        SyaAppellant appellant = new SyaAppellant();
        appellant.setTitle("Mr");
        appellant.setLastName("Lastname");

        SyaContactDetails contactDetails = new SyaContactDetails();
        contactDetails.setEmailAddress("appellant@test.com");
        appellant.setContactDetails(contactDetails);
        appellant.setNino("1234");

        SyaCaseWrapper caseWithNullBenefitCode = new SyaCaseWrapper();
        caseWithNullBenefitCode.setAppellant(appellant);
        caseWithNullBenefitCode.setCcdCaseId("123456");

        SyaReasonsForAppealing reasons = new SyaReasonsForAppealing();
        Reason reason = new Reason();
        reason.setReasonForAppealing("my reason");
        reason.setWhatYouDisagreeWith("i disagree");
        reasons.setReasons(Collections.singletonList(reason));
        caseWithNullBenefitCode.setReasonsForAppealing(reasons);

        controller.createAppeals(null, caseWithNullBenefitCode);
    }

    @Test(expected = NullPointerException.class)
    public void testLoggingMethodNullNino() throws Exception {
        SyaCaseWrapper caseWithNullNino = new SyaCaseWrapper();
        caseWithNullNino.setBenefitType(new SyaBenefitType("Universal Credit", "UC"));
        caseWithNullNino.setCcdCaseId("123456");

        controller.createAppeals(null, caseWithNullNino);

    }


    private String getSyaCaseWrapperJson(String resourcePath) throws IOException, URISyntaxException {
        URL resource = getClass().getClassLoader().getResource(resourcePath);
        return String.join("\n", Files.readAllLines(Paths.get(Objects.requireNonNull(resource).toURI())));
    }

}
