package uk.gov.hmcts.reform.sscs.tyanotifications.personalisation;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.getBenefitByCodeOrThrowException;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.getLongBenefitNameDescriptionWithOptionalAcronym;
import static uk.gov.hmcts.reform.sscs.ccd.domain.PanelComposition.JUDGE_DOCTOR_AND_DISABILITY_EXPERT_IF_APPLICABLE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.YES;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.AppConstants.*;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.NotificationEventTypeLists.EVENTS_WITH_SUBSCRIPTION_TYPE_DOCMOSIS_TEMPLATES;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.NotificationEventTypeLists.EVENTS_WITH_SUBSCRIPTION_TYPE_EMAIL_TEMPLATES;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.PersonalisationConfiguration.PersonalisationKey.*;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.PersonalisationMappingConstants.*;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.SubscriptionType.*;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.SubscriptionType.JOINT_PARTY;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.SubscriptionType.OTHER_PARTY;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.SubscriptionType.REPRESENTATIVE;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.*;
import static uk.gov.hmcts.reform.sscs.tyanotifications.personalisation.SyaAppealCreatedAndReceivedPersonalisation.TWO_NEW_LINES;
import static uk.gov.hmcts.reform.sscs.tyanotifications.personalisation.SyaAppealCreatedAndReceivedPersonalisation.getOptionalField;
import static uk.gov.hmcts.reform.sscs.tyanotifications.service.LetterUtils.getNameForOtherParty;
import static uk.gov.hmcts.reform.sscs.tyanotifications.service.NotificationUtils.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.exception.BenefitMappingException;
import uk.gov.hmcts.reform.sscs.service.RegionalProcessingCenterService;
import uk.gov.hmcts.reform.sscs.service.conversion.LocalDateToWelshStringConverter;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.*;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.properties.EvidenceProperties;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.NotificationSscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.SubscriptionWithType;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.Template;
import uk.gov.hmcts.reform.sscs.tyanotifications.extractor.HearingContactDateExtractor;
import uk.gov.hmcts.reform.sscs.tyanotifications.factory.NotificationWrapper;
import uk.gov.hmcts.reform.sscs.tyanotifications.service.LetterUtils;
import uk.gov.hmcts.reform.sscs.tyanotifications.service.MessageAuthenticationServiceImpl;
import uk.gov.hmcts.reform.sscs.tyanotifications.service.SendNotificationHelper;

@Component
@Slf4j
public class Personalisation<E extends NotificationWrapper> {

    private static final String CRLF = format("%c%c", (char) 0x0D, (char) 0x0A);
    public static final String TEMPLATE_NAME_TEMPLATE_WITH_DIRECTION_TYPE = "%s.%s.%s";
    public static final String TEMPLATE_NAME_TEMPLATE = "%s.%s";

    @Autowired
    protected NotificationConfig config;
    private boolean sendSmsSubscriptionConfirmation;
    @Autowired
    private HearingContactDateExtractor hearingContactDateExtractor;

    @Autowired
    private MessageAuthenticationServiceImpl macService;

    @Autowired
    private RegionalProcessingCenterService regionalProcessingCenterService;

    @Autowired
    private NotificationDateConverterUtil notificationDateConverterUtil;

    @Autowired
    private EvidenceProperties evidenceProperties;

    @Autowired
    private PersonalisationConfiguration personalisationConfiguration;

    @Value("${feature.postHearings.enabled}")
    private boolean isPostHearingsEnabled;

    private static String tya(Subscription subscription) {
        if (subscription != null) {
            return defaultIfBlank(subscription.getTya(), EMPTY);
        } else {
            return EMPTY;
        }
    }

    private static String email(Subscription subscription) {
        return subscription != null ? subscription.getEmail() : null;
    }

    private static boolean hasRegionalProcessingCenter(SscsCaseData ccdResponse) {
        return null != ccdResponse.getRegionalProcessingCenter()
            && null != ccdResponse.getRegionalProcessingCenter().getName();
    }

    protected static String getLatestInfoRequestDetail(SscsCaseData ccdResponse) {
        if (ccdResponse.getInfoRequests() != null) {
            List<AppellantInfoRequest> infoRequests = ccdResponse.getInfoRequests().getAppellantInfoRequest();

            if (infoRequests.isEmpty()) {
                return null;
            }

            AppellantInfoRequest latestAppellantInfoRequest = null;
            for (AppellantInfoRequest infoRequest : infoRequests) {
                latestAppellantInfoRequest = getLatestAppellantInfoRequest(latestAppellantInfoRequest, infoRequest);
            }

            if (latestAppellantInfoRequest != null && latestAppellantInfoRequest.getAppellantInfo() != null) {
                return latestAppellantInfoRequest.getAppellantInfo().getParagraph();
            }
        }

        return null;
    }

    private static AppellantInfoRequest getLatestAppellantInfoRequest(AppellantInfoRequest
                                                                          latestAppellantInfoRequest, AppellantInfoRequest infoRequest) {
        if (latestAppellantInfoRequest == null) {
            latestAppellantInfoRequest = infoRequest;
        } else {
            LocalDate latestDate = LocalDate.parse(latestAppellantInfoRequest.getAppellantInfo().getRequestDate(), CC_DATE_FORMAT);
            LocalDate currentDate = LocalDate.parse(infoRequest.getAppellantInfo().getRequestDate(), CC_DATE_FORMAT);
            if (currentDate.isAfter(latestDate)) {
                latestAppellantInfoRequest = infoRequest;
            }
        }
        return latestAppellantInfoRequest;
    }

    public static void translateToWelshDate(LocalDate localDate, SscsCaseData sscsCaseData, Consumer<? super String> placeholders) {
        if (sscsCaseData.isLanguagePreferenceWelsh()) {
            String translatedDate = LocalDateToWelshStringConverter.convert(localDate);
            placeholders.accept(translatedDate);
        }
    }


    public Map<String, Object> create(final E notificationWrapper, final SubscriptionWithType subscriptionWithType) {
        return create(notificationWrapper.getSscsCaseDataWrapper(), subscriptionWithType);
    }

    protected Map<String, Object> create(final NotificationSscsCaseDataWrapper responseWrapper, final SubscriptionWithType subscriptionWithType) {

        SscsCaseData ccdResponse = responseWrapper.getNewSscsCaseData();
        Map<String, Object> personalisation = new HashMap<>();
        Benefit benefit = null;

        try {
            if (hasBenefitType(ccdResponse)) {
                benefit = getBenefitByCodeOrThrowException(ccdResponse.getAppeal().getBenefitType().getCode());

                if (benefit.isHasAcronym()) {
                    personalisation.put(BENEFIT_NAME_ACRONYM_LITERAL, benefit.name());
                    personalisation.put(BENEFIT_NAME_ACRONYM_LITERAL_WELSH, benefit.name());
                } else {
                    personalisation.put(BENEFIT_NAME_ACRONYM_LITERAL, benefit.getDescription());
                    personalisation.put(BENEFIT_NAME_ACRONYM_LITERAL_WELSH, benefit.getWelshDescription());
                }

                personalisation.put(BENEFIT_NAME_ACRONYM_SHORT_LITERAL, benefit.name());
                personalisation.put(BENEFIT_FULL_NAME_LITERAL, benefit.getDescription());
                personalisation.put(BENEFIT_FULL_NAME_LITERAL_WELSH, benefit.getWelshDescription());
                personalisation.put(BENEFIT_NAME_AND_OPTIONAL_ACRONYM, getLongBenefitNameDescriptionWithOptionalAcronym(benefit.getShortName(), true));
                personalisation.put(BENEFIT_NAME_AND_OPTIONAL_ACRONYM_WELSH, getLongBenefitNameDescriptionWithOptionalAcronym(benefit.getShortName(), false));
            } else {
                log.warn("Proceeding with 'null' benefit type for case !");
            }
        } catch (BenefitMappingException bme) {
            log.warn("Proceeding with 'null' benefit type for case !");
        }

        translateToWelshDate(LocalDate.now(), ccdResponse, value -> personalisation.put(CURRENT_DATE_WELSH, value));

        PanelComposition panelComposition = ofNullable(benefit).map(Benefit::getPanelComposition).orElse(JUDGE_DOCTOR_AND_DISABILITY_EXPERT_IF_APPLICABLE);
        personalisation.put(PANEL_COMPOSITION, panelComposition.getEnglish());
        personalisation.put(PANEL_COMPOSITION_WELSH, panelComposition.getWelsh());

        LocalDate decisionPostedReceivedDate = LocalDate.now().plusDays(7);
        personalisation.put(DECISION_POSTED_RECEIVE_DATE, formatLocalDate(decisionPostedReceivedDate));
        translateToWelshDate(decisionPostedReceivedDate, ccdResponse, value -> personalisation.put(DECISION_POSTED_RECEIVE_DATE_WELSH, value));

        personalisation.put(APPEAL_REF, getAppealReference(ccdResponse));
        personalisation.put(APPELLANT_NAME, ccdResponse.getAppeal().getAppellant().getName().getFullNameNoTitle());
        personalisation.put(PersonalisationMappingConstants.NAME, getName(subscriptionWithType, ccdResponse, responseWrapper));
        personalisation.put(CCD_ID, defaultIfBlank(ccdResponse.getCcdCaseId(), EMPTY));
        personalisation.put(REPRESENTEE_NAME, subscriptionWithType.getParty().getName().getFullNameNoTitle());

        // Some templates (notably letters) can be sent out before the SC Ref is added to the case
        // this allows those templates to be populated with either the CCD Id or SC Ref
        personalisation.put(CASE_REFERENCE_ID, getAppealReference(ccdResponse));

        personalisation.put(INFO_REQUEST_DETAIL, defaultIfBlank(getLatestInfoRequestDetail(ccdResponse), EMPTY));

        Subscription subscription = subscriptionWithType.getSubscription();
        subscriptionDetails(personalisation, subscription, benefit, ccdResponse);

        addFirstTierAgencyFields(personalisation, benefit, ccdResponse);

        LocalDate createdDate = LocalDate.parse(ofNullable(ccdResponse.getCaseCreated()).orElse(LocalDate.now().toString()));
        translateToWelshDate(createdDate, ccdResponse, value -> personalisation.put(CREATED_DATE_WELSH, value));
        personalisation.put(CREATED_DATE, createdDate.toString());

        personalisation.put(JOINT, subscriptionWithType.getSubscriptionType() == JOINT_PARTY ? JOINT_TEXT_WITH_A_SPACE : EMPTY);
        personalisation.put(JOINT_WELSH, subscriptionWithType.getSubscriptionType() == JOINT_PARTY ? JOINT_TEXT_WITH_A_SPACE_WELSH : EMPTY);

        if (isYes(ccdResponse.getJointParty().getHasJointParty())) {
            personalisation.put(JOINT_PARTY_APPEAL, "Yes");
            personalisation.put(JOINT_PARTY_NAME, ccdResponse.getJointParty().getName().getFullNameNoTitle());
        } else {
            personalisation.put(JOINT_PARTY_APPEAL, "No");
        }

        Hearing latestHearing = ccdResponse.getLatestHearing();

        if (nonNull(latestHearing)) {
            HearingDetails latestHearingValue = latestHearing.getValue();
            LocalDateTime hearingDateTime = latestHearingValue.getHearingDateTime();
            if (HearingRoute.LIST_ASSIST == ccdResponse.getSchedulingAndListingFields().getHearingRoute()) {
                hearingDateTime = latestHearingValue.getStart();
            }

            personalisation.put(HEARING, latestHearing.getValue());

            if (nonNull(hearingDateTime) && nonNull(latestHearingValue.getVenue())) {
                personalisation.put(HEARING_DATE_LITERAL, hearingDateTime.toLocalDate().toString());
                translateToWelshDate(hearingDateTime.toLocalDate(), ccdResponse, value -> personalisation.put(HEARING_DATE_WELSH, value));
                personalisation.put(HEARING_TIME, formatLocalTime(hearingDateTime));
                personalisation.put(DAYS_TO_HEARING_LITERAL, calculateDaysToHearingText(hearingDateTime.toLocalDate()));
                personalisation.put(VENUE_ADDRESS_LITERAL, formatAddress(latestHearing));
                personalisation.put(VENUE_MAP_LINK_LITERAL, latestHearingValue.getVenue().getGoogleMapLink());
            }
        }

        setEvidenceProcessingAddress(personalisation, ccdResponse);

        NotificationEventType notificationEventType = responseWrapper.getNotificationEventType();
        setEventData(personalisation, ccdResponse, notificationEventType);
        setEvidenceReceivedNotificationData(personalisation, ccdResponse, notificationEventType);
        setHearingContactDate(personalisation, responseWrapper);

        LocalDate today = LocalDate.now();
        personalisation.put(TRIBUNAL_RESPONSE_DATE_LITERAL, notificationDateConverterUtil.toEmailDate(today.plusDays(56)));
        personalisation.put(ACCEPT_VIEW_BY_DATE_LITERAL, notificationDateConverterUtil.toEmailDate(today.plusDays(7)));
        personalisation.put(QUESTION_ROUND_EXPIRES_DATE_LITERAL, notificationDateConverterUtil.toEmailDate(today.plusDays(1)));

        final String tya = tya(subscription);
        personalisation.put(ONLINE_HEARING_REGISTER_LINK_LITERAL, config.getOnlineHearingLink() + "/register?tya=" + tya);
        personalisation.put(ONLINE_HEARING_SIGN_IN_LINK_LITERAL, config.getOnlineHearingLink() + "/sign-in");

        personalisation.put(APPOINTEE_DESCRIPTION, getAppointeeDescription(subscriptionWithType.getSubscriptionType(), ccdResponse));
        personalisation.put(APPOINTEE_NAME, getName(APPOINTEE, ccdResponse, responseWrapper));

        personalisation.put(HEARING_TYPE, responseWrapper.getNewSscsCaseData().getAppeal().getHearingType());

        if (subscriptionWithType.getSubscriptionType() == REPRESENTATIVE) {
            personalisation.put(PersonalisationMappingConstants.REPRESENTATIVE, "Yes");
        }

        if (subscriptionWithType.getSubscriptionType() == JOINT_PARTY) {
            personalisation.put(PersonalisationMappingConstants.JOINT_PARTY, "Yes");
        }

        setConfidentialFields(ccdResponse, subscriptionWithType, personalisation);

        setHelplineTelephone(ccdResponse, personalisation);
        if (subscriptionWithType.getSubscriptionType() == OTHER_PARTY) {
            personalisation.put(PersonalisationMappingConstants.OTHER_PARTY, personalisation.get(PersonalisationMappingConstants.NAME));
        }

        personalisation.put(PARTY_TYPE, subscriptionWithType.getParty().getClass().getSimpleName());
        personalisation.put(ENTITY_TYPE, subscriptionWithType.getEntity().getClass().getSimpleName());

        if (isPostHearingsEnabled) {
            LocalDate finalDecisionDate = ccdResponse.getSscsFinalDecisionCaseData().getFinalDecisionIssuedDate();

            if (nonNull(finalDecisionDate)) {
                String formattedDate = finalDecisionDate.format(DateTimeFormatter.ofPattern(FINAL_DECISION_DATE_FORMAT));
                personalisation.put(FINAL_DECISION_DATE, formattedDate);
            }
        }

        boolean isGranted = isGranted(ccdResponse.getDwpState());

        if (isGranted
            || PERMISSION_TO_APPEAL_REFUSED.equals(notificationEventType)
            || REVIEW_AND_SET_ASIDE.equals(notificationEventType)
            || BUNDLE_CREATED_FOR_UPPER_TRIBUNAL.equals(notificationEventType)) {
            setDecisionDate(personalisation, ccdResponse);
        }

        personalisation.put(IS_GRANTED, isGranted);
        personalisation.put(SENDER_NAME, LetterUtils.getNameForSender(ccdResponse));

        return personalisation;
    }

    private void setDecisionDate(Map<String, Object> personalisation, SscsCaseData ccdResponse) {
        if (isNull(ccdResponse.getSscsDocument())) {
            return;
        }

        ccdResponse.getSscsDocument().stream()
            .filter(Personalisation::hasFinalDecisionNoticeDocumentType)
            .max(Comparator.comparing(d -> LocalDate.parse(d.getValue().getDocumentDateAdded())))
            .ifPresent(document -> {
                personalisation.put(DECISION_DATE_LITERAL, document.getValue().getDocumentDateAdded());
            });
    }

    private static boolean hasFinalDecisionNoticeDocumentType(SscsDocument document) {
        return DocumentType.FINAL_DECISION_NOTICE.getValue().equals(document.getValue().getDocumentType());
    }

    private static boolean isGranted(DwpState dwpState) {
        return DwpState.SET_ASIDE_GRANTED.equals(dwpState)
            || DwpState.LIBERTY_TO_APPLY_GRANTED.equals(dwpState)
            || DwpState.CORRECTION_GRANTED.equals(dwpState)
            || DwpState.PERMISSION_TO_APPEAL_GRANTED.equals(dwpState)
            || DwpState.CORRECTED_DECISION_NOTICE_ISSUED.equals(dwpState);
    }

    private boolean hasBenefitType(SscsCaseData ccdResponse) {
        return ccdResponse.getAppeal() != null
            && ccdResponse.getAppeal().getBenefitType() != null
            && !isEmpty(ccdResponse.getAppeal().getBenefitType().getCode());
    }

    private void addFirstTierAgencyFields(Map<String, Object> personalisation, Benefit benefit, SscsCaseData ccdResponse) {
        personalisation.put(FIRST_TIER_AGENCY_ACRONYM, isHmrcBenefit(benefit, ccdResponse.getFormType()) ? HMRC_ACRONYM : DWP_ACRONYM);
        personalisation.put(FIRST_TIER_AGENCY_ACRONYM_WELSH, isHmrcBenefit(benefit, ccdResponse.getFormType()) ? HMRC_ACRONYM_WELSH : DWP_ACRONYM_WELSH);
        personalisation.put(FIRST_TIER_AGENCY_FULL_NAME, isHmrcBenefit(benefit, ccdResponse.getFormType()) ? HMRC_FULL_NAME : DWP_FULL_NAME);
        personalisation.put(FIRST_TIER_AGENCY_FULL_NAME_WELSH, isHmrcBenefit(benefit, ccdResponse.getFormType()) ? HMRC_FULL_NAME_WELSH : DWP_FULL_NAME_WELSH);
        personalisation.put(FIRST_TIER_AGENCY_GROUP, isHmrcBenefit(benefit, ccdResponse.getFormType()) ? HMRC_ACRONYM : DWP_FIRST_TIER_AGENCY_GROUP);
        personalisation.put(FIRST_TIER_AGENCY_GROUP_TITLE, isHmrcBenefit(benefit, ccdResponse.getFormType()) ? HMRC_ACRONYM : DWP_FIRST_TIER_AGENCY_GROUP_TITLE);
        personalisation.put(FIRST_TIER_AGENCY_GROUP_WELSH, isHmrcBenefit(benefit, ccdResponse.getFormType()) ? HMRC_ACRONYM : DWP_FIRST_TIER_AGENCY_GROUP_WELSH);
        personalisation.put(FIRST_TIER_AGENCY_OFFICE, ccdResponse.getDwpRegionalCentre());
        personalisation.put(WITH_OPTIONAL_THE, isHmrcBenefit(benefit, ccdResponse.getFormType()) ? "" : THE_STRING);
        personalisation.put(WITH_OPTIONAL_THE_WELSH, isHmrcBenefit(benefit, ccdResponse.getFormType()) ? "" : THE_STRING_WELSH);
    }

    private boolean isHmrcBenefit(Benefit benefit, FormType formType) {
        if (benefit == null) {
            return FormType.SSCS5.equals(formType);
        }
        return SscsType.SSCS5.equals(benefit.getSscsType());
    }

    private void setHelplineTelephone(SscsCaseData ccdResponse, Map<String, Object> personalisation) {
        if ("yes".equalsIgnoreCase(ccdResponse.getIsScottishCase())) {
            personalisation.put(HELPLINE_PHONE_NUMBER, config.getHelplineTelephoneScotland());
        } else {
            personalisation.put(HELPLINE_PHONE_NUMBER, config.getHelplineTelephone());
        }
    }

    private void setConfidentialFields(SscsCaseData ccdResponse, SubscriptionWithType subscriptionWithType, Map<String, Object> personalisation) {
        if (subscriptionWithType.getSubscriptionType().equals(JOINT_PARTY) && null != ccdResponse.getConfidentialityRequestOutcomeJointParty()) {
            personalisation.put(OTHER_PARTY_NAME, ccdResponse.getAppeal().getAppellant().getName().getFullNameNoTitle());
            personalisation.put(CONFIDENTIALITY_OUTCOME, getRequestOutcome(ccdResponse.getConfidentialityRequestOutcomeJointParty()));

        } else if (subscriptionWithType.getSubscriptionType().equals(APPELLANT) && null != ccdResponse.getJointParty().getName() && null != ccdResponse.getConfidentialityRequestOutcomeAppellant()) {
            personalisation.put(OTHER_PARTY_NAME, ccdResponse.getJointParty().getName().getFullNameNoTitle());
            personalisation.put(CONFIDENTIALITY_OUTCOME, getRequestOutcome(ccdResponse.getConfidentialityRequestOutcomeAppellant()));
        }
    }

    private String getRequestOutcome(DatedRequestOutcome datedRequestOutcome) {
        return datedRequestOutcome == null || datedRequestOutcome.getRequestOutcome() == null ? null : datedRequestOutcome.getRequestOutcome().getValue();
    }

    private String getAppealReference(SscsCaseData ccdResponse) {
        final String caseReference = ccdResponse.getCaseReference();
        return isBlank(caseReference) || (ccdResponse.getCreatedInGapsFrom() != null && ccdResponse.getCreatedInGapsFrom().equals("readyToList"))
            ? ccdResponse.getCcdCaseId() : caseReference;
    }

    private String getName(SubscriptionType subscriptionType, SscsCaseData sscsCaseData, NotificationSscsCaseDataWrapper wrapper) {
        if (sscsCaseData.getAppeal() == null) {
            return EMPTY;
        }

        if (subscriptionType.equals(APPELLANT)
            && sscsCaseData.getAppeal().getAppellant() != null) {
            return getDefaultName(sscsCaseData.getAppeal().getAppellant().getName());
        } else if (subscriptionType.equals(REPRESENTATIVE)
            && hasRepresentative(wrapper)) {
            return SendNotificationHelper.getRepSalutation(sscsCaseData.getAppeal().getRep(), true);
        } else if (subscriptionType.equals(APPOINTEE)
            && hasAppointee(wrapper)) {
            return getDefaultName(sscsCaseData.getAppeal().getAppellant().getAppointee().getName());
        } else if (subscriptionType.equals(JOINT_PARTY) && hasJointParty(sscsCaseData)) {
            Name partyName = sscsCaseData.getJointParty().getName();
            return (partyName == null) ? EMPTY :
                getDefaultName(new Name(partyName.getTitle(), partyName.getFirstName(), partyName.getLastName()));
        }
        return EMPTY;
    }

    private String getName(SubscriptionWithType subscriptionWithType, SscsCaseData sscsCaseData, NotificationSscsCaseDataWrapper wrapper) {
        if (nonNull(sscsCaseData.getAppeal()) && nonNull(subscriptionWithType.getPartyId())) {
            return getDefaultName(getNameForOtherParty(sscsCaseData, subscriptionWithType.getPartyId()).orElse(null));
        }
        return getName(subscriptionWithType.getSubscriptionType(), sscsCaseData, wrapper);
    }


    private String getDefaultName(Name name) {
        return name == null || name.getFirstName() == null || isBlank(name.getFirstName())
            || name.getLastName() == null || isBlank(name.getLastName()) ? EMPTY : name.getFullNameNoTitle();
    }

    private String getAppointeeDescription(SubscriptionType subscriptionType, SscsCaseData ccdResponse) {
        if (APPOINTEE.equals(subscriptionType) && ccdResponse.getAppeal() != null
            && ccdResponse.getAppeal().getAppellant().getName() != null) {
            return format("You are receiving this update as the appointee for %s.%s%s",
                ccdResponse.getAppeal().getAppellant().getName().getFullNameNoTitle(), CRLF, CRLF);
        } else {
            return EMPTY;
        }
    }

    private void subscriptionDetails(Map<String, Object> personalisation, Subscription subscription, Benefit benefit, SscsCaseData sscsCaseData) {
        final String tya = tya(subscription);
        personalisation.put(APPEAL_ID_LITERAL, tya);
        if (benefit != null) {
            personalisation.put(MANAGE_EMAILS_LINK_LITERAL, config.getManageEmailsLink().replace(MAC_LITERAL, getMacToken(tya, benefit.name())));
        }
        if (equalsIgnoreCase(State.READY_TO_LIST.getId(), sscsCaseData.getCreatedInGapsFrom())) {
            personalisation.put(TRACK_APPEAL_LINK_LITERAL, config.getMyaLink() != null ? config.getMyaLink().replace(APPEAL_ID_LITERAL, tya) : null);
            personalisation.put(CLAIMING_EXPENSES_LINK_LITERAL, config.getMyaClaimingExpensesLink().getLinkUrl());
            personalisation.put(SUBMIT_EVIDENCE_LINK_LITERAL, config.getMyaEvidenceSubmissionInfoLink().getLinkUrl());
            personalisation.put(SUBMIT_EVIDENCE_INFO_LINK_LITERAL, config.getMyaEvidenceSubmissionInfoLink().getLinkUrl());
            personalisation.put(HEARING_INFO_LINK_LITERAL, config.getMyaHearingInfoLink().getLinkUrl());
        } else {
            personalisation.put(TRACK_APPEAL_LINK_LITERAL, config.getTrackAppealLink() != null ? config.getTrackAppealLink().replace(APPEAL_ID_LITERAL, tya) : null);
            personalisation.put(CLAIMING_EXPENSES_LINK_LITERAL, config.getClaimingExpensesLink().replace(APPEAL_ID_LITERAL, tya));
            personalisation.put(SUBMIT_EVIDENCE_LINK_LITERAL, config.getEvidenceSubmissionInfoLink().replace(APPEAL_ID_LITERAL, tya));
            personalisation.put(SUBMIT_EVIDENCE_INFO_LINK_LITERAL, config.getEvidenceSubmissionInfoLink().replace(APPEAL_ID_LITERAL, tya));
            personalisation.put(HEARING_INFO_LINK_LITERAL, config.getHearingInfoLink().replace(APPEAL_ID_LITERAL, tya));
        }

        String email = email(subscription);
        if (email != null) {
            String encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8);
            personalisation.put(ONLINE_HEARING_LINK_LITERAL, config.getOnlineHearingLinkWithEmail().replace("{email}", encodedEmail));
        }
    }

    void setHearingContactDate(Map<String, Object> personalisation, NotificationSscsCaseDataWrapper wrapper) {
        Optional<ZonedDateTime> hearingContactDate = hearingContactDateExtractor.extract(wrapper);
        hearingContactDate.ifPresent(zonedDateTime -> personalisation.put(HEARING_CONTACT_DATE,
            formatLocalDate(zonedDateTime.toLocalDate())
        ));
    }

    Map<String, Object> setEventData(Map<String, Object> personalisation, SscsCaseData ccdResponse, NotificationEventType notificationEventType) {
        if (ccdResponse.getCreatedInGapsFrom() != null && ccdResponse.getCreatedInGapsFrom().equals("readyToList")) {
            LocalDate localDate = LocalDate.parse(ofNullable(ccdResponse.getDateSentToDwp()).orElse(LocalDate.now().toString())).plusDays(calculateMaxDwpResponseDays(ccdResponse.getBenefitCode()));
            String dwpResponseDateString = formatLocalDate(localDate);
            personalisation.put(APPEAL_RESPOND_DATE, dwpResponseDateString);
            translateToWelshDate(localDate, ccdResponse, value ->
                personalisation.put(APPEAL_RESPOND_DATE_WELSH, value)
            );

            return personalisation;
        } else if (ccdResponse.getEvents() != null) {
            //FIXME: Remove this block once digital RTL journey is live

            for (Event event : ccdResponse.getEvents()) {
                if ((event.getValue() != null) && isAppealReceivedAndUpdated(notificationEventType, event)
                    || notificationEventType.equals(CASE_UPDATED)
                    || JUDGE_DECISION_APPEAL_TO_PROCEED.equals(notificationEventType)
                    || TCW_DECISION_APPEAL_TO_PROCEED.equals(notificationEventType)) {
                    return setAppealReceivedDetails(personalisation, event.getValue(), ccdResponse);
                }
            }
        }
        return personalisation;
    }

    private boolean isAppealReceivedAndUpdated(NotificationEventType notificationEventType, Event event) {
        return NotificationEventType.APPEAL_RECEIVED == notificationEventType
            && EventType.APPEAL_RECEIVED == event.getValue().getEventType();
    }

    Map<String, Object> setEvidenceReceivedNotificationData(Map<String, Object> personalisation,
                                                            SscsCaseData ccdResponse,
                                                            NotificationEventType notificationEventType) {
        if (notificationEventType == EVIDENCE_RECEIVED) {
            if (ccdResponse.getEvidence() != null && ccdResponse.getEvidence().getDocuments() != null
                && !ccdResponse.getEvidence().getDocuments().isEmpty()) {
                LocalDate evidenceDateTimeFormatted = ccdResponse.getEvidence().getDocuments().get(0).getValue()
                    .getEvidenceDateTimeFormatted();
                personalisation.put(EVIDENCE_RECEIVED_DATE_LITERAL,
                    formatLocalDate(evidenceDateTimeFormatted));
                translateToWelshDate(evidenceDateTimeFormatted, ccdResponse, value ->
                    personalisation.put(EVIDENCE_RECEIVED_DATE_LITERAL_WELSH, value)
                );
            } else {
                personalisation.put(EVIDENCE_RECEIVED_DATE_LITERAL, EMPTY);
                personalisation.put(EVIDENCE_RECEIVED_DATE_LITERAL_WELSH, EMPTY);
            }
        }
        return personalisation;

    }

    private Map<String, Object> setAppealReceivedDetails(Map<String, Object> personalisation, EventDetails eventDetails, SscsCaseData ccdResponse) {
        LocalDate localDate = eventDetails.getDateTime().plusDays(calculateMaxDwpResponseDays(ccdResponse.getAppeal().getBenefitType().getCode())).toLocalDate();
        String dwpResponseDateString = formatLocalDate(localDate);
        personalisation.put(APPEAL_RESPOND_DATE, dwpResponseDateString);
        translateToWelshDate(localDate, ccdResponse, value ->
            personalisation.put(APPEAL_RESPOND_DATE_WELSH, value)
        );
        return personalisation;
    }


    Map<String, Object> setEvidenceProcessingAddress(Map<String, Object> personalisation, SscsCaseData ccdResponse) {
        RegionalProcessingCenter rpc;

        if (hasRegionalProcessingCenter(ccdResponse)) {
            rpc = ccdResponse.getRegionalProcessingCenter();
        } else {
            rpc = regionalProcessingCenterService.getByScReferenceCode(ccdResponse.getCaseReference());
        }

        if (EventType.READY_TO_LIST.getCcdType().equals(ccdResponse.getCreatedInGapsFrom())) {
            personalisation.put(REGIONAL_OFFICE_NAME_LITERAL, evidenceProperties.getAddress().getLine1());
            personalisation.put(SUPPORT_CENTRE_NAME_LITERAL, evidenceProperties.getAddress().getLine2());
            personalisation.put(ADDRESS_LINE_LITERAL, evidenceProperties.getAddress().getLine3(ccdResponse));
            personalisation.put(TOWN_LITERAL, evidenceProperties.getAddress().getTown());
            personalisation.put(COUNTY_LITERAL, evidenceProperties.getAddress().getCounty());
            personalisation.put(POSTCODE_LITERAL, evidenceProperties.getAddress().getPostcode(ccdResponse));
            personalisation.put(REGIONAL_OFFICE_POSTCODE_LITERAL, evidenceProperties.getAddress().getPostcode());
        } else if (rpc != null) {
            personalisation.put(REGIONAL_OFFICE_NAME_LITERAL, rpc.getAddress1());
            personalisation.put(SUPPORT_CENTRE_NAME_LITERAL, rpc.getAddress2());
            personalisation.put(ADDRESS_LINE_LITERAL, rpc.getAddress3());
            personalisation.put(TOWN_LITERAL, rpc.getAddress4());
            personalisation.put(COUNTY_LITERAL, rpc.getCity());
            personalisation.put(POSTCODE_LITERAL, rpc.getPostcode());
            personalisation.put(REGIONAL_OFFICE_POSTCODE_LITERAL, rpc.getPostcode());
        }

        personalisation.put(PHONE_NUMBER_WELSH, evidenceProperties.getAddress().getTelephoneWelsh());
        personalisation.put(PHONE_NUMBER, determinePhoneNumber(rpc));

        setHearingArrangementDetails(personalisation, ccdResponse);

        return personalisation;
    }

    private String determinePhoneNumber(RegionalProcessingCenter rpc) {

        if (rpc != null) {
            return rpc.getPhoneNumber();
        } else {
            return evidenceProperties.getAddress().getTelephone();
        }
    }

    private String formatAddress(Hearing hearing) {
        return newArrayList(hearing.getValue().getVenue().getName(),
            hearing.getValue().getVenue().getAddress().getFullAddress())
            .stream()
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.joining(", "));
    }

    private String calculateDaysToHearingText(LocalDate hearingDate) {
        long daysBetween = ChronoUnit.DAYS.between(LocalDate.now(), hearingDate);

        return daysBetween == 1 ? TOMORROW_STRING : "in " + daysBetween + DAYS_STRING;
    }

    private String getMacToken(String id, String benefitType) {
        return macService.generateToken(id, benefitType);
    }

    private String formatLocalDate(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern(RESPONSE_DATE_FORMAT));
    }

    private String formatLocalTime(LocalDateTime date) {
        return date.format(DateTimeFormatter.ofPattern(HEARING_TIME_FORMAT, Locale.ENGLISH)).toUpperCase();
    }

    public Template getTemplate(E notificationWrapper, Benefit benefit, SubscriptionType subscriptionType) {

        String templateConfig = getEmailTemplateName(subscriptionType, notificationWrapper);
        String smsTemplateName = templateConfig;
        if (isSendSmsSubscriptionConfirmation()) {
            smsTemplateName = getSubscriptionTemplateName(SUBSCRIPTION_CREATED, subscriptionType);
        }

        String letterTemplateName = getLetterTemplateName(subscriptionType, notificationWrapper.getNotificationType());

        String docmosisTemplateName = getDocmosisTemplateName(subscriptionType, notificationWrapper.getNotificationType(), notificationWrapper.getNewSscsCaseData());

        log.info("Trying to get docmosis template from config with name {}", docmosisTemplateName);

        return config.getTemplate(templateConfig, smsTemplateName, letterTemplateName, docmosisTemplateName,
            benefit, notificationWrapper, notificationWrapper.getNewSscsCaseData().getCreatedInGapsFrom());
    }

    public int calculateMaxDwpResponseDays(String benefitCode) {
        if (benefitCode != null && benefitCode.equals("childSupport")) {
            return MAX_DWP_RESPONSE_DAYS_CHILD_SUPPORT;
        } else {
            return MAX_DWP_RESPONSE_DAYS;
        }
    }

    private String getEmailTemplateName(SubscriptionType subscriptionType,
                                        NotificationWrapper notificationWrapper) {

        NotificationEventType notificationEventType = notificationWrapper.getNotificationType();

        if (EVENTS_WITH_SUBSCRIPTION_TYPE_EMAIL_TEMPLATES.contains(notificationEventType)) {
            return getSubscriptionTemplateName(notificationEventType, subscriptionType);
        }

        return notificationEventType.getId();
    }

    private String getDocmosisTemplateName(SubscriptionType subscriptionType, NotificationEventType notificationEventType, SscsCaseData caseData) {
        if (isNull(subscriptionType)) {
            return notificationEventType.getId();
        }

        String directionType = Optional.ofNullable(caseData.getDirectionTypeDl())
            .map(DynamicList::getValue)
            .map(DynamicListItem::getCode)
            .orElse(null);

        if (nonNull(directionType)
            && (DIRECTION_ISSUED.equals(notificationEventType)
            || DIRECTION_ISSUED_WELSH.equals(notificationEventType))) {
            return getSubscriptionTemplateNameWithDirection(notificationEventType, directionType, subscriptionType);
        }

        if (EVENTS_WITH_SUBSCRIPTION_TYPE_DOCMOSIS_TEMPLATES.contains(notificationEventType)) {
            return getSubscriptionTemplateName(notificationEventType, subscriptionType);
        }

        return notificationEventType.getId();
    }

    private String getLetterTemplateName(SubscriptionType subscriptionType,
                                         NotificationEventType notificationEventType) {
        if (nonNull(subscriptionType)
            && NotificationEventTypeLists.EVENT_TYPES_FOR_NOTIFY_LETTERS.contains(notificationEventType)) {
            return getSubscriptionTemplateName(notificationEventType, subscriptionType);
        }
        return notificationEventType.getId();
    }

    boolean isSendSmsSubscriptionConfirmation() {
        return sendSmsSubscriptionConfirmation;
    }

    void setSendSmsSubscriptionConfirmation(Boolean sendSmsSubscriptionConfirmation) {
        this.sendSmsSubscriptionConfirmation = sendSmsSubscriptionConfirmation;
    }

    protected Map<String, Object> setHearingArrangementDetails(Map<String, Object> personalisation, SscsCaseData ccdResponse) {
        if (null != ccdResponse.getAppeal() && null != ccdResponse.getAppeal().getHearingOptions()) {
            personalisation.put(HEARING_ARRANGEMENT_DETAILS_LITERAL, buildHearingArrangements(ccdResponse.getAppeal().getHearingOptions(), personalisationConfiguration.getPersonalisation().get(LanguagePreference.ENGLISH)));

            if (ccdResponse.isLanguagePreferenceWelsh()) {
                personalisation.put(HEARING_ARRANGEMENT_DETAILS_LITERAL_WELSH, buildHearingArrangements(ccdResponse.getAppeal().getHearingOptions(), personalisationConfiguration.getPersonalisation().get(LanguagePreference.WELSH)));
            }
        }
        return personalisation;
    }

    private String buildHearingArrangements(HearingOptions hearingOptions, Map<String, String> titleText) {
        if (null != hearingOptions) {

            String languageInterpreterRequired = convertBooleanToRequiredText(hearingOptions.getLanguageInterpreter() != null
                && StringUtils.equalsIgnoreCase(YES, hearingOptions.getLanguageInterpreter()), titleText);

            return titleText.get(LANGUAGE_INTERPRETER.name()) + languageInterpreterRequired + TWO_NEW_LINES + titleText.get(SIGN_INTERPRETER.name())
                + convertBooleanToRequiredText(findHearingArrangement("signLanguageInterpreter", hearingOptions.getArrangements()), titleText)
                + TWO_NEW_LINES + titleText.get(HEARING_LOOP.name()) + convertBooleanToRequiredText(findHearingArrangement("hearingLoop", hearingOptions.getArrangements()), titleText)
                + TWO_NEW_LINES + titleText.get(DISABLED_ACCESS.name()) + convertBooleanToRequiredText(findHearingArrangement("disabledAccess", hearingOptions.getArrangements()), titleText)
                + TWO_NEW_LINES + titleText.get(OTHER_ARRANGEMENTS.name()) + getOptionalField(hearingOptions.getOther(), titleText.get(PersonalisationConfiguration.PersonalisationKey.NOT_REQUIRED.name()));
        }
        return null;
    }

    private Boolean findHearingArrangement(String field, List<String> arrangements) {
        return arrangements != null && arrangements.contains(field);
    }

    private String convertBooleanToRequiredText(boolean bool, Map<String, String> titleText) {
        return bool ? titleText.get(PersonalisationConfiguration.PersonalisationKey.REQUIRED.name()) : titleText.get(PersonalisationConfiguration.PersonalisationKey.NOT_REQUIRED.name());
    }

    private static String getSubscriptionTemplateName(NotificationEventType notificationEventType,
                                                      SubscriptionType subscriptionType) {
        return String.format(TEMPLATE_NAME_TEMPLATE,
            notificationEventType.getId(),
            subscriptionType.name().toLowerCase());
    }

    private static String getSubscriptionTemplateNameWithDirection(NotificationEventType notificationEventType,
                                                                   String directionType,
                                                                   SubscriptionType subscriptionType) {
        return String.format(TEMPLATE_NAME_TEMPLATE_WITH_DIRECTION_TYPE,
            notificationEventType.getId(),
            directionType,
            subscriptionType.name().toLowerCase());
    }
}
