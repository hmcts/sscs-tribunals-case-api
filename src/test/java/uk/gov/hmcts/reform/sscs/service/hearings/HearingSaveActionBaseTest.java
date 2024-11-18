package uk.gov.hmcts.reform.sscs.service.hearings;

import java.util.ArrayList;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.model.hearings.HearingRequest;
import uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel;

abstract class HearingSaveActionBaseTest {
    protected static final long CASE_ID = 1625080769409918L;
    protected static final long HEARING_REQUEST_ID = 12345;
    protected static final String BENEFIT_CODE = "002";
    protected static final String ISSUE_CODE = "DD";
    protected static final String PROCESSING_VENUE = "Processing Venue";

    @BeforeEach
    void setup() {
    }

    protected static SscsCaseDetails createCaseDataWithHearings() {
        SscsCaseData sscsCaseData = SscsCaseData.builder()
            .ccdCaseId(String.valueOf(CASE_ID))
            .benefitCode(BENEFIT_CODE)
            .issueCode(ISSUE_CODE)
            .caseManagementLocation(CaseManagementLocation.builder().build())
            .appeal(Appeal.builder()
                        .rep(Representative.builder().hasRepresentative("No").build())
                        .hearingOptions(HearingOptions.builder().wantsToAttend("yes").build())
                        .hearingType("test")
                        .hearingSubtype(HearingSubtype.builder().hearingVideoEmail("email@email.com").wantsHearingTypeFaceToFace("yes").build())
                        .appellant(Appellant.builder()
                                       .name(Name.builder().firstName("first").lastName("surname").build())
                                       .build())
                        .build())
            .hearings(new ArrayList<>(Collections.singletonList(Hearing.builder()
                                                                    .value(HearingDetails.builder()
                                                                               .hearingId(String.valueOf(HEARING_REQUEST_ID))
                                                                               .hearingChannel(HearingChannel.PAPER)
                                                                               .versionNumber(1L)
                                                                               .build())
                                                                    .build())))
            .processingVenue(PROCESSING_VENUE)
            .build();

        return SscsCaseDetails.builder().data(sscsCaseData).build();
    }

    protected static SscsCaseDetails createCaseData() {
        SscsCaseData sscsCaseData = SscsCaseData.builder()
            .ccdCaseId(String.valueOf(CASE_ID))
            .benefitCode(BENEFIT_CODE)
            .issueCode(ISSUE_CODE)
            .caseManagementLocation(CaseManagementLocation.builder().build())
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
        return SscsCaseDetails.builder().data(sscsCaseData).build();
    }

    protected static HearingRequest createHearingRequestForState(HearingState state) {
        return HearingRequest.internalBuilder()
            .hearingState(state)
            .hearingRoute(HearingRoute.LIST_ASSIST)
            .ccdCaseId(String.valueOf(CASE_ID))
            .build();

    }
}
