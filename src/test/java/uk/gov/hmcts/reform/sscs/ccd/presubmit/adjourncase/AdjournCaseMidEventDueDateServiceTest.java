package uk.gov.hmcts.reform.sscs.ccd.presubmit.adjourncase;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDateType.FIRST_AVAILABLE_DATE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseTypeOfHearing.FACE_TO_FACE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.Adjournment;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CollectionItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.DirectionType;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.Identity;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@RunWith(MockitoJUnitRunner.class)
public class AdjournCaseMidEventDueDateServiceTest {

    private AdjournCaseMidEventDueDateService adjournCaseMidEventDueDateService;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;


    @Before
    public void setUp() throws IOException {
        adjournCaseMidEventDueDateService = new AdjournCaseMidEventDueDateService();
        sscsCaseData = SscsCaseData.builder()
                .ccdCaseId("ccdId")
                .directionTypeDl(new DynamicList(DirectionType.APPEAL_TO_PROCEED.toString()))
                .regionalProcessingCenter(RegionalProcessingCenter.builder().name("Birmingham").build())
                .appeal(Appeal.builder()
                        .benefitType(BenefitType.builder().code("PIP").build())
                        .appellant(Appellant.builder()
                                .name(Name.builder().firstName("APPELLANT")
                                        .lastName("Last'NamE")
                                        .build())
                                .identity(Identity.builder().build())
                                .build())
                        .build())
                .adjournment(Adjournment.builder()
                        .reasons(List.of(new CollectionItem<>(null, "Reason")))
                        .additionalDirections(List.of(new CollectionItem<>(null, "Additional Direction")))
                        .directionsDueDate(LocalDate.now().minusDays(1))
                        .typeOfHearing(FACE_TO_FACE)
                        .generateNotice(YES)
                        .typeOfNextHearing(FACE_TO_FACE)
                        .nextHearingDateType(FIRST_AVAILABLE_DATE)
                        .build())
                .build();
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

    }

    @Test
    public void shouldReturnErrorWhenDueDatePast() {
        Boolean dateIsInFuture = adjournCaseMidEventDueDateService
                .validateAdjournCaseDirectionsDueDateIsInFuture(callback.getCaseDetails().getCaseData());
        assertEquals(false, dateIsInFuture);

    }

    @Test
    public void shouldNotReturnErrorWhenDueDateFuture() {
        sscsCaseData.getAdjournment().setDirectionsDueDate(LocalDate.now().plusDays(1));
        Boolean dateIsInFuture = adjournCaseMidEventDueDateService
                .validateAdjournCaseDirectionsDueDateIsInFuture(callback.getCaseDetails().getCaseData());
        assertEquals(true, dateIsInFuture);

    }

}
