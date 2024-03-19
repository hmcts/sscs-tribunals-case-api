package uk.gov.hmcts.reform.sscs.service;

import static java.lang.String.format;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.of;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscription;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscriptions;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.ccd.service.SscsCcdConvertService;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.config.CitizenCcdService;
import uk.gov.hmcts.reform.sscs.domain.wrapper.OnlineHearing;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.util.PostcodeUtil;
import uk.gov.hmcts.reform.sscs.utility.AppealNumberGenerator;

@Slf4j
@Service
public class CitizenLoginService {
    private static final String UPDATED_SSCS = "Updated SSCS";

    private final CitizenCcdService citizenCcdService;
    private final CcdService ccdService;
    private final UpdateCcdCaseService updateCcdCaseService;
    private final SscsCcdConvertService sscsCcdConvertService;
    private final IdamService idamService;
    private final PostcodeUtil postcodeUtil;
    private final OnlineHearingService onlineHearingService;
    @Value("${feature.citizen-login-service-v2.enabled}")
    private boolean citizenLogicServiceV2Enabled;


    public CitizenLoginService(CitizenCcdService citizenCcdService, CcdService ccdService, UpdateCcdCaseService updateCcdCaseService, SscsCcdConvertService sscsCcdConvertService, IdamService idamService, PostcodeUtil postcodeUtil, OnlineHearingService onlineHearingService) {
        this.citizenCcdService = citizenCcdService;
        this.ccdService = ccdService;
        this.updateCcdCaseService = updateCcdCaseService;
        this.sscsCcdConvertService = sscsCcdConvertService;
        this.idamService = idamService;
        this.postcodeUtil = postcodeUtil;
        this.onlineHearingService = onlineHearingService;
    }

    public List<OnlineHearing> findCasesForCitizen(IdamTokens idamTokens, String tya) {
        log.info(format("Find case: Searching for case with tya [%s] for user [%s]", tya, idamTokens.getUserId()));
        List<CaseDetails> caseDetails = citizenCcdService.searchForCitizenAllCases(idamTokens);
        List<SscsCaseDetails> sscsCaseDetails = caseDetails.stream()
                .map(sscsCcdConvertService::getCaseDetails)
                .filter(AppealNumberGenerator::filterCaseNotDraftOrArchivedDraft)
                .peek(this::attachOtherPartyDetails)
                .toList();
        if (!isBlank(tya)) {
            log.info(format("Find case: Filtering for case with tya [%s] for user [%s]", tya, idamTokens.getUserId()));
            List<OnlineHearing> convert = convert(
                    sscsCaseDetails.stream()
                            .filter(casesWithSubscriptionMatchingTya(tya))
                            .toList(),
                    idamTokens.getEmail()
            );
            log.info(format("Find case: Found [%s] cases for tya [%s] for user [%s]", convert.size(), tya, idamTokens.getUserId()));

            return convert;
        }

        log.info(format("Searching for case without for user [%s]", idamTokens.getUserId()));
        List<OnlineHearing> convert = convert(sscsCaseDetails, idamTokens.getEmail());
        log.info(format("Found [%s] cases without tya for user [%s]", convert.size(), idamTokens.getUserId()));
        return convert;
    }

    private void attachOtherPartyDetails(SscsCaseDetails sscsCaseDetailsItem) {
        if (sscsCaseDetailsItem.getData().getOtherParties() == null) {
            SscsCaseDetails sscsCaseDetails = ccdService.getByCaseId(sscsCaseDetailsItem.getId(), idamService.getIdamTokens());
            if (sscsCaseDetails != null) {
                sscsCaseDetailsItem.getData().setOtherParties(sscsCaseDetails.getData().getOtherParties());
            }
        }
    }

    public List<OnlineHearing> findActiveCasesForCitizen(IdamTokens idamTokens) {
        log.info(format("Find case: Searching for active case with for user [%s]", idamTokens.getUserId()));
        List<CaseDetails> caseDetails = citizenCcdService.searchForCitizenAllCases(idamTokens);
        List<SscsCaseDetails> sscsCaseDetails = caseDetails.stream()
                .map(sscsCcdConvertService::getCaseDetails)
                .filter(AppealNumberGenerator::filterActiveCasesForCitizen)
                .toList();

        log.info(format("Searching for active case without for user [%s]", idamTokens.getUserId()));
        List<OnlineHearing> convert = convert(sscsCaseDetails, idamTokens.getEmail());
        log.info(format("Found [%s] active cases for user [%s]", convert.size(), idamTokens.getUserId()));
        return convert;
    }

    public List<OnlineHearing> findDormantCasesForCitizen(IdamTokens idamTokens) {
        log.info(format("Find case: Searching for dormant case with for user [%s]", idamTokens.getUserId()));
        List<CaseDetails> caseDetails = citizenCcdService.searchForCitizenAllCases(idamTokens);
        List<SscsCaseDetails> sscsCaseDetails = caseDetails.stream()
                .map(sscsCcdConvertService::getCaseDetails)
                .filter(AppealNumberGenerator::filterDormantCasesForCitizen)
                .toList();

        log.info(format("Searching for dormant case without for user [%s]", idamTokens.getUserId()));
        List<OnlineHearing> convert = convert(sscsCaseDetails, idamTokens.getEmail());
        log.info(format("Found [%s] dormant cases for user [%s]", convert.size(), idamTokens.getUserId()));
        return convert;
    }

    private List<OnlineHearing> convert(List<SscsCaseDetails> sscsCaseDetails, String email) {
        return sscsCaseDetails.stream()
                .map(sscsCase -> onlineHearingService.loadHearing(sscsCase, null, email))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    public Optional<OnlineHearing> associateCaseToCitizen(IdamTokens citizenIdamTokens, String tya, String email, String postcode) {
        SscsCaseDetails caseByAppealNumber = ccdService.findCaseByAppealNumber(tya, idamService.getIdamTokens());

        if (caseByAppealNumber != null) {
            log.info(format("Associate case: Found case to assign id [%s] for tya [%s] email [%s] postcode [%s]", caseByAppealNumber.getId(), tya, email, postcode));
            String appealPostcode = caseByAppealNumber.getData().getAppeal().getAppellant().getAddress().getPostcode();
            if (appealPostcode != null && !appealPostcode.isEmpty()) {
                if (postcodeUtil.hasAppellantOrOtherPartyPostcode(caseByAppealNumber, postcode, email)) {
                    log.info(format("Associate case: Found case to assign id [%s] for tya [%s] email [%s] postcode [%s] matches postcode", caseByAppealNumber.getId(), tya, email, postcode));
                    if (caseHasSubscriptionWithTyaAndEmail(caseByAppealNumber, tya, email)) {
                        log.info(format("Found case to assign id [%s] for tya [%s] email [%s] postcode [%s] has subscription", caseByAppealNumber.getId(), tya, email, postcode));
                        citizenCcdService.addUserToCase(idamService.getIdamTokens(), citizenIdamTokens.getUserId(), caseByAppealNumber.getId());
                        updateCaseWithLastLoggedIntoMya(email, caseByAppealNumber);
                        return onlineHearingService.loadHearing(caseByAppealNumber, tya, email);
                    } else {
                        log.info(format("Associate case: Subscription does not match id [%s] for tya [%s] email [%s] postcode [%s]", caseByAppealNumber.getId(), tya, email, postcode));
                    }
                } else {
                    log.info(format("Associate case: Postcode does not match id [%s] for tya [%s] email [%s] postcode [%s]", caseByAppealNumber.getId(), tya, email, postcode));
                }
            } else {
                log.info(format("Associate case: Found case to assign id [%s], however no appellant post code exists", caseByAppealNumber.getId()));
            }
        } else {
            log.info(format("Associate case: No case found for tya [%s] email [%s] postcode [%s]", tya, email, postcode));
        }
        return Optional.empty();
    }

    public void findAndUpdateCaseLastLoggedIntoMya(IdamTokens citizenIdamTokens, String caseId) {
        if (StringUtils.isNotEmpty(caseId)) {
            SscsCaseDetails caseDetails = ccdService.getByCaseId(Long.valueOf(caseId), idamService.getIdamTokens());
            if (caseDetails != null && caseHasSubscriptionWithMatchingEmail(caseDetails, citizenIdamTokens.getEmail())) {
                log.info("MYA log time: found matching email {} for case id {}", citizenIdamTokens.getEmail(), caseId);
                updateCaseWithLastLoggedIntoMya(citizenIdamTokens.getEmail(), caseDetails);
            }
        }

    }

    private void updateCaseWithLastLoggedIntoMya(String email, SscsCaseDetails caseByAppealNumber) {
        if (citizenLogicServiceV2Enabled) {
            log.info("Updating case with last logged in MYA using V2, case id: {}, matching email: {}", caseByAppealNumber.getId(), email);
            updateCcdCaseService.updateCaseV2(caseByAppealNumber.getId(), EventType.UPDATE_CASE_ONLY.getCcdType(), "SSCS - update last logged in MYA",
                    UPDATED_SSCS, idamService.getIdamTokens(), caseData -> updateSubscriptionWithLastLoggedIntoMya(caseData, email));
        } else {
            log.info("Updating case with last logged in MYA using V1, case id: {}, matching email: {}", caseByAppealNumber.getId(), email);
            updateSubscriptionWithLastLoggedIntoMya(caseByAppealNumber.getData(), email);
            ccdService.updateCase(caseByAppealNumber.getData(), caseByAppealNumber.getId(), EventType.UPDATE_CASE_ONLY.getCcdType(),
                    "SSCS - update last logged in MYA", UPDATED_SSCS, idamService.getIdamTokens()
            );
        }
    }

    private Predicate<SscsCaseDetails> casesWithSubscriptionMatchingTya(String tya) {
        return sscsCaseDetails -> {
            Subscriptions subscriptions = sscsCaseDetails.getData().getSubscriptions();
            final Stream<Subscription> otherPartySubscriptionStream = emptyIfNull(sscsCaseDetails.getData().getOtherParties()).stream()
                    .map(CcdValue::getValue)
                    .flatMap(op -> of(op.getOtherPartySubscription(), op.getOtherPartyAppointeeSubscription(), op.getOtherPartyRepresentativeSubscription()));


            return concat(of(subscriptions.getAppellantSubscription(), subscriptions.getAppointeeSubscription(), subscriptions.getRepresentativeSubscription()), otherPartySubscriptionStream)
                    .anyMatch(subscription -> subscription != null && tya.equals(subscription.getTya()));
        };
    }

    private boolean caseHasSubscriptionWithTyaAndEmail(SscsCaseDetails sscsCaseDetails, String tya, String email) {
        Subscriptions subscriptions = sscsCaseDetails.getData().getSubscriptions();

        final Stream<Subscription> otherPartySubscriptionStream = emptyIfNull(sscsCaseDetails.getData().getOtherParties()).stream()
                .map(CcdValue::getValue)
                .flatMap(op -> of(op.getOtherPartySubscription(), op.getOtherPartyAppointeeSubscription(), op.getOtherPartyRepresentativeSubscription()));

        return concat(of(subscriptions.getAppellantSubscription(), subscriptions.getAppointeeSubscription(), subscriptions.getRepresentativeSubscription(),
                subscriptions.getJointPartySubscription()), otherPartySubscriptionStream)
                .anyMatch(subscription -> subscription != null && tya.equals(subscription.getTya()) && email.equalsIgnoreCase(subscription.getEmail()));
    }

    private boolean caseHasSubscriptionWithMatchingEmail(SscsCaseDetails sscsCaseDetails, String email) {
        Subscriptions subscriptions = sscsCaseDetails.getData().getSubscriptions();

        return of(subscriptions.getAppellantSubscription(), subscriptions.getAppointeeSubscription(), subscriptions.getRepresentativeSubscription())
                .anyMatch(subscription -> subscription != null && email.equalsIgnoreCase(subscription.getEmail()));
    }

    private void updateSubscriptionWithLastLoggedIntoMya(SscsCaseData sscsCaseData, String email) {
        Subscriptions subscriptions = sscsCaseData.getSubscriptions();
        String lastLoggedIntoMya = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
        if (subscriptions != null && subscriptions.getAppellantSubscription() != null
                && email.equalsIgnoreCase(subscriptions.getAppellantSubscription().getEmail())) {
            subscriptions.getAppellantSubscription().setLastLoggedIntoMya(lastLoggedIntoMya);
        }
        if (subscriptions != null && subscriptions.getAppointeeSubscription() != null
                && email.equalsIgnoreCase(subscriptions.getAppointeeSubscription().getEmail())) {
            subscriptions.getAppointeeSubscription().setLastLoggedIntoMya(lastLoggedIntoMya);
        }

        if (subscriptions != null && subscriptions.getRepresentativeSubscription() != null
                && email.equalsIgnoreCase(subscriptions.getRepresentativeSubscription().getEmail())) {
            subscriptions.getRepresentativeSubscription().setLastLoggedIntoMya(lastLoggedIntoMya);
        }

    }

}