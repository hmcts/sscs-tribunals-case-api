package uk.gov.hmcts.reform.sscs.service;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.exception.CcdException;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.sscs.builder.TrackYourAppealJsonBuilder;
import uk.gov.hmcts.sscs.exception.AppealNotFoundException;
import uk.gov.hmcts.sscs.model.tya.SubscriptionRequest;
import uk.gov.hmcts.sscs.model.tya.SurnameResponse;
import uk.gov.hmcts.sscs.service.exceptions.InvalidSurnameException;
import uk.gov.hmcts.sscs.service.referencedata.RegionalProcessingCenterService;

@RunWith(MockitoJUnitRunner.class)
public class TribunalsServiceTest {

    private static final String APPEAL_NUMBER = "asfefsdf3223";
    private static final String SURNAME = "surname";
    public static final String CCD_CASE_ID = "1";
    private TribunalsService tribunalsService;

    @Mock
    private CcdService ccdService;

    @Mock
    private IdamService idamService;

    @Mock
    private RegionalProcessingCenterService regionalProcessingCenterService;

    @Mock
    private TrackYourAppealJsonBuilder trackYourAppealJsonBuilder;

    @Mock
    private SubscriptionRequest subscriptionRequest;

    @Captor
    private ArgumentCaptor<SscsCaseData> captor;

    SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(SscsCaseData.builder().appeal(Appeal.builder().benefitType(BenefitType.builder().code("JSA").build()).build()).build()).build();

    IdamTokens idamTokens;

    @Before
    public void setUp() {
        idamTokens = IdamTokens.builder().build();

        when(idamService.getIdamTokens()).thenReturn(idamTokens);

        tribunalsService = new TribunalsService(ccdService, regionalProcessingCenterService,
                trackYourAppealJsonBuilder, idamService);
    }

    @Test(expected = AppealNotFoundException.class)
    public void shouldThrowExceptionIfAppealNumberNotFound() throws CcdException {
        given(ccdService.findCaseByAppealNumber(APPEAL_NUMBER, idamTokens)).willReturn(null);

        tribunalsService.findAppeal(APPEAL_NUMBER);
    }

    @Test(expected = InvalidSurnameException.class)
    public void shouldThrowExceptionGivenValidationFails() throws CcdException {
        given(ccdService.findCcdCaseByAppealNumberAndSurname(APPEAL_NUMBER, SURNAME, idamTokens)).willReturn(null);

        tribunalsService.validateSurname(APPEAL_NUMBER, SURNAME);
    }

    @Test
    public void shouldUnsubscribe() throws CcdException {
        when(ccdService.updateSubscription(APPEAL_NUMBER, null, idamTokens)).thenReturn(sscsCaseDetails);
        String result = tribunalsService.unsubscribe(APPEAL_NUMBER);

        verify(ccdService).updateSubscription(eq(APPEAL_NUMBER), eq(null), eq(idamTokens));
        assertEquals(result, "jsa");
    }

    @Test
    public void shouldUpdateSubscriptionDetails() throws CcdException {
        when(ccdService.updateSubscription(APPEAL_NUMBER, subscriptionRequest.getEmail(), idamTokens)).thenReturn(sscsCaseDetails);
        String result = tribunalsService.updateSubscription(APPEAL_NUMBER, subscriptionRequest);

        verify(ccdService).updateSubscription(eq(APPEAL_NUMBER), any(), eq(idamTokens));
        assertEquals(result, "jsa");
    }

    @Test
    public void shouldAddRegionalProcessingCenterFromCcdIfItsPresent() {
        Mockito.when(ccdService.findCaseByAppealNumber(APPEAL_NUMBER, idamTokens)).thenReturn(getCaseDetailsWithRpc());

        tribunalsService.findAppeal(APPEAL_NUMBER);

        verify(regionalProcessingCenterService, never()).getByScReferenceCode(anyString());

    }

    @Test
    public void shouldGetRpcfromRegionalProcessingServiceIfItsNotPresentInCcdCase() {

        Mockito.when(ccdService.findCaseByAppealNumber(APPEAL_NUMBER, idamTokens)).thenReturn(getCaseDetails());

        tribunalsService.findAppeal(APPEAL_NUMBER);

        verify(regionalProcessingCenterService, times(1)).getByScReferenceCode(eq(null));
    }

    @Test
    public void shouldReturnSurnameResponseWithCcdIdIfSurnameIsValidForGivenAppealNumber() {
        given(ccdService.findCcdCaseByAppealNumberAndSurname(APPEAL_NUMBER, SURNAME, idamTokens)).willReturn(getCaseData());

        SurnameResponse surnameResponse =  tribunalsService.validateSurname(APPEAL_NUMBER, SURNAME);

        assertNotNull(surnameResponse);
        assertThat(surnameResponse.getCaseId(), equalTo(CCD_CASE_ID));
    }

    private SscsCaseDetails getCaseDetailsWithRpc() {
        return SscsCaseDetails.builder().data(SscsCaseData.builder().regionalProcessingCenter(getRegionalProcessingCenter()).build()).build();
    }

    private SscsCaseDetails getCaseDetails() {
        return SscsCaseDetails.builder().data(getCaseData()).build();
    }

    private SscsCaseData getCaseData() {
        return SscsCaseData.builder().ccdCaseId(CCD_CASE_ID).build();
    }

    private RegionalProcessingCenter getRegionalProcessingCenter() {
        RegionalProcessingCenter rpc = RegionalProcessingCenter.builder()
                .name("CARDIFF")
                .address1("HM Courts & Tribunals Service")
                .address2("Social Security & Child Support Appeals")
                .address3("Eastgate House")
                .address4("Newport Road")
                .city("CARDIFF")
                .postcode("CF24 0AB")
                .phoneNumber("0300 123 1142")
                .faxNumber("0870 739 4438")
                .build();
        return rpc;
    }

}
