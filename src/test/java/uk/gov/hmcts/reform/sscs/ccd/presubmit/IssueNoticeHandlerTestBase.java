package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import static org.junit.Assert.assertEquals;
import static org.mockito.MockitoAnnotations.openMocks;

import java.io.IOException;
import java.util.function.Function;
import junitparams.JUnitParamsRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.service.UserDetailsService;


@RunWith(JUnitParamsRunner.class)
public abstract class IssueNoticeHandlerTestBase {
    public static final String APPELLANT_LAST_NAME = "APPELLANT Last'NamE";
    protected IssueNoticeHandler service;

    protected SscsCaseData sscsCaseData;
    protected String benefitType;
    @Mock
    public GenerateFile generateFile;
    @Mock
    public UserDetailsService userDetailsService;
    @Mock
    public Function<LanguagePreference, String> templateId;

    public IssueNoticeHandlerTestBase(String benefitType) {
        this.benefitType = benefitType;
    }

    protected abstract IssueNoticeHandler createIssueNoticeHandler(GenerateFile generateFile, UserDetailsService userDetailsService,
                                                                   Function<LanguagePreference, String> templateId);

    @Before
    public void setUp() throws IOException {
        openMocks(this);

        service = createIssueNoticeHandler(generateFile, userDetailsService, templateId);
        sscsCaseData = SscsCaseData.builder()
                .ccdCaseId("ccdId")
                .directionTypeDl(new DynamicList(DirectionType.APPEAL_TO_PROCEED.toString()))
                .regionalProcessingCenter(RegionalProcessingCenter.builder().name("Birmingham").build())
                .appeal(Appeal.builder()
                        .benefitType(BenefitType.builder().build())
                        .appellant(Appellant.builder()
                                .name(Name.builder().firstName("APPELLANT")
                                        .lastName("Last'NamE")
                                        .build())
                                .identity(Identity.builder().build()).isAppointee("Yes")
                                .appointee(Appointee.builder()
                                        .name(Name.builder().firstName("APPOINTEE").lastName("T-Est").build()).build())
                                .build())
                        .build()).build();

    }

    @Test
    public void givenBuildNameWhenDisplayAppointeeNameIsFalse() {
        String actual = service.buildName(sscsCaseData, false);
        assertEquals(APPELLANT_LAST_NAME, actual);
    }

    @Test
    public void givenBuildNameWhenDisplayAppointeeNameIsTrue() {
        String actual = service.buildName(sscsCaseData, true);
        assertEquals("APPOINTEE T-Est", actual);
    }

    @Test
    public void givenBuildNameWhenDisplayAppointeeNameIsTrueAndHasAppointeeIsNo() {
        sscsCaseData.getAppeal().getAppellant().setIsAppointee("No");
        String actual = service.buildName(sscsCaseData, true);
        assertEquals(APPELLANT_LAST_NAME, actual);
    }

    @Test
    public void givenBuildNameWhenDisplayAppointeeNameIsTrueAndAppointeeIsNull() {
        sscsCaseData.getAppeal().getAppellant().setAppointee(null);

        String actual = service.buildName(sscsCaseData, true);
        assertEquals(APPELLANT_LAST_NAME, actual);
    }
}
