package uk.gov.hmcts.reform.sscs.service;

import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.VALID_APPEAL;
import static uk.gov.hmcts.reform.sscs.transform.deserialize.SubmitYourAppealToCcdCaseDataDeserializer.convertSyaToCcdCaseData;

import feign.FeignException;
import java.time.LocalDate;
import java.util.*;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.exception.CcdException;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.config.CitizenCcdService;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;
import uk.gov.hmcts.reform.sscs.model.SaveCaseResult;
import uk.gov.hmcts.reform.sscs.model.draft.SessionDraft;
import uk.gov.hmcts.reform.sscs.service.converter.ConvertAintoBService;

@Service
@Slf4j
public class SubmitAppealService {
    public static final String DM_STORE_USER_ID = "sscs";

    private final CcdService ccdService;
    private final CitizenCcdService citizenCcdService;
    private final SscsPdfService sscsPdfService;
    private final RegionalProcessingCenterService regionalProcessingCenterService;
    private final IdamService idamService;
    private final ConvertAintoBService convertAintoBService;
    private final List<String> offices;

    @Autowired
    SubmitAppealService(CcdService ccdService,
                        CitizenCcdService citizenCcdService,
                        SscsPdfService sscsPdfService,
                        RegionalProcessingCenterService regionalProcessingCenterService,
                        IdamService idamService,
                        ConvertAintoBService convertAintoBService,
                        @Value("#{'${readyToList.offices}'.split(',')}") List<String> offices) {

        this.ccdService = ccdService;
        this.citizenCcdService = citizenCcdService;
        this.sscsPdfService = sscsPdfService;
        this.regionalProcessingCenterService = regionalProcessingCenterService;
        this.idamService = idamService;
        this.convertAintoBService = convertAintoBService;
        this.offices = offices;
    }

    public Long submitAppeal(SyaCaseWrapper appeal, String userToken) {
        String firstHalfOfPostcode = regionalProcessingCenterService
            .getFirstHalfOfPostcode(appeal.getContactDetails().getPostCode());
        SscsCaseData caseData = prepareCaseForCcd(appeal, firstHalfOfPostcode);

        EventType event = findEventType(caseData);
        IdamTokens idamTokens = idamService.getIdamTokens();
        SscsCaseDetails caseDetails = createCaseInCcd(caseData, event, idamTokens);
        postCreateCaseInCcdProcess(caseData, idamTokens, caseDetails, event, userToken);
        // in case of duplicate case the caseDetails will be null
        return (caseDetails != null) ? caseDetails.getId() : null;
    }

    public Optional<SaveCaseResult> submitDraftAppeal(String oauth2Token, SyaCaseWrapper appeal) {
        appeal.setCaseType("draft");
        IdamTokens idamTokens = getUserTokens(oauth2Token);
        try {
            return Optional.of(saveDraftCaseInCcd(convertSyaToCcdCaseData(appeal), idamTokens));
        } catch (FeignException e) {
            if (e.status() == HttpStatus.SC_CONFLICT) {
                log.error("The case data has been altered outside of this transaction for case with nino {} and idam id {}",
                        appeal.getAppellant().getNino(),
                        idamTokens.getUserId());
                return Optional.empty();
            } else {
                throw e;
            }
        }
    }

    @SuppressWarnings("unchecked")
    public Optional<SessionDraft> getDraftAppeal(String oauth2Token) {
        SscsCaseData caseDetails = null;
        SessionDraft sessionDraft = null;
        IdamTokens idamTokens = getUserTokens(oauth2Token);
        List<SscsCaseData> caseDetailsList = citizenCcdService.findCase(idamTokens);
        if (CollectionUtils.isNotEmpty(caseDetailsList)) {
            caseDetails = caseDetailsList.get(0);
            sessionDraft = (SessionDraft) convertAintoBService.convert(caseDetails);
        }
        log.info("GET Draft case with CCD Id {} , IDAM Id {} and roles {} ",
                (caseDetails == null) ? null : caseDetails.getCcdCaseId(),
                idamTokens.getUserId(),
                idamTokens.getRoles());
        return (sessionDraft != null) ? Optional.of(sessionDraft) : Optional.empty();
    }

    private IdamTokens getUserTokens(String oauth2Token) {
        UserDetails userDetails = idamService.getUserDetails(oauth2Token);
        return IdamTokens.builder()
            .idamOauth2Token(oauth2Token)
            .serviceAuthorization(idamService.generateServiceAuthorization())
            .userId(userDetails.getId())
            .roles(userDetails.getRoles())
            .build();
    }

    private void postCreateCaseInCcdProcess(SscsCaseData caseData,
                                            IdamTokens idamTokens, SscsCaseDetails caseDetails, EventType event,
                                            String userToken) {
        if (null != caseDetails) {
            sscsPdfService.generateAndSendPdf(caseData, caseDetails.getId(), idamTokens, "sscs1");
            if (event.equals(SYA_APPEAL_CREATED) || event.equals(VALID_APPEAL_CREATED)) {
                log.info("About to update case with sendToDwp event for id {}", caseDetails.getId());
                ccdService.updateCase(caseData, caseDetails.getId(), SEND_TO_DWP.getCcdType(), "Send to DWP", "Send to DWP event has been triggered from Tribunals service", idamTokens);
                log.info("Case updated with sendToDwp event for id {}", caseDetails.getId());
            }
            if (StringUtils.isNotEmpty(userToken)) {
                citizenCcdService.draftArchived(caseData, getUserTokens(userToken), idamTokens);
            }
        }
    }

    SscsCaseData prepareCaseForCcd(SyaCaseWrapper appeal, String postcode) {
        RegionalProcessingCenter rpc = regionalProcessingCenterService.getByPostcode(postcode);

        SscsCaseData sscsCaseData;
        if (rpc == null) {
            sscsCaseData = convertSyaToCcdCaseData(appeal);
        } else {
            sscsCaseData = convertSyaToCcdCaseData(appeal, rpc.getName(), rpc);
        }

        setCreatedInGapsFromField(sscsCaseData);

        return sscsCaseData;
    }

    private SscsCaseData setCreatedInGapsFromField(SscsCaseData sscsCaseData) {
        String createdInGapsFrom = offices.contains(sscsCaseData.getAppeal().getMrnDetails().getDwpIssuingOffice())
            ? READY_TO_LIST.getId() : VALID_APPEAL.getId();
        sscsCaseData.setCreatedInGapsFrom(createdInGapsFrom);
        return sscsCaseData;
    }

    String getFirstHalfOfPostcode(String postcode) {
        if (postcode != null && postcode.length() > 3) {
            return postcode.substring(0, postcode.length() - 3).trim();
        }
        return "";
    }

    private SscsCaseDetails createCaseInCcd(SscsCaseData caseData, EventType eventType, IdamTokens idamTokens) {
        SscsCaseDetails caseDetails = null;
        try {
            caseDetails = ccdService.findCcdCaseByNinoAndBenefitTypeAndMrnDate(caseData, idamTokens);;

            List<SscsCaseDetails> matchedByNinoCases = getMatchedCases(caseData.getGeneratedNino(), idamTokens);

            log.info("Found " + matchedByNinoCases.size() + " matching cases for Nino " + caseData.getGeneratedNino());

            if (caseDetails == null) {
                caseData = addAssociatedCases(caseData, matchedByNinoCases);

                caseDetails = ccdService.createCase(caseData,
                    eventType.getCcdType(),
                    "SSCS - new case created",
                    "Created SSCS case from Submit Your Appeal online with event " + eventType.getCcdType(),
                    idamTokens);
                log.info("Case {} successfully created in CCD for benefit type {} with event {}",
                    caseDetails.getId(),
                    caseData.getAppeal().getBenefitType().getCode(),
                    eventType);
                return caseDetails;
            } else {
                log.info("Duplicate case {} found for Nino {} and benefit type {}. "
                        + "No need to continue with post create case processing.",
                    caseDetails.getId(), caseData.getGeneratedNino(),
                    caseData.getAppeal().getBenefitType().getCode());
                return null;
            }
        } catch (Exception e) {
            throw new CcdException(
                String.format("Error found in the creating case process for case with Id - %s"
                        + " and Nino - %s and Benefit type - %s and exception: %s",
                    caseDetails != null ? caseDetails.getId() : "", caseData.getGeneratedNino(),
                    caseData.getAppeal().getBenefitType().getCode(), e.getMessage()), e);
        }
    }

    protected List<SscsCaseDetails> getMatchedCases(String generatedNino, IdamTokens idamTokens) {
        HashMap<String, String> map = new HashMap<String, String>();

        map.put("case.generatedNino", generatedNino);

        return ccdService.findCaseBy(map, idamTokens);
    }

    protected SscsCaseData addAssociatedCases(SscsCaseData caseData, List<SscsCaseDetails> matchedByNinoCases) {
        log.info("Adding " + matchedByNinoCases.size() + " associated cases");
        List<CaseLink> associatedCases = new ArrayList<>();

        for (SscsCaseDetails sscsCaseDetails: matchedByNinoCases) {
            log.info("Linking case " + sscsCaseDetails.getId().toString());
            CaseLink caseLink = CaseLink.builder().value(
                    CaseLinkDetails.builder().caseReference(sscsCaseDetails.getId().toString()).build()).build();
            associatedCases.add(caseLink);
        }
        if (associatedCases.size() > 0) {
            return caseData.toBuilder().associatedCase(associatedCases).linkedCasesBoolean("Yes").build();
        } else {
            return caseData.toBuilder().linkedCasesBoolean("No").build();
        }
    }


    private SaveCaseResult saveDraftCaseInCcd(SscsCaseData caseData, IdamTokens idamTokens) {
        SaveCaseResult result = citizenCcdService.saveCase(caseData, idamTokens);
        log.info("POST Draft case with CCD Id {} , IDAM id {} and roles {} ",
                result.getCaseDetailsId(),
                idamTokens.getUserId(),
                idamTokens.getRoles());
        log.info("Draft Case {} successfully {} in CCD", result.getCaseDetailsId(), result.getSaveCaseOperation().name());
        return result;
    }

    private EventType findEventType(SscsCaseData caseData) {
        if (caseData.getAppeal().getMrnDetails() != null && caseData.getAppeal().getMrnDetails().getMrnDate() != null) {
            LocalDate mrnDate = LocalDate.parse(caseData.getAppeal().getMrnDetails().getMrnDate());
            boolean moveToNoneCompliant = mrnDate.plusMonths(13L).isBefore(LocalDate.now());

            if (moveToNoneCompliant) {
                log.info("Moving case for NINO {} to non-compliant as MRN Date is older than 13 months", caseData.getAppeal().getAppellant().getIdentity().getNino());
                return NON_COMPLIANT;
            } else {
                log.info("Valid appeal to be created for case with NINO {}", caseData.getAppeal().getAppellant().getIdentity().getNino());
                return VALID_APPEAL_CREATED;
            }
        } else {
            log.info("Moving case for NINO {} to incomplete due to MRN Details {} present and MRN Date {} present",
                caseData.getAppeal().getAppellant().getIdentity().getNino(),
                (caseData.getAppeal().getMrnDetails() != null ? "" : "not"),
                (caseData.getAppeal().getMrnDetails().getMrnDate() != null ? "" : "not"));
            return INCOMPLETE_APPLICATION_RECEIVED;
        }
    }
}
