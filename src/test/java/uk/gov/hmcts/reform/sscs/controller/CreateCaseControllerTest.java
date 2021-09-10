package uk.gov.hmcts.reform.sscs.controller;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URISyntaxException;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

public class CreateCaseControllerTest {

    private CcdService ccdService;
    private IdamService idamService;

    @Before
    public void setUp() {
        ccdService = mock(CcdService.class);

        idamService = mock(IdamService.class);
        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
    }

    @Test
    public void createCase() throws URISyntaxException {
        Long caseId = 123L;
        String caseRef = "someCaseRef";
        String someTyaValue = "someTyaValue";
        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().id(caseId).data(SscsCaseData.builder()
                .caseReference(caseRef)
                .subscriptions(Subscriptions.builder()
                        .appellantSubscription(Subscription.builder()
                                .tya(someTyaValue)
                                .build())
                        .build())
                .build()
        ).build();
        when(ccdService.createCase(any(SscsCaseData.class), eq(EventType.CREATE_TEST_CASE.getCcdType()), eq("SSCS - create test case event"), eq("Created SSCS"), any(IdamTokens.class))).thenReturn(sscsCaseDetails);
        CreateCaseController createCaseController = new CreateCaseController(ccdService, idamService);

        ResponseEntity<Map<String, String>> createCaseResponse = createCaseController.createCase("someEmail", "someMobile", "oral");

        assertThat(createCaseResponse.getStatusCode(), is(HttpStatus.CREATED));
        assertThat(createCaseResponse.getBody().get("id"), is(caseId.toString()));
        assertThat(createCaseResponse.getBody().get("case_reference"), is(caseRef));
        assertThat(createCaseResponse.getBody().get("appellant_tya"), is(someTyaValue));
    }

    @Test
    public void twoCallsToRandomNinoAreDifferent() {
        CreateCaseController createCaseController = new CreateCaseController(ccdService, idamService);
        String nino1 = createCaseController.getRandomNino();
        String nino2 = createCaseController.getRandomNino();
        assertThat(nino1, not(nino2));
    }

    @Test
    public void twoCallsToRandomMrnDateHaveAGoodChanceOfBeingDifferent() {
        CreateCaseController createCaseController = new CreateCaseController(ccdService, idamService);
        String mrnDate1 = createCaseController.getRandomMrnDate();
        String mrnDate2 = createCaseController.getRandomMrnDate();
        assertThat(mrnDate1, not(mrnDate2));
    }
}
