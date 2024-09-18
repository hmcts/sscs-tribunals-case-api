package uk.gov.hmcts.reform.sscs.service;

import static uk.gov.hmcts.reform.sscs.transform.deserialize.SubmitYourAppealToCcdCaseDataDeserializer.convertSyaToCcdCaseDataV1;

import feign.FeignException;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.config.CitizenCcdService;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.reform.sscs.exception.ApplicationErrorException;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.model.SaveCaseOperation;
import uk.gov.hmcts.reform.sscs.model.SaveCaseResult;
import uk.gov.hmcts.reform.sscs.model.draft.SessionDraft;
import uk.gov.hmcts.reform.sscs.service.converter.ConvertAIntoBService;

@Service
@Slf4j
@ConditionalOnProperty(name = "feature.submit-appeal-service-v2.enabled", havingValue = "false")
public class SubmitAppealService extends AbstractSubmitAppealService {

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
