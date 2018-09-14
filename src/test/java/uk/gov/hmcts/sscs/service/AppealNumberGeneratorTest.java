package uk.gov.hmcts.sscs.service;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.exception.CcdException;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@RunWith(MockitoJUnitRunner.class)
public class AppealNumberGeneratorTest {

    @Mock
    private CcdService ccdService;

    @Mock
    private IdamService idamService;

    private AppealNumberGenerator appealNumberGenerator;

    private IdamTokens idamTokens;

    @Before
    public void setup() {
        appealNumberGenerator = new AppealNumberGenerator(ccdService, idamService);

        idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);
    }

    @Test
    public void shouldGenerateAppealNumberWhenCcdReturnNullObject() throws CcdException {

        given(ccdService.findCaseByAppealNumber(anyString(), eq(idamTokens))).willReturn(null);

        String appealNumber = appealNumberGenerator.generate();

        verify(ccdService).findCaseByAppealNumber(anyString(), eq(idamTokens));

        assertNotNull(appealNumber);
    }

    @Test
    public void shouldGenerateAppealNumberInSecondAttempt() throws Exception {

        when(ccdService.findCaseByAppealNumber(anyString(), eq(idamTokens))).thenReturn(getCaseDetails(), (SscsCaseDetails) null);

        appealNumberGenerator.generate();

        verify(ccdService, times(2)).findCaseByAppealNumber(anyString(), eq(idamTokens));
    }

    @Test
    public void shouldGenerateAppealNumberInThirdAttempt() throws Exception {

        when(ccdService.findCaseByAppealNumber(anyString(), eq(idamTokens))).thenReturn(getCaseDetails(), getCaseDetails(), null);

        appealNumberGenerator.generate();

        verify(ccdService, times(3)).findCaseByAppealNumber(anyString(), eq(idamTokens));
    }

    @Test(expected = CcdException.class)
    public void shouldThrowExceptionTryingThreeAttempts() throws Exception {

        given(ccdService.findCaseByAppealNumber(anyString(), eq(idamTokens))).willReturn(getCaseDetails());

        appealNumberGenerator.generate();
    }

    private SscsCaseDetails getCaseDetails() {
        return SscsCaseDetails.builder().build();
    }
}