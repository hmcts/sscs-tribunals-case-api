package uk.gov.hmcts.reform.sscs.callback;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.assertHttpStatus;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.getRequestWithAuthHeader;
import static uk.gov.hmcts.reform.sscs.idam.UserRole.SUPER_USER;

import java.io.IOException;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.client.RefDataApi;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;
import uk.gov.hmcts.reform.sscs.model.CourtVenue;

@SpringBootTest
@AutoConfigureMockMvc
public class CaseUpdatedIt extends AbstractEventIt {

    @MockBean
    private IdamService idamService;

    @MockBean
    private CcdService ccdService;

    @MockBean
    private RefDataApi refDataApi;

    @Before
    public void setup() throws IOException {
        setup("callback/caseUpdated.json");

        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().idamOauth2Token("Bearer Token:")
            .serviceAuthorization("sscs").build());
        when(idamService.getUserDetails(anyString())).thenReturn(UserDetails.builder()
            .roles(List.of(SUPER_USER.getValue())).build());
        when(refDataApi.courtVenueByEpimsId(eq("Bearer Token:"), eq("sscs"),
            anyString())).thenReturn(List.of(CourtVenue.builder().courtTypeId("31")
            .regionId("2").venueName("Basildon CC").courtStatus("Open").build()));
    }

    @Test
    public void callToAboutToSubmit_willUpdateProcessingVenueWhenChanged() throws Exception {
        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());
        assertEquals(result.getErrors().size(), 0);
        assertEquals("Basildon CC", result.getData().getProcessingVenue());
        assertEquals("698118", result.getData().getCaseManagementLocation().getBaseLocation());
        assertEquals("2", result.getData().getCaseManagementLocation().getRegion());
    }

}


