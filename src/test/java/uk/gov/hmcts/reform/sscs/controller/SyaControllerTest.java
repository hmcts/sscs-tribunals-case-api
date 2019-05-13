package uk.gov.hmcts.reform.sscs.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.reform.sscs.exception.PdfGenerationException;
import uk.gov.hmcts.reform.sscs.model.SaveCaseOperation;
import uk.gov.hmcts.reform.sscs.model.SaveCaseResult;
import uk.gov.hmcts.reform.sscs.model.draft.*;
import uk.gov.hmcts.reform.sscs.service.SubmitAppealService;

@RunWith(SpringRunner.class)
@WebMvcTest(SyaController.class)
public class SyaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SubmitAppealService submitAppealService;

    @Test
    public void shouldReturnHttpStatusCode201ForTheSubmittedAppeal() throws Exception {
        when(submitAppealService.submitAppeal(any(SyaCaseWrapper.class))).thenReturn(1L);

        String json = getSyaCaseWrapperJson();

        mockMvc.perform(post("/appeals")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json))
            .andExpect(status().isCreated());
    }

    @Test
    public void shouldReturnHttpStatusCode201ForTheSubmittedDraft() throws Exception {
        when(submitAppealService.submitDraftAppeal(any(), any()))
            .thenReturn(SaveCaseResult.builder()
                .caseDetailsId(1L)
                .saveCaseOperation(SaveCaseOperation.CREATE)
                .build());

        String json = getSyaCaseWrapperJson();

        mockMvc.perform(put("/drafts")
            .header("Authorization", "Bearer myToken")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json))
            .andExpect(status().isCreated());
    }

    @Test
    public void shouldHandleErrorWhileSubmitAppeal() throws Exception {
        doThrow(new PdfGenerationException("malformed html template", new Exception()))
            .when(submitAppealService).submitAppeal(any(SyaCaseWrapper.class));
        String json = getSyaCaseWrapperJson();

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
            .appellantName(new SessionAppellantName("Mrs.","Ap","Pellant"))
            .appellantDob(new SessionAppellantDob(new SessionDate("31", "12", "1998")))
            .appellantNino(new SessionAppellantNino("AB123456C"))
            .appellantContactDetails(
                new SessionAppellantContactDetails(
                    "line1",
                    "line2",
                    "town-city",
                    "county",
                    "AP1 4NT",
                    "07000000000",
                    "appointee@test.com"
                )
            )
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
            .andExpect(jsonPath("$.AppellantContactDetails.emailAddress").value("appointee@test.com"))
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
            .appellantName(new SessionAppellantName("Mrs.","Ap","Pellant"))
            .appellantDob(new SessionAppellantDob(new SessionDate("31", "12", "1998")))
            .appellantNino(new SessionAppellantNino("AB123456C"))
            .appellantContactDetails(
                new SessionAppellantContactDetails(
                    "line1",
                    "line2",
                    "town-city",
                    "county",
                    "AP1 4NT",
                    "07000000000",
                    "appointee@test.com"
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
                    "representative@test.com"
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
            .andExpect(jsonPath("$.AppellantContactDetails.emailAddress").value("appointee@test.com"))
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
            .build();

        when(submitAppealService.getDraftAppeal(any())).thenReturn(Optional.of(sessionDraft));

        mockMvc.perform(
            get("/drafts")
                .header("Authorization", "Bearer myToken")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.Appointee.isAppointee").value("yes"));
    }

    @Test
    public void getDraftWillReturn404WhenNoneExistForTheUser() throws Exception {
        when(submitAppealService.getDraftAppeal(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/drafts")
            .header("Authorization", "Bearer myToken")
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    private String getSyaCaseWrapperJson() throws IOException, URISyntaxException {
        URL resource = getClass().getClassLoader().getResource("json/sya.json");
        return String.join("\n", Files.readAllLines(Paths.get(resource.toURI())));
    }

}