package uk.gov.hmcts.reform.sscs.service.v2;

import static uk.gov.hmcts.reform.sscs.transform.deserialize.SubmitYourAppealToCcdCaseDataDeserializer.convertSyaToCcdCaseDataV2;

import feign.FeignException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.config.CitizenCcdService;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.reform.sscs.exception.ApplicationErrorException;
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

    @Override
    protected SscsCaseDetails getUpdatedCaseDetails(SscsCaseData caseData, EventType eventType, IdamTokens idamTokens, List<SscsCaseDetails> matchedByNinoCases) {
        return updateCcdCaseService.updateCaseV2(Long.valueOf(caseData.getCcdCaseId()),
                eventType.getCcdType(),
                "SSCS - new case created",
                "Created SSCS case from Submit Your Appeal online draft with event " + eventType.getCcdType(),
                idamTokens,
                caseDetails -> {
                    SscsCaseData sscsCaseData = addAssociatedCases(caseDetails.getData(), matchedByNinoCases);
                    sscsCaseData.setCaseCreated(LocalDate.now().toString());
                });
    }
}
