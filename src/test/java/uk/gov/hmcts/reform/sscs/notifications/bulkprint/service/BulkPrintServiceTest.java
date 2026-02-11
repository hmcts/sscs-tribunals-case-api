package uk.gov.hmcts.reform.sscs.notifications.bulkprint.service;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.INFECTED_BLOOD_COMPENSATION;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.PIP;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ISSUE_FURTHER_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.domain.FurtherEvidenceLetterType.APPELLANT_LETTER;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import uk.gov.hmcts.reform.sendletter.api.LetterWithPdfsRequest;
import uk.gov.hmcts.reform.sendletter.api.SendLetterApi;
import uk.gov.hmcts.reform.sendletter.api.SendLetterResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appointee;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.JointParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.ReasonableAdjustmentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.ReasonableAdjustments;
import uk.gov.hmcts.reform.sscs.ccd.domain.Representative;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.UkPortOfEntry;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.thirdparty.docmosis.domain.Pdf;
import uk.gov.hmcts.reform.sscs.notifications.bulkprint.exception.BulkPrintException;
import uk.gov.hmcts.reform.sscs.notifications.bulkprint.exception.NonPdfBulkPrintException;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.BulkPrintService;
import uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.BulkPrintServiceHelper;
import uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.CcdNotificationService;

@ExtendWith(MockitoExtension.class)
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

    @BeforeEach
    public void setUp() {
        this.bulkPrintService = new BulkPrintService(sendLetterApi, idamService, bulkPrintServiceHelper,
                true, 1, ccdNotificationService);
        lenient().when(idamService.generateServiceAuthorization()).thenReturn(AUTH_TOKEN);
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

    @Test
    public void willThrowAnyExceptionsToBulkPrint() {
        when(sendLetterApi.sendLetter(eq(AUTH_TOKEN), any(LetterWithPdfsRequest.class)))
                .thenThrow(new RuntimeException("error"));

        assertThrows(BulkPrintException.class, () -> {
            bulkPrintService.sendToBulkPrint(PDF_LIST, SSCS_CASE_DATA, null);
        });
    }

    @Test
    public void shouldThrowANonPdfBulkPrintExceptionOnHttpClientErrorExceptionFromBulkPrint() {
        when(sendLetterApi.sendLetter(eq(AUTH_TOKEN), any(LetterWithPdfsRequest.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.valueOf(400)));

        assertThrows(NonPdfBulkPrintException.class, () -> {
            bulkPrintService.sendToBulkPrint(PDF_LIST, SSCS_CASE_DATA, null);
        });
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

    @Test
    public void shouldSendToBulkPrint_noAdditionalDataInternationalFlag() {
        SscsCaseData sscsCaseDataNonUK = SscsCaseData.builder()
                .ccdCaseId("234")
                .appeal(
                        Appeal.builder()
                                .appellant(
                                        Appellant.builder()
                                                .name(Name.builder().firstName("Appellant").lastName("LastName").build())
                                                .address(Address.builder().line1("line1").postcode("PO1 1AY").country("United Kingdom").inMainlandUk(YES).build())
                                                .build())
                                .build())
                .build();

        when(sendLetterApi.sendLetter(eq(AUTH_TOKEN), captor.capture()))
                .thenReturn(new SendLetterResponse(LETTER_ID));
        Optional<UUID> letterIdOptional = bulkPrintService.sendToBulkPrint(PDF_LIST, sscsCaseDataNonUK, null);

        assertEquals("letterIds must be equal", Optional.of(LETTER_ID), letterIdOptional);
        assertFalse("isInternational", captor.getValue().getAdditionalData().containsKey("isInternational"));
    }

    @Test
    public void shouldSendToBulkPrint_noAdditionalDataInternationalFlagAsInMainlandUkNull() {
        SscsCaseData sscsCaseDataNonUK = SscsCaseData.builder()
                .ccdCaseId("234")
                .appeal(
                        Appeal.builder()
                                .appellant(
                                        Appellant.builder()
                                                .name(Name.builder().firstName("Appellant").lastName("LastName").build())
                                                .address(Address.builder().line1("line1").postcode("PO1 1AY").country("United Kingdom").inMainlandUk(null).build())
                                                .build())
                                .build())
                .build();

        when(sendLetterApi.sendLetter(eq(AUTH_TOKEN), captor.capture()))
                .thenReturn(new SendLetterResponse(LETTER_ID));
        Optional<UUID> letterIdOptional = bulkPrintService.sendToBulkPrint(PDF_LIST, sscsCaseDataNonUK, null);

        assertEquals("letterIds must be equal", Optional.of(LETTER_ID), letterIdOptional);
        assertFalse("isInternational", captor.getValue().getAdditionalData().containsKey("isInternational"));
    }

    @Test
    public void shouldSendToBulkPrint_additionalDataInternationalFlagTrue() {
        SscsCaseData sscsCaseDataUK = SscsCaseData.builder()
                .ccdCaseId("234")
                .appeal(Appeal.builder().appellant(
                                Appellant.builder()
                                        .name(Name.builder().firstName("Appellant").lastName("LastName").build())
                                        .address(Address.builder().line1("line1").postcode("PH17-26").country("Australia").portOfEntry(UkPortOfEntry.LONDON_LUTON_AIRPORT.getLabel()).inMainlandUk(NO).build())
                                        .build())
                        .build())
                .build();

        when(sendLetterApi.sendLetter(eq(AUTH_TOKEN), captor.capture()))
                .thenReturn(new SendLetterResponse(LETTER_ID));

        Optional<UUID> letterIdOptional = bulkPrintService.sendToBulkPrint(PDF_LIST, sscsCaseDataUK, null);

        assertEquals("letterIds must be equal", Optional.of(LETTER_ID), letterIdOptional);
        assertEquals("true", captor.getValue().getAdditionalData().get("isInternational"));
    }

    @ParameterizedTest
    @MethodSource("benefitParameters")
    public void shouldSendToBulkPrint_additionalDataIsIbcaFlagFalse(Benefit benefit, String isIbca) {
        SscsCaseData sscsCaseDataUK = SscsCaseData.builder()
                .benefitCode(benefit.getBenefitCode())
                .ccdCaseId("234")
                .appeal(Appeal.builder().appellant(
                                Appellant.builder()
                                        .name(Name.builder().firstName("Appellant").lastName("LastName").build())
                                        .address(Address.builder().build())
                                        .build())
                        .build())
                .build();

        when(sendLetterApi.sendLetter(eq(AUTH_TOKEN), captor.capture()))
                .thenReturn(new SendLetterResponse(LETTER_ID));

        Optional<UUID> letterIdOptional = bulkPrintService.sendToBulkPrint(PDF_LIST, sscsCaseDataUK, null);

        assertEquals("letterIds must be equal", Optional.of(LETTER_ID), letterIdOptional);
        assertEquals(isIbca, captor.getValue().getAdditionalData().get("isIbca"));
    }

    public static Stream<Arguments> benefitParameters() {
        return Stream.of(
                Arguments.of(INFECTED_BLOOD_COMPENSATION, "true"),
                Arguments.of(PIP, "false")
        );
    }
}
