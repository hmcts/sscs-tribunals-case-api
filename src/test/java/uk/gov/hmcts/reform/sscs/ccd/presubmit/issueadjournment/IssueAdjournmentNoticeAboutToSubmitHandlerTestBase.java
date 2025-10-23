package uk.gov.hmcts.reform.sscs.ccd.presubmit.issueadjournment;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_ADJOURNMENT_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ISSUE_ADJOURNMENT_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.LIST_ASSIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.HEARING;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseDaysOffset;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDateOrPeriod;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDateType;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDurationType;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDurationUnits;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingPeriod;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingVenue;
import uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseTypeOfHearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.Adjournment;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CollectionItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.OverrideFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SchedulingAndListingFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.resendtogaps.ListAssistHearingMessageHelper;
import uk.gov.hmcts.reform.sscs.reference.data.model.HearingDuration;
import uk.gov.hmcts.reform.sscs.reference.data.service.HearingDurationsService;
import uk.gov.hmcts.reform.sscs.service.AirLookupService;
import uk.gov.hmcts.reform.sscs.service.FooterService;
import uk.gov.hmcts.reform.sscs.service.RegionalProcessingCenterService;
import uk.gov.hmcts.reform.sscs.service.VenueService;

@ExtendWith(MockitoExtension.class)
abstract class IssueAdjournmentNoticeAboutToSubmitHandlerTestBase {

    protected static final String USER_AUTHORISATION = "Bearer token";
    protected static final String SPANISH = "Spanish";
    protected IssueAdjournmentNoticeAboutToSubmitHandler handler;

    protected Callback<SscsCaseData> callback;

    protected CaseDetails<CaseData> caseDetails;

    @Mock
    protected FooterService footerService;

    @Mock
    protected ListAssistHearingMessageHelper hearingMessageHelper;

    @Mock
    protected AirLookupService airLookupService;

    @Mock
    protected HearingDurationsService hearingDurationsService;

    @Mock
    protected RegionalProcessingCenterService regionalProcessingCenterService;

    @Mock
    protected VenueService venueService;

    protected SscsCaseData sscsCaseData;

    protected static Validator validator = Validation
        .byDefaultProvider()
        .configure()
        .messageInterpolator(new ParameterMessageInterpolator())
        .buildValidatorFactory()
        .getValidator();

    @BeforeEach
    protected void setUp() {
        handler = new IssueAdjournmentNoticeAboutToSubmitHandler(footerService, validator, hearingMessageHelper, airLookupService, regionalProcessingCenterService, hearingDurationsService, venueService);

        List<SscsDocument> documentList = new ArrayList<>();

        SscsDocumentDetails details = SscsDocumentDetails.builder().documentType(DRAFT_ADJOURNMENT_NOTICE.getValue()).build();
        documentList.add(new SscsDocument(details));
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId")
            .appeal(Appeal.builder().build())
            .sscsDocument(documentList)
            .state(HEARING)
            .adjournment(Adjournment.builder()
                .generateNotice(YES)
                .typeOfHearing(AdjournCaseTypeOfHearing.VIDEO)
                .canCaseBeListedRightAway(YES)
                .areDirectionsBeingMadeToParties(NO)
                .directionsDueDateDaysOffset(AdjournCaseDaysOffset.FOURTEEN_DAYS)
                .directionsDueDate(LocalDate.now().plusDays(14))
                .typeOfNextHearing(AdjournCaseTypeOfHearing.VIDEO)
                .nextHearingVenue(AdjournCaseNextHearingVenue.SAME_VENUE)
                .nextHearingVenueSelected(new DynamicList(
                    new DynamicListItem("",""), List.of(new DynamicListItem("", ""))))
                .nextHearingListingDurationType(AdjournCaseNextHearingDurationType.STANDARD)
                .nextHearingListingDuration(2)
                .nextHearingListingDurationUnits(AdjournCaseNextHearingDurationUnits.SESSIONS)
                .interpreterRequired(YES)
                .interpreterLanguage(new DynamicList("spanish"))
                .nextHearingDateType(AdjournCaseNextHearingDateType.FIRST_AVAILABLE_DATE_AFTER)
                .nextHearingDateOrPeriod(AdjournCaseNextHearingDateOrPeriod.PROVIDE_PERIOD)
                .nextHearingDateOrTime("")
                .nextHearingFirstAvailableDateAfterDate(LocalDate.now().plusMonths(2))
                .nextHearingFirstAvailableDateAfterPeriod(AdjournCaseNextHearingPeriod.NINETY_DAYS)
                .reasons(List.of(new CollectionItem<>(null, "")))
                .additionalDirections(List.of(new CollectionItem<>(null, "")))
                .previewDocument(DocumentLink.builder()
                    .documentUrl("url")
                    .documentFilename("adjournedcasedoc.pdf").build())
                .adjournmentInProgress(YES)
                .build())
            .schedulingAndListingFields(SchedulingAndListingFields.builder()
                .defaultListingValues(OverrideFields.builder().duration(45).build()).build())
        .build();
        CaseData sscsCaseDataBefore = SscsCaseData.builder()
                .ccdCaseId("ccdId")
                .appeal(Appeal.builder().hearingOptions(HearingOptions.builder().languageInterpreter("YES").build()).build())
                .dwpUcb("yes")
                .build();
        caseDetails = new CaseDetails<>(1L, "", null, sscsCaseData, null, null);
        var caseDetailsBefore = new CaseDetails<>(1L, "", null, sscsCaseDataBefore, null, null);

        callback = new Callback(caseDetails, Optional.of(caseDetailsBefore), ISSUE_ADJOURNMENT_NOTICE, true);
    }

    protected PreSubmitCallbackResponse<SscsCaseData> cannotBeListedAndNoDirectionsGiven() {
        sscsCaseData.getSchedulingAndListingFields().setHearingRoute(LIST_ASSIST);
        sscsCaseData.getAdjournment().setCanCaseBeListedRightAway(NO);
        sscsCaseData.getAdjournment().setAreDirectionsBeingMadeToParties(NO);

        return handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }

    protected PreSubmitCallbackResponse<SscsCaseData> canBeListed() {
        sscsCaseData.getSchedulingAndListingFields().setHearingRoute(LIST_ASSIST);
        sscsCaseData.getAdjournment().setCanCaseBeListedRightAway(YES);
        sscsCaseData.getAdjournment().setAreDirectionsBeingMadeToParties(NO);

        return handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }

    protected PreSubmitCallbackResponse<SscsCaseData> cannotBeListedAndDirectionsGiven() {
        sscsCaseData.getSchedulingAndListingFields().setHearingRoute(LIST_ASSIST);
        sscsCaseData.getAdjournment().setCanCaseBeListedRightAway(NO);
        sscsCaseData.getAdjournment().setAreDirectionsBeingMadeToParties(YES);

        return handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }

    protected void setupHearingDurationValues() {
        HearingDuration hearingDuration = new HearingDuration();
        hearingDuration.setDurationPaper(30);
        hearingDuration.setDurationInterpreter(90);
        hearingDuration.setDurationFaceToFace(60);
        when(hearingDurationsService.getHearingDuration(eq(sscsCaseData.getBenefitCode()), eq(sscsCaseData.getIssueCode()))).thenReturn(hearingDuration);

    }

}
