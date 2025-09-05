package uk.gov.hmcts.reform.sscs.helper.mapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseAccessManagementFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseManagementLocation;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingSubtype;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.Representative;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.model.HearingWrapper;
import uk.gov.hmcts.reform.sscs.model.single.hearing.CaseDetails;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingDetails;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingRequestPayload;
import uk.gov.hmcts.reform.sscs.service.holder.ReferenceDataServiceHolder;

@ExtendWith(MockitoExtension.class)
class HearingsMappingTest extends HearingsMappingBase {

    @Mock
    private ReferenceDataServiceHolder refData;
    @Mock
    private HearingsDetailsMapping hearingsDetailsMapping;
    @Mock
    private HearingsCaseMapping hearingsCaseMapping;

    private HearingsMapping hearingsMapping;

    @DisplayName("When a valid hearing wrapper is given buildHearingPayload returns the correct Hearing Request Payload")
    @Test
    void buildHearingPayload() throws Exception {
        hearingsMapping = new HearingsMapping(hearingsDetailsMapping, hearingsCaseMapping);
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
        given(hearingsDetailsMapping.buildHearingDetails(any(HearingWrapper.class), any(ReferenceDataServiceHolder.class)))
                .willReturn(HearingDetails.builder().build());
        given(hearingsCaseMapping.buildHearingCaseDetails(eq(wrapper), eq(refData))).willReturn(CaseDetails.builder().build());

        HearingRequestPayload result = hearingsMapping.buildHearingPayload(wrapper, refData);

        assertThat(result).isNotNull();
        assertThat(result.getRequestDetails()).isNotNull();
        assertThat(result.getHearingDetails()).isNotNull();
        assertThat(result.getCaseDetails()).isNotNull();
        assertThat(result.getRequestDetails()).isNotNull();
    }
}
