package uk.gov.hmcts.reform.sscs.docassembly;

import static org.junit.Assert.assertNotNull;
import static uk.gov.hmcts.reform.sscs.service.SubmitAppealService.DM_STORE_USER_ID;

import java.io.IOException;
import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.sscs.service.EvidenceManagementService;

@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
public class GenerateFileTest {

    @Autowired
    private GenerateFile generateFile;

    @Autowired
    EvidenceManagementService evidenceManagementService;

    @Test
    public void canUseDocAssembly() throws IOException {

        String documentUrl = generateFile.assemble();

        log.info("Document Assembly Url", documentUrl);
        assertNotNull(documentUrl);
        byte[] bytes = evidenceManagementService.download(URI.create(documentUrl), DM_STORE_USER_ID);
        try (PDDocument pdDocument = PDDocument.load(bytes)) {
            String text = new PDFTextStripper().getText(pdDocument);
            log.info("Got text.", text);
        }

    }
}
