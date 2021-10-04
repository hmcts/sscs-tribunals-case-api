package uk.gov.hmcts.reform.sscs.service;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.domain.wrapper.*;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

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

        HearingOptions hearingOptions = HearingOptions.builder()
                .languageInterpreter("yes")
                .languages("french")
                .arrangements(asList("signLanguageInterpreter", "hearingLoop", "disabledAccess"))
                .signLanguageType("BSL")
                .other("other arrangements")
                .build();
        sscsCaseDetails.getData().getAppeal().setHearingOptions(hearingOptions);

        Optional<OnlineHearing> onlineHearing = underTest.loadHearing(sscsCaseDetails);

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
                new AppellantDetails(new AddressDetails("line1","line2","town", "county","postcode"), "email", "012", "120"),
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
                .data(SscsCaseData.builder()
                        .caseReference(expectedCaseReference)
                        .caseCreated(LocalDateTime.now().toString())
                        .state(State.HEARING)
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
                                .build()
                        )
                        .decisionNotes("decision notes")
                        .events(new ArrayList<>())
                        .build()
                ).build();
    }
}
