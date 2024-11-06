package uk.gov.hmcts.reform.sscs.service;

import static java.util.Objects.nonNull;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.DRAFT_TO_INCOMPLETE_APPLICATION;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.DRAFT_TO_NON_COMPLIANT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.DRAFT_TO_VALID_APPEAL_CREATED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.INCOMPLETE_APPLICATION_RECEIVED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.NON_COMPLIANT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.VALID_APPEAL_CREATED;

import feign.FeignException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseLinkDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.config.CitizenCcdService;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaAppointee;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaContactDetails;
import uk.gov.hmcts.reform.sscs.exception.ApplicationErrorException;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;
import uk.gov.hmcts.reform.sscs.model.SaveCaseResult;
import uk.gov.hmcts.reform.sscs.model.draft.SessionDraft;
import uk.gov.hmcts.reform.sscs.service.converter.ConvertAIntoBService;

@Slf4j
public abstract class SubmitAppealServiceBase {
    public static final String DM_STORE_USER_ID = "sscs";
    public static final String CITIZEN_ROLE = "citizen";
    public static final String DRAFT = "draft";
    public static final String USER_HAS_A_INVALID_ROLE_MESSAGE = "User has a invalid role";

    protected final CcdService ccdService;
    protected final CitizenCcdService citizenCcdService;
    protected final IdamService idamService;
    protected final ConvertAIntoBService<SscsCaseData, SessionDraft> convertAIntoBService;
    protected final RegionalProcessingCenterService regionalProcessingCenterService;
    protected final AirLookupService airLookupService;
    protected final RefDataService refDataService;
    protected final VenueService venueService;
    protected UpdateCcdCaseService updateCcdCaseService;
    protected final boolean caseAccessManagementFeature;

    public SubmitAppealServiceBase(CcdService ccdService,
                                   CitizenCcdService citizenCcdService,
                                   IdamService idamService,
                                   ConvertAIntoBService<SscsCaseData, SessionDraft> convertAIntoBService,
                                   RegionalProcessingCenterService regionalProcessingCenterService,
                                   AirLookupService airLookupService,
                                   RefDataService refDataService,
                                   VenueService venueService,
                                   boolean caseAccessManagementFeature) {
        this.ccdService = ccdService;
        this.citizenCcdService = citizenCcdService;
        this.idamService = idamService;
        this.convertAIntoBService = convertAIntoBService;
        this.regionalProcessingCenterService = regionalProcessingCenterService;
        this.caseAccessManagementFeature  = caseAccessManagementFeature;
        this.airLookupService = airLookupService;
        this.refDataService = refDataService;
        this.venueService = venueService;
    }

    public SubmitAppealServiceBase(CcdService ccdService,
                                   CitizenCcdService citizenCcdService,
                                   IdamService idamService,
                                   ConvertAIntoBService<SscsCaseData, SessionDraft> convertAIntoBService,
                                   RegionalProcessingCenterService regionalProcessingCenterService,
                                   AirLookupService airLookupService,
                                   RefDataService refDataService,
                                   VenueService venueService,
                                   UpdateCcdCaseService updateCcdCaseService,
                                   boolean caseAccessManagementFeature) {
        this(ccdService,
                citizenCcdService,
                idamService,
                convertAIntoBService,
                regionalProcessingCenterService,
                airLookupService,
                refDataService,
                venueService,
                caseAccessManagementFeature);
        this.updateCcdCaseService = updateCcdCaseService;
    }

    public abstract Long submitAppeal(SyaCaseWrapper appeal, String userToken);

    public abstract Optional<SaveCaseResult> submitDraftAppeal(String oauth2Token, SyaCaseWrapper appeal, Boolean forceCreate);

    public abstract Optional<SaveCaseResult> updateDraftAppeal(String oauth2Token, SyaCaseWrapper syaCaseWrapper);

    public abstract Optional<SaveCaseResult> archiveDraftAppeal(String oauth2Token, SyaCaseWrapper syaCaseWrapper, Long ccdCaseId) throws FeignException;

    public Optional<SessionDraft> getDraftAppeal(String oauth2Token) {
        SscsCaseData caseDetails = null;
        SessionDraft sessionDraft = null;
        IdamTokens idamTokens = getUserTokens(oauth2Token);

        if (!hasValidCitizenRole(idamTokens)) {
            throw new ApplicationErrorException(new Exception(USER_HAS_A_INVALID_ROLE_MESSAGE));
        }

        List<SscsCaseData> caseDetailsList = citizenCcdService.findCase(idamTokens);

        if (isNotEmpty(caseDetailsList)) {
            caseDetails = caseDetailsList.get(0);
            sessionDraft = convertAIntoBService.convert(caseDetails);
        }

        log.info("GET Draft case with CCD Id {} , IDAM Id {} and roles {} ",
                (caseDetails == null) ? null : caseDetails.getCcdCaseId(), idamTokens.getUserId(),
                idamTokens.getRoles());

        return (sessionDraft != null) ? Optional.of(sessionDraft) : Optional.empty();
    }

    public List<SessionDraft> getDraftAppeals(String oauth2Token) {
        IdamTokens idamTokens = getUserTokens(oauth2Token);

        if (!hasValidCitizenRole(idamTokens)) {
            throw new ApplicationErrorException(new Exception(USER_HAS_A_INVALID_ROLE_MESSAGE));
        }
        List<SscsCaseData> caseDetailsList = citizenCcdService.findCase(idamTokens);

        log.info("GET all Draft cases with IDAM Id {} and roles {}", idamTokens.getUserId(), idamTokens.getRoles());

        return caseDetailsList.stream()
                .map(convertAIntoBService::convert)
                .toList();
    }

    protected EventType findEventType(SscsCaseData caseData, boolean saveAndReturnCase) {

        if (caseData.getAppeal().getMrnDetails() != null && caseData.getAppeal().getMrnDetails().getMrnDate() != null) {
            LocalDate mrnDate = LocalDate.parse(caseData.getAppeal().getMrnDetails().getMrnDate());
            boolean moveToNoneCompliant = mrnDate.plusMonths(13L).isBefore(LocalDate.now());

            return handleMoveToNonCompliant(caseData, saveAndReturnCase, moveToNoneCompliant);
        } else {
            log.info("Moving case for NINO {} to incomplete due to MRN Details {} present and MRN Date {} present",
                    caseData.getAppeal().getAppellant().getIdentity().getNino(),
                    (caseData.getAppeal().getMrnDetails() != null ? "" : "not"),
                    (caseData.getAppeal().getMrnDetails().getMrnDate() != null ? "" : "not"));
            return saveAndReturnCase ? DRAFT_TO_INCOMPLETE_APPLICATION : INCOMPLETE_APPLICATION_RECEIVED;
        }
    }


    protected void associateCase(IdamTokens idamTokens,
                               SscsCaseDetails caseDetails,
                               String userToken) {
        if (nonNull(caseDetails) && isNotEmpty(userToken)) {
            citizenCcdService.associateCaseToCitizen(getUserTokens(userToken), caseDetails.getId(), idamTokens);
        }
    }

    protected String resolvePostCode(SyaCaseWrapper appeal) {
        if (Boolean.TRUE.equals(appeal.getIsAppointee())) {
            return Optional.ofNullable(appeal.getAppointee())
                    .map(SyaAppointee::getContactDetails)
                    .map(SyaContactDetails::getPostCode)
                    .filter(appointeePostCode -> !StringUtils.isEmpty(appointeePostCode))
                    .orElse(null);
        } else {
            return appeal.getAppellant().getContactDetails().getPostCode();
        }
    }

    @NotNull
    private EventType handleMoveToNonCompliant(SscsCaseData caseData, boolean saveAndReturnCase, boolean moveToNoneCompliant) {
        if (moveToNoneCompliant) {
            log.info("Moving case for NINO {} to non-compliant as MRN Date is older than 13 months", caseData.getAppeal().getAppellant().getIdentity().getNino());
            return saveAndReturnCase ? DRAFT_TO_NON_COMPLIANT : NON_COMPLIANT;
        } else {
            log.info("Valid appeal to be created for case with NINO {}", caseData.getAppeal().getAppellant().getIdentity().getNino());
            return saveAndReturnCase ? DRAFT_TO_VALID_APPEAL_CREATED : VALID_APPEAL_CREATED;
        }
    }

    protected List<SscsCaseDetails> getMatchedCases(String nino, IdamTokens idamTokens) {
        log.info("Find matching cases for Nino " + nino);
        return ccdService.findCaseBy("data.appeal.appellant.identity.nino", nino, idamTokens);
    }

    protected Predicate<SscsCaseDetails> createNinoAndBenefitTypeAndMrnDatePredicate(SscsCaseData caseData) {
        return c -> c.getData().getAppeal().getAppellant().getIdentity() != null
                && c.getData().getAppeal().getAppellant().getIdentity().getNino().equalsIgnoreCase(caseData.getAppeal().getAppellant().getIdentity().getNino())
                && c.getData().getAppeal().getBenefitType() != null
                && c.getData().getAppeal().getBenefitType().getCode().equals(caseData.getAppeal().getBenefitType().getCode())
                && c.getData().getAppeal().getMrnDetails().getMrnDate() != null
                && c.getData().getAppeal().getMrnDetails().getMrnDate().equalsIgnoreCase(caseData.getAppeal().getMrnDetails().getMrnDate());
    }

    protected void addAssociatedCases(final SscsCaseData caseData, List<SscsCaseDetails> matchedByNinoCases) {
        log.info("Adding {} associated cases for case id {}", matchedByNinoCases.size(), caseData.getCcdCaseId());

        List<CaseLink> caseLinks = matchedByNinoCases.stream().map(sscsCaseDetails -> CaseLink.builder().value(
                        CaseLinkDetails.builder().caseReference(sscsCaseDetails.getId().toString()).build()).build())
                .toList();

        if (caseLinks.isEmpty()) {
            caseData.setLinkedCasesBoolean("No");
        } else {
            caseData.setAssociatedCase(caseLinks);
            caseData.setLinkedCasesBoolean("Yes");
        }
    }

    protected IdamTokens getUserTokens(String oauth2Token) {
        UserDetails userDetails = idamService.getUserDetails(oauth2Token);
        return IdamTokens.builder()
                .idamOauth2Token(oauth2Token)
                .serviceAuthorization(idamService.generateServiceAuthorization())
                .userId(userDetails.getId())
                .roles(userDetails.getRoles())
                .email(userDetails.getEmail())
                .build();
    }

    protected boolean hasValidCitizenRole(IdamTokens idamTokens) {
        boolean hasRole = false;
        if (idamTokens != null && !CollectionUtils.isEmpty(idamTokens.getRoles())) {
            hasRole = idamTokens.getRoles().stream().anyMatch(CITIZEN_ROLE::equalsIgnoreCase);
        }
        return hasRole;
    }

    protected void logError(SyaCaseWrapper appeal, IdamTokens idamTokens) {
        if (nonNull(appeal.getAppellant().getNino())) {
            log.error("The case data has been altered outside of this transaction for case with nino {} and idam id {}",
                    appeal.getAppellant().getNino(),
                    idamTokens.getUserId());
        } else {
            log.error("The case data has been altered outside of this transaction for idam id {}",
                    idamTokens.getUserId());
        }
    }

}
