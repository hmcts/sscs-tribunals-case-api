package uk.gov.hmcts.reform.sscs.ccd.presubmit.issueadjournment;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_ADJOURNMENT_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDateOrPeriod.PROVIDE_DATE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDateOrPeriod.PROVIDE_PERIOD;
import static uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDateType.DATE_TO_BE_FIXED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDateType.FIRST_AVAILABLE_DATE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDateType.FIRST_AVAILABLE_DATE_AFTER;
import static uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDurationType.NON_STANDARD;
import static uk.gov.hmcts.reform.sscs.ccd.domain.AdjournCaseNextHearingDurationType.STANDARD;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DwpState.ADJOURNMENT_NOTICE_ISSUED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isNoOrNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;
import static uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel.PAPER;
import static uk.gov.hmcts.reform.sscs.utility.HearingChannelUtil.isInterpreterRequired;

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
import uk.gov.hmcts.reform.sscs.ccd.presubmit.IssueDocumentHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.resendtogaps.ListAssistHearingMessageHelper;
import uk.gov.hmcts.reform.sscs.model.VenueDetails;
import uk.gov.hmcts.reform.sscs.model.client.JudicialUserBase;
import uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel;
import uk.gov.hmcts.reform.sscs.reference.data.service.HearingDurationsService;
import uk.gov.hmcts.reform.sscs.service.*;
import uk.gov.hmcts.reform.sscs.util.SscsUtil;

@Component
@Slf4j
@AllArgsConstructor
public class IssueAdjournmentNoticeAboutToSubmitHandler extends IssueDocumentHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    private final FooterService footerService;
    private final Validator validator;
    private final ListAssistHearingMessageHelper hearingMessageHelper;
    private final AirLookupService airLookupService;
    private final RegionalProcessingCenterService regionalProcessingCenterService;
    private final HearingDurationsService hearingDurationsService;
    private final VenueService venueService;

    @Value("${feature.snl.adjournment.enabled}")
    private boolean isAdjournmentEnabled;

    private static final int DURATION_SESSIONS_MULTIPLIER = 165;
    private static final int DURATION_DEFAULT = 60;
    private static final int MIN_HEARING_DURATION = 30;
    private static final int MIN_HEARING_SESSION_DURATION = 1;
    private static final int FIRST_AVAILABLE_DATE_DAYS = 14;

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

        sscsCaseData.clearPoDetails();

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
            sscsCaseData.setState(isCaseListable(sscsCaseData));
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

            if (SscsUtil.isSAndLCase(sscsCaseData) && State.READY_TO_LIST.equals(sscsCaseData.getState())) {
                adjournment.setAdjournmentInProgress(YES);
                hearingMessageHelper.sendListAssistCreateAdjournmentHearingMessage(sscsCaseData.getCcdCaseId());
            }
        }

        clearBasicTransientFields(sscsCaseData);
        clearAdjournmentTransientFields(sscsCaseData);

        preSubmitCallbackResponse.getData().getSscsDocument()
                .removeIf(doc -> doc.getValue().getDocumentType().equals(DRAFT_ADJOURNMENT_NOTICE.getValue()));
    }

    private static State isCaseListable(SscsCaseData sscsCaseData) {
        Adjournment adjournment = sscsCaseData.getAdjournment();
        boolean generateNotice = isYes(adjournment.getGenerateNotice());
        boolean canCaseBeListedRightAway = isYes(adjournment.getCanCaseBeListedRightAway());
        boolean areDirectionsNotBeingMade = isNoOrNull(adjournment.getAreDirectionsBeingMadeToParties());

        return (generateNotice && canCaseBeListedRightAway) || (! generateNotice && areDirectionsNotBeingMade) ? State.READY_TO_LIST : State.NOT_LISTABLE;
    }

    private void clearAdjournmentTransientFields(SscsCaseData caseData) {
        if (isAdjournmentEnabled) {
            Adjournment adjournment = caseData.getAdjournment();
            if (nonNull(adjournment)) {
                adjournment.setPreviewDocument(null);
                adjournment.setSignedInUser(null);
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

    private void updateHearingOptions(SscsCaseData sscsCaseData) {
        HearingOptions hearingOptions  = sscsCaseData.getAppeal().getHearingOptions();
        if (isNull(hearingOptions)) {
            hearingOptions = HearingOptions.builder().build();
        }

        Adjournment adjournment = sscsCaseData.getAdjournment();
        YesNo interpreterRequired = adjournment.getInterpreterRequired();
        if (nonNull(interpreterRequired) && isYes(interpreterRequired)) {
            DynamicList interpreterLanguage = adjournment.getInterpreterLanguage();
            hearingOptions.setLanguages(nonNull(interpreterLanguage.getValue()) ? interpreterLanguage.getValue().getLabel() : NO.getValue());
            hearingOptions.setLanguageInterpreter(interpreterRequired.getValue());
        } else {
            hearingOptions.setLanguages(null);
            hearingOptions.setLanguageInterpreter(NO.getValue());
            adjournment.setInterpreterLanguage(null);
            adjournment.setInterpreterRequired(NO);
        }

        sscsCaseData.getAppeal().setHearingOptions(hearingOptions);
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

    private HearingChannel getNextHearingChannel(SscsCaseData caseData) {
        return Arrays.stream(HearingChannel.values())
            .filter(hearingChannel -> caseData.getAdjournment().getTypeOfNextHearing().getHearingChannel().getValueTribunals().equalsIgnoreCase(
                hearingChannel.getValueTribunals()))
            .findFirst().orElse(HearingChannel.PAPER);
    }

    private void updatePanelMembers(SscsCaseData caseData) {
        Adjournment adjournment = caseData.getAdjournment();
        AdjournCasePanelMembersExcluded panelMemberExcluded = adjournment.getPanelMembersExcluded();

        if (nonNull(panelMemberExcluded)) {
            PanelMemberExclusions panelMemberExclusions = caseData.getSchedulingAndListingFields().getPanelMemberExclusions();

            if (isNull(panelMemberExclusions)) {
                panelMemberExclusions = PanelMemberExclusions.builder().build();
                caseData.getSchedulingAndListingFields().setPanelMemberExclusions(panelMemberExclusions);
            }

            List<JudicialUserBase> membersToExcluded = adjournment.getPanelMembers();
            membersToExcluded.add(adjournment.getSignedInUser());

            SscsUtil.setAdjournmentPanelMembersExclusions(panelMemberExclusions, membersToExcluded, panelMemberExcluded);
        }
    }

    private void updateOverrideFields(SscsCaseData caseData) {
        OverrideFields fields = caseData.getSchedulingAndListingFields().getOverrideFields();

        if (isNull(fields)) {
            fields = OverrideFields.builder().build();
            caseData.getSchedulingAndListingFields().setOverrideFields(fields);
        }

        Adjournment adjournment = caseData.getAdjournment();

        if (AdjournCaseTypeOfHearing.PAPER.equals(adjournment.getTypeOfNextHearing())) {
            List<VenueDetails> paperVenues = venueService.getActiveRegionalEpimsIdsForRpc(caseData.getRegionalProcessingCenter().getEpimsId());

            List<CcdValue<CcdValue<String>>> venueEpimsIds = paperVenues.stream().map(VenueDetails::getEpimsId)
                    .map(CcdValue::new)
                    .map(CcdValue::new)
                    .toList();

            fields.setHearingVenueEpimsIds(venueEpimsIds);
        } else {
            var nextHearingVenueSelected = adjournment.getNextHearingVenueSelected();

            String epimsId = nonNull(nextHearingVenueSelected)
                    ? venueService.getEpimsIdForVenueId(nextHearingVenueSelected.getValue().getCode())
                    : venueService.getEpimsIdForVenue(caseData.getProcessingVenue());

            if (nonNull(epimsId)) {
                CcdValue<String> venueDetailsValue = new CcdValue<>(epimsId);
                CcdValue<CcdValue<String>> ccdValue = new CcdValue<>(venueDetailsValue);
                fields.setHearingVenueEpimsIds(List.of(ccdValue));
            }
        }

        if (isYes(adjournment.getInterpreterRequired())) {
            HearingInterpreter interpreter = HearingInterpreter.builder()
                .interpreterLanguage(adjournment.getInterpreterLanguage())
                .isInterpreterWanted(adjournment.getInterpreterRequired())
                .build();
            fields.setAppellantInterpreter(interpreter);
        }

        updateHearingChannelAndWantsToAttend(caseData);
        handleHearingWindow(caseData, fields);

        Integer duration = handleHearingDuration(caseData);
        fields.setDuration(duration);
    }

    private void updateHearingChannelAndWantsToAttend(SscsCaseData sscsCaseData) {
        AdjournCaseTypeOfHearing nextHearingType = sscsCaseData.getAdjournment().getTypeOfNextHearing();
        if (nonNull(nextHearingType)) {
            Appeal appeal = sscsCaseData.getAppeal();
            HearingChannel hearingChannel = getNextHearingChannel(sscsCaseData);

            SscsUtil.updateHearingChannel(sscsCaseData, hearingChannel);

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

    private Integer handleHearingDuration(SscsCaseData caseData) {
        AdjournCaseNextHearingDurationType durationType = caseData.getAdjournment().getNextHearingListingDurationType();

        if (STANDARD.equals(durationType)) {
            OverrideFields defaultListingValues = caseData.getSchedulingAndListingFields().getDefaultListingValues();

            if (nonNull(defaultListingValues)) {
                Integer existingDuration = caseData.getSchedulingAndListingFields().getDefaultListingValues().getDuration();
                if (nonNull(existingDuration)) {
                    if (isYes(caseData.getAppeal().getHearingOptions().getWantsToAttend())
                            && isInterpreterRequired(caseData)) {
                        return existingDuration + MIN_HEARING_DURATION;
                    } else {
                        return existingDuration;
                    }
                }
            }
        }

        if (NON_STANDARD.equals(durationType)) {
            Integer nextDuration = caseData.getAdjournment().getNextHearingListingDuration();
            if (nonNull(nextDuration)) {
                AdjournCaseNextHearingDurationUnits units = caseData.getAdjournment().getNextHearingListingDurationUnits();
                if (units == AdjournCaseNextHearingDurationUnits.SESSIONS && nextDuration >= MIN_HEARING_SESSION_DURATION) {
                    return nextDuration * DURATION_SESSIONS_MULTIPLIER;
                } else if (units == AdjournCaseNextHearingDurationUnits.MINUTES && nextDuration >= MIN_HEARING_DURATION) {
                    return nextDuration;
                }

                return DURATION_DEFAULT;
            }
        }

        return hearingDurationsService.getHearingDurationBenefitIssueCodes(caseData);
    }

    private void handleHearingWindow(SscsCaseData caseData, OverrideFields overrideFields) {
        HearingWindow hearingWindow = overrideFields.getHearingWindow();
        Adjournment adjournment = caseData.getAdjournment();
        AdjournCaseNextHearingDateType hearingDateType = adjournment.getNextHearingDateType();

        if (isNull(hearingDateType) || DATE_TO_BE_FIXED.equals(hearingDateType)) {
            return;
        }

        if (hearingWindow == null) {
            hearingWindow = HearingWindow.builder().build();
            overrideFields.setHearingWindow(hearingWindow);
        }

        AdjournCaseNextHearingDateOrPeriod hearingDateOrPeriod = adjournment.getNextHearingDateOrPeriod();

        if (FIRST_AVAILABLE_DATE.equals(hearingDateType)) {
            hearingWindow.setDateRangeStart(LocalDate.now().plusDays(FIRST_AVAILABLE_DATE_DAYS));
        }

        if (FIRST_AVAILABLE_DATE_AFTER.equals(hearingDateType)) {
            if (PROVIDE_DATE.equals(hearingDateOrPeriod)) {
                hearingWindow.setDateRangeStart(adjournment.getNextHearingFirstAvailableDateAfterDate());
            } else if (PROVIDE_PERIOD.equals(hearingDateOrPeriod)) {
                long after = Long.parseLong(adjournment.getNextHearingFirstAvailableDateAfterPeriod().toString());
                hearingWindow.setDateRangeStart(LocalDate.now().plusDays(after));
            }
        }
    }
}
