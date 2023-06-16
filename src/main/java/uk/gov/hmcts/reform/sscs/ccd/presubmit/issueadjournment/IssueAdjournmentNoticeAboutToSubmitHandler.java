package uk.gov.hmcts.reform.sscs.ccd.presubmit.issueadjournment;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_ADJOURNMENT_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DwpState.ADJOURNMENT_NOTICE_ISSUED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;
import static uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel.PAPER;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.IssueDocumentHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.resendtogaps.ListAssistHearingMessageHelper;
import uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel;
import uk.gov.hmcts.reform.sscs.service.AirLookupService;
import uk.gov.hmcts.reform.sscs.service.FooterService;
import uk.gov.hmcts.reform.sscs.service.RegionalProcessingCenterService;
import uk.gov.hmcts.reform.sscs.service.VenueDataLoader;
import uk.gov.hmcts.reform.sscs.util.SscsUtil;

@Component
@Slf4j
@AllArgsConstructor
public class IssueAdjournmentNoticeAboutToSubmitHandler extends IssueDocumentHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final FooterService footerService;
    private final Validator validator;
    private final ListAssistHearingMessageHelper hearingMessageHelper;
    private final VenueDataLoader venueDataLoader;
    private final AirLookupService airLookupService;
    private final RegionalProcessingCenterService regionalProcessingCenterService;

    @Value("${feature.snl.adjournment.enabled}")
    private boolean isAdjournmentEnabled; // TODO SSCS-10951

    private static final int DURATION_SESSIONS_MULTIPLIER = 165;
    private static final int DURATION_DEFAULT = 30;
    private static final int MIN_HEARING_DURATION = 30;
    private static final int MIN_HEARING_SESSION_DURATION = 1;

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType == CallbackType.ABOUT_TO_SUBMIT
            && callback.getEvent() == EventType.ISSUE_ADJOURNMENT_NOTICE
            && Objects.nonNull(callback.getCaseDetails())
            && Objects.nonNull(callback.getCaseDetails().getCaseData());
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();

        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        Set<ConstraintViolation<SscsCaseData>> violations = validator.validate(sscsCaseData);
        for (ConstraintViolation<SscsCaseData> violation : violations) {
            preSubmitCallbackResponse.addError(violation.getMessage());
        }

        if (preSubmitCallbackResponse.getErrors().isEmpty()) {
            SscsDocumentTranslationStatus documentTranslationStatus = sscsCaseData.isLanguagePreferenceWelsh() ? SscsDocumentTranslationStatus.TRANSLATION_REQUIRED : null;

            calculateDueDate(sscsCaseData);

            if (sscsCaseData.getAdjournment().getPreviewDocument() != null) {
                processResponse(sscsCaseData, preSubmitCallbackResponse, documentTranslationStatus);
            } else {
                preSubmitCallbackResponse.addError("There is no Draft Adjournment Notice on the case so adjournment cannot be issued");
            }
        }

        return preSubmitCallbackResponse;
    }

    private void processResponse(SscsCaseData sscsCaseData, PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse,
        SscsDocumentTranslationStatus documentTranslationStatus) {
        createAdjournmentNoticeFromPreviewDraft(preSubmitCallbackResponse, documentTranslationStatus);

        if (!SscsDocumentTranslationStatus.TRANSLATION_REQUIRED.equals(documentTranslationStatus)) {
            sscsCaseData.setDwpState(ADJOURNMENT_NOTICE_ISSUED);
            if (isYes(sscsCaseData.getAdjournment().getCanCaseBeListedRightAway())) {
                sscsCaseData.setState(State.READY_TO_LIST);
            } else {
                sscsCaseData.setState(State.NOT_LISTABLE);
            }
        } else {
            log.info("Case is a Welsh case so Adjournment Notice requires translation for case id : {}", sscsCaseData.getCcdCaseId());
            sscsCaseData.setInterlocReviewState(InterlocReviewState.WELSH_TRANSLATION);
            log.info("Set the InterlocReviewState to {},  for case id : {}", sscsCaseData.getInterlocReviewState(), sscsCaseData.getCcdCaseId());
            sscsCaseData.setTranslationWorkOutstanding("Yes");
        }

        Adjournment adjournment = sscsCaseData.getAdjournment();

        if (isAdjournmentEnabled) {
            updateHearingOptions(sscsCaseData);
            updateRpc(sscsCaseData);
            updatePanelMembers(sscsCaseData);
            updateOverrideFields(sscsCaseData);
            updateHearingChannelAndWantsToAttend(sscsCaseData);

            if (SscsUtil.isSAndLCase(sscsCaseData)
                && (isYes(adjournment.getCanCaseBeListedRightAway()))) {
                adjournment.setAdjournmentInProgress(YES);
                hearingMessageHelper.sendListAssistCreateHearingMessage(sscsCaseData.getCcdCaseId());
            }
        }

        clearBasicTransientFields(sscsCaseData);

        preSubmitCallbackResponse.getData().getSscsDocument()
                .removeIf(doc -> doc.getValue().getDocumentType().equals(DRAFT_ADJOURNMENT_NOTICE.getValue()));
    }

    private void updateHearingOptions(SscsCaseData sscsCaseData) {
        Adjournment adjournment = sscsCaseData.getAdjournment();

        if (adjournment.getInterpreterRequired() != null) {
            HearingOptions hearingOptions = HearingOptions.builder().build();
            if (sscsCaseData.getAppeal().getHearingOptions() != null) {
                hearingOptions = sscsCaseData.getAppeal().getHearingOptions();
            }
            DynamicList interpreterLanguage = adjournment.getInterpreterLanguage();
            hearingOptions.setLanguages(nonNull(interpreterLanguage.getValue()) ? interpreterLanguage.getValue().getLabel() : "");
            hearingOptions.setLanguageInterpreter(adjournment.getInterpreterRequired().getValue());

            sscsCaseData.getAppeal().setHearingOptions(hearingOptions);
        }
    }

    private void updateRpc(SscsCaseData sscsCaseData) {
        Adjournment adjournment = sscsCaseData.getAdjournment();

        if (nonNull(adjournment.getNextHearingVenueSelected())) {
            String venueId = adjournment.getNextHearingVenueSelected().getValue().getCode();

            RegionalProcessingCenter rpc = regionalProcessingCenterService.getByVenueId(venueId);

            sscsCaseData.setRegionalProcessingCenter(rpc);

            if (nonNull(rpc)) {
                sscsCaseData.setRegion(rpc.getName());

                String processingVenue = airLookupService.lookupAirVenueNameByPostCode(
                    rpc.getPostcode(),
                    sscsCaseData.getAppeal().getBenefitType());

                sscsCaseData.setProcessingVenue(processingVenue);
            }
        }
    }

    private void calculateDueDate(SscsCaseData sscsCaseData) {
        if (sscsCaseData.getAdjournment().getDirectionsDueDate() != null) {
            sscsCaseData.setDirectionDueDate(sscsCaseData.getAdjournment().getDirectionsDueDate().toString());
        } else if (sscsCaseData.getAdjournment().getDirectionsDueDateDaysOffset() != null) {
            sscsCaseData.setDirectionDueDate(LocalDate.now()
                .plusDays(sscsCaseData.getAdjournment().getDirectionsDueDateDaysOffset().getCcdDefinition())
                .toString());
        }
    }

    private void createAdjournmentNoticeFromPreviewDraft(
        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse,
        SscsDocumentTranslationStatus documentTranslationStatus) {

        SscsCaseData sscsCaseData = preSubmitCallbackResponse.getData();

        DocumentLink docLink = sscsCaseData.getAdjournment().getPreviewDocument();

        DocumentLink documentLink = DocumentLink.builder()
            .documentUrl(docLink.getDocumentUrl())
            .documentFilename(docLink.getDocumentFilename())
            .documentBinaryUrl(docLink.getDocumentBinaryUrl())
            .build();

        String now = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

        footerService.createFooterAndAddDocToCase(documentLink, sscsCaseData, DocumentType.ADJOURNMENT_NOTICE, now,
                null, null, documentTranslationStatus);
    }

    public void updateHearingChannelAndWantsToAttend(SscsCaseData sscsCaseData) {
        AdjournCaseTypeOfHearing nextHearingType = sscsCaseData.getAdjournment().getTypeOfNextHearing();
        if (nonNull(nextHearingType)) {
            Appeal appeal = sscsCaseData.getAppeal();
            HearingChannel hearingChannel = getNextHearingChannel(sscsCaseData);

            if (isAdjournmentEnabled) {
                String wantsToAttend = YES.toString();
                String hearingType = uk.gov.hmcts.reform.sscs.ccd.domain.HearingType.ORAL.getValue();

                if (PAPER.equals(nextHearingType.getHearingChannel())) {
                    wantsToAttend = NO.toString();
                    hearingType = uk.gov.hmcts.reform.sscs.ccd.presubmit.adjourncase.HearingType.PAPER.getValue();
                }

                log.info("Updating hearing type to {} and wants to attend to {}", hearingType, wantsToAttend);
                appeal.getHearingOptions().setWantsToAttend(wantsToAttend);
                appeal.setHearingType(hearingType);
                sscsCaseData.getSchedulingAndListingFields().getOverrideFields().setAppellantHearingChannel(hearingChannel);
            }

            Hearing latestHearing = sscsCaseData.getLatestHearing();
            if (nonNull(latestHearing) && nonNull(latestHearing.getValue())) {
                latestHearing.getValue().setHearingChannel(hearingChannel);

                if (hearingChannel.getValueTribunals().equalsIgnoreCase(PAPER.getValueTribunals())) {
                    appeal.setHearingType(PAPER.getValueTribunals());
                } else {
                    appeal.setHearingType(uk.gov.hmcts.reform.sscs.ccd.domain.HearingType.ORAL.getValue());
                }
            }
        }
    }

    private HearingChannel getNextHearingChannel(SscsCaseData caseData) {
        return Arrays.stream(HearingChannel.values())
            .filter(hearingChannel -> caseData.getAdjournment().getTypeOfNextHearing().getHearingChannel().getValueTribunals().equalsIgnoreCase(
                hearingChannel.getValueTribunals()))
            .findFirst().orElse(HearingChannel.PAPER);
    }

    private static void updatePanelMembers(SscsCaseData caseData) {
        Adjournment adjournment = caseData.getAdjournment();
        AdjournCasePanelMembersExcluded panelMemberExcluded = adjournment.getPanelMembersExcluded();

        if (nonNull(panelMemberExcluded)) {
            PanelMemberExclusions panelMemberExclusions = caseData.getSchedulingAndListingFields().getPanelMemberExclusions();

            if (isNull(panelMemberExclusions)) {
                panelMemberExclusions = PanelMemberExclusions.builder().build();
                caseData.getSchedulingAndListingFields().setPanelMemberExclusions(panelMemberExclusions);
            }

            SscsUtil.setAdjournmentPanelMembersExclusions(panelMemberExclusions, adjournment.getPanelMembers(), panelMemberExcluded);
        }
    }

    private void updateOverrideFields(SscsCaseData caseData) {
        OverrideFields fields = caseData.getSchedulingAndListingFields().getOverrideFields();
        Adjournment adjournment = caseData.getAdjournment();

        if (isNull(fields)) {
            fields = OverrideFields.builder().build();
            caseData.getSchedulingAndListingFields().setOverrideFields(fields);
        }

        if (nonNull(adjournment.getTypeOfNextHearing())) {
            fields.setAppellantHearingChannel(adjournment.getTypeOfNextHearing().getHearingChannel());
        }

        var nextHearingVenueSelected = adjournment.getNextHearingVenueSelected();

        if (nonNull(nextHearingVenueSelected)) {
            var venueDetails = venueDataLoader.getVenueDetailsMap().get(nextHearingVenueSelected.getValue().getCode());

            if (nonNull(venueDetails)) {
                CcdValue<String> venueDetailsValue = new CcdValue<>(venueDetails.getEpimsId());
                CcdValue<CcdValue<String>> ccdValue = new CcdValue<>(venueDetailsValue);
                fields.setHearingVenueEpimsIds(List.of(ccdValue));
            }
        }

        Integer duration = caseData.getAdjournment().getNextHearingListingDuration();
        if (duration != null && caseData.getAdjournment().getNextHearingListingDurationType() == AdjournCaseNextHearingDurationType.NON_STANDARD) {
            fields.setDuration(handleNonStandardDuration(caseData, duration));
        }

        if (isYes(adjournment.getInterpreterRequired())) {
            HearingInterpreter interpreter = HearingInterpreter.builder()
                .interpreterLanguage(adjournment.getInterpreterLanguage())
                .isInterpreterWanted(adjournment.getInterpreterRequired())
                .build();
            fields.setAppellantInterpreter(interpreter);
        }

        handleHearingWindow(caseData, fields);
    }

    private static Integer handleNonStandardDuration(SscsCaseData caseData, Integer duration) {
        AdjournCaseNextHearingDurationUnits units = caseData.getAdjournment().getNextHearingListingDurationUnits();
        if (units == AdjournCaseNextHearingDurationUnits.SESSIONS && duration >= MIN_HEARING_SESSION_DURATION) {
            return duration * DURATION_SESSIONS_MULTIPLIER;
        } else if (units == AdjournCaseNextHearingDurationUnits.MINUTES && duration >= MIN_HEARING_DURATION) {
            return duration;
        }
        return DURATION_DEFAULT;
    }

    private void handleHearingWindow(SscsCaseData caseData, OverrideFields overrideFields) {
        HearingWindow hearingWindow = overrideFields.getHearingWindow();
        Adjournment adjournment = caseData.getAdjournment();

        if (hearingWindow == null) {
            hearingWindow = HearingWindow.builder().build();
            overrideFields.setHearingWindow(hearingWindow);
        }

        if (AdjournCaseNextHearingDateType.FIRST_AVAILABLE_DATE_AFTER.equals(adjournment.getNextHearingDateType())) {
            if (AdjournCaseNextHearingDateOrPeriod.PROVIDE_DATE.equals(adjournment.getNextHearingDateOrPeriod())) {
                hearingWindow.setDateRangeStart(adjournment.getNextHearingFirstAvailableDateAfterDate().plusDays(1));
            } else if (AdjournCaseNextHearingDateOrPeriod.PROVIDE_PERIOD.equals(adjournment.getNextHearingDateOrPeriod())) {
                long after = Long.parseLong(adjournment.getNextHearingFirstAvailableDateAfterPeriod().toString());
                hearingWindow.setDateRangeStart(LocalDate.now().plusDays(after));
            }
        }
    }
}
