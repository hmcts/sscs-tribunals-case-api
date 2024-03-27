package uk.gov.hmcts.reform.sscs.evidenceshare.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.MrnDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Template;

@SpringBootTest
@RunWith(SpringRunner.class)
public class TemplateServiceTest {

    @Autowired
    private TemplateService service;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Test
    public void givenACaseDataWithMrnWithin30Days_thenGenerateThePlaceholderMappingsForDl6() {
        LocalDate date = LocalDate.now();
        String dateAsString = date.format(formatter);

        SscsCaseData caseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .mrnDetails(MrnDetails.builder().mrnDate(dateAsString).build())
            .build()).build();

        Template result = service.findTemplate(caseData);

        assertEquals("dl6", result.getHmctsDocName());
    }

    @Test
    public void givenACaseDataWithMrnWithin30Days_whenLanguageIsWelsh_thenGenerateTheWelshPlaceholderMappingsForDl6() {
        LocalDate date = LocalDate.now();
        String dateAsString = date.format(formatter);

        SscsCaseData caseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .mrnDetails(MrnDetails.builder().mrnDate(dateAsString).build())
                .build())
            .languagePreferenceWelsh("Yes")
            .build();

        Template result = service.findTemplate(caseData);

        assertEquals("dl6", result.getHmctsDocName());
    }

    @Test
    public void givenACaseDataWithMrnEqualTo30Days_thenGenerateThePlaceholderMappings() {
        LocalDate date = LocalDate.now().minusDays(30);
        String dateAsString = date.format(formatter);

        SscsCaseData caseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .mrnDetails(MrnDetails.builder().mrnDate(dateAsString).build())
                .build())
            .languagePreferenceWelsh("No")
            .build();

        Template result = service.findTemplate(caseData);

        assertEquals("dl6", result.getHmctsDocName());
    }

    @Test
    public void givenACaseDataWithMrnEqualTo30Days_whenLanguageIsWelsh_thenGenerateTheWelshPlaceholderMappings() {
        LocalDate date = LocalDate.now().minusDays(30);
        String dateAsString = date.format(formatter);

        SscsCaseData caseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .mrnDetails(MrnDetails.builder().mrnDate(dateAsString).build())
                .build())
            .languagePreferenceWelsh("Yes")
            .build();

        Template result = service.findTemplate(caseData);

        assertEquals("dl6", result.getHmctsDocName());
    }

    @Test
    public void givenACaseDataWithMrnGreaterThan30Days_thenGenerateThePlaceholderMappingsForDl16() {
        LocalDate date = LocalDate.now().minusDays(31);
        String dateAsString = date.format(formatter);

        SscsCaseData caseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .mrnDetails(MrnDetails.builder().mrnDate(dateAsString).build())
                .build())
            .languagePreferenceWelsh("No")
            .build();

        Template result = service.findTemplate(caseData);

        assertEquals("dl16", result.getHmctsDocName());
    }

    @Test
    public void givenACaseDataWithMrnGreaterThan30Days_whenLanguageIsWelsh_thenGenerateTheWelshPlaceholderMappingsForDl16() {
        LocalDate date = LocalDate.now().minusDays(31);
        String dateAsString = date.format(formatter);

        SscsCaseData caseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .mrnDetails(MrnDetails.builder().mrnDate(dateAsString).build())
                .build())
            .languagePreferenceWelsh("Yes")
            .build();

        Template result = service.findTemplate(caseData);

        assertEquals("dl16", result.getHmctsDocName());
    }


    @Test
    public void givenACaseDataWithMrnMissing_thenGenerateThePlaceholderMappings() {
        SscsCaseData caseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .mrnDetails(null)
                .build())
            .build();

        Template result = service.findTemplate(caseData);

        assertNull(result);
    }
}
