package uk.gov.hmcts.reform.sscs.service;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscription;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.domain.wrapper.*;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@RunWith(JUnitParamsRunner.class)
public class OnlineHearingServiceTest {
    private CcdService ccdService;

    private OnlineHearingService underTest;

    private Long someCaseId;
    private IdamTokens idamTokens;
    private IdamService idamService;

    @Before
    public void setUp() {
        ccdService = mock(CcdService.class);
        idamTokens = IdamTokens.builder().build();
        idamService = mock(IdamService.class);
        when(idamService.getIdamTokens()).thenReturn(idamTokens);

        underTest = new OnlineHearingService(ccdService, idamService);

        someCaseId = 1234321L;
    }

    @Test
    public void getsACcdCaseByIdentifierFromCcdId() {
        SscsCaseDetails caseDetails = createCaseDetails(someCaseId, "someCaseReference", "firstName", "lastName");
        when(ccdService.getByCaseId(someCaseId, idamTokens)).thenReturn(caseDetails);

        Optional<SscsCaseDetails> sscsCaseDetails = underTest.getCcdCaseByIdentifier(someCaseId.toString());

        assertThat(sscsCaseDetails.isPresent(), is(true));
        assertThat(sscsCaseDetails.get(), is(caseDetails));
    }

    @Test
    public void loadHearingWithoutCorCase() {
        SscsCaseDetails sscsCaseDetails = createCaseDetails(someCaseId, "caseref", "firstname", "lastname", "paper");

        Optional<OnlineHearing> onlineHearing = underTest.loadHearing(sscsCaseDetails, null, "appellant@hmct.com");

        assertOnlineHearing(onlineHearing, sscsCaseDetails, "firstname lastname");
    }

    @Test
    @Parameters({"appellantTya,appellant@hmct.com", "appointeeTya, appointee@hmct.com"})
    public void loadHearingWithTyaAndEmailForAppellant(String tya, String email) {
        SscsCaseDetails sscsCaseDetails = createCaseDetails(someCaseId, "caseref", "firstname", "lastname", "paper");

        Optional<OnlineHearing> onlineHearing = underTest.loadHearing(sscsCaseDetails, tya, email);

        assertOnlineHearingForAppellant(onlineHearing, sscsCaseDetails);
    }

    @Test
    @Parameters({"otherpartyTya,otherparty@hmct.com", "otherpartyAppointeeTya,otherpartyAppointee@hmct.com"})
    public void loadHearingWithTyaAndEmailForOtherParty(String tya, String email) {
        SscsCaseDetails sscsCaseDetails = createCaseDetails(someCaseId, "caseref", "firstname", "lastname", "paper");

        Optional<OnlineHearing> onlineHearing = underTest.loadHearing(sscsCaseDetails, tya, email);

        assertOnlineHearingForOtherParty(onlineHearing, sscsCaseDetails);
    }

    private void assertOnlineHearingForOtherParty(Optional<OnlineHearing> onlineHearing, SscsCaseDetails sscsCaseDetails) {
        assertThat(onlineHearing.isPresent(), is(true));
        assertThat(onlineHearing.get(), is(new OnlineHearing(
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
        assertThat(onlineHearing.get().getUserDetails().getSubscriptions(),
                containsInAnyOrder(new uk.gov.hmcts.reform.sscs.domain.wrapper.Subscription(UserType.OTHER_PARTY.getType(), "otherparty@hmct.com", "777"),
                        new uk.gov.hmcts.reform.sscs.domain.wrapper.Subscription(UserType.OTHER_PARTY_APPOINTEE.getType(), "otherpartyAppointee@hmct.com", "888")));
    }

    private void assertOnlineHearingForAppellant(Optional<OnlineHearing> onlineHearing, SscsCaseDetails sscsCaseDetails) {
        assertOnlineHearing(onlineHearing, sscsCaseDetails, "firstname lastname");
        assertThat(onlineHearing.get().getUserDetails().getSubscriptions(),
                containsInAnyOrder(new uk.gov.hmcts.reform.sscs.domain.wrapper.Subscription(UserType.APPELLANT.getType(), "appellant@hmct.com", "444"),
                        new uk.gov.hmcts.reform.sscs.domain.wrapper.Subscription(UserType.APPOINTEE.getType(), "appointee@hmct.com", "333")));
    }

    private void assertOnlineHearing(Optional<OnlineHearing> onlineHearing, SscsCaseDetails sscsCaseDetails, String name) {
        assertThat(onlineHearing.isPresent(), is(true));
        assertThat(onlineHearing.get(), is(new OnlineHearing(
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
        )));
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
