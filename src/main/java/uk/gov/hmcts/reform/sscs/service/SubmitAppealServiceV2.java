package uk.gov.hmcts.reform.sscs.service;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.sscs.transform.deserialize.SubmitYourAppealToCcdCaseDataDeserializer.convertSyaToCcdCaseDataV1;
import static uk.gov.hmcts.reform.sscs.transform.deserialize.SubmitYourAppealToCcdCaseDataDeserializer.convertSyaToCcdCaseDataV2;

import feign.FeignException;
import java.util.Optional;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
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
import uk.gov.hmcts.reform.sscs.idam.UserDetails;
import uk.gov.hmcts.reform.sscs.model.SaveCaseOperation;
import uk.gov.hmcts.reform.sscs.model.SaveCaseResult;
import uk.gov.hmcts.reform.sscs.model.draft.SessionDraft;
import uk.gov.hmcts.reform.sscs.service.converter.ConvertAIntoBService;

@Service
@Slf4j
public class SubmitAppealServiceV2 {

    public static final String DM_STORE_USER_ID = "sscs";
    private static final String CITIZEN_ROLE = "citizen";
    public static final String DRAFT = "draft";
    public static final String USER_HAS_A_INVALID_ROLE_MESSAGE = "User has a invalid role";

    private final CcdService ccdService;
    private final CitizenCcdService citizenCcdService;
    private final RegionalProcessingCenterService regionalProcessingCenterService;
    private final IdamService idamService;
    private final ConvertAIntoBService<SscsCaseData, SessionDraft> convertAIntoBService;
    private final AirLookupService airLookupService;
    private final RefDataService refDataService;
    private final VenueService venueService;
    private final boolean caseAccessManagementFeature;

    @SuppressWarnings("squid:S107")
    SubmitAppealServiceV2(CcdService ccdService,
                          CitizenCcdService citizenCcdService,
                          RegionalProcessingCenterService regionalProcessingCenterService,
                          IdamService idamService,
                          ConvertAIntoBService<SscsCaseData, SessionDraft> convertAIntoBService,
                          AirLookupService airLookupService,
                          RefDataService refDataService,
                          VenueService venueService,
                          @Value("${feature.case-access-management.enabled}")  boolean caseAccessManagementFeature) {
        this.ccdService = ccdService;
        this.citizenCcdService = citizenCcdService;
        this.regionalProcessingCenterService = regionalProcessingCenterService;
        this.idamService = idamService;
        this.convertAIntoBService = convertAIntoBService;
        this.airLookupService = airLookupService;
        this.refDataService = refDataService;
        this.caseAccessManagementFeature = caseAccessManagementFeature;
        this.venueService = venueService;
    }

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

    public Optional<SaveCaseResult> updateDraftAppeal(String oauth2Token, SyaCaseWrapper syaCaseWrapper) {
        syaCaseWrapper.setCaseType(DRAFT);

        IdamTokens idamTokens = getUserTokens(oauth2Token);
        if (!hasValidCitizenRole(idamTokens)) {
            throw new ApplicationErrorException(new Exception(USER_HAS_A_INVALID_ROLE_MESSAGE));
        }

        try {
            Consumer<SscsCaseData> mutator = caseData -> convertSyaToCcdCaseDataV2(syaCaseWrapper, caseAccessManagementFeature, caseData);

            CaseDetails caseDetails = citizenCcdService.updateCaseV2(syaCaseWrapper.getCcdCaseId(), EventType.UPDATE_DRAFT.getCcdType(),
                    "Update draft", "Update draft in CCD", idamTokens, mutator);

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

    private SaveCaseResult saveDraftCaseInCcd(SyaCaseWrapper syaCaseWrapper, IdamTokens idamTokens, Boolean forceCreate) {

        SaveCaseResult result;

        log.info("SubmitAppealServiceV2 saveDraftCaseInCcd {}", forceCreate);

        if (Boolean.TRUE.equals(forceCreate)) {
            SscsCaseData caseData = convertSyaToCcdCaseDataV2(syaCaseWrapper, caseAccessManagementFeature, new SscsCaseData());
            log.info("SubmitAppealServiceV2 saveDraftCaseInCcd createDraft");
            result = citizenCcdService.createDraft(caseData, idamTokens);
        } else {
            log.info("SubmitAppealServiceV2 saveDraftCaseInCcd saveCaseV2");
            Consumer<SscsCaseData> mutator = caseData -> convertSyaToCcdCaseDataV2(syaCaseWrapper, caseAccessManagementFeature, caseData);
            result = citizenCcdService.saveCaseV2(idamTokens, mutator);
        }

        log.info("SubmitAppealServiceV2 POST Draft case with CCD Id {} , IDAM id {} and roles {} ",
                result.getCaseDetailsId(),
                idamTokens.getUserId(),
                idamTokens.getRoles());

        log.info("SubmitAppealServiceV2 Draft Case {} successfully {} in CCD", result.getCaseDetailsId(), result.getSaveCaseOperation().name());
        return result;
    }
}
