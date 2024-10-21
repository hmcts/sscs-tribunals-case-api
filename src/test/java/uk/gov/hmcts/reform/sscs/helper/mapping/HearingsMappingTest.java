package uk.gov.hmcts.reform.sscs.helper.mapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.model.HearingWrapper;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingRequestPayload;
import uk.gov.hmcts.reform.sscs.reference.data.model.SessionCategoryMap;
import uk.gov.hmcts.reform.sscs.reference.data.service.HearingDurationsService;
import uk.gov.hmcts.reform.sscs.reference.data.service.SessionCategoryMapService;
import uk.gov.hmcts.reform.sscs.service.VenueService;
import uk.gov.hmcts.reform.sscs.service.holder.ReferenceDataServiceHolder;

class HearingsMappingTest extends HearingsMappingBase {

    @Mock
    private HearingDurationsService hearingDurations;

    @Mock
    private SessionCategoryMapService sessionCategoryMaps;

    @Mock
    private ReferenceDataServiceHolder refData;

    @Mock
    private VenueService venueService;

    @DisplayName("When a valid hearing wrapper is given buildHearingPayload returns the correct Hearing Request Payload")
    @Test
    void buildHearingPayload() throws Exception {
        given(sessionCategoryMaps.getSessionCategory(BENEFIT_CODE,ISSUE_CODE,false,false))
                .willReturn(new SessionCategoryMap(BenefitCode.PIP_NEW_CLAIM, Issue.DD,
                                                   false, false, SessionCategory.CATEGORY_03, null));

        given(refData.getHearingDurations()).willReturn(hearingDurations);
        given(refData.getSessionCategoryMaps()).willReturn(sessionCategoryMaps);
        given(refData.getVenueService()).willReturn(venueService);

        SscsCaseData caseData = SscsCaseData.builder()
            .ccdCaseId(String.valueOf(CASE_ID))
            .benefitCode(BENEFIT_CODE)
            .issueCode(ISSUE_CODE)
            .caseCreated(CASE_CREATED)
            .caseAccessManagementFields(CaseAccessManagementFields.builder()
                .caseNameHmctsInternal(CASE_NAME_INTERNAL)
                .caseNamePublic(CASE_NAME_PUBLIC)
                .build())
            .appeal(Appeal.builder()
                .rep(Representative.builder().hasRepresentative("no").build())
                .hearingOptions(HearingOptions.builder().wantsToAttend("yes").build())
                .hearingType("test")
                .hearingSubtype(HearingSubtype.builder().hearingVideoEmail("email@email.com").wantsHearingTypeFaceToFace("yes").build())
                .appellant(Appellant.builder()
                    .name(Name.builder()
                        .title("title")
                        .firstName("first")
                        .lastName("last")
                        .build())
                    .build())
                .build())
            .caseManagementLocation(CaseManagementLocation.builder()
                .baseLocation(EPIMS_ID)
                .region(REGION)
                .build())
            .build();
        HearingWrapper wrapper = HearingWrapper.builder()
            .caseData(caseData)
            .caseData(caseData)
            .build();
        HearingRequestPayload result = HearingsMapping.buildHearingPayload(wrapper, refData);

        assertThat(result).isNotNull();
        assertThat(result.getRequestDetails()).isNotNull();
        assertThat(result.getHearingDetails()).isNotNull();
        assertThat(result.getCaseDetails()).isNotNull();
        assertThat(result.getRequestDetails()).isNotNull();
    }
}
