package uk.gov.hmcts.reform.sscs.tyanotifications.service.docmosis;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.UPDATE_OTHER_PARTY_DATA;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.AppealReasons;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.Contact;
import uk.gov.hmcts.reform.sscs.ccd.domain.Event;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.Identity;
import uk.gov.hmcts.reform.sscs.ccd.domain.LanguagePreference;
import uk.gov.hmcts.reform.sscs.ccd.domain.MrnDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.Representative;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscription;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.AppealHearingType;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.DocmosisTemplatesConfig;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.SubscriptionType;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.properties.EvidenceProperties;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.NotificationSscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.SubscriptionWithType;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.docmosis.PdfCoverSheet;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.factory.CcdNotificationWrapper;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.factory.NotificationWrapper;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.service.docmosis.PdfLetterService;
import uk.gov.hmcts.reform.sscs.thirdparty.pdfservice.DocmosisPdfService;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("integration")
@AutoConfigureMockMvc
@Slf4j
public class PdfLetterServiceIt {
    private static final String CASE_ID = "1000001";
    private static final String DATE = "2018-01-01T14:01:18.243";
    private static final String YES = "Yes";
    private static final Subscription EMPTY_SUBSCRIPTION = Subscription.builder().build();

    @Autowired
    private PdfLetterService pdfLetterService;

    @Autowired
    private DocmosisTemplatesConfig docmosisTemplatesConfig;

    @MockitoBean
    private DocmosisPdfService docmosisPdfService;

    @Autowired
    private EvidenceProperties evidenceProperties;

    @Test
    public void canGenerateACoversheetOnAppealReceived() throws IOException {
        byte[] pdfbytes = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream(
                "pdf/direction-notice-coversheet-sample.pdf"));
        when(docmosisPdfService.createPdf(any(Object.class), anyString())).thenReturn(pdfbytes);
        SscsCaseData sscsCaseData = getSscsCaseData();
        NotificationSscsCaseDataWrapper dataWrapper = NotificationSscsCaseDataWrapper.builder()
                .newSscsCaseData(sscsCaseData)
                .oldSscsCaseData(sscsCaseData)
                .notificationEventType(NotificationEventType.APPEAL_RECEIVED)
                .build();
        NotificationWrapper wrapper = new CcdNotificationWrapper(dataWrapper);
        byte[] bytes = pdfLetterService.buildCoversheet(wrapper, new SubscriptionWithType(EMPTY_SUBSCRIPTION,
            SubscriptionType.APPELLANT, sscsCaseData.getAppeal().getAppellant(), sscsCaseData.getAppeal().getAppellant()));
        assertNotNull(bytes);
        PdfCoverSheet pdfCoverSheet = new PdfCoverSheet(
                wrapper.getCaseId(),
                wrapper.getNewSscsCaseData().getAppeal().getAppellant().getName().getFullNameNoTitle(),
                wrapper.getNewSscsCaseData().getAppeal().getAppellant().getAddress().getLine1(),
                wrapper.getNewSscsCaseData().getAppeal().getAppellant().getAddress().getLine2(),
                wrapper.getNewSscsCaseData().getAppeal().getAppellant().getAddress().getTown(),
                wrapper.getNewSscsCaseData().getAppeal().getAppellant().getAddress().getCounty(),
                wrapper.getNewSscsCaseData().getAppeal().getAppellant().getAddress().getPostcode(),
                evidenceProperties.getAddress().getLine2(),
                evidenceProperties.getAddress().getLine3(),
                evidenceProperties.getAddress().getTown(),
                evidenceProperties.getAddress().getPostcode(),
                docmosisTemplatesConfig.getHmctsImgVal(),
                docmosisTemplatesConfig.getHmctsWelshImgVal());

        verify(docmosisPdfService).createPdf(eq(pdfCoverSheet), eq(docmosisTemplatesConfig.getCoversheets()
                .get(LanguagePreference.ENGLISH).get(APPEAL_RECEIVED.getType())));
    }

    @Test
    public void canGenerateACoversheetOnUpdateOtherPartyData() throws IOException {
        byte[] pdfbytes = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream(
                "pdf/direction-notice-coversheet-sample.pdf"));
        when(docmosisPdfService.createPdf(any(Object.class), anyString())).thenReturn(pdfbytes);
        SscsCaseData sscsCaseData = getSscsCaseData();
        NotificationSscsCaseDataWrapper dataWrapper = NotificationSscsCaseDataWrapper.builder()
                .newSscsCaseData(sscsCaseData)
                .oldSscsCaseData(sscsCaseData)
                .notificationEventType(NotificationEventType.UPDATE_OTHER_PARTY_DATA)
                .build();
        NotificationWrapper wrapper = new CcdNotificationWrapper(dataWrapper);
        byte[] bytes = pdfLetterService.buildCoversheet(wrapper, new SubscriptionWithType(EMPTY_SUBSCRIPTION,
            SubscriptionType.OTHER_PARTY, sscsCaseData.getOtherParties().get(0).getValue(),
            sscsCaseData.getOtherParties().get(0).getValue(), "1"));
        assertNotNull(bytes);
        PdfCoverSheet pdfCoverSheet = new PdfCoverSheet(
                wrapper.getCaseId(),
                wrapper.getNewSscsCaseData().getOtherParties().get(0).getValue().getName().getFullNameNoTitle(),
                wrapper.getNewSscsCaseData().getOtherParties().get(0).getValue().getAddress().getLine1(),
                wrapper.getNewSscsCaseData().getOtherParties().get(0).getValue().getAddress().getLine2(),
                wrapper.getNewSscsCaseData().getOtherParties().get(0).getValue().getAddress().getTown(),
                wrapper.getNewSscsCaseData().getOtherParties().get(0).getValue().getAddress().getCounty(),
                wrapper.getNewSscsCaseData().getOtherParties().get(0).getValue().getAddress().getPostcode(),
                evidenceProperties.getAddress().getLine2(),
                evidenceProperties.getAddress().getLine3(),
                evidenceProperties.getAddress().getTown(),
                evidenceProperties.getAddress().getPostcode(),
                docmosisTemplatesConfig.getHmctsImgVal(),
                docmosisTemplatesConfig.getHmctsWelshImgVal());

        verify(docmosisPdfService).createPdf(eq(pdfCoverSheet), eq(docmosisTemplatesConfig.getCoversheets()
                .get(LanguagePreference.ENGLISH).get(UPDATE_OTHER_PARTY_DATA.getType())));
    }

    @Test
    public void willNotGenerateACoversheetOnAppealDormant() {

        SscsCaseData sscsCaseData = getSscsCaseData();
        NotificationSscsCaseDataWrapper dataWrapper = NotificationSscsCaseDataWrapper.builder()
                .newSscsCaseData(sscsCaseData)
                .oldSscsCaseData(sscsCaseData)
                .notificationEventType(NotificationEventType.APPEAL_DORMANT)
                .build();
        NotificationWrapper wrapper = new CcdNotificationWrapper(dataWrapper);
        pdfLetterService.buildCoversheet(wrapper, new SubscriptionWithType(EMPTY_SUBSCRIPTION,
            SubscriptionType.APPELLANT, sscsCaseData.getAppeal().getAppellant(), sscsCaseData.getAppeal().getAppellant()));
        verifyNoInteractions(docmosisPdfService);
    }

    private SscsCaseData getSscsCaseData() {
        List<Event> events = new ArrayList<>();
        events.add(Event.builder().value(EventDetails.builder().date(DATE).type(APPEAL_RECEIVED.getCcdType()).build()).build());

        return SscsCaseData.builder().ccdCaseId(CASE_ID).events(events)
                .appeal(Appeal.builder()
                        .mrnDetails(MrnDetails.builder().mrnDate(DATE).dwpIssuingOffice("office").build())
                        .appealReasons(AppealReasons.builder().build())
                        .rep(Representative.builder()
                                .hasRepresentative(YES)
                                .name(Name.builder().firstName("Rep").lastName("lastName").build())
                                .contact(Contact.builder().build())
                                .address(Address.builder().line1("Rep Line 1").town("Rep Town").county("Rep County").postcode("RE9 7SE").build())
                                .build())
                        .appellant(Appellant.builder()
                                .name(Name.builder().firstName("firstName").lastName("lastName").build())
                                .address(Address.builder().line1("122 Breach Street").line2("The Village").town("My town").county("Cardiff").postcode("CF11 2HB").build())
                                .contact(Contact.builder().build())
                                .identity(Identity.builder().nino("NP 27 28 67 B").dob("12 March 1971").build()).build())
                        .hearingType(AppealHearingType.ORAL.name())
                        .benefitType(BenefitType.builder().code(Benefit.PIP.name()).build())
                        .hearingOptions(HearingOptions.builder()
                                .wantsToAttend(YES)
                                .build())
                        .build())
                .otherParties(List.of(CcdValue.<OtherParty>builder()
                                .value(OtherParty.builder()
                                        .id("1")
                                        .sendNewOtherPartyNotification(YesNo.YES)
                                        .name(Name.builder().firstName("Other").lastName("Party").build())
                                        .address(Address.builder().line1("122 Breach Street").line2("The Village").town("My town").county("Cardiff").postcode("CF11 2HB").build())
                                        .build())
                        .build()))
                .build();
    }

}
