package uk.gov.hmcts.reform.sscs.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.builder.TrackYourAppealJsonBuilder;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.exception.AppealNotFoundException;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.model.tya.SubscriptionRequest;
import uk.gov.hmcts.reform.sscs.model.tya.SurnameResponse;
import uk.gov.hmcts.reform.sscs.service.exceptions.InvalidSurnameException;

@Service
@Slf4j
public class TribunalsService {
    private CcdService ccdService;
    private RegionalProcessingCenterService regionalProcessingCenterService;
    private TrackYourAppealJsonBuilder trackYourAppealJsonBuilder;
    private IdamService idamService;

    @Autowired
    TribunalsService(CcdService ccdService,
                     RegionalProcessingCenterService regionalProcessingCenterService,
                     TrackYourAppealJsonBuilder trackYourAppealJsonBuilder,
                     IdamService idamService) {
        this.ccdService = ccdService;
        this.regionalProcessingCenterService = regionalProcessingCenterService;
        this.trackYourAppealJsonBuilder = trackYourAppealJsonBuilder;
        this.idamService = idamService;
    }

    public ObjectNode findAppeal(String appealNumber) {
        SscsCaseDetails caseByAppealNumber = ccdService.findCaseByAppealNumber(appealNumber, idamService.getIdamTokens());
        if (caseByAppealNumber == null) {
            log.info("Appeal does not exist for appeal number: " + appealNumber);
            throw new AppealNotFoundException(appealNumber);
        }

        return trackYourAppealJsonBuilder.build(caseByAppealNumber.getData(), getRegionalProcessingCenter(caseByAppealNumber.getData()), caseByAppealNumber.getId());
    }

    private RegionalProcessingCenter getRegionalProcessingCenter(SscsCaseData caseByAppealNumber) {
        RegionalProcessingCenter regionalProcessingCenter;

        if (null == caseByAppealNumber.getRegionalProcessingCenter()) {
            regionalProcessingCenter =
                    regionalProcessingCenterService.getByScReferenceCode(caseByAppealNumber.getCaseReference());
        } else {
            regionalProcessingCenter = caseByAppealNumber.getRegionalProcessingCenter();
        }
        return regionalProcessingCenter;
    }

    public String unsubscribe(String appealNumber) {
        SscsCaseDetails sscsCaseDetails = ccdService.updateSubscription(appealNumber, null, idamService.getIdamTokens());

        return sscsCaseDetails != null ? sscsCaseDetails.getData().getAppeal().getBenefitType().getCode().toLowerCase() : "";
    }

    public String updateSubscription(String appealNumber, SubscriptionRequest subscriptionRequest) {
        SscsCaseDetails sscsCaseDetails = ccdService.updateSubscription(appealNumber, subscriptionRequest.getEmail(), idamService.getIdamTokens());

        return sscsCaseDetails != null ? sscsCaseDetails.getData().getAppeal().getBenefitType().getCode().toLowerCase() : "";
    }

    public SurnameResponse validateSurname(String appealNumber, String surname) {
        IdamTokens idamTokens = idamService.getIdamTokens();

        // Try raw e.g. "Surname(appointee)"
        SscsCaseData caseData = ccdService.findCcdCaseByAppealNumberAndSurname(appealNumber, surname, idamTokens);
        if (caseData != null) {
            return new SurnameResponse(caseData.getCcdCaseId(), appealNumber, surname);
        }
        
        // Try clean e.g. "Surname"
        String cleanSurname = stripBracketedSuffix(surname);

        if (!surname.equalsIgnoreCase(cleanSurname)) {
            caseData = ccdService.findCcdCaseByAppealNumberAndSurname(appealNumber, cleanSurname, idamTokens);

            if (caseData != null) {
                return new SurnameResponse(caseData.getCcdCaseId(), appealNumber, cleanSurname);
            }
        }

        // Try neat e.g. "Surname (Appointee)"
        String neatSurname = cleanSurname + " (Appointee)";

        if (!surname.equalsIgnoreCase(neatSurname)) {
            caseData = ccdService.findCcdCaseByAppealNumberAndSurname(appealNumber, neatSurname, idamTokens);

            if (caseData != null) {
                return new SurnameResponse(caseData.getCcdCaseId(), appealNumber, neatSurname);
            }
        }

        log.info("Not a valid surname: '" + surname + "' for appeal " + appealNumber);
        throw new InvalidSurnameException();
    }

    private String stripBracketedSuffix(String input) {
        Pattern p = Pattern.compile("^([A-Za-zÀ-ž '-]{2,})(\\s*\\(\\s*[APOINTEapointe ]{0,}\\)\\s*){0,1}$");
        Matcher m = p.matcher(input);
        if (m.matches()) {
            return m.group(1).trim();
        }

        return "";
    }
}
