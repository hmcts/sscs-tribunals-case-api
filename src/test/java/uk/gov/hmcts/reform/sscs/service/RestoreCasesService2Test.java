package uk.gov.hmcts.reform.sscs.service;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DwpState.UNREGISTERED;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import feign.FeignException.UnprocessableEntity;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.ClassPathResource;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.admin.RestoreCasesService2;
import uk.gov.hmcts.reform.sscs.service.admin.RestoreCasesStatus;

@RunWith(MockitoJUnitRunner.class)
public class RestoreCasesService2Test {

    private RestoreCasesService2 restoreCasesService2;

    @Mock
    private CcdService ccdService;

    @Mock
    private IdamService idamService;

    @Mock
    private IdamTokens idamTokens;

    @Mock
    CSVReader reader;

    private static final RegionalProcessingCenterService regionalProcessingCenterService;

    private static AirLookupService airLookupService;

    private RegionalProcessingCenter rpc = RegionalProcessingCenter.builder().name("Basildon").build();
    private RegionalProcessingCenter rpc2 = RegionalProcessingCenter.builder().name("Glasgow").build();

    ArgumentCaptor<SscsCaseData> sscsCaseDataCaptor = ArgumentCaptor.forClass(SscsCaseData.class);
    ArgumentCaptor<Long> caseIdCaptor = ArgumentCaptor.forClass(Long.class);

    private SscsCaseDetails sscsCaseDetails;
    private SscsCaseDetails sscsCaseDetails2;

    static {
        airLookupService = new AirLookupService();
        airLookupService.init();
        regionalProcessingCenterService = new RegionalProcessingCenterService(airLookupService);
        regionalProcessingCenterService.init();
    }

    @Before
    public void setup() {
        restoreCasesService2 = new RestoreCasesService2(ccdService, idamService, new ObjectMapper(), regionalProcessingCenterService, airLookupService);
        when(idamService.getIdamTokens()).thenReturn(idamTokens);

        sscsCaseDetails = SscsCaseDetails.builder().state(State.WITH_DWP.getId())
        .id(1234112842952455L).data(
            SscsCaseData.builder().appeal(
                Appeal.builder().appellant(
                    Appellant.builder()
                            .name(Name.builder().firstName("Jeff").lastName("Smith").build())
                            .address(Address.builder().postcode("CM15 1TH").build())
                            .build())
                    .benefitType(BenefitType.builder().code("pip").build())
                    .build())
                .build()).build();

        sscsCaseDetails2 = SscsCaseDetails.builder().state(State.WITH_DWP.getId())
            .id(1234118573817544L).data(
                SscsCaseData.builder().appeal(
                    Appeal.builder().appellant(
                        Appellant.builder()
                                .name(Name.builder().firstName("Mary").lastName("Berry").build())
                                .address(Address.builder().postcode("G41 1TH").build())
                                .build())
                        .benefitType(BenefitType.builder().code("esa").build())
                        .build())
                    .build()).build();
    }

    @Test
    public void givenRestoreEventForWithDwpCases_thenSetCaseDataAndRestoreCaseWithAppealReceivedEvent() throws IOException {

        when(ccdService.getByCaseId(eq(1234112842952455L), Mockito.eq(idamTokens))).thenReturn(sscsCaseDetails);
        when(ccdService.getByCaseId(eq(1234118573817544L), Mockito.eq(idamTokens))).thenReturn(sscsCaseDetails2);

        ClassPathResource classPathResource = new ClassPathResource("csv/restore-cases-test.csv");
        CSVReader reader = new CSVReader(new InputStreamReader(classPathResource.getInputStream()));

        final RestoreCasesStatus status = restoreCasesService2.restoreCases(reader);

        Mockito.verify(ccdService, Mockito.times(2)).updateCase(sscsCaseDataCaptor.capture(), caseIdCaptor.capture(),
            eq("appealReceived"), eq("Restore case details"), eq("Automatically restore missing case details"), eq(idamTokens));

        Assert.assertEquals(Arrays.asList(1234112842952455L, 1234118573817544L), caseIdCaptor.getAllValues());
        Assert.assertEquals("BRADFORD", sscsCaseDataCaptor.getAllValues().get(0).getRegionalProcessingCenter().getCity());
        Assert.assertEquals("BRADFORD", sscsCaseDataCaptor.getAllValues().get(0).getRegion());
        Assert.assertEquals("002", sscsCaseDataCaptor.getAllValues().get(0).getBenefitCode());
        Assert.assertEquals("DD", sscsCaseDataCaptor.getAllValues().get(0).getIssueCode());
        Assert.assertEquals("002DD", sscsCaseDataCaptor.getAllValues().get(0).getCaseCode());
        Assert.assertEquals("No", sscsCaseDataCaptor.getAllValues().get(0).getIsScottishCase());
        Assert.assertEquals("Basildon CC", sscsCaseDataCaptor.getAllValues().get(0).getProcessingVenue());
        Assert.assertEquals("Jeff Smith", sscsCaseDataCaptor.getAllValues().get(0).getAppeal().getSigner());
        Assert.assertEquals(UNREGISTERED, sscsCaseDataCaptor.getAllValues().get(0).getDwpState());

        Assert.assertEquals("GLASGOW", sscsCaseDataCaptor.getAllValues().get(1).getRegionalProcessingCenter().getCity());
        Assert.assertEquals("GLASGOW", sscsCaseDataCaptor.getAllValues().get(1).getRegion());
        Assert.assertEquals("051", sscsCaseDataCaptor.getAllValues().get(1).getBenefitCode());
        Assert.assertEquals("DD", sscsCaseDataCaptor.getAllValues().get(1).getIssueCode());
        Assert.assertEquals("051DD", sscsCaseDataCaptor.getAllValues().get(1).getCaseCode());
        Assert.assertEquals("Yes", sscsCaseDataCaptor.getAllValues().get(1).getIsScottishCase());
        Assert.assertEquals("Glasgow", sscsCaseDataCaptor.getAllValues().get(1).getProcessingVenue());
        Assert.assertEquals("Mary Berry", sscsCaseDataCaptor.getAllValues().get(1).getAppeal().getSigner());
        Assert.assertEquals(UNREGISTERED, sscsCaseDataCaptor.getAllValues().get(1).getDwpState());

        Assert.assertFalse(status.isCompleted());
        Assert.assertTrue(status.isOk());
        Assert.assertEquals("RestoreCasesStatus{processedCount=2, successCount=2, failureCount=0, failureIds=[], completed=false}", status.toString());
    }

    @Test
    public void givenRestoreEventForInvalidCases_thenSetCaseDataAndRestoreCaseWithUpdateCaseOnlyEvent() throws IOException {

        sscsCaseDetails.setState(State.INCOMPLETE_APPLICATION.getId());
        sscsCaseDetails2.setState(State.INTERLOCUTORY_REVIEW_STATE.getId());

        when(ccdService.getByCaseId(eq(1234112842952455L), Mockito.eq(idamTokens))).thenReturn(sscsCaseDetails);
        when(ccdService.getByCaseId(eq(1234118573817544L), Mockito.eq(idamTokens))).thenReturn(sscsCaseDetails2);

        ClassPathResource classPathResource = new ClassPathResource("csv/restore-cases-test.csv");
        CSVReader reader = new CSVReader(new InputStreamReader(classPathResource.getInputStream()));

        final RestoreCasesStatus status = restoreCasesService2.restoreCases(reader);

        Mockito.verify(ccdService, Mockito.times(2)).updateCase(sscsCaseDataCaptor.capture(), caseIdCaptor.capture(),
                eq("updateCaseOnly"), eq("Restore case details"), eq("Automatically restore missing case details"), eq(idamTokens));

        Assert.assertEquals(Arrays.asList(1234112842952455L, 1234118573817544L), caseIdCaptor.getAllValues());
        Assert.assertEquals("BRADFORD", sscsCaseDataCaptor.getAllValues().get(0).getRegionalProcessingCenter().getCity());
        Assert.assertEquals("BRADFORD", sscsCaseDataCaptor.getAllValues().get(0).getRegion());
        Assert.assertEquals("002", sscsCaseDataCaptor.getAllValues().get(0).getBenefitCode());
        Assert.assertEquals("DD", sscsCaseDataCaptor.getAllValues().get(0).getIssueCode());
        Assert.assertEquals("002DD", sscsCaseDataCaptor.getAllValues().get(0).getCaseCode());
        Assert.assertEquals("No", sscsCaseDataCaptor.getAllValues().get(0).getIsScottishCase());
        Assert.assertEquals("Basildon CC", sscsCaseDataCaptor.getAllValues().get(0).getProcessingVenue());
        Assert.assertEquals("Jeff Smith", sscsCaseDataCaptor.getAllValues().get(0).getAppeal().getSigner());
        Assert.assertNull(sscsCaseDataCaptor.getAllValues().get(0).getDwpState());

        Assert.assertEquals("GLASGOW", sscsCaseDataCaptor.getAllValues().get(1).getRegionalProcessingCenter().getCity());
        Assert.assertEquals("GLASGOW", sscsCaseDataCaptor.getAllValues().get(1).getRegion());
        Assert.assertEquals("051", sscsCaseDataCaptor.getAllValues().get(1).getBenefitCode());
        Assert.assertEquals("DD", sscsCaseDataCaptor.getAllValues().get(1).getIssueCode());
        Assert.assertEquals("051DD", sscsCaseDataCaptor.getAllValues().get(1).getCaseCode());
        Assert.assertEquals("Yes", sscsCaseDataCaptor.getAllValues().get(1).getIsScottishCase());
        Assert.assertEquals("Glasgow", sscsCaseDataCaptor.getAllValues().get(1).getProcessingVenue());
        Assert.assertEquals("Mary Berry", sscsCaseDataCaptor.getAllValues().get(1).getAppeal().getSigner());
        Assert.assertNull(sscsCaseDataCaptor.getAllValues().get(1).getDwpState());

        Assert.assertFalse(status.isCompleted());
        Assert.assertTrue(status.isOk());
        Assert.assertEquals("RestoreCasesStatus{processedCount=2, successCount=2, failureCount=0, failureIds=[], completed=false}", status.toString());
    }

    @Test
    public void testRestoreBatchOfCasesWhenNoReturnedCases() throws IOException {

        when(ccdService.getByCaseId(eq(1234112842952455L), Mockito.eq(idamTokens))).thenReturn(null);

        ClassPathResource classPathResource = new ClassPathResource("csv/restore-cases-test.csv");
        CSVReader reader = new CSVReader(new InputStreamReader(classPathResource.getInputStream()));

        final RestoreCasesStatus status = restoreCasesService2.restoreCases(reader);

        Mockito.verify(ccdService, Mockito.times(0)).updateCase(sscsCaseDataCaptor.capture(), caseIdCaptor.capture(),
            any(), any(), any(), any());

        Assert.assertFalse(status.isCompleted());
        Assert.assertFalse(status.isOk());
        Assert.assertEquals("RestoreCasesStatus{processedCount=2, successCount=0, failureCount=2, failureIds=[1234112842952455, 1234118573817544], completed=false}", status.toString());

    }

    @Test
    public void testRestoreCasesAndUpdateCaseThrowsUnprocessableEntityExceptionForOne() throws IOException {

        when(ccdService.getByCaseId(eq(1234112842952455L), Mockito.eq(idamTokens))).thenReturn(sscsCaseDetails);
        when(ccdService.getByCaseId(eq(1234118573817544L), Mockito.eq(idamTokens))).thenReturn(sscsCaseDetails2);

        UnprocessableEntity unprocessableEntity = Mockito.mock(UnprocessableEntity.class);
        Mockito.when(unprocessableEntity.getMessage()).thenReturn("some exception message");

        Mockito.when(ccdService.updateCase(any(), Mockito.eq(1234118573817544L), Mockito.anyString(),
            Mockito.anyString(), Mockito.anyString(), Mockito.eq(idamTokens))).thenThrow(unprocessableEntity);

        ClassPathResource classPathResource = new ClassPathResource("csv/restore-cases-test.csv");
        CSVReader reader = new CSVReader(new InputStreamReader(classPathResource.getInputStream()));

        final RestoreCasesStatus status = restoreCasesService2.restoreCases(reader);

        Mockito.verify(ccdService, Mockito.times(2)).updateCase(sscsCaseDataCaptor.capture(), caseIdCaptor.capture(),
                eq("appealReceived"), eq("Restore case details"), eq("Automatically restore missing case details"), eq(idamTokens));

        Assert.assertEquals(Arrays.asList(1234112842952455L, 1234118573817544L), caseIdCaptor.getAllValues());

        Assert.assertFalse(status.isCompleted());
        Assert.assertFalse(status.isOk());
        Assert.assertEquals("RestoreCasesStatus{processedCount=2, successCount=1, failureCount=1, failureIds=[1234118573817544], completed=false}", status.toString());

    }

    @Test
    public void testRestoreCasesAndUpdateCaseThrowsRuntimeExceptionForOne() throws IOException {

        when(ccdService.getByCaseId(eq(1234112842952455L), Mockito.eq(idamTokens))).thenReturn(sscsCaseDetails);
        when(ccdService.getByCaseId(eq(1234118573817544L), Mockito.eq(idamTokens))).thenReturn(sscsCaseDetails2);

        Mockito.when(ccdService.updateCase(any(), Mockito.eq(1234118573817544L), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyString(), Mockito.eq(idamTokens))).thenThrow(new RuntimeException("some exception message"));

        ClassPathResource classPathResource = new ClassPathResource("csv/restore-cases-test.csv");
        CSVReader reader = new CSVReader(new InputStreamReader(classPathResource.getInputStream()));

        final RestoreCasesStatus status = restoreCasesService2.restoreCases(reader);

        Mockito.verify(ccdService, Mockito.times(2)).updateCase(sscsCaseDataCaptor.capture(), caseIdCaptor.capture(),
                eq("appealReceived"), eq("Restore case details"), eq("Automatically restore missing case details"), eq(idamTokens));

        Assert.assertEquals(Arrays.asList(1234112842952455L, 1234118573817544L), caseIdCaptor.getAllValues());

        Assert.assertFalse(status.isCompleted());
        Assert.assertFalse(status.isOk());
        Assert.assertEquals("RestoreCasesStatus{processedCount=2, successCount=1, failureCount=1, failureIds=[1234118573817544], completed=false}", status.toString());

    }

    @Test
    public void testGetRestoreCaseFileNameWhenValidJsonWithRestoreCasesDate() throws JsonProcessingException {
        String json = "{\"case_details\" : {\"case_data\" : {\"restoreCaseFileName\" : \"restore-case-.csv\"}}}";
        String fileName = restoreCasesService2.getRestoreCaseFileName(json);
        Assert.assertEquals("restore-case-.csv", fileName);
    }

    @Test
    public void testRestoreCasesStatusThrowsCsvException() throws IOException, CsvValidationException {
        doThrow(CsvValidationException.class).when(reader).readNext();

        assertThatNoException().isThrownBy(
                () -> restoreCasesService2.restoreCases(reader));
    }

    @Test
    public void testRestoreCasesStatusThrowsIoException() throws IOException, CsvValidationException {
        doThrow(IOException.class).when(reader).readNext();
        doThrow(IOException.class).when(reader).close();

        assertThatNoException().isThrownBy(
                () -> restoreCasesService2.restoreCases(reader));
    }

}
