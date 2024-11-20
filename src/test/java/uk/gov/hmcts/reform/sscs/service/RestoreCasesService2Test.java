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
import java.util.function.Consumer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.admin.RestoreCasesService2;
import uk.gov.hmcts.reform.sscs.service.admin.RestoreCasesStatus;

@ExtendWith(MockitoExtension.class)
public class RestoreCasesService2Test {

    private RestoreCasesService2 restoreCasesService2;

    @Mock
    private CcdService ccdService;

    @Mock
    private UpdateCcdCaseService updateCcdCaseService;

    @Mock
    private IdamService idamService;

    @Mock
    private IdamTokens idamTokens;

    @Mock
    CSVReader reader;

    @Captor
    private ArgumentCaptor<Consumer<SscsCaseDetails>> sscsCaseDetailsCaptor;

    @Captor
    private ArgumentCaptor<Long> caseIdCaptor = ArgumentCaptor.forClass(Long.class);

    private static final RegionalProcessingCenterService regionalProcessingCenterService;

    private static final AirLookupService airLookupService;

    private SscsCaseDetails sscsCaseDetails;

    private SscsCaseDetails sscsCaseDetails2;

    static {
        airLookupService = new AirLookupService();
        airLookupService.init();
        regionalProcessingCenterService = new RegionalProcessingCenterService(airLookupService);
        regionalProcessingCenterService.init();
    }

    @BeforeEach
    public void setup() {
        restoreCasesService2 = new RestoreCasesService2(ccdService, updateCcdCaseService, idamService, new ObjectMapper(), regionalProcessingCenterService, airLookupService);
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

        Mockito.verify(updateCcdCaseService, Mockito.times(2)).updateCaseV2(caseIdCaptor.capture(),
            eq("appealReceived"), eq("Restore case details"), eq("Automatically restore missing case details"),
            eq(idamTokens), sscsCaseDetailsCaptor.capture());
        sscsCaseDetailsCaptor.getValue().accept(sscsCaseDetails);
        sscsCaseDetailsCaptor.getValue().accept(sscsCaseDetails2);

        Assertions.assertEquals(Arrays.asList(1234112842952455L, 1234118573817544L), caseIdCaptor.getAllValues());
        Assertions.assertEquals("BRADFORD", sscsCaseDetails.getData().getRegionalProcessingCenter().getCity());
        Assertions.assertEquals("BRADFORD", sscsCaseDetails.getData().getRegion());
        Assertions.assertEquals("002", sscsCaseDetails.getData().getBenefitCode());
        Assertions.assertEquals("DD", sscsCaseDetails.getData().getIssueCode());
        Assertions.assertEquals("002DD", sscsCaseDetails.getData().getCaseCode());
        Assertions.assertEquals("No", sscsCaseDetails.getData().getIsScottishCase());
        Assertions.assertEquals("Basildon CC", sscsCaseDetails.getData().getProcessingVenue());
        Assertions.assertEquals("Jeff Smith", sscsCaseDetails.getData().getAppeal().getSigner());
        Assertions.assertEquals(UNREGISTERED, sscsCaseDetails.getData().getDwpState());

        Assertions.assertEquals("GLASGOW", sscsCaseDetails2.getData().getRegionalProcessingCenter().getCity());
        Assertions.assertEquals("GLASGOW", sscsCaseDetails2.getData().getRegion());
        Assertions.assertEquals("051", sscsCaseDetails2.getData().getBenefitCode());
        Assertions.assertEquals("DD", sscsCaseDetails2.getData().getIssueCode());
        Assertions.assertEquals("051DD", sscsCaseDetails2.getData().getCaseCode());
        Assertions.assertEquals("Yes", sscsCaseDetails2.getData().getIsScottishCase());
        Assertions.assertEquals("Glasgow", sscsCaseDetails2.getData().getProcessingVenue());
        Assertions.assertEquals("Mary Berry", sscsCaseDetails2.getData().getAppeal().getSigner());
        Assertions.assertEquals(UNREGISTERED, sscsCaseDetails2.getData().getDwpState());

        Assertions.assertFalse(status.isCompleted());
        Assertions.assertTrue(status.isOk());
        Assertions.assertEquals("RestoreCasesStatus{processedCount=2, successCount=2, failureCount=0, failureIds=[], completed=false}", status.toString());
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

        Mockito.verify(updateCcdCaseService, Mockito.times(2)).updateCaseV2(caseIdCaptor.capture(),
            eq("updateCaseOnly"), eq("Restore case details"), eq("Automatically restore missing case details"),
            eq(idamTokens), sscsCaseDetailsCaptor.capture());
        sscsCaseDetailsCaptor.getValue().accept(sscsCaseDetails);
        sscsCaseDetailsCaptor.getValue().accept(sscsCaseDetails2);

        Assertions.assertEquals(Arrays.asList(1234112842952455L, 1234118573817544L), caseIdCaptor.getAllValues());
        Assertions.assertEquals("BRADFORD", sscsCaseDetails.getData().getRegionalProcessingCenter().getCity());
        Assertions.assertEquals("BRADFORD", sscsCaseDetails.getData().getRegion());
        Assertions.assertEquals("002", sscsCaseDetails.getData().getBenefitCode());
        Assertions.assertEquals("DD", sscsCaseDetails.getData().getIssueCode());
        Assertions.assertEquals("002DD", sscsCaseDetails.getData().getCaseCode());
        Assertions.assertEquals("No", sscsCaseDetails.getData().getIsScottishCase());
        Assertions.assertEquals("Basildon CC", sscsCaseDetails.getData().getProcessingVenue());
        Assertions.assertEquals("Jeff Smith", sscsCaseDetails.getData().getAppeal().getSigner());
        Assertions.assertNull(sscsCaseDetails.getData().getDwpState());

        Assertions.assertEquals("GLASGOW", sscsCaseDetails2.getData().getRegionalProcessingCenter().getCity());
        Assertions.assertEquals("GLASGOW", sscsCaseDetails2.getData().getRegion());
        Assertions.assertEquals("051", sscsCaseDetails2.getData().getBenefitCode());
        Assertions.assertEquals("DD", sscsCaseDetails2.getData().getIssueCode());
        Assertions.assertEquals("051DD", sscsCaseDetails2.getData().getCaseCode());
        Assertions.assertEquals("Yes", sscsCaseDetails2.getData().getIsScottishCase());
        Assertions.assertEquals("Glasgow", sscsCaseDetails2.getData().getProcessingVenue());
        Assertions.assertEquals("Mary Berry", sscsCaseDetails2.getData().getAppeal().getSigner());
        Assertions.assertNull(sscsCaseDetails2.getData().getDwpState());

        Assertions.assertFalse(status.isCompleted());
        Assertions.assertTrue(status.isOk());
        Assertions.assertEquals("RestoreCasesStatus{processedCount=2, successCount=2, failureCount=0, failureIds=[], completed=false}", status.toString());
    }

    @Test
    public void testRestoreBatchOfCasesWhenNoReturnedCases() throws IOException {

        when(ccdService.getByCaseId(eq(1234112842952455L), Mockito.eq(idamTokens))).thenReturn(null);

        ClassPathResource classPathResource = new ClassPathResource("csv/restore-cases-test.csv");
        CSVReader reader = new CSVReader(new InputStreamReader(classPathResource.getInputStream()));

        final RestoreCasesStatus status = restoreCasesService2.restoreCases(reader);

        Mockito.verify(updateCcdCaseService, Mockito.times(0)).updateCaseV2(caseIdCaptor.capture(),
            any(), any(), any(), any(), ArgumentMatchers.<Consumer<SscsCaseDetails>>any());

        Assertions.assertFalse(status.isCompleted());
        Assertions.assertFalse(status.isOk());
        Assertions.assertEquals("RestoreCasesStatus{processedCount=2, successCount=0, failureCount=2, failureIds=[1234112842952455, 1234118573817544], completed=false}", status.toString());

    }

    @Test
    public void testRestoreCasesAndUpdateCaseThrowsUnprocessableEntityExceptionForOne() throws IOException {

        when(ccdService.getByCaseId(eq(1234112842952455L), Mockito.eq(idamTokens))).thenReturn(sscsCaseDetails);
        when(ccdService.getByCaseId(eq(1234118573817544L), Mockito.eq(idamTokens))).thenReturn(sscsCaseDetails2);

        UnprocessableEntity unprocessableEntity = Mockito.mock(UnprocessableEntity.class);
        Mockito.when(unprocessableEntity.getMessage()).thenReturn("some exception message");

        Mockito.when(updateCcdCaseService.updateCaseV2(Mockito.eq(1234118573817544L), Mockito.anyString(),
            Mockito.anyString(), Mockito.anyString(), Mockito.eq(idamTokens),
            ArgumentMatchers.<Consumer<SscsCaseDetails>>any())).thenThrow(unprocessableEntity);

        ClassPathResource classPathResource = new ClassPathResource("csv/restore-cases-test.csv");
        CSVReader reader = new CSVReader(new InputStreamReader(classPathResource.getInputStream()));

        final RestoreCasesStatus status = restoreCasesService2.restoreCases(reader);

        Mockito.verify(updateCcdCaseService, Mockito.times(2)).updateCaseV2(caseIdCaptor.capture(),
            eq("appealReceived"), eq("Restore case details"), eq("Automatically restore missing case details"),
            eq(idamTokens), sscsCaseDetailsCaptor.capture());

        Assertions.assertEquals(Arrays.asList(1234112842952455L, 1234118573817544L), caseIdCaptor.getAllValues());

        Assertions.assertFalse(status.isCompleted());
        Assertions.assertFalse(status.isOk());
        Assertions.assertEquals("RestoreCasesStatus{processedCount=2, successCount=1, failureCount=1, failureIds=[1234118573817544], completed=false}", status.toString());

    }

    @Test
    public void testRestoreCasesAndUpdateCaseThrowsRuntimeExceptionForOne() throws IOException {

        when(ccdService.getByCaseId(eq(1234112842952455L), Mockito.eq(idamTokens))).thenReturn(sscsCaseDetails);
        when(ccdService.getByCaseId(eq(1234118573817544L), Mockito.eq(idamTokens))).thenReturn(sscsCaseDetails2);

        when(updateCcdCaseService.updateCaseV2(eq(1234118573817544L), Mockito.anyString(), Mockito.anyString(),
            Mockito.anyString(), eq(idamTokens), ArgumentMatchers.<Consumer<SscsCaseDetails>>any())).thenThrow(new RuntimeException("some exception message"));

        ClassPathResource classPathResource = new ClassPathResource("csv/restore-cases-test.csv");
        CSVReader reader = new CSVReader(new InputStreamReader(classPathResource.getInputStream()));

        final RestoreCasesStatus status = restoreCasesService2.restoreCases(reader);

        Mockito.verify(updateCcdCaseService, Mockito.times(2)).updateCaseV2(caseIdCaptor.capture(),
                eq("appealReceived"), eq("Restore case details"), eq("Automatically restore missing case details"), eq(idamTokens), ArgumentMatchers.<Consumer<SscsCaseDetails>>any());

        Assertions.assertEquals(Arrays.asList(1234112842952455L, 1234118573817544L), caseIdCaptor.getAllValues());

        Assertions.assertFalse(status.isCompleted());
        Assertions.assertFalse(status.isOk());
        Assertions.assertEquals("RestoreCasesStatus{processedCount=2, successCount=1, failureCount=1, failureIds=[1234118573817544], completed=false}", status.toString());

    }

    @Test
    public void testGetRestoreCaseFileNameWhenValidJsonWithRestoreCasesDate() throws JsonProcessingException {
        String json = "{\"case_details\" : {\"case_data\" : {\"restoreCaseFileName\" : \"restore-case-.csv\"}}}";
        String fileName = restoreCasesService2.getRestoreCaseFileName(json);
        Assertions.assertEquals("restore-case-.csv", fileName);
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
