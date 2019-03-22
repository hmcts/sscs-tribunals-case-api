package uk.gov.hmcts.reform.sscs.config;

import static org.mockito.MockitoAnnotations.initMocks;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
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
    }

    @Test
    public void shouldInvokeCoreCaseDataApi() {
        citizenCcdService.createCase(null, "draft", "summaery", "description", null);
    }
}
