package uk.gov.hmcts.sscs.service;

import static org.junit.Assert.assertNotNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import uk.gov.hmcts.sscs.exception.CcdException;
import uk.gov.hmcts.sscs.model.ccd.CaseData;

@RunWith(MockitoJUnitRunner.class)
public class AppealNumberGeneratorTest {

    @Mock
    private CcdService ccdService;

    private AppealNumberGenerator appealNumberGenerator;

    @Before
    public void setup() {
        appealNumberGenerator = new AppealNumberGenerator(ccdService);
    }

    @Test
    public void shouldGenerateAppealNumberWhenCcdReturnNullObject() throws CcdException {

        given(ccdService.findCcdCaseByAppealNumber(anyString())).willReturn(null);

        String appealNumber = appealNumberGenerator.generate();

        verify(ccdService).findCcdCaseByAppealNumber(anyString());

        assertNotNull(appealNumber);
    }

    @Test
    public void shouldGenerateAppealNumberInSecondAttempt() throws Exception {

        when(ccdService.findCcdCaseByAppealNumber(anyString())).thenReturn(getCaseData(), null);

        appealNumberGenerator.generate();

        verify(ccdService, times(2)).findCcdCaseByAppealNumber(anyString());
    }

    @Test
    public void shouldGenerateAppealNumberInThirdAttempt() throws Exception {

        when(ccdService.findCcdCaseByAppealNumber(anyString())).thenReturn(getCaseData(), getCaseData(), null);

        appealNumberGenerator.generate();

        verify(ccdService, times(3)).findCcdCaseByAppealNumber(anyString());
    }

    @Test(expected = CcdException.class)
    public void shouldThrowExceptionTryingThreeAttempts() throws Exception {

        given(ccdService.findCcdCaseByAppealNumber(anyString())).willReturn(getCaseData());

        appealNumberGenerator.generate();
    }

    private CaseData getCaseData() {
        return CaseData.builder().build();
    }
}