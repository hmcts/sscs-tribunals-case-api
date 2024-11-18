package uk.gov.hmcts.reform.sscs.service.hearings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDateType.FIRST_AVAILABLE_DATE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.model.hearings.HearingRequest;
import uk.gov.hmcts.reform.sscs.model.hmc.reference.HmcStatus;
import uk.gov.hmcts.reform.sscs.model.multi.hearing.HearingsGetResponse;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingRequestPayload;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HmcUpdateResponse;
import uk.gov.hmcts.reform.sscs.reference.data.model.CancellationReason;
import uk.gov.hmcts.reform.sscs.reference.data.model.SessionCategoryMap;
import uk.gov.hmcts.reform.sscs.reference.data.service.HearingDurationsService;
import uk.gov.hmcts.reform.sscs.reference.data.service.SessionCategoryMapService;
import uk.gov.hmcts.reform.sscs.service.HmcHearingApiService;
import uk.gov.hmcts.reform.sscs.service.HmcHearingsApiService;
import uk.gov.hmcts.reform.sscs.service.VenueService;
import uk.gov.hmcts.reform.sscs.service.holder.ReferenceDataServiceHolder;

@ExtendWith(MockitoExtension.class)
class AdjournCreateHearingCaseUpdaterTest extends HearingSaveActionBaseTest {

    @Mock
    private HmcHearingApiService hmcHearingApiService;
    @Mock
    private HmcHearingsApiService hmcHearingsApiService;
    @Mock
    private ReferenceDataServiceHolder refData;
    @Mock
    public HearingDurationsService hearingDurations;
    @Mock
    public SessionCategoryMapService sessionCategoryMaps;
    @Mock
    private VenueService venueService;
    @InjectMocks
    private AdjournCreateHearingCaseUpdater adjournCreateHearingCaseUpdater;

    private static final long CASE_ID = 1625080769409918L;
    private static final String BENEFIT_CODE = "002";
    private static final String ISSUE_CODE = "DD";
    private static final String PROCESSING_VENUE = "Processing Venue";

    @Test
    void adjournAndShouldCreateHearing() {
        SscsCaseDetails sscsCaseDetails = execute(true);
        assertThat(sscsCaseDetails.getData().getAdjournment().getAdjournmentInProgress()).isEqualTo(YesNo.NO);
    }

    @Test
    void isAdjournmentFlagEnabledNotEnabled() {
        SscsCaseDetails sscsCaseDetails = execute(false);
        assertThat(sscsCaseDetails.getData().getAdjournment().getAdjournmentInProgress()).isEqualTo(YES);
    }

    private SscsCaseDetails execute(boolean isAdjournmentFlagEnabled) {
        given(sessionCategoryMaps.getSessionCategory(BENEFIT_CODE, ISSUE_CODE, false, false))
                .willReturn(new SessionCategoryMap(BenefitCode.PIP_NEW_CLAIM, Issue.DD,
                        false, false, SessionCategory.CATEGORY_03, null));
        given(refData.getHearingDurations()).willReturn(hearingDurations);
        given(refData.getSessionCategoryMaps()).willReturn(sessionCategoryMaps);
        given(refData.getVenueService()).willReturn(venueService);
        given(refData.isAdjournmentFlagEnabled()).willReturn(isAdjournmentFlagEnabled);
        given(venueService.getEpimsIdForVenue(PROCESSING_VENUE)).willReturn("219164");

        given(hmcHearingApiService.sendCreateHearingRequest(any(HearingRequestPayload.class)))
                .willReturn(HmcUpdateResponse.builder().hearingRequestId(123L).versionNumber(1234L).status(HmcStatus.HEARING_REQUESTED).build());

        given(hmcHearingsApiService.getHearingsRequest(anyString(), eq(null)))
                .willReturn(HearingsGetResponse.builder().build());

        SscsCaseData sscsCaseData = SscsCaseData.builder()
                .ccdCaseId(String.valueOf(CASE_ID))
                .benefitCode(BENEFIT_CODE)
                .issueCode(ISSUE_CODE)
                .caseManagementLocation(CaseManagementLocation.builder().build())
                .adjournment(Adjournment.builder().nextHearingDateType(FIRST_AVAILABLE_DATE).interpreterRequired(YesNo.YES).adjournmentInProgress(YesNo.YES).build())
                .appeal(Appeal.builder()
                        .rep(Representative.builder().hasRepresentative("No").build())
                        .hearingOptions(HearingOptions.builder().wantsToAttend("yes").build())
                        .hearingType("test")
                        .hearingSubtype(HearingSubtype.builder().hearingVideoEmail("email@email.com").wantsHearingTypeFaceToFace("yes").build())
                        .appellant(Appellant.builder()
                                .name(Name.builder().firstName("first").lastName("surname").build())
                                .build())
                        .build())
                .processingVenue(PROCESSING_VENUE)
                .build();
        SscsCaseDetails caseDetails = SscsCaseDetails.builder().data(sscsCaseData).build();

        HearingRequest hearingRequest = HearingRequest.internalBuilder()
                .hearingState(HearingState.ADJOURN_CREATE_HEARING)
                .cancellationReason(CancellationReason.PARTY_DID_NOT_ATTEND)
                .hearingRoute(HearingRoute.LIST_ASSIST)
                .ccdCaseId(String.valueOf(CASE_ID))
                .build();

        Assertions.assertDoesNotThrow(
                () -> adjournCreateHearingCaseUpdater.applyUpdate(caseDetails, hearingRequest));

        return caseDetails;
    }
}
