package uk.gov.hmcts.reform.sscs.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.sscs.builder.TrackYourAppealJsonBuilder;
import uk.gov.hmcts.reform.sscs.ccd.client.CcdClient;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.exception.CcdException;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.ccd.service.SscsCcdConvertService;
import uk.gov.hmcts.reform.sscs.exception.AppealNotFoundException;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.model.tya.SubscriptionRequest;
import uk.gov.hmcts.reform.sscs.model.tya.SurnameResponse;


@RunWith(MockitoJUnitRunner.class)
public class TribunalsServiceTest {

    private static final String APPEAL_NUMBER = "asfefsdf3223";
    private static final String SURNAME = "surname";
    public static final String CCD_CASE_ID = "1";
    public static final long CASE_ID = 123456789L;
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

    @Mock
    private CcdClient ccdClient;

    @Mock
    private SscsCcdConvertService sscsCcdConvertService;

    @SuppressWarnings("PMD.UnusedPrivateField")
    @Captor
    private ArgumentCaptor<SscsCaseData> captor;

    SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(SscsCaseData.builder().appeal(Appeal.builder().benefitType(BenefitType.builder().code("JSA").build()).build()).build()).build();

    IdamTokens idamTokens;

    private ObjectNode rootObjectNode;

    @Before
    public void setUp() {
        idamTokens = IdamTokens.builder().build();

        when(idamService.getIdamTokens()).thenReturn(idamTokens);

        tribunalsService = new TribunalsService(ccdService, regionalProcessingCenterService,
                trackYourAppealJsonBuilder, idamService, ccdClient, sscsCcdConvertService);


        ObjectNode caseNode = JsonNodeFactory.instance.objectNode();
        caseNode.put("appealNumber", APPEAL_NUMBER);
        rootObjectNode = JsonNodeFactory.instance.objectNode();
        rootObjectNode.set("appeal", caseNode);
    }

    @Test(expected = AppealNotFoundException.class)
    public void shouldThrowExceptionIfAppealNumberNotFound() throws CcdException {
        given(ccdService.findCaseByAppealNumber(APPEAL_NUMBER, idamTokens)).willReturn(null);

        tribunalsService.findAppeal(APPEAL_NUMBER);
    }

    @Test(expected = AppealNotFoundException.class)
    public void shouldThrowExceptionIfCaseIdNotFound() throws CcdException {
        given(ccdClient.readForCaseworker(idamTokens, CASE_ID)).willReturn(null);

        tribunalsService.findAppeal(CASE_ID);
    }

    @Test
    public void shouldLoadCaseForCaseIdWithAppealState() throws CcdException {
        CaseDetails caseDetails = CaseDetails.builder().state("withDwp").build();
        given(ccdClient.readForCaseworker(idamTokens, CASE_ID)).willReturn(caseDetails);


        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().id(CASE_ID).data(getCaseData()).build();
        given(sscsCcdConvertService.getCaseDetails(caseDetails)).willReturn(sscsCaseDetails);
        ObjectNode objectNode = mock(ObjectNode.class);
        given(trackYourAppealJsonBuilder.build(eq(sscsCaseDetails.getData()), any(), eq(CASE_ID), eq(true), eq("withDwp"))).willReturn(objectNode);

        ObjectNode appeal = tribunalsService.findAppeal(CASE_ID, true);
        assertThat(appeal, is(objectNode));
    }

    @Test
    public void shouldLoadCaseForCaseId() throws CcdException {
        CaseDetails caseDetails = CaseDetails.builder().build();
        given(ccdClient.readForCaseworker(idamTokens, CASE_ID)).willReturn(caseDetails);
        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().id(CASE_ID).data(getCaseData()).build();
        given(sscsCcdConvertService.getCaseDetails(caseDetails)).willReturn(sscsCaseDetails);
        ObjectNode objectNode = mock(ObjectNode.class);
        given(trackYourAppealJsonBuilder.build(eq(sscsCaseDetails.getData()), any(), eq(CASE_ID), eq(false), eq(null))).willReturn(objectNode);

        ObjectNode appeal = tribunalsService.findAppeal(CASE_ID);
        assertThat(appeal, is(objectNode));
    }

    @Test
    public void shouldThrowExceptionAndReturnEmptyWhenGivenNotValidSurname() throws CcdException {
        given(ccdService.findCcdCaseByAppealNumberAndSurname(APPEAL_NUMBER, SURNAME, idamTokens))
                .willThrow(uk.gov.hmcts.reform.sscs.ccd.exception.AppealNotFoundException.class);

        Optional<SurnameResponse> actualResponse = tribunalsService.validateSurname(APPEAL_NUMBER, SURNAME);

        assertFalse(actualResponse.isPresent());
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
        SscsCaseDetails caseDetailsWithRpc = getCaseDetailsWithRpc();
        when(ccdService.findCaseByAppealNumber(APPEAL_NUMBER, idamTokens)).thenReturn(caseDetailsWithRpc);
        given(trackYourAppealJsonBuilder.build(eq(caseDetailsWithRpc.getData()), any(), eq(caseDetailsWithRpc.getId()))).willReturn(rootObjectNode);

        tribunalsService.findAppeal(APPEAL_NUMBER);

        verify(regionalProcessingCenterService, never()).getByScReferenceCode(anyString());

    }

    @Test
    public void shouldGetRpcfromRegionalProcessingServiceIfItsNotPresentInCcdCase() {

        SscsCaseDetails caseDetails = getCaseDetails();
        when(ccdService.findCaseByAppealNumber(APPEAL_NUMBER, idamTokens)).thenReturn(caseDetails);
        given(trackYourAppealJsonBuilder.build(eq(caseDetails.getData()), any(), eq(caseDetails.getId()))).willReturn(rootObjectNode);

        tribunalsService.findAppeal(APPEAL_NUMBER);

        verify(regionalProcessingCenterService, times(1)).getByScReferenceCode(eq(null));
    }

    @Test
    public void findAppealWillReturnTheCalledAppealNumberInResponse() {

        final String appealNumber = "trueppealNumber";
        when(ccdService.findCaseByAppealNumber(appealNumber, idamTokens)).thenReturn(sscsCaseDetails);
        given(trackYourAppealJsonBuilder.build(eq(sscsCaseDetails.getData()), any(), eq(sscsCaseDetails.getId()))).willReturn(rootObjectNode);

        ObjectNode objectNode = tribunalsService.findAppeal(appealNumber);
        assertEquals(appealNumber, objectNode.findValue("appeal").get("appealNumber").asText());

    }

    @Test
    public void shouldReturnSurnameResponseWithCcdIdIfSurnameIsValidForGivenAppealNumber() {
        given(ccdService.findCcdCaseByAppealNumberAndSurname(APPEAL_NUMBER, SURNAME, idamTokens)).willReturn(getCaseData());

        Optional<SurnameResponse> surnameResponse =  tribunalsService.validateSurname(APPEAL_NUMBER, SURNAME);

        assertTrue(surnameResponse.isPresent());
        assertThat(surnameResponse.get().getCaseId(), equalTo(CCD_CASE_ID));
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
        return RegionalProcessingCenter.builder()
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
    }

}
