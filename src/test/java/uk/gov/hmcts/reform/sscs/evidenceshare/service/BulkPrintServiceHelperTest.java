package uk.gov.hmcts.reform.sscs.evidenceshare.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;
import uk.gov.hmcts.reform.sscs.evidenceshare.domain.FurtherEvidenceLetterType;
import uk.gov.hmcts.reform.sscs.service.CcdNotificationsPdfService;

@ExtendWith(MockitoExtension.class)
public class BulkPrintServiceHelperTest {
    @Mock
    private CcdNotificationsPdfService ccdNotificationsPdfService;

    BulkPrintServiceHelper bulkPrintServiceHelper;

    private SscsCaseData appellantWantsRa;
    private SscsCaseData repWantsRa;
    List<Pdf> pdfs;

    @BeforeEach
    public void setUp() {
        bulkPrintServiceHelper =
            new BulkPrintServiceHelper(ccdNotificationsPdfService);

        appellantWantsRa  = SscsCaseData.builder()
            .reasonableAdjustments(ReasonableAdjustments.builder()
                .appellant(ReasonableAdjustmentDetails.builder()
                    .wantsReasonableAdjustment(YesNo.YES).build()).build())
            .build();

        repWantsRa  = SscsCaseData.builder()
            .reasonableAdjustments(ReasonableAdjustments.builder()
                .representative(ReasonableAdjustmentDetails.builder()
                    .wantsReasonableAdjustment(YesNo.YES).build()).build())
            .build();

        pdfs = new ArrayList<>();
        Pdf pdf = new Pdf(new byte[]{0,0,0,0}, "document");
        pdfs.add(pdf);
    }

    @Test
    public void testForAppellantWhoWantsAdjustmentWithAppellantLetter() {
        assertTrue(bulkPrintServiceHelper.sendForReasonableAdjustment(appellantWantsRa,
            FurtherEvidenceLetterType.APPELLANT_LETTER));
    }

    @Test
    public void testForAppellantWhoWantsAdjustmentWithRepLetter() {
        assertFalse(bulkPrintServiceHelper.sendForReasonableAdjustment(appellantWantsRa,
            FurtherEvidenceLetterType.REPRESENTATIVE_LETTER));
    }

    @Test
    public void testForRepWhoWantsAdjustmentWithAppellantLetter() {
        assertFalse(bulkPrintServiceHelper.sendForReasonableAdjustment(repWantsRa,
            FurtherEvidenceLetterType.APPELLANT_LETTER));
    }

    @Test
    public void testForRepWhoWantsAdjustmentWithRepLetter() {
        assertTrue(bulkPrintServiceHelper.sendForReasonableAdjustment(repWantsRa,
            FurtherEvidenceLetterType.REPRESENTATIVE_LETTER));
    }

    @Test
    public void testSaveAsReasonableAdjustment() {
        bulkPrintServiceHelper.saveAsReasonableAdjustment(SscsCaseData.builder()
            .ccdCaseId("111111111111111")
                .appeal(Appeal.builder()
                    .appellant(Appellant.builder()
                        .name(new Name("Mr", "Jimmy", "Greg")).build()).build()).build(),
            pdfs, FurtherEvidenceLetterType.APPELLANT_LETTER);
    }
}
