package uk.gov.hmcts.reform.sscs.evidenceshare.service;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.UUID;
import junitparams.JUnitParamsRunner;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.sscs.ccd.client.CcdClient;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;
import uk.gov.hmcts.reform.sscs.service.AirLookupService;


@RunWith(JUnitParamsRunner.class)
@SpringBootTest
@ActiveProfiles("integration")
public class BulkPrintServiceIt {
    //end of rules needed for junitParamsRunner

    private static final SscsCaseData SSCS_CASE_DATA = SscsCaseData.builder()
        .ccdCaseId("234")
        .appeal(Appeal.builder().appellant(
                Appellant.builder()
                    .name(Name.builder().firstName("Appellant").lastName("LastName").build())
                    .address(Address.builder().line1("line1").build())
                    .build())
            .build())
        .build();


    @MockBean
    @SuppressWarnings({"PMD.UnusedPrivateField"})
    private CcdClient ccdClient;

    @MockBean
    protected AirLookupService airLookupService;

    @Autowired
    private BulkPrintService bulkPrintService;

    @Test
    @Disabled("need to get send-letter-service working locally")
    public void willSendFileToBulkPrint() {
        Optional<UUID> uuidOptional = bulkPrintService.sendToBulkPrint(
            singletonList(new Pdf("my data".getBytes(), "file.pdf")), SSCS_CASE_DATA, "Appellant LastName");
        assertTrue(uuidOptional.isPresent(), "a uuid should exist");
    }
}
