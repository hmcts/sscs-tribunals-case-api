package uk.gov.hmcts.reform.sscs.service;

import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.service.RegionalProcessingCenterService.getFirstHalfOfPostcode;
import static uk.gov.hmcts.reform.sscs.transform.deserialize.SubmitYourAppealToCcdCaseDataDeserializer.convertSyaToCcdCaseData;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.exception.CcdException;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.config.CitizenCcdService;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.reform.sscs.exception.ApplicationErrorException;
import uk.gov.hmcts.reform.sscs.exception.DuplicateCaseException;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;
import uk.gov.hmcts.reform.sscs.model.SaveCaseOperation;
import uk.gov.hmcts.reform.sscs.model.SaveCaseResult;
import uk.gov.hmcts.reform.sscs.model.draft.SessionDraft;
import uk.gov.hmcts.reform.sscs.service.converter.ConvertAIntoBService;

@Service
@Slf4j
public class SubmitAppealService {
    public static final String DM_STORE_USER_ID = "sscs";
    private static final String CITIZEN_ROLE = "citizen";

    private final CcdService ccdService;
    private final CitizenCcdService citizenCcdService;
    private final RegionalProcessingCenterService regionalProcessingCenterService;
    private final IdamService idamService;
    private final ConvertAIntoBService<SscsCaseData, SessionDraft> convertAIntoBService;
    private final AirLookupService airLookupService;
    private final EvidenceManagementSecureDocStoreService secureDocStoreService;
    private final boolean workAllocationFeature;

    @SuppressWarnings("squid:S107")
    @Autowired
    SubmitAppealService(CcdService ccdService,
                        CitizenCcdService citizenCcdService,
                        RegionalProcessingCenterService regionalProcessingCenterService,
                        IdamService idamService,
                        ConvertAIntoBService<SscsCaseData, SessionDraft> convertAIntoBService,
                        AirLookupService airLookupService,
                        EvidenceManagementSecureDocStoreService secureDocStoreService,
                        @Value("${feature.work-allocation.enabled}")  boolean workAllocationFeature) {

        this.ccdService = ccdService;
        this.citizenCcdService = citizenCcdService;
        this.regionalProcessingCenterService = regionalProcessingCenterService;
        this.idamService = idamService;
        this.convertAIntoBService = convertAIntoBService;
        this.airLookupService = airLookupService;
        this.secureDocStoreService = secureDocStoreService;
        this.workAllocationFeature = workAllocationFeature;
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

    public Optional<SaveCaseResult> submitDraftAppeal(String oauth2Token, SyaCaseWrapper appeal, Boolean forceCreate) {
        appeal.setCaseType("draft");

        IdamTokens idamTokens = getUserTokens(oauth2Token);
        if (!hasValidCitizenRole(idamTokens)) {
            throw new ApplicationErrorException(new Exception("User has a invalid role"));
        }

        try {
            return Optional.of(saveDraftCaseInCcd(convertSyaToCcdCaseData(appeal, workAllocationFeature), idamTokens, forceCreate));
        } catch (FeignException e) {
            if (e.status() == HttpStatus.SC_CONFLICT) {
                logError(appeal, idamTokens);
                return Optional.empty();
            } else {
                throw e;
            }
        }

    }

    public Optional<SaveCaseResult> updateDraftAppeal(String oauth2Token, SyaCaseWrapper appeal) {
        appeal.setCaseType("draft");

        IdamTokens idamTokens = getUserTokens(oauth2Token);
        if (!hasValidCitizenRole(idamTokens)) {
            throw new ApplicationErrorException(new Exception("User has a invalid role"));
        }

        try {
            SscsCaseData sscsCaseData = convertSyaToCcdCaseData(appeal, workAllocationFeature);
            
            CaseDetails caseDetails = citizenCcdService.updateCase(sscsCaseData, EventType.UPDATE_DRAFT.getCcdType(), "Update draft",
                    "Update draft in CCD", idamTokens, appeal.getCcdCaseId());

            return Optional.of(SaveCaseResult.builder()
                    .caseDetailsId(caseDetails.getId())
                    .saveCaseOperation(SaveCaseOperation.UPDATE)
                    .build());

        } catch (FeignException e) {

            if (e.status() == HttpStatus.SC_CONFLICT) {
                logError(appeal, idamTokens);
                return Optional.empty();
            } else {
                throw e;
            }
        }
    }

    private void logError(SyaCaseWrapper appeal, IdamTokens idamTokens) {
        if (nonNull(appeal.getAppellant().getNino())) {
            log.error("The case data has been altered outside of this transaction for case with nino {} and idam id {}",
                    appeal.getAppellant().getNino(),
                    idamTokens.getUserId());
        } else {
            log.error("The case data has been altered outside of this transaction for idam id {}",
                    idamTokens.getUserId());
        }
    }

    public Optional<SaveCaseResult> archiveDraftAppeal(String oauth2Token, SyaCaseWrapper appeal, Long ccdCaseId) {
        appeal.setCaseType("draft");

        IdamTokens idamTokens = getUserTokens(oauth2Token);

        if (!hasValidCitizenRole(idamTokens)) {
            throw new ApplicationErrorException(new Exception("User has a invalid role"));
        }

        try {
            SscsCaseData sscsCaseData = convertSyaToCcdCaseData(appeal, workAllocationFeature);
            citizenCcdService.archiveDraft(sscsCaseData, idamTokens, ccdCaseId);

            return Optional.of(SaveCaseResult.builder()
                    .caseDetailsId(ccdCaseId)
                    .saveCaseOperation(SaveCaseOperation.ARCHIVE)
                    .build());

        } catch (FeignException e) {
            throw e;
        }
    }

    public Optional<SessionDraft> getDraftAppeal(String oauth2Token) {
        SscsCaseData caseDetails = null;
        SessionDraft sessionDraft = null;
        IdamTokens idamTokens = getUserTokens(oauth2Token);

        if (!hasValidCitizenRole(idamTokens)) {
            throw new ApplicationErrorException(new Exception("User has a invalid role"));
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
            throw new ApplicationErrorException(new Exception("User has a invalid role"));
        }
        List<SscsCaseData> caseDetailsList = citizenCcdService.findCase(idamTokens);

        log.info("GET all Draft cases with IDAM Id {} and roles {}", idamTokens.getUserId(), idamTokens.getRoles());

        return caseDetailsList.stream()
                .map(convertAIntoBService::convert)
                .collect(toList());
    }

    private IdamTokens getUserTokens(String oauth2Token) {
        UserDetails userDetails = idamService.getUserDetails(oauth2Token);
        return IdamTokens.builder()
            .idamOauth2Token(oauth2Token)
            .serviceAuthorization(idamService.generateServiceAuthorization())
            .userId(userDetails.getId())
            .roles(userDetails.getRoles())
            .email(userDetails.getEmail())
            .build();
    }

    private boolean hasValidCitizenRole(IdamTokens idamTokens) {
        boolean hasRole = false;
        if (idamTokens != null && !CollectionUtils.isEmpty(idamTokens.getRoles())) {
            hasRole = idamTokens.getRoles().stream().anyMatch(CITIZEN_ROLE::equalsIgnoreCase);
        }
        return hasRole;
    }

    private void associateCase(IdamTokens idamTokens,
                               SscsCaseDetails caseDetails,
                               String userToken) {
        if (null != caseDetails && StringUtils.isNotEmpty(userToken)) {
            citizenCcdService.associateCaseToCitizen(getUserTokens(userToken), caseDetails.getId(), idamTokens);
        }
    }

    SscsCaseData convertAppealToSscsCaseData(SyaCaseWrapper appeal) {

        String postCode = appeal.getContactDetails().getPostCode();
        String firstHalfOfPostcode = getFirstHalfOfPostcode(postCode);
        RegionalProcessingCenter rpc = regionalProcessingCenterService.getByPostcode(firstHalfOfPostcode);

        SscsCaseData sscsCaseData;
        if (rpc == null) {
            sscsCaseData = convertSyaToCcdCaseData(appeal, workAllocationFeature);
        } else {
            sscsCaseData = convertSyaToCcdCaseData(appeal, rpc.getName(), rpc, workAllocationFeature);
        }

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            log.info("sscsCaseData" + objectMapper.writeValueAsString(sscsCaseData));
        } catch (JsonProcessingException e) {
            log.error("error with json", e);
        }

        sscsCaseData.setCreatedInGapsFrom(READY_TO_LIST.getId());
        sscsCaseData.setProcessingVenue(airLookupService.lookupAirVenueNameByPostCode(postCode, sscsCaseData.getAppeal().getBenefitType()));

        log.info("{} - setting venue name to {}", sscsCaseData.getAppeal().getAppellant().getIdentity().getNino(), sscsCaseData.getProcessingVenue());

        return sscsCaseData;
    }

    private SscsCaseDetails createOrUpdateCase(SscsCaseData caseData, EventType eventType, IdamTokens idamTokens) {
        SscsCaseDetails caseDetails = null;

        try {
            List<SscsCaseDetails> matchedByNinoCases = getMatchedCases(caseData.getAppeal().getAppellant().getIdentity().getNino(), idamTokens);
            if (!matchedByNinoCases.isEmpty()) {
                log.info("Found " + matchedByNinoCases.size() + " matching cases for Nino "
                        + caseData.getAppeal().getAppellant().getIdentity().getNino() + " before filtering non exact matches");
            } else {
                log.info("No matching cases for Nino {}", caseData.getAppeal().getAppellant().getIdentity().getNino());
            }
            caseDetails = matchedByNinoCases.stream().filter(createNinoAndBenefitTypeAndMrnDatePredicate(caseData)).findFirst().orElse(null);

            if (caseDetails == null) {
                if (!matchedByNinoCases.isEmpty()) {
                    log.info("Found " + matchedByNinoCases.size() + " matching cases for Nino "
                            + caseData.getAppeal().getAppellant().getIdentity().getNino());
                    caseData = addAssociatedCases(caseData, matchedByNinoCases);
                }

                log.info("About to attempt creating case or updating draft case in CCD with event {} for benefit type {} and event {} and isScottish {} and languagePreference {}",
                        eventType,
                        caseData.getAppeal().getBenefitType().getCode(),
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
                            caseData.getAppeal().getBenefitType().getCode(),
                            eventType);
                } else {
                    caseDetails = ccdService.createCase(caseData,
                            eventType.getCcdType(),
                            "SSCS - new case created",
                            "Created SSCS case from Submit Your Appeal online with event " + eventType.getCcdType(),
                            idamTokens);
                    log.info("Case {} successfully created in CCD for benefit type {} with event {}",
                            caseDetails.getId(),
                            caseData.getAppeal().getBenefitType().getCode(),
                            eventType);
                }
                return caseDetails;
            }
        } catch (Exception e) {
            throw new CcdException(
                String.format("Error found in the creating case process for case with Id - %s"
                        + " and Nino - %s and Benefit type - %s and exception: %s",
                    caseDetails != null ? caseDetails.getId() : "", caseData.getAppeal().getAppellant().getIdentity().getNino(),
                    caseData.getAppeal().getBenefitType().getCode(), e.getMessage()), e);
        }

        log.info("Duplicate case {} found for Nino {} and benefit type {}. "
                        + "No need to continue with post create case processing.",
                caseDetails.getId(), caseData.getAppeal().getAppellant().getIdentity().getNino(),
                caseData.getAppeal().getBenefitType().getCode());
        throw new DuplicateCaseException(
                String.format("An appeal has already been submitted, for that decision date %s ",
                        caseData.getAppeal().getMrnDetails().getMrnDate()));
    }

    protected List<SscsCaseDetails> getMatchedCases(String nino, IdamTokens idamTokens) {
        log.info("Find matching cases for Nino " + nino);
        return ccdService.findCaseBy("data.appeal.appellant.identity.nino", nino, idamTokens);
    }

    protected SscsCaseData addAssociatedCases(SscsCaseData caseData, List<SscsCaseDetails> matchedByNinoCases) {
        log.info("Adding " + matchedByNinoCases.size() + " associated cases for case id {}", caseData.getCcdCaseId());

        List<CaseLink> associatedCases = new ArrayList<>();

        for (SscsCaseDetails sscsCaseDetails: matchedByNinoCases) {
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


    private Predicate<SscsCaseDetails> createNinoAndBenefitTypeAndMrnDatePredicate(SscsCaseData caseData) {
        return c -> c.getData().getAppeal().getAppellant().getIdentity() != null
                && c.getData().getAppeal().getAppellant().getIdentity().getNino().equalsIgnoreCase(caseData.getAppeal().getAppellant().getIdentity().getNino())
                && c.getData().getAppeal().getBenefitType() != null
                && c.getData().getAppeal().getBenefitType().getCode().equals(caseData.getAppeal().getBenefitType().getCode())
                && c.getData().getAppeal().getMrnDetails().getMrnDate() != null
                && c.getData().getAppeal().getMrnDetails().getMrnDate().equalsIgnoreCase(caseData.getAppeal().getMrnDetails().getMrnDate());
    }


    private SaveCaseResult saveDraftCaseInCcd(SscsCaseData caseData, IdamTokens idamTokens, Boolean forceCreate) {

        SaveCaseResult result;

        if (Boolean.TRUE.equals(forceCreate)) {
            result = citizenCcdService.createDraft(caseData, idamTokens);
        } else {
            result = citizenCcdService.saveCase(caseData, idamTokens);
        }

        log.info("POST Draft case with CCD Id {} , IDAM id {} and roles {} ",
                result.getCaseDetailsId(),
                idamTokens.getUserId(),
                idamTokens.getRoles());

        log.info("Draft Case {} successfully {} in CCD", result.getCaseDetailsId(), result.getSaveCaseOperation().name());
        return result;
    }

    private EventType findEventType(SscsCaseData caseData, boolean saveAndReturnCase) {

        if (caseData.getAppeal().getMrnDetails() != null && caseData.getAppeal().getMrnDetails().getMrnDate() != null) {
            LocalDate mrnDate = LocalDate.parse(caseData.getAppeal().getMrnDetails().getMrnDate());
            boolean moveToNoneCompliant = mrnDate.plusMonths(13L).isBefore(LocalDate.now());

            if (moveToNoneCompliant) {
                log.info("Moving case for NINO {} to non-compliant as MRN Date is older than 13 months", caseData.getAppeal().getAppellant().getIdentity().getNino());
                return saveAndReturnCase ? DRAFT_TO_NON_COMPLIANT : NON_COMPLIANT;
            } else {
                log.info("Valid appeal to be created for case with NINO {}", caseData.getAppeal().getAppellant().getIdentity().getNino());
                return saveAndReturnCase ? DRAFT_TO_VALID_APPEAL_CREATED : VALID_APPEAL_CREATED;
            }
        } else {
            log.info("Moving case for NINO {} to incomplete due to MRN Details {} present and MRN Date {} present",
                caseData.getAppeal().getAppellant().getIdentity().getNino(),
                (caseData.getAppeal().getMrnDetails() != null ? "" : "not"),
                (caseData.getAppeal().getMrnDetails().getMrnDate() != null ? "" : "not"));
            return saveAndReturnCase ? DRAFT_TO_INCOMPLETE_APPLICATION : INCOMPLETE_APPLICATION_RECEIVED;
        }
    }
}
