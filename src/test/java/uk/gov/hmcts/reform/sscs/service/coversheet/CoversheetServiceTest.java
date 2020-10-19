package uk.gov.hmcts.reform.sscs.service.coversheet;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.LanguagePreference;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.config.DocumentConfiguration;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.OnlineHearingService;
import uk.gov.hmcts.reform.sscs.thirdparty.pdfservice.PdfService;

public class CoversheetServiceTest {

    private String onlineHearingId;
    private long caseId = 1234567890L;
    private OnlineHearingService onlineHearingService;
    private PdfService pdfService;
    private CcdService ccdService;
    private IdamService idamService;
    private IdamTokens idamTokens;
    private DocumentConfiguration documentConfiguration;
    private Map<String, String> englishTempateDetails;
    private  Map<String, String> welshTempateDetails;
    private static final String template = "template";
    private static final String hmctsImgVal = "hmctsImgVal";

    @Before
    public void setUp() {
        onlineHearingId = "onlineHearingId";
        onlineHearingService = mock(OnlineHearingService.class);
        pdfService = mock(PdfService.class);
        ccdService = mock(CcdService.class);
        idamService = mock(IdamService.class);
        idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);

        documentConfiguration = spy(DocumentConfiguration.class);

        englishTempateDetails = new HashMap<>();
        englishTempateDetails.put(template,"TB-SCS-GNO-ENG-00012.docx");
        englishTempateDetails.put(hmctsImgVal,"\"[userImage:hmcts.png]\"");

        welshTempateDetails = new HashMap<>();
        welshTempateDetails.put(template,"TB-SCS-GNO-WEL-00479.docx");
        welshTempateDetails.put(hmctsImgVal,"\"[userImage:welshhmcts.png]\"");

        Map<LanguagePreference, Map<String, String>>  evidence =  new HashMap<>();
        evidence.put(LanguagePreference.ENGLISH, englishTempateDetails);
        evidence.put(LanguagePreference.WELSH, welshTempateDetails);
        documentConfiguration.setEvidence(evidence);
    }

    @Test
    public void canLoadCcdCaseAndProducePdf() {
        when(onlineHearingService.getCcdCase(onlineHearingId))
                .thenReturn(Optional.of(createSscsCaseDetails()));

        byte[] pdf = {2, 4, 6, 0, 1};
        PdfCoverSheet pdfSummary = new PdfCoverSheet("12345", "firstname lastname", "line1", "line2",
                "town", "county", "postcode", englishTempateDetails.get(hmctsImgVal),
                welshTempateDetails.get(hmctsImgVal));
        when(pdfService.createPdf(pdfSummary, englishTempateDetails.get(template))).thenReturn(pdf);

        Optional<byte[]> pdfOptional =
                new CoversheetService(onlineHearingService, pdfService, ccdService, idamService, documentConfiguration).createCoverSheet(onlineHearingId);

        assertThat(pdfOptional.isPresent(), is(true));
        assertThat(pdfOptional.get(), is(pdf));
        verify(pdfService).createPdf(eq(pdfSummary), eq(englishTempateDetails.get(template)));
    }

    @Test
    public void canLoadWelsh_CcdCaseAndProducePdf() {
        when(onlineHearingService.getCcdCase(onlineHearingId))
                .thenReturn(Optional.of(createWelshSscsCaseDetails()));

        byte[] pdf = {2, 4, 6, 0, 1};
        PdfCoverSheet pdfSummary = new PdfCoverSheet("12345", "firstname lastname", "line1", "line2",
                "town", "county", "postcode", englishTempateDetails.get(hmctsImgVal),
                welshTempateDetails.get(hmctsImgVal));
        when(pdfService.createPdf(pdfSummary, welshTempateDetails.get(template))).thenReturn(pdf);

        Optional<byte[]> pdfOptional =
                new CoversheetService(onlineHearingService, pdfService, ccdService, idamService, documentConfiguration).createCoverSheet(onlineHearingId);

        assertThat(pdfOptional.isPresent(), is(true));
        assertThat(pdfOptional.get(), is(pdf));
        verify(pdfService).createPdf(eq(pdfSummary), eq(welshTempateDetails.get(template)));
    }

    @Test
    public void cannotLoadCcdCase() {
        when(onlineHearingService.getCcdCase(onlineHearingId)).thenReturn(Optional.empty());

        Optional<byte[]> pdfOptional =
                new CoversheetService(onlineHearingService, pdfService, ccdService, idamService, documentConfiguration).createCoverSheet(onlineHearingId);

        assertThat(pdfOptional.isPresent(), is(false));
    }

    @Test
    public void canLoadCcdCaseByCaseIdAndProducePdf() {
        when(ccdService.getByCaseId(caseId, idamTokens)).thenReturn(createSscsCaseDetails());

        byte[] pdf = {2, 4, 6, 0, 1};
        PdfCoverSheet pdfSummary = new PdfCoverSheet("12345", "firstname lastname", "line1", "line2",
                "town", "county", "postcode", englishTempateDetails.get(hmctsImgVal), welshTempateDetails.get(hmctsImgVal));
        when(pdfService.createPdf(pdfSummary, englishTempateDetails.get(template))).thenReturn(pdf);

        Optional<byte[]> pdfOptional =
                new CoversheetService(onlineHearingService, pdfService,  ccdService, idamService, documentConfiguration).createCoverSheet(caseId + "");

        assertThat(pdfOptional.isPresent(), is(true));
        assertThat(pdfOptional.get(), is(pdf));
        verify(pdfService).createPdf(eq(pdfSummary), eq(englishTempateDetails.get(template)));
    }

    @Test
    public void canLoadWelsh_CcdCaseByCaseIdAndProducePdf() {
        when(ccdService.getByCaseId(caseId, idamTokens)).thenReturn(createWelshSscsCaseDetails());

        byte[] pdf = {2, 4, 6, 0, 1};
        PdfCoverSheet pdfSummary = new PdfCoverSheet("12345", "firstname lastname", "line1", "line2",
                "town", "county", "postcode", englishTempateDetails.get(hmctsImgVal), welshTempateDetails.get(hmctsImgVal));
        when(pdfService.createPdf(pdfSummary, welshTempateDetails.get(template))).thenReturn(pdf);

        Optional<byte[]> pdfOptional =
                new CoversheetService(onlineHearingService, pdfService,  ccdService, idamService, documentConfiguration).createCoverSheet(caseId + "");

        assertThat(pdfOptional.isPresent(), is(true));
        assertThat(pdfOptional.get(), is(pdf));
        verify(pdfService).createPdf(eq(pdfSummary), eq(welshTempateDetails.get(template)));
    }

    @Test
    public void cannotLoadCcdCaseByCaseId() {
        when(ccdService.getByCaseId(caseId, idamTokens)).thenReturn(null);

        Optional<byte[]> pdfOptional =
                new CoversheetService(onlineHearingService, pdfService,  ccdService, idamService, documentConfiguration).createCoverSheet(onlineHearingId);

        assertThat(pdfOptional.isPresent(), is(false));
    }

    private SscsCaseDetails createSscsCaseDetails() {
        return SscsCaseDetails.builder()
                .id(12345L)
                .data(getSscsCaseDataBuiler()
                        .build())
                .build();
    }

    private SscsCaseDetails createWelshSscsCaseDetails() {
        return SscsCaseDetails.builder()
                .id(12345L)
                .data(getSscsCaseDataBuiler()
                        .languagePreferenceWelsh("Yes")
                        .build())
                .build();
    }

    private SscsCaseData.SscsCaseDataBuilder getSscsCaseDataBuiler() {
        return SscsCaseData.builder()
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
                        .build());
    }
}
