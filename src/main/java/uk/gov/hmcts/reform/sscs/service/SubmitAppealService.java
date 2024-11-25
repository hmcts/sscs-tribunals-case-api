package uk.gov.hmcts.reform.sscs.service;

import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.DRAFT_TO_INCOMPLETE_APPLICATION;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.DRAFT_TO_NON_COMPLIANT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.DRAFT_TO_VALID_APPEAL_CREATED;
import static uk.gov.hmcts.reform.sscs.transform.deserialize.SubmitYourAppealToCcdCaseDataDeserializer.convertSyaToCcdCaseDataV1;

import feign.FeignException;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.exception.CcdException;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.config.CitizenCcdService;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.reform.sscs.exception.ApplicationErrorException;
import uk.gov.hmcts.reform.sscs.exception.DuplicateCaseException;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.model.SaveCaseOperation;
import uk.gov.hmcts.reform.sscs.model.SaveCaseResult;
import uk.gov.hmcts.reform.sscs.model.draft.SessionDraft;
import uk.gov.hmcts.reform.sscs.service.converter.ConvertAIntoBService;

@Service
@Slf4j
@ConditionalOnProperty(name = "feature.submit-appeal-service-v2.enabled", havingValue = "false")
public class SubmitAppealService extends SubmitAppealServiceBase {

    @SuppressWarnings("squid:S107")
    SubmitAppealService(CcdService ccdService,
                        CitizenCcdService citizenCcdService,
                        RegionalProcessingCenterService regionalProcessingCenterService,
                        IdamService idamService,
                        ConvertAIntoBService<SscsCaseData, SessionDraft> convertAIntoBService,
                        AirLookupService airLookupService,
                        RefDataService refDataService,
                        VenueService venueService,
                        @Value("${feature.case-access-management.enabled}")  boolean caseAccessManagementFeature) {
        super(ccdService,
                citizenCcdService,
                idamService,
                convertAIntoBService,
                regionalProcessingCenterService,
                airLookupService,
                refDataService,
                venueService,
                caseAccessManagementFeature);
    }

    @Override
    public Optional<SaveCaseResult> submitDraftAppeal(String oauth2Token, SyaCaseWrapper appeal, Boolean forceCreate) {
        appeal.setCaseType(DRAFT);

        IdamTokens idamTokens = getUserTokens(oauth2Token);
        if (!hasValidCitizenRole(idamTokens)) {
            throw new ApplicationErrorException(new Exception(USER_HAS_A_INVALID_ROLE_MESSAGE));
        }

        try {
            return Optional.of(saveDraftCaseInCcd(convertSyaToCcdCaseDataV1(appeal, caseAccessManagementFeature), idamTokens, forceCreate));
        } catch (FeignException e) {
            if (e.status() == HttpStatus.SC_CONFLICT) {
                logError(appeal, idamTokens);
                return Optional.empty();
            } else {
                throw e;
            }
        }

    }

    @Override
    public Optional<SaveCaseResult> updateDraftAppeal(String oauth2Token, SyaCaseWrapper appeal) {
        appeal.setCaseType(DRAFT);

        IdamTokens idamTokens = getUserTokens(oauth2Token);
        if (!hasValidCitizenRole(idamTokens)) {
            throw new ApplicationErrorException(new Exception(USER_HAS_A_INVALID_ROLE_MESSAGE));
        }

        try {
            SscsCaseData sscsCaseData = convertSyaToCcdCaseDataV1(appeal, caseAccessManagementFeature);

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

    @Override
    public Optional<SaveCaseResult> archiveDraftAppeal(String oauth2Token, SyaCaseWrapper appeal, Long ccdCaseId) throws FeignException {
        appeal.setCaseType(DRAFT);

        IdamTokens idamTokens = getUserTokens(oauth2Token);

        if (!hasValidCitizenRole(idamTokens)) {
            throw new ApplicationErrorException(new Exception(USER_HAS_A_INVALID_ROLE_MESSAGE));
        }

        SscsCaseData sscsCaseData = convertSyaToCcdCaseDataV1(appeal, caseAccessManagementFeature);
        citizenCcdService.archiveDraft(sscsCaseData, idamTokens, ccdCaseId);

        return Optional.of(SaveCaseResult.builder()
            .caseDetailsId(ccdCaseId)
            .saveCaseOperation(SaveCaseOperation.ARCHIVE)
            .build());
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
        return convertAppealToSscsCaseData(
                appeal,
                rpc -> Objects.isNull(rpc)
                        ? convertSyaToCcdCaseDataV1(appeal, caseAccessManagementFeature)
                        : convertSyaToCcdCaseDataV1(appeal, rpc.getName(), rpc, caseAccessManagementFeature)
                );
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
                    addAssociatedCases(caseData, matchedByNinoCases);
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

    private SaveCaseResult saveDraftCaseInCcd(SscsCaseData caseData, IdamTokens idamTokens, Boolean forceCreate) {

        SaveCaseResult result;

        log.info("saveDraftCaseInCcd {}", forceCreate);

        if (Boolean.TRUE.equals(forceCreate)) {
            log.info("saveDraftCaseInCcd createDraft");
            result = citizenCcdService.createDraft(caseData, idamTokens);
        } else {
            log.info("saveDraftCaseInCcd saveCase");
            result = citizenCcdService.saveCase(caseData, idamTokens);
        }

        log.info("POST Draft case with CCD Id {} , IDAM id {} and roles {} ",
                result.getCaseDetailsId(),
                idamTokens.getUserId(),
                idamTokens.getRoles());

        log.info("Draft Case {} successfully {} in CCD", result.getCaseDetailsId(), result.getSaveCaseOperation().name());
        return result;
    }
}
