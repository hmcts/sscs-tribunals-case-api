package uk.gov.hmcts.reform.sscs.controller;

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
import uk.gov.hmcts.reform.sscs.model.draft.SessionBenefitType;
import uk.gov.hmcts.reform.sscs.model.draft.SessionDraft;
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
        SessionDraft sessionDraft = new SessionDraft(new SessionBenefitType("PIP"));
        when(submitAppealService.getDraftAppeal(any())).thenReturn(Optional.of(sessionDraft));

        mockMvc.perform(
            get("/drafts")
                .header("Authorization", "Bearer myToken")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.BenefitType.benefitType").value("PIP"));
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