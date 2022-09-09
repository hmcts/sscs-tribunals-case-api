package uk.gov.hmcts.reform.sscs.callback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.assertHttpStatus;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.getRequestWithAuthHeader;
import static uk.gov.hmcts.reform.sscs.idam.UserRole.CTSC_CLERK;

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
import uk.gov.hmcts.reform.sscs.ccd.domain.Entity;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;


@SpringBootTest
@AutoConfigureMockMvc
public class UpdateSscs5OtherPartyDataIt extends AbstractEventIt {

    public static final String OTHER_PARTY_ID_1 = "34c1ef28-b6c7-4b45-bb40-4bfe1e7133c5";
    public static final String OTHER_PARTY_ID_2 = "bc81340e-a116-4b74-a091-9cf1564df9ca";
    @MockBean
    private IdamService idamService;

    @Before
    public void setup() throws IOException {
        setup("callback/updateOtherPartySscs5Callback.json");
        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
        when(idamService.getUserDetails(anyString())).thenReturn(
            UserDetails.builder().roles(List.of(CTSC_CLERK.getValue())).build());
    }

    @Test
    public void callToAboutToSubmit_willUpdateSscs5CaseWhenRoleIsNotEntered() throws Exception {

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        List<CcdValue<OtherParty>> otherParties = result.getData().getOtherParties();

        assertThat(otherParties)
            .hasSize(2)
            .extracting(CcdValue::getValue)
            .extracting(OtherParty::getSendNewOtherPartyNotification, OtherParty::getRole, OtherParty::getShowRole)
            .contains(
                tuple(YES, null, NO),
                tuple(NO, null, NO));

        assertThat(otherParties)
            .extracting(CcdValue::getValue)
            .extracting(Entity::getId)
            .hasSize(2)
            .contains(OTHER_PARTY_ID_1, OTHER_PARTY_ID_2);

        assertThat(otherParties)
            .extracting(CcdValue::getValue)
            .flatExtracting(otherParty -> List.of(otherParty.getAppointee(), otherParty.getRep()))
            .extracting(Entity::getId)
            .hasSize(4)
            .doesNotContainNull()
            .extracting(String::length)
            .containsOnly(36);
    }

}


