package uk.gov.hmcts.reform.sscs.service;

import static java.util.Objects.nonNull;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.DRAFT_TO_INCOMPLETE_APPLICATION;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.DRAFT_TO_NON_COMPLIANT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.DRAFT_TO_VALID_APPEAL_CREATED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.INCOMPLETE_APPLICATION_RECEIVED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.NON_COMPLIANT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.VALID_APPEAL_CREATED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.service.RegionalProcessingCenterService.getFirstHalfOfPostcode;
import static uk.gov.hmcts.reform.sscs.transform.deserialize.SubmitYourAppealToCcdCaseDataDeserializer.convertSyaToCcdCaseDataV1;

import feign.FeignException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseLinkDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseManagementLocation;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Identity;
import uk.gov.hmcts.reform.sscs.ccd.domain.MrnDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.exception.CcdException;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.config.CitizenCcdService;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaAppointee;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaContactDetails;
import uk.gov.hmcts.reform.sscs.exception.ApplicationErrorException;
import uk.gov.hmcts.reform.sscs.exception.DuplicateCaseException;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;
import uk.gov.hmcts.reform.sscs.model.CourtVenue;
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
        this.caseAccessManagementFeature = caseAccessManagementFeature;
        this.airLookupService = airLookupService;
        this.refDataService = refDataService;
        this.venueService = venueService;
    }

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

    public Long submitAppeal(SyaCaseWrapper appeal, String userToken) {

        IdamTokens idamTokens = idamService.getIdamTokens();

        Long caseId = appeal.getCcdCaseId() != null ? Long.valueOf(appeal.getCcdCaseId()) : null;

        log.info("Converting sya appeal data to sscs case");

        SscsCaseData caseData = convertAppealToSscsCaseData(appeal);

        EventType event = findEventType(caseData, caseId != null);
        SscsCaseDetails caseDetails = createOrUpdateCase(caseData, event, idamTokens);

        associateCase(idamTokens, caseDetails, userToken);

        return caseDetails.getId();
    }

    SscsCaseData convertAppealToSscsCaseData(SyaCaseWrapper appeal) {
        boolean isIbc = appeal.getBenefitType().isIbc();
        String postCode = resolvePostCode(appeal, isIbc);
        RegionalProcessingCenter rpc = regionalProcessingCenterService.getByPostcode(isIbc ? postCode : getFirstHalfOfPostcode(postCode), isIbc);

        SscsCaseData sscsCaseData = rpc == null
            ? convertSyaToCcdCaseDataV1(appeal, caseAccessManagementFeature)
            : convertSyaToCcdCaseDataV1(appeal, rpc.getName(), rpc, caseAccessManagementFeature);

        sscsCaseData.setCreatedInGapsFrom(READY_TO_LIST.getId());
        String processingVenue = airLookupService.lookupAirVenueNameByPostCode(postCode, sscsCaseData.getAppeal().getBenefitType());
        sscsCaseData.setProcessingVenue(processingVenue);

        if (caseAccessManagementFeature
            && StringUtils.isNotEmpty(processingVenue)
            && rpc != null) {
            String venueEpimsId = venueService.getEpimsIdForVenue(processingVenue);
            CourtVenue courtVenue = refDataService.getCourtVenueRefDataByEpimsId(venueEpimsId);

            sscsCaseData.setCaseManagementLocation(CaseManagementLocation.builder()
                .baseLocation(rpc.getEpimsId())
                .region(courtVenue.getRegionId()).build());

            log.info("Successfully updated case management location details for case {}. Processing venue {}, epimsId {}",
                appeal.getCcdCaseId(), processingVenue, venueEpimsId);
        }

        log.info("{} - setting venue name to {}",
            isIbc
                ? sscsCaseData.getAppeal().getAppellant().getIdentity().getIbcaReference()
                : maskNino(sscsCaseData.getAppeal().getAppellant().getIdentity().getNino()),
            sscsCaseData.getProcessingVenue());

        return sscsCaseData;
    }

    private String maskNino(String nino) {
        return "XXXX" + (nino == null ? "" : nino.substring(4));
    }

    private EventType findEventType(SscsCaseData caseData, boolean saveAndReturnCase) {
        Appeal appeal = caseData.getAppeal();
        MrnDetails mrnDetails = appeal.getMrnDetails();
        if (mrnDetails != null && mrnDetails.getMrnDate() != null) {
            LocalDate mrnDate = LocalDate.parse(mrnDetails.getMrnDate());
            boolean moveToNoneCompliant = mrnDate.plusMonths(13L).isBefore(LocalDate.now());

            return handleMoveToNonCompliant(caseData, saveAndReturnCase, moveToNoneCompliant);
        } else {
            Identity identity = appeal.getAppellant().getIdentity();
            log.info("Moving case for {} {} to incomplete due to MRN Details {} present and MRN Date {} present",
                caseData.isIbcCase() ? "Ibca Reference" : "NINO",
                caseData.isIbcCase() ? identity.getIbcaReference() : maskNino(identity.getNino()),
                (mrnDetails != null ? "" : "not"),
                (mrnDetails != null && mrnDetails.getMrnDate() != null ? "" : "not"));
            return saveAndReturnCase ? DRAFT_TO_INCOMPLETE_APPLICATION : INCOMPLETE_APPLICATION_RECEIVED;
        }
    }

    private Pair<List<SscsCaseDetails>, SscsCaseDetails> getMatchedNinoCasesAndCaseDetails(SscsCaseData caseData,
                                                                                           IdamTokens idamTokens,
                                                                                           String nino,
                                                                                           String ibcaReference) {
        SscsCaseDetails caseDetails = null;
        List<SscsCaseDetails> matchedByNinoCases = List.of();
        if (!caseData.isIbcCase()) {
            matchedByNinoCases = getMatchedCases(nino, idamTokens);
            if (!matchedByNinoCases.isEmpty()) {
                log.info("Found " + matchedByNinoCases.size() + " matching cases for Nino "
                    + maskNino(nino) + " before filtering non exact matches");
            } else {
                log.info("No matching cases for Nino {}", maskNino(nino));
            }
            caseDetails = matchedByNinoCases.stream().filter(createNinoAndBenefitTypeAndMrnDatePredicate(caseData)).findFirst().orElse(null);
        } else {
            log.info("Case is IBA with reference {}", ibcaReference);
        }
        return Pair.of(matchedByNinoCases, caseDetails);
    }

    private SscsCaseDetails createOrUpdateCase(SscsCaseData caseData, EventType eventType, IdamTokens idamTokens) {
        SscsCaseDetails caseDetails = null;
        String benefitShortName = caseData.getAppeal().getBenefitType().getCode();
        String nino = caseData.getAppeal().getAppellant().getIdentity().getNino();
        String ibcaReference = caseData.getAppeal().getAppellant().getIdentity().getIbcaReference();
        try {
            Pair<List<SscsCaseDetails>, SscsCaseDetails> matchedByNinoCasesCaseDetails =
                getMatchedNinoCasesAndCaseDetails(caseData, idamTokens, nino, ibcaReference);
            List<SscsCaseDetails> matchedByNinoCases = matchedByNinoCasesCaseDetails.getLeft();
            caseDetails = matchedByNinoCasesCaseDetails.getRight();
            if (caseDetails == null) {
                if (!matchedByNinoCases.isEmpty()) {
                    log.info("Found " + matchedByNinoCases.size() + " matching cases for Nino "
                        + maskNino(nino));
                    caseData = addAssociatedCases(caseData, matchedByNinoCases);
                }

                log.info("About to attempt creating case or updating draft case in CCD with event {} for benefit type {} and event {} and isScottish {} and languagePreference {}",
                    eventType,
                    benefitShortName,
                    eventType,
                    caseData.getIsScottishCase(),
                    caseData.getLanguagePreference().getCode());

                if (eventType == DRAFT_TO_VALID_APPEAL_CREATED || eventType == DRAFT_TO_INCOMPLETE_APPLICATION || eventType == DRAFT_TO_NON_COMPLIANT) {
                    caseData.setCaseCreated(LocalDate.now().toString());

                    caseDetails = ccdService.updateCase(caseData,
                        Long.valueOf(caseData.getCcdCaseId()),
                        eventType.getCcdType(),
                        "SSCS - new case created",
                        "Created SSCS case from Submit Your Appeal online draft with event " + eventType.getCcdType(),
                        idamTokens);

                    log.info("Case {} successfully converted from Draft to SSCS case in CCD for benefit type {} with event {}",
                        caseDetails.getId(),
                        benefitShortName,
                        eventType);
                } else {
                    caseDetails = ccdService.createCase(caseData,
                        eventType.getCcdType(),
                        "SSCS - new case created",
                        "Created SSCS case from Submit Your Appeal online with event " + eventType.getCcdType(),
                        idamTokens);
                    log.info("Case {} successfully created in CCD for benefit type {} with event {}",
                        caseDetails.getId(),
                        benefitShortName,
                        eventType);
                }
                return caseDetails;
            }
        } catch (Exception e) {
            String caseId = caseDetails != null ? caseDetails.getId().toString() : "";
            String referenceName = caseData.isIbcCase() ? "IBCA Reference" : "Nino";
            String referenceValue = caseData.isIbcCase() ? ibcaReference : maskNino(nino);
            throw new CcdException(
                String.format("Error found in the creating case process for case with Id - %s"
                        + " and %s - %s and Benefit type - %s and exception: %s",
                    caseId, referenceName, referenceValue,
                    benefitShortName, e.getMessage()), e);
        }

        log.info("Duplicate case {} found for Nino {} and benefit type {}. "
                + "No need to continue with post create case processing.",
            caseDetails.getId(), maskNino(nino),
            benefitShortName);
        throw new DuplicateCaseException(
            String.format("An appeal has already been submitted, for that decision date %s ",
                caseData.getAppeal().getMrnDetails().getMrnDate()));
    }

    private void associateCase(IdamTokens idamTokens,
                               SscsCaseDetails caseDetails,
                               String userToken) {
        if (null != caseDetails && StringUtils.isNotEmpty(userToken)) {
            citizenCcdService.associateCaseToCitizen(getUserTokens(userToken), caseDetails.getId(), idamTokens);
        }
    }

    private String resolvePostCode(SyaCaseWrapper appeal, boolean isIbc) {
        Boolean inMainlandUk = appeal.getContactDetails().getInMainlandUk();
        if (isIbc && inMainlandUk != null && inMainlandUk.equals(Boolean.FALSE)) {
            return appeal.getAppellant().getContactDetails().getPortOfEntry();
        } else if (Boolean.TRUE.equals(appeal.getIsAppointee())) {
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
            log.info("Moving case for NINO {} to non-compliant as MRN Date is older than 13 months", maskNino(caseData.getAppeal().getAppellant().getIdentity().getNino()));
            return saveAndReturnCase ? DRAFT_TO_NON_COMPLIANT : NON_COMPLIANT;
        } else {
            log.info("Valid appeal to be created for case with NINO {}", maskNino(caseData.getAppeal().getAppellant().getIdentity().getNino()));
            return saveAndReturnCase ? DRAFT_TO_VALID_APPEAL_CREATED : VALID_APPEAL_CREATED;
        }
    }

    protected List<SscsCaseDetails> getMatchedCases(String nino, IdamTokens idamTokens) {
        log.info("Find matching cases for Nino " + maskNino(nino));
        return ccdService.findCaseBy("data.appeal.appellant.identity.nino", nino, idamTokens);
    }

    private Predicate<SscsCaseDetails> createNinoAndBenefitTypeAndMrnDatePredicate(SscsCaseData caseData) {
        return c -> c.getData().getAppeal().getAppellant().getIdentity() != null
            && c.getData().getAppeal().getAppellant().getIdentity().getNino().equalsIgnoreCase(caseData.getAppeal().getAppellant().getIdentity().getNino())
            && c.getData().getAppeal().getBenefitType() != null
            && c.getData().getAppeal().getBenefitType().getCode().equals(caseData.getAppeal().getBenefitType().getCode())
            && c.getData().getAppeal().getMrnDetails().getMrnDate() != null
            && c.getData().getAppeal().getMrnDetails().getMrnDate().equalsIgnoreCase(caseData.getAppeal().getMrnDetails().getMrnDate());
    }

    protected SscsCaseData addAssociatedCases(SscsCaseData caseData, List<SscsCaseDetails> matchedByNinoCases) {
        log.info("Adding " + matchedByNinoCases.size() + " associated cases for case id {}", caseData.getCcdCaseId());

        List<CaseLink> associatedCases = new ArrayList<>();

        for (SscsCaseDetails sscsCaseDetails : matchedByNinoCases) {
            log.info("Linking case " + sscsCaseDetails.getId().toString());
            associatedCases.add(CaseLink.builder().value(
                CaseLinkDetails.builder().caseReference(sscsCaseDetails.getId().toString()).build()).build());
        }

        if (!matchedByNinoCases.isEmpty()) {
            return caseData.toBuilder().associatedCase(associatedCases).linkedCasesBoolean("Yes").build();
        } else {
            return caseData.toBuilder().linkedCasesBoolean("No").build();
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
                maskNino(appeal.getAppellant().getNino()),
                idamTokens.getUserId());
        } else {
            log.error("The case data has been altered outside of this transaction for idam id {}",
                idamTokens.getUserId());
        }
    }

}
