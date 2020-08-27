package uk.gov.hmcts.reform.sscs.transform.deserialize;

import static uk.gov.hmcts.reform.sscs.service.CaseCodeService.*;
import static uk.gov.hmcts.reform.sscs.utility.AppealNumberGenerator.generateAppealNumber;
import static uk.gov.hmcts.reform.sscs.utility.PhoneNumbersUtil.cleanPhoneNumber;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.domain.wrapper.*;
import uk.gov.hmcts.reform.sscs.service.DwpAddressLookupService;
import uk.gov.hmcts.reform.sscs.utility.PhoneNumbersUtil;

public final class SubmitYourAppealToCcdCaseDataDeserializer {

    private static final String YES = "Yes";
    private static final String NO = "No";


    private SubmitYourAppealToCcdCaseDataDeserializer() {
        // Empty
    }

    public static SscsCaseData convertSyaToCcdCaseData(SyaCaseWrapper syaCaseWrapper, String region, RegionalProcessingCenter rpc) {
        SscsCaseData caseData = convertSyaToCcdCaseData(syaCaseWrapper);

        return caseData.toBuilder()
                .region(region)
                .regionalProcessingCenter(rpc)
                .build();
    }

    public static SscsCaseData convertSyaToCcdCaseData(SyaCaseWrapper syaCaseWrapper) {
        Appeal appeal = getAppeal(syaCaseWrapper);

        boolean isDraft = isDraft(syaCaseWrapper);

        String benefitCode = isDraft ? null : generateBenefitCode(appeal.getBenefitType().getCode());
        String issueCode = isDraft ? null : generateIssueCode();
        String caseCode = isDraft ? null : generateCaseCode(benefitCode, issueCode);

        List<SscsDocument> sscsDocuments = getEvidenceDocumentDetails(syaCaseWrapper);
        return SscsCaseData.builder()
                .caseCreated(LocalDate.now().toString())
                .appeal(appeal)
                .subscriptions(getSubscriptions(syaCaseWrapper))
                .sscsDocument(sscsDocuments.isEmpty() ? Collections.emptyList() : sscsDocuments)
                .evidencePresent(hasEvidence(syaCaseWrapper.getEvidenceProvide()))
                .benefitCode(benefitCode)
                .issueCode(issueCode)
                .caseCode(caseCode)
                .dwpRegionalCentre(getDwpRegionalCenterGivenDwpIssuingOffice(appeal.getBenefitType().getCode(),
                        appeal.getMrnDetails().getDwpIssuingOffice()))
                .pcqId(syaCaseWrapper.getPcqId())
                .languagePreferenceWelsh(booleanToYesNo(syaCaseWrapper.getLanguagePreferenceWelsh()))
                .build();
    }

    private static String getDwpRegionalCenterGivenDwpIssuingOffice(String benefitTypeCode, String dwpIssuingOffice) {
        if (dwpIssuingOffice == null) {
            return null;
        }
        DwpAddressLookupService dwpAddressLookupService = new DwpAddressLookupService();
        return dwpAddressLookupService.getDwpRegionalCenterByBenefitTypeAndOffice(benefitTypeCode, dwpIssuingOffice);
    }

    private static boolean isDraft(SyaCaseWrapper syaCaseWrapper) {
        if (syaCaseWrapper.getCaseType() == null) {
            return false;
        }
        return syaCaseWrapper.getCaseType().equals("draft");
    }

    private static String booleanToYesNo(Boolean flag) {
        if (flag == null) {
            return null;
        }
        return flag ? "Yes" : "No";
    }

    private static Subscriptions getSubscriptions(SyaCaseWrapper syaCaseWrapper) {
        return populateSubscriptions(syaCaseWrapper);
    }


    private static Appeal getAppeal(SyaCaseWrapper syaCaseWrapper) {

        MrnDetails mrnDetails = getMrnDetails(syaCaseWrapper);

        Appellant appellant = getAppellant(syaCaseWrapper);

        BenefitType benefitType = BenefitType.builder()
                .code(syaCaseWrapper.getBenefitType().getCode())
                .description(syaCaseWrapper.getBenefitType().getDescription())
                .build();

        HearingOptions hearingOptions = getHearingOptions(syaCaseWrapper.getSyaHearingOptions());

        AppealReasons appealReasons = getReasonsForAppealing(syaCaseWrapper.getReasonsForAppealing());

        Representative representative = getRepresentative(syaCaseWrapper);

        return Appeal.builder()
                .mrnDetails(mrnDetails)
                .appellant(appellant)
                .benefitType(benefitType)
                .hearingOptions(hearingOptions)
                .appealReasons(appealReasons)
                .rep(representative)
                .signer(syaCaseWrapper.getSignAndSubmit() != null ? syaCaseWrapper.getSignAndSubmit().getSigner() : null)
                .hearingType(getHearingType(hearingOptions))
                .receivedVia("Online")
                .build();
    }

    private static String getHearingType(HearingOptions hearingOptions) {
        if (hearingOptions == null || hearingOptions.getWantsToAttend() == null) {
            return null;
        }
        return YES.equals(hearingOptions.getWantsToAttend()) ? HearingType.ORAL.getValue() :
                HearingType.PAPER.getValue();
    }

    private static MrnDetails getMrnDetails(SyaCaseWrapper syaCaseWrapper) {
        return MrnDetails.builder()
                .dwpIssuingOffice(getDwpIssuingOffice(syaCaseWrapper))
                .mrnDate(getMrnDate(syaCaseWrapper))
                .mrnLateReason(getReasonForBeingLate(syaCaseWrapper))
                .mrnMissingReason(getReasonForNoMrn(syaCaseWrapper))
                .build();

    }

    private static String getReasonForNoMrn(SyaCaseWrapper syaCaseWrapper) {
        if (mrnIsNull(syaCaseWrapper)) {
            return null;
        }
        return syaCaseWrapper.getMrn().getReasonForNoMrn();
    }

    private static boolean mrnIsNull(SyaCaseWrapper syaCaseWrapper) {
        return null == syaCaseWrapper.getMrn();
    }

    private static boolean mrnIsNotProvided(SyaCaseWrapper syaCaseWrapper) {
        if (mrnIsNull(syaCaseWrapper)) {
            return true;
        }

        return "DWP PIP (undefined)".equalsIgnoreCase(syaCaseWrapper.getMrn().getDwpIssuingOffice());
    }

    private static String getReasonForBeingLate(SyaCaseWrapper syaCaseWrapper) {
        if (mrnIsNull(syaCaseWrapper)) {
            return null;
        }
        return syaCaseWrapper.getMrn().getReasonForBeingLate();
    }

    private static String getDwpIssuingOffice(SyaCaseWrapper syaCaseWrapper) {
        String benefitType = syaCaseWrapper.getBenefitType().getCode();
        if (benefitType.equalsIgnoreCase("uc")
                && (syaCaseWrapper.getMrn() == null
                || syaCaseWrapper.getMrn().getDwpIssuingOffice() == null)) {
            return "Universal Credit";
        } else {
            if (!mrnIsNotProvided(syaCaseWrapper)) {
                String dwpIssuingOffice = syaCaseWrapper.getMrn().getDwpIssuingOffice();
                if (dwpIssuingOffice != null) {
                    return new DwpAddressLookupService().getDwpMappingByOffice(benefitType, dwpIssuingOffice)
                            .map(office -> office.getMapping().getCcd())
                            .orElse(null);
                }
            }
            return null;
        }
    }

    private static String getMrnDate(SyaCaseWrapper syaCaseWrapper) {
        if (mrnIsNull(syaCaseWrapper)) {
            return null;
        }
        return syaCaseWrapper.getMrn().getDate() != null ? syaCaseWrapper.getMrn().getDate().toString() :
                null;
    }

    private static Appellant getAppellant(SyaCaseWrapper syaCaseWrapper) {

        SyaAppellant syaAppellant = syaCaseWrapper.getAppellant();

        SyaContactDetails contactDetails = (null != syaAppellant) ? syaAppellant.getContactDetails() : null;

        Address address = null;
        Contact contact;
        if (null != contactDetails) {
            address = Address.builder()
                    .line1(contactDetails.getAddressLine1())
                    .line2(contactDetails.getAddressLine2())
                    .town(contactDetails.getTownCity())
                    .county(contactDetails.getCounty())
                    .postcode(contactDetails.getPostCode())
                    .postcodeLookup(contactDetails.getPostcodeLookup())
                    .postcodeAddress(contactDetails.getPostcodeAddress())
                    .build();

            contact = Contact.builder()
                    .email(contactDetails.getEmailAddress())
                    .mobile(getPhoneNumberWithOutSpaces(contactDetails.getPhoneNumber()))
                    .build();
        } else {
            contact = Contact.builder().build();
        }

        Identity identity = buildAppellantIdentity(syaAppellant);

        String isAppointee = buildAppellantIsAppointee(syaCaseWrapper);


        Appointee appointee = getAppointee(syaCaseWrapper);

        if (null != appointee
                && null != syaAppellant
                && null != syaAppellant.getIsAddressSameAsAppointee()
                && syaAppellant.getIsAddressSameAsAppointee()) {
            address = Address.builder()
                    .line1(appointee.getAddress().getLine1())
                    .line2(appointee.getAddress().getLine2())
                    .town(appointee.getAddress().getTown())
                    .county(appointee.getAddress().getCounty())
                    .postcode(appointee.getAddress().getPostcode())
                    .postcodeLookup(appointee.getAddress().getPostcodeLookup())
                    .postcodeAddress(appointee.getAddress().getPostcodeAddress())
                    .build();
        }

        String useSameAddress = null;
        if (syaAppellant != null && syaAppellant.getIsAddressSameAsAppointee() != null) {
            useSameAddress = !syaAppellant.getIsAddressSameAsAppointee() ? "No" : "Yes";
        }

        return Appellant.builder()
                .name(getName(syaAppellant))
                .address(address)
                .contact(appointee == null ? contact : Contact.builder().build())
                .identity(identity)
                .isAppointee(isAppointee)
                .appointee(appointee)
                .isAddressSameAsAppointee(useSameAddress)
                .build();
    }

    private static Identity buildAppellantIdentity(SyaAppellant syaAppellant) {
        Identity identity = Identity.builder().build();
        if (null != syaAppellant) {
            identity = identity.toBuilder()
                    .dob(syaAppellant.getDob() == null ? null : syaAppellant.getDob().toString())
                    .nino(syaAppellant.getNino())
                    .build();
        }

        return identity;
    }

    private static String buildAppellantIsAppointee(SyaCaseWrapper syaCaseWrapper) {
        if (syaCaseWrapper.getIsAppointee() == null) {
            return null;
        }

        return syaCaseWrapper.getIsAppointee() ? "Yes" : "No";
    }

    private static Name getName(SyaAppellant syaAppellant) {
        Name name = Name.builder().build();

        if (null != syaAppellant) {
            name = name.toBuilder()
                    .title(syaAppellant.getTitle())
                    .firstName(syaAppellant.getFirstName())
                    .lastName(syaAppellant.getLastName())
                    .build();
        }
        return name;
    }

    private static Appointee getAppointee(SyaCaseWrapper syaCaseWrapper) {

        SyaAppointee syaAppointee = syaCaseWrapper.getAppointee();

        if (null != syaAppointee) {
            Address address = null;
            if (null != syaAppointee.getContactDetails()) {
                address = Address.builder()
                        .line1(syaAppointee.getContactDetails().getAddressLine1())
                        .line2(syaAppointee.getContactDetails().getAddressLine2())
                        .town(syaAppointee.getContactDetails().getTownCity())
                        .county(syaAppointee.getContactDetails().getCounty())
                        .postcode(syaAppointee.getContactDetails().getPostCode())
                        .postcodeLookup(syaAppointee.getContactDetails().getPostcodeLookup())
                        .postcodeAddress(syaAppointee.getContactDetails().getPostcodeAddress())
                        .build();
            }

            Contact contact = null;
            if (null != syaAppointee.getContactDetails()) {
                contact = Contact.builder()
                        .email(syaAppointee.getContactDetails().getEmailAddress())
                        .mobile(getPhoneNumberWithOutSpaces(syaAppointee.getContactDetails().getPhoneNumber()))
                        .build();
            }

            Identity identity = null;
            if (null != syaAppointee.getDob()) {
                identity = Identity.builder()
                        .dob(syaAppointee.getDob().toString())
                        .build();
            }

            return Appointee.builder()
                    .name(Name.builder()
                            .title(syaAppointee.getTitle())
                            .firstName(syaAppointee.getFirstName())
                            .lastName(syaAppointee.getLastName())
                            .build()
                    )
                    .address(address)
                    .contact(contact)
                    .identity(identity)
                    .build();
        } else {
            return null;
        }
    }

    private static AppealReasons getReasonsForAppealing(SyaReasonsForAppealing syaReasonsForAppealing) {
        if (syaReasonsForAppealing == null) {
            return null;
        }
        List<AppealReason> appealReasons = new ArrayList<>();
        for (Reason reason : syaReasonsForAppealing.getReasons()) {
            AppealReasonDetails appealReasonDetails = AppealReasonDetails.builder()
                    .reason(reason.getWhatYouDisagreeWith())
                    .description(reason.getReasonForAppealing())
                    .build();
            AppealReason appealReason = AppealReason.builder()
                    .value(appealReasonDetails)
                    .build();
            appealReasons.add(appealReason);
        }
        return AppealReasons.builder()
                .reasons(appealReasons)
                .otherReasons(syaReasonsForAppealing.getOtherReasons())
                .build();
    }

    private static HearingOptions getHearingOptions(SyaHearingOptions syaHearingOptions) {
        if (syaHearingOptions == null) {
            return null;
        }
        if (syaHearingOptions.getWantsToAttend() == null) {
            return HearingOptions.builder().wantsToAttend(null).build();
        }
        if (syaHearingOptions.getWantsToAttend()) {

            if (syaHearingOptions.getWantsSupport() == null) {
                return HearingOptions.builder()
                        .wantsToAttend(YES)
                        .wantsSupport(null)
                        .build();
            }

            String languageInterpreter = null;
            List<String> arrangements = null;
            String wantsSupport = syaHearingOptions.getWantsSupport() ? YES : NO;
            if (syaHearingOptions.getWantsSupport()) {
                if (syaHearingOptions.getArrangements() == null) {
                    return HearingOptions.builder()
                            .wantsToAttend(YES)
                            .wantsSupport(wantsSupport)
                            .arrangements(null)
                            .build();
                }
                arrangements = getArrangements(syaHearingOptions.getArrangements());
                languageInterpreter = getLanguageInterpreter(syaHearingOptions.getArrangements().getLanguageInterpreter());
            }

            if (syaHearingOptions.getScheduleHearing() == null) {
                return HearingOptions.builder()
                        .wantsToAttend(YES)
                        .wantsSupport(wantsSupport)
                        .arrangements(arrangements)
                        .languageInterpreter(languageInterpreter)
                        .languages(languageInterpreter != null && languageInterpreter.equals(YES)
                                ? syaHearingOptions.getInterpreterLanguageType() : null)
                        .scheduleHearing(null)
                        .signLanguageType(syaHearingOptions.getSignLanguageType())
                        .other(syaHearingOptions.getAnythingElse())
                        .build();
            }

            String scheduleHearing = syaHearingOptions.getScheduleHearing() ? YES : NO;
            List<ExcludeDate> excludedDates = null;
            if (syaHearingOptions.getScheduleHearing()) {
                excludedDates = getExcludedDates(syaHearingOptions.getDatesCantAttend());
            }

            return HearingOptions.builder()
                    .wantsToAttend(YES)
                    .wantsSupport(wantsSupport)
                    .languageInterpreter(languageInterpreter)
                    .languages(syaHearingOptions.getInterpreterLanguageType())
                    .signLanguageType(syaHearingOptions.getSignLanguageType())
                    .scheduleHearing(scheduleHearing)
                    .arrangements(arrangements)
                    .excludeDates(excludedDates)
                    .other(syaHearingOptions.getAnythingElse())
                    .build();
        } else {
            return HearingOptions.builder()
                    .wantsToAttend(NO)
                    .build();
        }
    }

    private static String getLanguageInterpreter(Boolean languageInterpreter) {
        if (languageInterpreter == null) {
            return null;
        }
        return languageInterpreter ? YES : NO;
    }

    private static List<ExcludeDate> getExcludedDates(String[] dates) {
        if (dates == null) {
            return null;
        }
        List<ExcludeDate> excludeDates = new ArrayList<>();
        for (String date : dates) {
            DateRange dateRange = DateRange.builder()
                    .start(getLocalDate(date))
                    .end(getLocalDate(date))
                    .build();
            excludeDates.add(ExcludeDate.builder().value(dateRange).build());
        }
        return excludeDates;
    }

    private static List<String> getArrangements(SyaArrangements syaArrangements) {

        List<String> arrangements = new ArrayList<>();

        if (syaArrangements.getSignLanguageInterpreter() != null && syaArrangements.getSignLanguageInterpreter()) {
            arrangements.add(HearingOptionArrangements.SIGN_LANGUAGE_INTERPRETER.getValue());
        }

        if (syaArrangements.getHearingLoop() != null && syaArrangements.getHearingLoop()) {
            arrangements.add(HearingOptionArrangements.HEARING_LOOP.getValue());
        }

        if (syaArrangements.getAccessibleHearingRoom() != null && syaArrangements.getAccessibleHearingRoom()) {
            arrangements.add(HearingOptionArrangements.DISABLE_ACCESS.getValue());
        }

        return arrangements;
    }

    private static Subscriptions populateSubscriptions(SyaCaseWrapper syaCaseWrapper) {

        return Subscriptions.builder()
                .appellantSubscription(syaCaseWrapper.getIsAppointee() != null && !syaCaseWrapper.getIsAppointee()
                        ? getAppellantSubscription(syaCaseWrapper) : null)
                .appointeeSubscription(syaCaseWrapper.getIsAppointee() != null && syaCaseWrapper.getIsAppointee()
                        ? getAppointeeSubscription(syaCaseWrapper) : null)
                .representativeSubscription(getRepresentativeSubscription(syaCaseWrapper))
                .build();
    }

    private static Subscription getAppellantSubscription(SyaCaseWrapper syaCaseWrapper) {
        if (syaCaseWrapper.getContactDetails() != null) {
            final SyaSmsNotify smsNotify = syaCaseWrapper.getSmsNotify();

            final String subscribeSms = getSubscribeSms(smsNotify);

            final String email = syaCaseWrapper.getContactDetails().getEmailAddress();
            final String wantEmailNotifications = StringUtils.isNotBlank(email) ? YES : NO;

            final String mobile = getMobile(syaCaseWrapper, smsNotify);

            return Subscription.builder()
                    .wantSmsNotifications(subscribeSms)
                    .subscribeSms(subscribeSms)
                    .tya(generateAppealNumber())
                    .mobile(cleanPhoneNumber(mobile).orElse(mobile))
                    .subscribeEmail(wantEmailNotifications)
                    .email(email)
                    .build();
        }

        return null;
    }

    private static String getMobile(SyaCaseWrapper syaCaseWrapper, SyaSmsNotify smsNotify) {
        if (smsNotify == null || smsNotify.isWantsSmsNotifications() == null || smsNotify.isUseSameNumber() == null) {
            return null;
        }
        return getPhoneNumberWithOutSpaces(getNotificationSmsNumber(smsNotify, syaCaseWrapper.getContactDetails()));
    }

    private static String getSubscribeSms(SyaSmsNotify smsNotify) {
        if (smsNotify == null || smsNotify.isWantsSmsNotifications() == null) {
            return null;
        }
        return smsNotify.isWantsSmsNotifications() ? YES : NO;
    }

    private static Subscription getRepresentativeSubscription(SyaCaseWrapper syaCaseWrapper) {

        if (syaCaseWrapper.getHasRepresentative() != null
                && syaCaseWrapper.getHasRepresentative()
                && syaCaseWrapper.getRepresentative() != null
                && syaCaseWrapper.getRepresentative().getContactDetails() != null) {

            boolean emailAddressExists = StringUtils
                    .isNotBlank(syaCaseWrapper.getRepresentative().getContactDetails().getEmailAddress());
            boolean isMobileNumber = PhoneNumbersUtil.isValidMobileNumber(
                    syaCaseWrapper.getRepresentative().getContactDetails().getPhoneNumber());

            final String mobileNumber = getPhoneNumberWithOutSpaces(syaCaseWrapper
                    .getRepresentative().getContactDetails().getPhoneNumber());

            final String cleanedMobileNumber = cleanPhoneNumber(mobileNumber).orElse(mobileNumber);

            return Subscription.builder()
                    .wantSmsNotifications(isMobileNumber ? YES : NO)
                    .subscribeSms(isMobileNumber ? YES : NO)
                    .mobile(cleanedMobileNumber)
                    .subscribeEmail(emailAddressExists ? YES : NO)
                    .email(syaCaseWrapper.getRepresentative().getContactDetails().getEmailAddress())
                    .tya(generateAppealNumber())
                    .build();
        }

        return Subscription.builder()
                .wantSmsNotifications(NO)
                .subscribeSms(NO)
                .subscribeEmail(NO)
                .tya(generateAppealNumber())
                .build();
    }

    private static Subscription getAppointeeSubscription(SyaCaseWrapper syaCaseWrapper) {
        if (null != syaCaseWrapper.getAppointee() && null != syaCaseWrapper.getSmsNotify()) {
            SyaSmsNotify smsNotify = syaCaseWrapper.getSmsNotify();

            String subscribeSms = getSubscribeSms(smsNotify);

            String email = syaCaseWrapper.getAppointee().getContactDetails().getEmailAddress();
            String wantEmailNotifications = StringUtils.isNotBlank(email) ? YES : NO;
            String mobile = getPhoneNumberWithOutSpaces(
                    getNotificationSmsNumber(smsNotify, syaCaseWrapper.getAppointee().getContactDetails()));

            return Subscription.builder()
                    .wantSmsNotifications(subscribeSms)
                    .subscribeSms(subscribeSms)
                    .tya(generateAppealNumber())
                    .mobile(cleanPhoneNumber(mobile).orElse(mobile))
                    .subscribeEmail(wantEmailNotifications)
                    .email(email)
                    .build();
        }
        return null;
    }

    private static String getNotificationSmsNumber(SyaSmsNotify smsNotify, SyaContactDetails contactDetails) {
        return smsNotify != null
                && smsNotify.isWantsSmsNotifications() != null
                && smsNotify.isWantsSmsNotifications()
                && (null == smsNotify.isUseSameNumber() || !smsNotify.isUseSameNumber())
                ? smsNotify.getSmsNumber()
                : contactDetails.getPhoneNumber();
    }

    private static Representative getRepresentative(SyaCaseWrapper syaCaseWrapper) {
        if (syaCaseWrapper.getHasRepresentative() != null) {

            if (syaCaseWrapper.getHasRepresentative()) {

                SyaRepresentative syaRepresentative = syaCaseWrapper.getRepresentative();
                if (syaRepresentative == null) {
                    return Representative.builder()
                            .hasRepresentative(YES)
                            .build();
                }

                String repFirstName = "undefined".equalsIgnoreCase(syaRepresentative.getFirstName()) ? null : syaRepresentative.getFirstName();
                String repLastName = "undefined".equalsIgnoreCase(syaRepresentative.getLastName()) ? null : syaRepresentative.getLastName();

                Name name = Name.builder()
                        .title(syaRepresentative.getTitle())
                        .firstName(repFirstName)
                        .lastName(repLastName)
                        .build();

                Address address = Address.builder()
                        .line1(syaRepresentative.getContactDetails().getAddressLine1())
                        .line2(syaRepresentative.getContactDetails().getAddressLine2())
                        .town(syaRepresentative.getContactDetails().getTownCity())
                        .county(syaRepresentative.getContactDetails().getCounty())
                        .postcode(syaRepresentative.getContactDetails().getPostCode())
                        .postcodeLookup(syaRepresentative.getContactDetails().getPostcodeLookup())
                        .postcodeAddress(syaRepresentative.getContactDetails().getPostcodeAddress())
                        .build();

                Contact contact = Contact.builder()
                        .email(syaRepresentative.getContactDetails().getEmailAddress())
                        .mobile(getPhoneNumberWithOutSpaces(syaRepresentative.getContactDetails().getPhoneNumber()))
                        .build();

                return Representative.builder()
                        .hasRepresentative(YES)
                        .organisation(syaRepresentative.getOrganisation())
                        .name(name)
                        .address(address)
                        .contact(contact)
                        .build();
            } else {
                return Representative.builder()
                        .hasRepresentative(NO)
                        .build();
            }
        }
        return Representative.builder().hasRepresentative(null).build();
    }

    private static String getLocalDate(String dateStr) {
        LocalDate localDate = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        return localDate.toString();
    }

    private static List<SscsDocument> getEvidenceDocumentDetails(SyaCaseWrapper syaCaseWrapper) {
        if (syaCaseWrapper.getReasonsForAppealing() == null
                || syaCaseWrapper.getReasonsForAppealing().getEvidences() == null) {
            return Collections.emptyList();
        }
        List<SyaEvidence> evidences = syaCaseWrapper.getReasonsForAppealing().getEvidences();

        if (null != evidences && !evidences.isEmpty()) {
            return evidences.stream()
                    .map(syaEvidence -> {
                        DocumentLink documentLink = DocumentLink.builder().documentUrl(syaEvidence.getUrl()).build();
                        SscsDocumentDetails sscsDocumentDetails = SscsDocumentDetails.builder()
                                .documentFileName(syaEvidence.getFileName())
                                .documentDateAdded(syaEvidence.getUploadedDate().format(DateTimeFormatter.ISO_DATE))
                                .documentLink(documentLink)
                                .documentType("appellantEvidence")
                                .documentComment(syaCaseWrapper.getReasonsForAppealing().getEvidenceDescription())
                                .build();
                        return SscsDocument.builder().value(sscsDocumentDetails).build();
                    }).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private static String getPhoneNumberWithOutSpaces(String phoneNumber) {
        if (StringUtils.isNotBlank(phoneNumber)) {
            return phoneNumber.replaceAll("\\s", "");
        }
        return phoneNumber;
    }

    private static String hasEvidence(String evidenceProvide) {
        if (StringUtils.isEmpty(evidenceProvide)) {
            return StringUtils.EMPTY;
        }
        return Boolean.TRUE.toString().equals(evidenceProvide) ? YES : NO;
    }
}
