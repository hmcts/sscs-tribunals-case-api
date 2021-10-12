package uk.gov.hmcts.reform.sscs.service;

import static java.util.Collections.emptyList;

import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.domain.wrapper.*;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@Slf4j
@Service
public class OnlineHearingService {
    private final CcdService ccdService;
    private final IdamService idamService;

    public OnlineHearingService(
            @Autowired CcdService ccdService,
            @Autowired IdamService idamService) {
        this.ccdService = ccdService;
        this.idamService = idamService;
    }

    public Optional<SscsCaseDetails> getCcdCaseByIdentifier(String identifier) {
        return !NumberUtils.isDigits(identifier)
                ? getCcdCase(identifier) :
                Optional.ofNullable(ccdService.getByCaseId(Long.parseLong(identifier), idamService.getIdamTokens()));
    }

    public Optional<SscsCaseDetails> getCcdCase(String onlineHearingId) {
        return getCcdCaseByIdentifier(onlineHearingId).map(sscsCaseDetails -> {
            IdamTokens idamTokens = idamService.getIdamTokens();
            SscsCaseDetails caseDetails = ccdService.getByCaseId(sscsCaseDetails.getId(), idamTokens);

            if (caseDetails == null) {
                throw new IllegalStateException("Online hearing for ccdCaseId [" + sscsCaseDetails.getId() + "] found but cannot find the case in CCD");
            }

            return caseDetails;
        });
    }

    public Optional<OnlineHearing> loadHearing(SscsCaseDetails sscsCaseDeails) {
        SscsCaseData data = sscsCaseDeails.getData();
        HearingOptions hearingOptions = data.getAppeal().getHearingOptions();
        Appellant appellant = sscsCaseDeails.getData().getAppeal().getAppellant();
        AppellantDetails appellantDetails = convertAppellantDetails(appellant);
        AppealDetails appealDetails = convertAppealDetails(sscsCaseDeails);
        Name name = appellant.getName();
        String nameString = name.getFirstName() + " " + name.getLastName();

        List<String> arrangements = (hearingOptions.getArrangements() != null)
                ? hearingOptions.getArrangements() : emptyList();
        return Optional.of(new OnlineHearing(
                nameString,
                sscsCaseDeails.getData().getCaseReference(),
                sscsCaseDeails.getId(),
                new HearingArrangements(
                        "yes".equalsIgnoreCase(hearingOptions.getLanguageInterpreter()),
                        hearingOptions.getLanguages(),
                        arrangements.contains("signLanguageInterpreter"),
                        hearingOptions.getSignLanguageType(),
                        arrangements.contains("hearingLoop"),
                        arrangements.contains("disabledAccess"),
                        hearingOptions.getOther()
                ),
                appellantDetails,
                appealDetails
        ));
    }

    private AppealDetails convertAppealDetails(SscsCaseDetails sscsCaseDetails) {
        return new AppealDetails(sscsCaseDetails.getData().getCaseCreated(),
                sscsCaseDetails.getData().getAppeal().getMrnDetails().getMrnDate(),
                sscsCaseDetails.getData().getAppeal().getBenefitType().getCode(),
                sscsCaseDetails.getState()
        );
    }

    private AppellantDetails convertAppellantDetails(Appellant appellant) {
        Address address = appellant.getAddress();
        Optional<Contact> contact = Optional.ofNullable(appellant.getContact());
        String email = null;
        String phone = null;
        String mobile = null;

        AddressDetails addressDetails = new AddressDetails(address.getLine1(), address.getLine2(), address.getTown(), address.getCounty(), address.getPostcode());

        if (contact.isPresent()) {
            Contact contactObj = contact.get();
            email = contactObj.getEmail();
            phone = contactObj.getPhone();
            mobile = contactObj.getMobile();
        }

        AppellantDetails appellantDetails = new AppellantDetails(addressDetails, email, phone, mobile);

        return appellantDetails;
    }
}
