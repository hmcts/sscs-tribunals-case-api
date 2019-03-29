package uk.gov.hmcts.reform.sscs.config;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.MockitoAnnotations.initMocks;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.SscsCcdConvertService;

public class CitizenCcdServiceTest {

    private CitizenCcdService citizenCcdService;

    @Mock
    private CitizenCcdClient citizenCcdClient;

    @Mock
    private SscsCcdConvertService sscsCcdConvertService;

    @Before
    public void setup() {
        initMocks(this);
        citizenCcdService = new CitizenCcdService(citizenCcdClient, sscsCcdConvertService);
        given(sscsCcdConvertService.getCaseDetails(any())).willReturn(SscsCaseDetails.builder().id(123L).build());
    }

    @Test
    public void shouldInvokeCoreCaseDataApi() {
        SscsCaseDetails caseDetails = citizenCcdService.createCase(null, "draft", "summaery", "description", null);
        assertNotNull(caseDetails);
    }
}
