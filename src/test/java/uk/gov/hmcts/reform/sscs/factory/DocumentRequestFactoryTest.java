package uk.gov.hmcts.reform.sscs.factory;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.MockitoAnnotations.openMocks;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.docmosis.domain.DocumentHolder;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Template;
import uk.gov.hmcts.reform.sscs.evidenceshare.service.TemplateService;
import uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderService;
import uk.gov.hmcts.reform.sscs.service.DwpAddressLookupService;

public class DocumentRequestFactoryTest {

    @Mock
    private PlaceholderService placeholderService;

    @Mock
    private DwpAddressLookupService dwpAddressLookup;

    @Mock
    private TemplateService templateService;

    @InjectMocks
    private DocumentRequestFactory factory;

    @Before
    public void setup() {
        openMocks(this);
    }

    @Test
    public void givenACaseData_thenCreateTheDocumentHolderObject() {
        SscsCaseData caseData = SscsCaseData.builder().build();
        Map<String, Object> placeholders = new HashMap<>();
        placeholders.put("Test", "Value");
        String now = (DateTimeFormatter.ISO_LOCAL_DATE).format(LocalDateTime.now());

        Template template = new Template("bla", "bla2");
        Address address = Address.builder().build();
        given(templateService.findTemplate(caseData)).willReturn(template);
        given(dwpAddressLookup.lookupDwpAddress(caseData)).willReturn(address);

        DocumentHolder holder = factory.create(caseData, now);

        assertEquals(template, holder.getTemplate());
        assertEquals(true, holder.isPdfArchiveMode());
    }
}
