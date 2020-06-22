package uk.gov.hmcts.reform.sscs.service.coversheet;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.OnlineHearingService;
import uk.gov.hmcts.reform.sscs.thirdparty.pdfservice.PdfService;

public class CoversheetServiceTest {

    private String onlineHearingId;
    private long caseId = 1234567890L;
    private OnlineHearingService onlineHearingService;
    private PdfService pdfService;
    private String template;
    private CcdService ccdService;
    private IdamService idamService;
    private IdamTokens idamTokens;
    private String hmctsImg;

    @Before
    public void setUp() {
        onlineHearingId = "onlineHearingId";
        onlineHearingService = mock(OnlineHearingService.class);
        pdfService = mock(PdfService.class);
        template = "template";
        ccdService = mock(CcdService.class);
        idamService = mock(IdamService.class);
        idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);
        hmctsImg = "hmcts.img";
    }

    @Test
    public void canLoadCcdCaseAndProducePdf() {
        when(onlineHearingService.getCcdCase(onlineHearingId))
                .thenReturn(Optional.of(createSscsCaseDetails()));

        byte[] pdf = {2, 4, 6, 0, 1};
        PdfCoverSheet pdfSummary = new PdfCoverSheet("12345", "firstname lastname", "line1", "line2", "town", "county", "postcode", hmctsImg);
        when(pdfService.createPdf(pdfSummary, template)).thenReturn(pdf);

        Optional<byte[]> pdfOptional =
                new CoversheetService(onlineHearingService, pdfService, template, hmctsImg, ccdService, idamService).createCoverSheet(onlineHearingId);

        assertThat(pdfOptional.isPresent(), is(true));
        assertThat(pdfOptional.get(), is(pdf));
    }

    @Test
    public void cannotLoadCcdCase() {
        when(onlineHearingService.getCcdCase(onlineHearingId)).thenReturn(Optional.empty());

        Optional<byte[]> pdfOptional =
                new CoversheetService(onlineHearingService, pdfService, template, hmctsImg, ccdService, idamService).createCoverSheet(onlineHearingId);

        assertThat(pdfOptional.isPresent(), is(false));
    }

    @Test
    public void canLoadCcdCaseByCaseIdAndProducePdf() {
        when(ccdService.getByCaseId(caseId, idamTokens)).thenReturn(createSscsCaseDetails());

        byte[] pdf = {2, 4, 6, 0, 1};
        PdfCoverSheet pdfSummary = new PdfCoverSheet("12345", "firstname lastname", "line1", "line2", "town", "county", "postcode", hmctsImg);
        when(pdfService.createPdf(pdfSummary, template)).thenReturn(pdf);

        Optional<byte[]> pdfOptional =
                new CoversheetService(onlineHearingService, pdfService, template, hmctsImg, ccdService, idamService).createCoverSheet(caseId + "");

        assertThat(pdfOptional.isPresent(), is(true));
        assertThat(pdfOptional.get(), is(pdf));
    }

    @Test
    public void cannotLoadCcdCaseByCaseId() {
        when(ccdService.getByCaseId(caseId, idamTokens)).thenReturn(null);

        Optional<byte[]> pdfOptional =
                new CoversheetService(onlineHearingService, pdfService, template, hmctsImg, ccdService, idamService).createCoverSheet(onlineHearingId);

        assertThat(pdfOptional.isPresent(), is(false));
    }

    private SscsCaseDetails createSscsCaseDetails() {
        return SscsCaseDetails.builder()
                .id(12345L)
                .data(SscsCaseData.builder()
                        .appeal(Appeal.builder()
                                .appellant(Appellant.builder()
                                        .name(Name.builder()
                                                .firstName("firstname")
                                                .lastName("lastname")
                                                .build())
                                        .address(Address.builder()
                                                .line1("line1")
                                                .line2("line2")
                                                .town("town")
                                                .county("county")
                                                .postcode("postcode")
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();
    }
}
