package uk.gov.hmcts.reform.sscs.callback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
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
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;


@SpringBootTest
@AutoConfigureMockMvc
public class UpdateOtherPartyDataIt extends AbstractEventIt {

    public static final String OTHER_PARTY_ID = "34c1ef28-b6c7-4b45-bb40-4bfe1e7133c5";
    public static final int UUID_SIZE = 36;
    @MockBean
    private IdamService idamService;

    @Before
    public void setup() throws IOException {
        setup("callback/updateOtherPartyCallback.json");
        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
        when(idamService.getUserDetails(anyString())).thenReturn(
            UserDetails.builder().roles(List.of(SUPER_USER.getValue())).build());
    }

    @Test
    public void callToAboutToSubmit_willUpdateProcessingVenueWhenChanged() throws Exception {
        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        List<CcdValue<OtherParty>> otherParties = result.getData().getOtherParties();

        assertThat(otherParties)
            .hasSize(2)
            .extracting(CcdValue::getValue)
            .anySatisfy((OtherParty otherParty) -> {
                assertThat(otherParty.getId()).isEqualTo(OTHER_PARTY_ID);
                assertThat(otherParty.getSendNewOtherPartyNotification()).isEqualTo(NO);
            })
            .anySatisfy((OtherParty otherParty) -> {
                assertThat(otherParty.getId())
                    .isNotEqualTo(OTHER_PARTY_ID)
                    .hasSize(UUID_SIZE);
                assertThat(otherParty.getSendNewOtherPartyNotification()).isEqualTo(YES);
            })
            .allSatisfy((OtherParty otherParty) -> {
                assertThat(otherParty.getAppointee().getId()).hasSize(UUID_SIZE);
                assertThat(otherParty.getRep().getId()).hasSize(UUID_SIZE);
            });
    }

}


