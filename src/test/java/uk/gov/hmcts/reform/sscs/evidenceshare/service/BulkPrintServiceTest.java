package uk.gov.hmcts.reform.sscs.evidenceshare.service;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ISSUE_FURTHER_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.evidenceshare.domain.FurtherEvidenceLetterType.APPELLANT_LETTER;

import java.util.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import uk.gov.hmcts.reform.sendletter.api.LetterWithPdfsRequest;
import uk.gov.hmcts.reform.sendletter.api.SendLetterApi;
import uk.gov.hmcts.reform.sendletter.api.SendLetterResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;
import uk.gov.hmcts.reform.sscs.evidenceshare.exception.BulkPrintException;
import uk.gov.hmcts.reform.sscs.evidenceshare.exception.NonPdfBulkPrintException;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

@RunWith(MockitoJUnitRunner.class)
public class BulkPrintServiceTest {

    private static final List<Pdf> PDF_LIST = singletonList(new Pdf("myData".getBytes(), "file.pdf"));
    private static final UUID LETTER_ID = UUID.randomUUID();

    private static final SscsCaseData SSCS_CASE_DATA = SscsCaseData.builder()
        .ccdCaseId("234")
        .appeal(Appeal.builder().appellant(
            Appellant.builder()
                .name(Name.builder().firstName("Appellant").lastName("LastName").build())
                .address(Address.builder().line1("line1").build())
                .build())
            .build())
        .build();
    private static final String AUTH_TOKEN = "Auth_Token";

    private BulkPrintService bulkPrintService;
    @Mock
    private SendLetterApi sendLetterApi;
    @Mock
    private IdamService idamService;
    @Mock
    private BulkPrintServiceHelper bulkPrintServiceHelper;

    @Mock
    private CcdNotificationService ccdNotificationService;

    @Captor
    ArgumentCaptor<LetterWithPdfsRequest> captor;

    @Before
    public void setUp() {
        this.bulkPrintService = new BulkPrintService(sendLetterApi, idamService, bulkPrintServiceHelper,
            true, 1, ccdNotificationService);
        when(idamService.generateServiceAuthorization()).thenReturn(AUTH_TOKEN);
    }

    @Test
    public void willSendToBulkPrint() {
        when(sendLetterApi.sendLetter(eq(AUTH_TOKEN), captor.capture()))
            .thenReturn(new SendLetterResponse(LETTER_ID));
        Optional<UUID> letterIdOptional = bulkPrintService.sendToBulkPrint(PDF_LIST, SSCS_CASE_DATA, null);
        assertEquals("letterIds must be equal", Optional.of(LETTER_ID), letterIdOptional);
        assertEquals("sscs-data-pack", captor.getValue().getAdditionalData().get("letterType"));
        assertEquals("Appellant LastName", captor.getValue().getAdditionalData().get("appellantName"));
        assertEquals("234", captor.getValue().getAdditionalData().get("caseIdentifier"));
    }

    @Test
    public void willSendToBulkPrintWithAdditionalData() {
        when(sendLetterApi.sendLetter(eq(AUTH_TOKEN), any(LetterWithPdfsRequest.class)))
            .thenReturn(new SendLetterResponse(LETTER_ID));
        Optional<UUID> letterIdOptional = bulkPrintService.sendToBulkPrint(PDF_LIST, SSCS_CASE_DATA, null);
        assertEquals("letterIds must be equal", Optional.of(LETTER_ID), letterIdOptional);
    }

    @Test
    public void willSendToBulkPrintWithAdditionalDataThatIncludesRecipients() {
        when(sendLetterApi.sendLetter(eq(AUTH_TOKEN), captor.capture())).thenReturn(new SendLetterResponse(LETTER_ID));
        Name name1 = Name.builder().firstName("Barry").lastName("Allen").build();
        Address address1 = Address.builder().line1("line1").build();
        Appointee appointee1 = Appointee.builder()
            .name(name1)
            .address(address1)
            .build();

        Appellant appellant = SSCS_CASE_DATA.getAppeal().getAppellant();
        appellant.setAppointee(appointee1);
        appellant.setIsAppointee("Yes");

        Name name2 = Name.builder().firstName("Jay").lastName("Garrick").build();
        JointParty jointParty = JointParty.builder()
            .hasJointParty(YES)
            .name(name2)
            .build();
        SSCS_CASE_DATA.setJointParty(jointParty);

        Name name3 = Name.builder().firstName("Wally").lastName("West").build();
        Representative representative = Representative.builder()
            .hasRepresentative("YES")
            .name(name3)
            .build();
        SSCS_CASE_DATA.getAppeal().setRep(representative);

        Name name4 = Name.builder().firstName("Hunter").lastName("Zolomon").build();
        CcdValue<OtherParty> otherParty1 = CcdValue.<OtherParty>builder()
            .value(OtherParty.builder()
                .id("1")
                .name(name4)
                .isAppointee(NO.getValue())
                .build())
            .build();

        Name name5 = Name.builder().firstName("Jessie").lastName("Quick").build();
        Name name6 = Name.builder().firstName("Max").lastName("Mercury").build();
        Representative representative1 = Representative.builder()
            .hasRepresentative("YES")
            .name(name6)
            .build();
        CcdValue<OtherParty> otherParty2 = CcdValue.<OtherParty>builder()
            .value(OtherParty.builder()
                .id("2")
                .name(name5)
                .isAppointee(NO.getValue())
                .rep(representative1)
                .build())
            .build();

        Name name7 = Name.builder().firstName("Caitlin").lastName("Snow").build();
        Name name8 = Name.builder().firstName("Cisco").lastName("Ramone").build();
        Appointee appointee2 = Appointee.builder()
            .name(name8)
            .address(address1)
            .build();
        CcdValue<OtherParty> otherParty3 = CcdValue.<OtherParty>builder()
            .value(OtherParty.builder()
                .id("3")
                .name(name7)
                .isAppointee(YES.getValue())
                .appointee(appointee2)
                .build())
            .build();

        Name name9 = Name.builder().firstName("Harrison").lastName("Wells").build();
        Name name10 = Name.builder().firstName("Eddie").lastName("Thawne").build();
        Name name11 = Name.builder().firstName("Eobard").lastName("Thawne").build();
        Representative representative2 = Representative.builder()
            .hasRepresentative("YES")
            .name(name10)
            .build();
        Appointee appointee3 = Appointee.builder()
            .name(name11)
            .address(address1)
            .build();
        CcdValue<OtherParty> otherParty4 = CcdValue.<OtherParty>builder()
            .value(OtherParty.builder()
                .id("4")
                .name(name9)
                .rep(representative2)
                .isAppointee(YES.getValue())
                .appointee(appointee3)
                .build())
            .build();

        SSCS_CASE_DATA.setOtherParties(Arrays.asList(otherParty1, otherParty2, otherParty3, otherParty4));
        bulkPrintService.sendToBulkPrint(PDF_LIST, SSCS_CASE_DATA, "Appellant LastName");

        List<String> parties = new ArrayList<>();
        parties.add("Appellant LastName");
        assertEquals(parties, captor.getValue().getAdditionalData().get("recipients"));
    }

    @Test(expected = BulkPrintException.class)
    public void willThrowAnyExceptionsToBulkPrint() {
        when(sendLetterApi.sendLetter(eq(AUTH_TOKEN), any(LetterWithPdfsRequest.class)))
            .thenThrow(new RuntimeException("error"));
        bulkPrintService.sendToBulkPrint(PDF_LIST, SSCS_CASE_DATA, null);
    }

    @Test(expected = NonPdfBulkPrintException.class)
    public void shouldThrowANonPdfBulkPrintExceptionOnHttpClientErrorExceptionFromBulkPrint() {
        when(sendLetterApi.sendLetter(eq(AUTH_TOKEN), any(LetterWithPdfsRequest.class)))
            .thenThrow(new HttpClientErrorException(HttpStatus.valueOf(400)));
        bulkPrintService.sendToBulkPrint(PDF_LIST, SSCS_CASE_DATA, null);
    }

    @Test
    public void sendLetterNotEnabledWillNotSendToBulkPrint() {
        BulkPrintService notEnabledBulkPrint = new BulkPrintService(sendLetterApi, idamService, bulkPrintServiceHelper, false, 1, ccdNotificationService);
        notEnabledBulkPrint.sendToBulkPrint(PDF_LIST, SSCS_CASE_DATA, null);
        verifyNoInteractions(idamService);
        verifyNoInteractions(sendLetterApi);
    }

    @Test
    public void willSendToBulkPrintWithReasonableAdjustment() {
        this.bulkPrintService = new BulkPrintService(sendLetterApi, idamService, bulkPrintServiceHelper, true, 1, ccdNotificationService);

        SSCS_CASE_DATA.setReasonableAdjustments(ReasonableAdjustments.builder()
            .appellant(ReasonableAdjustmentDetails.builder()
                .wantsReasonableAdjustment(YesNo.YES).reasonableAdjustmentRequirements("Big text")
                .build()).build());

        when(bulkPrintServiceHelper.sendForReasonableAdjustment(SSCS_CASE_DATA, APPELLANT_LETTER)).thenReturn(true);
        bulkPrintService.sendToBulkPrint(PDF_LIST, SSCS_CASE_DATA, APPELLANT_LETTER, ISSUE_FURTHER_EVIDENCE, null);

        verify(bulkPrintServiceHelper).saveAsReasonableAdjustment(any(), any(), any());
    }
}
