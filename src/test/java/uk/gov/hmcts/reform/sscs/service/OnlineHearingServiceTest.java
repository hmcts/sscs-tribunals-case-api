package uk.gov.hmcts.reform.sscs.service;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.getMaskedEmail;

import ch.qos.logback.classic.Level;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.Contact;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.JointParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.MrnDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscription;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscriptions;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.domain.wrapper.AddressDetails;
import uk.gov.hmcts.reform.sscs.domain.wrapper.AppealDetails;
import uk.gov.hmcts.reform.sscs.domain.wrapper.HearingArrangements;
import uk.gov.hmcts.reform.sscs.domain.wrapper.OnlineHearing;
import uk.gov.hmcts.reform.sscs.domain.wrapper.UserDetails;
import uk.gov.hmcts.reform.sscs.domain.wrapper.UserType;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.util.LogCaptureExtension;

@ExtendWith(MockitoExtension.class)
public class OnlineHearingServiceTest {
    private CcdService ccdService;

    private OnlineHearingService underTest;

    private Long someCaseId;
    private IdamTokens idamTokens;
    private IdamService idamService;

    @RegisterExtension
    private final LogCaptureExtension logCapture =
            new LogCaptureExtension(OnlineHearingService.class);

    @BeforeEach
    void setUp() {
        ccdService = mock(CcdService.class);
        idamTokens = IdamTokens.builder().build();
        idamService = mock(IdamService.class);

        underTest = new OnlineHearingService(ccdService, idamService);

        someCaseId = 1234321L;
    }

    @Test
    void getsACcdCaseByIdentifierFromCcdId() {
        SscsCaseDetails caseDetails = createCaseDetails(someCaseId, "someCaseReference", "firstName", "lastName");
        when(idamService.getIdamTokens()).thenReturn(idamTokens);
        when(ccdService.getByCaseId(someCaseId, idamTokens)).thenReturn(caseDetails);

        Optional<SscsCaseDetails> sscsCaseDetails = underTest.getCcdCaseByIdentifier(someCaseId.toString());

        assertThat(sscsCaseDetails.isPresent()).isTrue();
        assertThat(sscsCaseDetails.get()).isSameAs(caseDetails);
    }

    @Test
    void loadHearingWithoutCorCase() {
        SscsCaseDetails sscsCaseDetails = createCaseDetails(someCaseId, "caseref", "firstname", "lastname", "paper");

        Optional<OnlineHearing> onlineHearing = underTest.loadHearing(sscsCaseDetails, null, "appellant@hmct.com");

        assertOnlineHearing(onlineHearing, sscsCaseDetails, "firstname lastname");
    }

    @ParameterizedTest
    @CsvSource({"appellantTya,appellant@hmct.com", "appointeeTya, appointee@hmct.com"})
    void loadHearingWithTyaAndEmailForAppellant(String tya, String email) {
        SscsCaseDetails sscsCaseDetails = createCaseDetails(someCaseId, "caseref", "firstname", "lastname", "paper");

        Optional<OnlineHearing> onlineHearing = underTest.loadHearing(sscsCaseDetails, tya, email);

        assertOnlineHearingForAppellant(onlineHearing, sscsCaseDetails);
        logCapture.assertLogContains(getMaskedEmail(email), Level.INFO);
        logCapture.assertLogDoesNotContain(email, Level.INFO);
    }

    @Test
    void loadHearingWithTyaAndEmailForJointParty() {
        SscsCaseDetails sscsCaseDetails = createCaseDetails(someCaseId, "caseref", "firstname", "lastname", "paper");

        Optional<OnlineHearing> onlineHearing = underTest.loadHearing(sscsCaseDetails, "jointPartyTya", "jointparty@hmcts.com");

        assertOnlineHearingForJointParty(onlineHearing, sscsCaseDetails);
        logCapture.assertLogContains(getMaskedEmail("jointparty@hmcts.com"), Level.INFO);
        logCapture.assertLogDoesNotContain("jointparty@hmcts.com", Level.INFO);
    }

    @ParameterizedTest
    @CsvSource({"otherpartyTya,otherparty@hmct.com", "otherpartyAppointeeTya,otherpartyAppointee@hmct.com"})
    void loadHearingWithTyaAndEmailForOtherParty(String tya, String email) {
        SscsCaseDetails sscsCaseDetails = createCaseDetails(someCaseId, "caseref", "firstname", "lastname", "paper");

        Optional<OnlineHearing> onlineHearing = underTest.loadHearing(sscsCaseDetails, tya, email);

        assertOnlineHearingForOtherParty(onlineHearing, sscsCaseDetails);
        logCapture.assertLogContains(getMaskedEmail(email), Level.INFO);
        logCapture.assertLogDoesNotContain(email, Level.INFO);
    }

    private void assertOnlineHearingForOtherParty(Optional<OnlineHearing> onlineHearing, SscsCaseDetails sscsCaseDetails) {
        assertThat(onlineHearing.isPresent()).isTrue();
        assertThat(onlineHearing.get()).isEqualTo((new OnlineHearing(
                "firstname lastname",
                "caseref",
                1234321L,
                new HearingArrangements(
                        true,
                        "french",
                        true,
                        "BSL",
                        true,
                        true,
                        "other arrangements"
                ),
                new UserDetails(UserType.OTHER_PARTY.getType(), "Other Party",
                        new AddressDetails("other","street","other town", "UK","other"),
                        "other@hmct.com", "999", "777",
                        List.of(new uk.gov.hmcts.reform.sscs.domain.wrapper.Subscription(UserType.OTHER_PARTY.getType(), "otherparty@hmct.com", "777"),
                                new uk.gov.hmcts.reform.sscs.domain.wrapper.Subscription(UserType.OTHER_PARTY_APPOINTEE.getType(), "otherpartyAppointee@hmct.com", "888"))),
                new AppealDetails(sscsCaseDetails.getData().getCaseCreated(), "12-12-2019", "PIP", State.HEARING.getId())
        )));
        assertThat(onlineHearing.get().getUserDetails().getSubscriptions()).contains(new uk.gov.hmcts.reform.sscs.domain.wrapper.Subscription(UserType.OTHER_PARTY.getType(), "otherparty@hmct.com", "777"),
                        new uk.gov.hmcts.reform.sscs.domain.wrapper.Subscription(UserType.OTHER_PARTY_APPOINTEE.getType(), "otherpartyAppointee@hmct.com", "888"));
    }

    private void assertOnlineHearingForJointParty(Optional<OnlineHearing> onlineHearing, SscsCaseDetails sscsCaseDetails) {
        assertThat(onlineHearing.isPresent()).isTrue();
        assertThat(onlineHearing.get()).isEqualTo(new OnlineHearing(
                "firstname lastname",
                "caseref",
                1234321L,
                new HearingArrangements(
                        true,
                        "french",
                        true,
                        "BSL",
                        true,
                        true,
                        "other arrangements"
                ),
                new UserDetails(UserType.JOINT_PARTY.getType(), "joint party", new AddressDetails("joint","street","joint town", "UK","joint"),
                        "joint@hmcts.com", "007", "120", List.of()),
                new AppealDetails(sscsCaseDetails.getData().getCaseCreated(), "12-12-2019", "PIP", State.HEARING.getId())
        ));
        assertThat(onlineHearing.get().getUserDetails().getSubscriptions()).isEqualTo(List.of(
                new uk.gov.hmcts.reform.sscs.domain.wrapper.Subscription(UserType.JOINT_PARTY.getType(), "jointparty@hmcts.com", "555")));
    }

    private void assertOnlineHearingForAppellant(Optional<OnlineHearing> onlineHearing, SscsCaseDetails sscsCaseDetails) {
        assertOnlineHearing(onlineHearing, sscsCaseDetails, "firstname lastname");
        assertThat(onlineHearing.get().getUserDetails().getSubscriptions()).containsExactlyInAnyOrder(
                new uk.gov.hmcts.reform.sscs.domain.wrapper.Subscription(UserType.APPELLANT.getType(), "appellant@hmct.com", "444"),
                new uk.gov.hmcts.reform.sscs.domain.wrapper.Subscription(UserType.APPOINTEE.getType(), "appointee@hmct.com", "333")
        );
    }

    private void assertOnlineHearing(Optional<OnlineHearing> onlineHearing, SscsCaseDetails sscsCaseDetails, String name) {
        assertThat(onlineHearing.isPresent()).isTrue();
        assertThat(onlineHearing.get()).isEqualTo(new OnlineHearing(
                "firstname lastname",
                "caseref",
                1234321L,
                new HearingArrangements(
                        true,
                        "french",
                        true,
                        "BSL",
                        true,
                        true,
                        "other arrangements"
                ),
                new UserDetails(UserType.APPELLANT.getType(), name, new AddressDetails("line1","line2","town", "county","postcode"),
                        "email", "012", "120", List.of()),
                new AppealDetails(sscsCaseDetails.getData().getCaseCreated(), "12-12-2019", "PIP", State.HEARING.getId())
        ));
    }

    private SscsCaseDetails createCaseDetails(Long caseId, String expectedCaseReference, String firstName, String lastName) {
        return createCaseDetails(caseId, expectedCaseReference, firstName, lastName, "cor");
    }

    private SscsCaseDetails createCaseDetails(Long caseId, String expectedCaseReference, String firstName, String lastName, String hearingType) {
        return SscsCaseDetails.builder()
                .id(caseId)
                .createdDate(LocalDateTime.now())
                .state(State.HEARING.getId())
                .data(SscsCaseData.builder()
                        .caseReference(expectedCaseReference)
                        .caseCreated(LocalDateTime.now().toString())
                        .subscriptions(Subscriptions.builder()
                                .appellantSubscription(Subscription.builder()
                                        .email("appellant@hmct.com")
                                        .mobile("444")
                                        .tya("appellantTya")
                                        .build())
                                .appointeeSubscription(Subscription.builder()
                                        .email("appointee@hmct.com")
                                        .mobile("333")
                                        .tya("appointeeTya")
                                        .build())
                                .jointPartySubscription(Subscription.builder()
                                        .email("jointparty@hmcts.com")
                                        .mobile("555")
                                        .tya("jointPartyTya")
                                        .build())
                                .build())
                        .appeal(Appeal.builder()
                                .hearingType(hearingType)
                                .appellant(Appellant.builder()
                                        .name(Name.builder()
                                                .firstName(firstName)
                                                .lastName(lastName)
                                                .build()
                                        )
                                        .address(Address.builder()
                                                .line1("line1")
                                                .line2("line2")
                                                .town("town")
                                                .county("county")
                                                .postcode("postcode")
                                                .build())
                                        .contact(Contact.builder()
                                                .email("email")
                                                .phone("012")
                                                .mobile("120")
                                                .build())
                                        .build()
                                )
                                .mrnDetails(MrnDetails.builder()
                                        .mrnDate("12-12-2019")
                                        .build())
                                .benefitType(BenefitType.builder()
                                        .code("PIP")
                                        .build())
                                .hearingOptions(HearingOptions.builder()
                                        .languageInterpreter("yes")
                                        .languages("french")
                                        .arrangements(asList("signLanguageInterpreter", "hearingLoop", "disabledAccess"))
                                        .signLanguageType("BSL")
                                        .other("other arrangements")
                                        .build())
                                .build()
                        )
                        .jointParty(JointParty.builder()
                                .name(Name.builder()
                                        .firstName("joint")
                                        .lastName("party")
                                        .build())
                                .address(Address.builder()
                                        .line1("joint")
                                        .line2("street")
                                        .town("joint town")
                                        .county("UK")
                                        .postcode("joint")
                                        .build())
                                .contact(Contact.builder()
                                        .email("joint@hmcts.com")
                                        .phone("007")
                                        .mobile("120")
                                        .build())
                                .hasJointParty(YES)
                                .build())
                        .otherParties(List.of(
                                CcdValue.<OtherParty>builder()
                                        .value(OtherParty.builder()
                                                .otherPartySubscription(Subscription.builder()
                                                        .email("firstOtherparty@hmct.com")
                                                        .tya("firstOtherpartyTya")
                                                        .build())
                                                .build())
                                        .build(),
                                CcdValue.<OtherParty>builder()
                                        .value(OtherParty.builder()
                                                .name(Name.builder()
                                                        .firstName("Other")
                                                        .lastName("Party")
                                                        .build())
                                                .address(Address.builder()
                                                        .line1("other")
                                                        .line2("street")
                                                        .town("other town")
                                                        .county("UK")
                                                        .postcode("other")
                                                        .build())
                                                .contact(Contact.builder()
                                                        .email("other@hmct.com")
                                                        .phone("999")
                                                        .mobile("777")
                                                        .build())
                                                .otherPartySubscription(Subscription.builder()
                                                        .email("otherparty@hmct.com")
                                                        .mobile("777")
                                                        .tya("otherpartyTya")
                                                        .build())
                                                .otherPartyAppointeeSubscription(Subscription.builder()
                                                        .email("otherpartyAppointee@hmct.com")
                                                        .mobile("888")
                                                        .tya("otherpartyAppointeeTya")
                                                        .build())
                                                .build())
                                .build()))
                        .decisionNotes("decision notes")
                        .events(new ArrayList<>())
                        .build()
                ).build();
    }
}
