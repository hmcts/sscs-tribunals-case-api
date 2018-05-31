package uk.gov.hmcts.sscs.service;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.pdf.service.client.PDFServiceClient;
import uk.gov.hmcts.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.sscs.email.SubmitYourAppealEmail;
import uk.gov.hmcts.sscs.exception.CcdException;
import uk.gov.hmcts.sscs.model.ccd.CaseData;
import uk.gov.hmcts.sscs.model.pdf.PdfWrapper;
import uk.gov.hmcts.sscs.transform.deserialize.SubmitYourAppealToCcdCaseDataDeserializer;

@RunWith(MockitoJUnitRunner.class)
public class SubmitAppealServiceTest {
    private static final String TEMPLATE_PATH = "/templates/appellant_appeal_template.html";

    private ObjectMapper mapper;

    @Mock
    private AppealNumberGenerator appealNumberGenerator;

    @Mock
    private CcdService ccdService;

    @Mock
    private PDFServiceClient pdfServiceClient;

    @Mock
    private EmailService emailService;

    @Captor
    private ArgumentCaptor captor;

    private SubmitYourAppealEmail submitYourAppealEmail;

    private SubmitAppealService submitAppealService;

    @Before
    public void setUp() {
        mapper = new ObjectMapper().registerModule(new JavaTimeModule());

        SubmitYourAppealToCcdCaseDataDeserializer submitYourAppealToCcdCaseDataDeserializer = new
                SubmitYourAppealToCcdCaseDataDeserializer();

        submitYourAppealEmail = new SubmitYourAppealEmail("from", "to", "dummy", "message");

        submitAppealService = new SubmitAppealService(TEMPLATE_PATH, appealNumberGenerator,
                submitYourAppealToCcdCaseDataDeserializer, ccdService,
                pdfServiceClient, emailService, submitYourAppealEmail);

        given(ccdService.createCase(any(CaseData.class)))
                .willReturn(CaseDetails.builder().id(123L).build());
    }

    @Test
    public void shouldSendPdfByEmailWhenCcdIsDown() {
        given(ccdService.createCase(any(CaseData.class))).willThrow(new CcdException(
                new RuntimeException("Error while creating case in CCD")));

        byte[] expected = {};
        given(pdfServiceClient.generateFromHtml(any(byte[].class), (Map<String, Object>) captor.capture()))
                .willReturn(expected);

        submitAppealService.submitAppeal(getSyaCaseWrapper());

        then(pdfServiceClient).should(times(1)).generateFromHtml(any(), any());
        then(emailService).should(times(1)).sendEmail(any(SubmitYourAppealEmail.class));

        assertNull(getPdfWrapper().getCcdCaseId());
    }

    private PdfWrapper getPdfWrapper() {
        Map placeHolders = (Map) captor.getAllValues().get(0);
        return (PdfWrapper) placeHolders.get("PdfWrapper");
    }

    @Test
    public void shouldCreateCaseWithAppealDetails() {
        SyaCaseWrapper appealData = getSyaCaseWrapper();
        byte[] expected = {};
        given(pdfServiceClient.generateFromHtml(any(byte[].class),
                any(Map.class))).willReturn(expected);

        submitAppealService.submitAppeal(appealData);

        verify(appealNumberGenerator).generate();
        verify(ccdService).createCase(any(CaseData.class));
    }


    @Test
    public void shouldCreatePdfWithAppealDetails() {
        SyaCaseWrapper appealData = getSyaCaseWrapper();
        byte[] expected = {};
        given(pdfServiceClient.generateFromHtml(any(byte[].class),
                any(Map.class))).willReturn(expected);

        submitAppealService.submitAppeal(appealData);

        assertThat(submitYourAppealEmail.getSubject(), is("Bloggs_33C"));
        verify(emailService).sendEmail(any(SubmitYourAppealEmail.class));
    }

    private SyaCaseWrapper getSyaCaseWrapper() {
        URL resource = getClass().getClassLoader().getResource("json/sya.json");
        try {
            return mapper.readValue(resource, SyaCaseWrapper.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}