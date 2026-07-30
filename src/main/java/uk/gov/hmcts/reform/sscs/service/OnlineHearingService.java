package uk.gov.hmcts.reform.sscs.service;

import static java.util.Collections.emptyList;
import static java.util.stream.Stream.of;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;

import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.Contact;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscription;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscriptions;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.domain.wrapper.*;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@Slf4j
@Service
public class OnlineHearingService {
    private final CcdService ccdService;
    private final IdamService idamService;

    public OnlineHearingService(
            @Autowired CcdService ccdService,
            @Autowired IdamService idamService) {
        this.ccdService = ccdService;
        this.idamService = idamService;
    }

    public Optional<SscsCaseDetails> getCcdCaseByIdentifier(String identifier) {
        return !NumberUtils.isDigits(identifier)
                ? getCcdCase(identifier) :
                Optional.ofNullable(ccdService.getByCaseId(Long.parseLong(identifier), idamService.getIdamTokens()));
    }

    public Optional<SscsCaseDetails> getCcdCase(String onlineHearingId) {
        return getCcdCaseByIdentifier(onlineHearingId).map(sscsCaseDetails -> {
            IdamTokens idamTokens = idamService.getIdamTokens();
            SscsCaseDetails caseDetails = ccdService.getByCaseId(sscsCaseDetails.getId(), idamTokens);

            if (caseDetails == null) {
                throw new IllegalStateException("Online hearing for ccdCaseId [" + sscsCaseDetails.getId() + "] found but cannot find the case in CCD");
            }

            return caseDetails;
        });
    }

    //Load hearing with signed-in user details
    public Optional<OnlineHearing> loadHearing(SscsCaseDetails sscsCaseDeails, String tya, String email) {
        UserDetails userDetails = convertUserDetails(sscsCaseDeails, tya, email);
        return populateHearing(sscsCaseDeails, userDetails);
    }

    private Optional<OnlineHearing> populateHearing(SscsCaseDetails sscsCaseDetails, UserDetails userDetails) {
        SscsCaseData data = sscsCaseDetails.getData();
        HearingOptions hearingOptions = data.getAppeal().getHearingOptions();
        Appellant appellant = sscsCaseDetails.getData().getAppeal().getAppellant();
        AppealDetails appealDetails = convertAppealDetails(sscsCaseDetails);
        Name name = appellant.getName();
        String nameString = name.getFirstName() + " " + name.getLastName();

        List<String> arrangements = (hearingOptions.getArrangements() != null)
                ? hearingOptions.getArrangements() : emptyList();
        return Optional.of(new OnlineHearing(
                nameString,
                sscsCaseDetails.getData().getCaseReference(),
                sscsCaseDetails.getId(),
                new HearingArrangements(
                        "yes".equalsIgnoreCase(hearingOptions.getLanguageInterpreter()),
                        hearingOptions.getLanguages(),
                        arrangements.contains("signLanguageInterpreter"),
                        hearingOptions.getSignLanguageType(),
                        arrangements.contains("hearingLoop"),
                        arrangements.contains("disabledAccess"),
                        hearingOptions.getOther()
                ),
                userDetails,
                appealDetails
        ));
    }

    private AppealDetails convertAppealDetails(SscsCaseDetails sscsCaseDetails) {
        return new AppealDetails(sscsCaseDetails.getData().getCaseCreated(),
                sscsCaseDetails.getData().getAppeal().getMrnDetails().getMrnDate(),
                sscsCaseDetails.getData().getAppeal().getBenefitType().getCode(),
                sscsCaseDetails.getState()
        );
    }

    private UserDetails convertUserDetails(SscsCaseDetails sscsCaseDetails, String tya, String email) {
        log.info("Converting user details for case id {} and email {}", sscsCaseDetails.getId(), email);
        Map<UserType, Subscription> appellantSubscriptions = getAppealSubscriptionMap(sscsCaseDetails);
        Map<UserType, Subscription> jointPartySubscriptions = getJointPartySubscriptionMap(sscsCaseDetails);
        boolean isSignInSubscription = isSignInSubscription(appellantSubscriptions.values(), tya, email);
        log.info("Is sign in subscription for case id {} and email {}: {}", sscsCaseDetails.getId(), email, isSignInSubscription);
        log.info(sscsCaseDetails.getData().toString());
        log.info("Appellant address: {}", sscsCaseDetails.getData().getAppeal().getAppellant().getAddress().toString());
        log.info("Joint Party Address: {}", sscsCaseDetails.getData().getJointParty().getAddress().toString());
        if (isSignInSubscription) {
            log.info("Populating user details for case id {} and email {} as APPELLANT", sscsCaseDetails.getId(), email);
            return populateUserDetails(UserType.APPELLANT, sscsCaseDetails.getData().getAppeal().getAppellant().getName(),
                    sscsCaseDetails.getData().getAppeal().getAppellant().getAddress(),
                    Optional.ofNullable(sscsCaseDetails.getData().getAppeal().getAppellant().getContact()),
                    appellantSubscriptions);
        } else if (sscsCaseDetails.getData().isThereAJointParty() && isSignInSubscription(jointPartySubscriptions.values(), tya, email)) {
            log.info("Populating user details for case id {} and email {} as JOINT_PARTY", sscsCaseDetails.getId(), email);
            return populateUserDetails(UserType.JOINT_PARTY, sscsCaseDetails.getData().getJointParty().getName(),
                    sscsCaseDetails.getData().getJointParty().getAddress(),
                    Optional.ofNullable(sscsCaseDetails.getData().getJointParty().getContact()),
                    jointPartySubscriptions);
        } else {
            log.info("{} was not populated with user details for Appellant or Joint Party for {}", sscsCaseDetails.getId(), email);
            log.info("Case has Joint Party {}", sscsCaseDetails.getData().getJointParty().getHasJointParty());
            log.info("Joint Party Subscriptions {}", isSignInSubscription(jointPartySubscriptions.values(), tya, email));
            List<CcdValue<OtherParty>> otherParties = sscsCaseDetails.getData().getOtherParties();
            for (CcdValue<OtherParty> op : emptyIfNull(otherParties)) {
                Map<UserType, Subscription> otherPartySubscriptions = getOtherPartySubscriptionMap(op);
                if (isSignInSubscription(otherPartySubscriptions.values(), tya, email)) {
                    log.info("Populating user details for case id {} and email {} as OTHER_PARTY", sscsCaseDetails.getId(), email);
                    return populateUserDetails(UserType.OTHER_PARTY, op.getValue().getName(),
                            op.getValue().getAddress(),
                            Optional.ofNullable(op.getValue().getContact()),
                            otherPartySubscriptions);
                }
            }
        }
        return null;
    }

    @NotNull
    private Map<UserType, Subscription> getOtherPartySubscriptionMap(CcdValue<OtherParty> op) {
        return of(Pair.of(UserType.OTHER_PARTY, op.getValue().getOtherPartySubscription()),
                Pair.of(UserType.OTHER_PARTY_APPOINTEE, op.getValue().getOtherPartyAppointeeSubscription()),
                Pair.of(UserType.OTHER_PARTY_REP, op.getValue().getOtherPartyRepresentativeSubscription()))
                .filter(p -> p.getLeft() != null && p.getRight() != null)
                .filter(p -> p.getRight().getEmail() != null || p.getRight().getMobile() != null)
                .collect(Collectors.toMap(Pair::getLeft, Pair::getValue));
    }

    @NotNull
    private Map<UserType, Subscription> getAppealSubscriptionMap(SscsCaseDetails sscsCaseDetails) {
        Subscriptions subscriptions = sscsCaseDetails.getData().getSubscriptions();
        return of(Pair.of(UserType.APPELLANT, subscriptions.getAppellantSubscription()),
                Pair.of(UserType.APPOINTEE, subscriptions.getAppointeeSubscription()),
                Pair.of(UserType.REP, subscriptions.getRepresentativeSubscription()),
                Pair.of(UserType.SUPPORTER, subscriptions.getSupporterSubscription()))
                .filter(p -> p.getLeft() != null && p.getRight() != null)
                .filter(p -> p.getRight().getEmail() != null || p.getRight().getMobile() != null)
                .collect(Collectors.toMap(Pair::getLeft, Pair::getValue));
    }

    @NotNull
    private Map<UserType, Subscription> getJointPartySubscriptionMap(SscsCaseDetails sscsCaseDetails) {
        Subscriptions subscriptions = sscsCaseDetails.getData().getSubscriptions();
        return of(Pair.of(UserType.JOINT_PARTY, subscriptions.getJointPartySubscription()))
                .filter(p -> p.getLeft() != null && p.getRight() != null)
                .filter(p -> p.getRight().getEmail() != null || p.getRight().getMobile() != null)
                .collect(Collectors.toMap(Pair::getLeft, Pair::getValue));
    }


    private UserDetails populateUserDetails(UserType type, Name name, Address address, Optional<Contact> contact, Map<UserType,Subscription> subscriptionsMap) {
        String email = null;
        String phone = null;
        String mobile = null;
        String nameString = name.getFirstName() + " " + name.getLastName();

        AddressDetails addressDetails = new AddressDetails(address.getLine1(), address.getLine2(), address.getTown(), address.getCounty(), address.getPostcode());

        if (contact.isPresent()) {
            Contact contactObj = contact.get();
            email = contactObj.getEmail();
            phone = contactObj.getPhone();
            mobile = contactObj.getMobile();
        }

        List<uk.gov.hmcts.reform.sscs.domain.wrapper.Subscription> subscriptions = getSubscriptions(subscriptionsMap);

        return new UserDetails(type.getType(), nameString, addressDetails, email, phone, mobile, subscriptions);
    }

    @NotNull
    private List<uk.gov.hmcts.reform.sscs.domain.wrapper.Subscription> getSubscriptions(Map<UserType, Subscription> subscriptionsMap) {
        return subscriptionsMap.entrySet().stream()
                .filter(Objects::nonNull)
                .filter(s -> !UserType.JOINT_PARTY.equals(s.getKey()))
                .map(s -> new uk.gov.hmcts.reform.sscs.domain.wrapper.Subscription(s.getKey().getType(), s.getValue().getEmail(), s.getValue().getMobile()))
                .collect(Collectors.toList());
    }

    private boolean isSignInSubscription(Collection<Subscription> subscriptionStream, String tya, String email) {
        return subscriptionStream.stream().anyMatch(subscription -> subscription != null
                && email.equalsIgnoreCase(subscription.getEmail()));
    }
}
