package uk.gov.hmcts.reform.sscs.service.v2;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.DRAFT_TO_INCOMPLETE_APPLICATION;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.DRAFT_TO_NON_COMPLIANT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.DRAFT_TO_VALID_APPEAL_CREATED;
import static uk.gov.hmcts.reform.sscs.transform.deserialize.SubmitYourAppealToCcdCaseDataDeserializer.convertSyaToCcdCaseDataV2;

import feign.FeignException;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
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
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.config.CitizenCcdService;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.reform.sscs.exception.ApplicationErrorException;
import uk.gov.hmcts.reform.sscs.exception.DuplicateCaseException;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.model.SaveCaseOperation;
import uk.gov.hmcts.reform.sscs.model.SaveCaseResult;
import uk.gov.hmcts.reform.sscs.model.draft.SessionDraft;
import uk.gov.hmcts.reform.sscs.service.AirLookupService;
import uk.gov.hmcts.reform.sscs.service.RefDataService;
import uk.gov.hmcts.reform.sscs.service.RegionalProcessingCenterService;
import uk.gov.hmcts.reform.sscs.service.SubmitAppealServiceBase;
import uk.gov.hmcts.reform.sscs.service.VenueService;
import uk.gov.hmcts.reform.sscs.service.converter.ConvertAIntoBService;

@Service
@Slf4j
@ConditionalOnProperty(name = "feature.submit-appeal-service-v2.enabled", havingValue = "true")
public class SubmitAppealService extends SubmitAppealServiceBase {

    @SuppressWarnings("squid:S107")
    public SubmitAppealService(CcdService ccdService,
                               CitizenCcdService citizenCcdService,
                               RegionalProcessingCenterService regionalProcessingCenterService,
                               IdamService idamService,
                               ConvertAIntoBService<SscsCaseData, SessionDraft> convertAIntoBService,
                               AirLookupService airLookupService,
                               RefDataService refDataService,
                               VenueService venueService,
                               UpdateCcdCaseService updateCcdCaseService,
                               @Value("${feature.case-access-management.enabled}") boolean caseAccessManagementFeature) {
        super(ccdService,
                citizenCcdService,
                idamService,
                convertAIntoBService,
                regionalProcessingCenterService,
                airLookupService,
                refDataService,
                venueService,
                updateCcdCaseService,
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
            return Optional.of(saveDraftCaseInCcd(appeal, idamTokens, forceCreate));

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
    public Optional<SaveCaseResult> updateDraftAppeal(String oauth2Token, SyaCaseWrapper syaCaseWrapper) {
        syaCaseWrapper.setCaseType(DRAFT);

        IdamTokens idamTokens = getUserTokens(oauth2Token);
        if (!hasValidCitizenRole(idamTokens)) {
            throw new ApplicationErrorException(new Exception(USER_HAS_A_INVALID_ROLE_MESSAGE));
        }

        try {
            UnaryOperator<SscsCaseData> newCaseData = caseData -> convertSyaToCcdCaseDataV2(syaCaseWrapper, caseAccessManagementFeature, caseData);
            CaseDetails caseDetails = citizenCcdService.updateCaseCitizenV2(syaCaseWrapper.getCcdCaseId(), EventType.UPDATE_DRAFT.getCcdType(),
                    "Update draft", "Update draft in CCD", idamTokens, newCaseData);

            return Optional.of(SaveCaseResult.builder()
                    .caseDetailsId(caseDetails.getId())
                    .saveCaseOperation(SaveCaseOperation.UPDATE)
                    .build());

        } catch (FeignException e) {
            if (e.status() == HttpStatus.SC_CONFLICT) {
                logError(syaCaseWrapper, idamTokens);
                return Optional.empty();
            } else {
                throw e;
            }
        }
    }

    @Override
    public Optional<SaveCaseResult> archiveDraftAppeal(String oauth2Token, SyaCaseWrapper syaCaseWrapper, Long ccdCaseId) throws FeignException {
        syaCaseWrapper.setCaseType(DRAFT);

        IdamTokens idamTokens = getUserTokens(oauth2Token);

        if (!hasValidCitizenRole(idamTokens)) {
            throw new ApplicationErrorException(new Exception(USER_HAS_A_INVALID_ROLE_MESSAGE));
        }

        Consumer<SscsCaseData> mutator = caseData -> convertSyaToCcdCaseDataV2(syaCaseWrapper, caseAccessManagementFeature, caseData);

        citizenCcdService.archiveDraftV2(idamTokens, ccdCaseId, mutator);

        return Optional.of(SaveCaseResult.builder()
                .caseDetailsId(ccdCaseId)
                .saveCaseOperation(SaveCaseOperation.ARCHIVE)
                .build());
    }

    private SaveCaseResult saveDraftCaseInCcd(SyaCaseWrapper syaCaseWrapper, IdamTokens idamTokens, Boolean forceCreate) {

        SaveCaseResult result;

        log.info("SubmitAppealServiceV2 saveDraftCaseInCcd {}", forceCreate);

        if (Boolean.TRUE.equals(forceCreate)) {
            SscsCaseData caseData = convertSyaToCcdCaseDataV2(syaCaseWrapper, caseAccessManagementFeature, SscsCaseData.builder().build());
            log.info("SubmitAppealServiceV2 saveDraftCaseInCcd createDraft");
            result = citizenCcdService.createDraft(caseData, idamTokens);
        } else {
            log.info("SubmitAppealServiceV2 saveDraftCaseInCcd saveCaseV2");
            Consumer<SscsCaseData> mutator = caseData -> convertSyaToCcdCaseDataV2(syaCaseWrapper, caseAccessManagementFeature, caseData);
            result = citizenCcdService.saveCaseV2(idamTokens, mutator);
        }

        log.info("SubmitAppealServiceV2 Draft case with CCD Id {} successfully {}, IDAM id {} and roles {} ",
                result.getCaseDetailsId(),
                result.getSaveCaseOperation().name(),
                idamTokens.getUserId(),
                idamTokens.getRoles());

        return result;
    }

    public Long submitAppeal(SyaCaseWrapper appeal, String userToken) {

        IdamTokens idamTokens = idamService.getIdamTokens();

        Long caseId = nonNull(appeal.getCcdCaseId()) ? Long.valueOf(appeal.getCcdCaseId()) : null;

        log.info("Converting sya appeal data to sscs case");
        SscsCaseData caseData = convertAppealToSscsCaseData(appeal,SscsCaseData.builder().build());

        EventType eventType = findEventType(caseData, nonNull(caseId));

        SscsCaseDetails caseDetails = createOrUpdateCase(appeal, caseData, eventType, idamTokens);
        associateCase(idamTokens, caseDetails, userToken);
        return caseDetails.getId();
    }

    private SscsCaseDetails createOrUpdateCase(SyaCaseWrapper appeal, SscsCaseData caseData, EventType eventType, IdamTokens idamTokens) {
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
                }

                log.info("About to attempt creating case or updating draft case in CCD with event {} for benefit type {} and event {} and isScottish {} and languagePreference {}",
                        eventType,
                        benefitShortName,
                        eventType,
                        caseData.getIsScottishCase(),
                        caseData.getLanguagePreference().getCode());

                if (eventType == DRAFT_TO_VALID_APPEAL_CREATED || eventType == DRAFT_TO_INCOMPLETE_APPLICATION || eventType == DRAFT_TO_NON_COMPLIANT) {
                    caseDetails = updateCcdCaseService.updateCaseV2WithUnaryFunction(
                            Long.valueOf(caseData.getCcdCaseId()),
                            eventType.getCcdType(),
                            "SSCS - new case created",
                            "Created SSCS case from Submit Your Appeal online draft with event " + eventType.getCcdType(),
                            idamTokens,
                            dbCaseDetails -> {
                                SscsCaseData updatedCaseData = convertAppealToSscsCaseData(appeal, dbCaseDetails.getData());
                                updatedCaseData.setCaseCreated(LocalDate.now().toString());
                                addAssociatedCases(updatedCaseData, matchedByNinoCases);
                                dbCaseDetails.setData(updatedCaseData);
                                return dbCaseDetails;
                            });

                    log.info("Case {} successfully converted from Draft to SSCS case in CCD for benefit type {} with event {}",
                            caseDetails.getId(),
                            caseData.getAppeal().getBenefitType().getCode(),
                            eventType);
                } else {
                    addAssociatedCases(caseData, matchedByNinoCases);

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

    SscsCaseData convertAppealToSscsCaseData(SyaCaseWrapper appeal, SscsCaseData caseData) {
        return convertAppealToSscsCaseData(
                appeal,
                rpc -> Objects.isNull(rpc)
                        ? convertSyaToCcdCaseDataV2(appeal, caseAccessManagementFeature, caseData)
                        : convertSyaToCcdCaseDataV2(appeal, rpc.getName(), rpc, caseAccessManagementFeature, caseData)
        );
    }
}
