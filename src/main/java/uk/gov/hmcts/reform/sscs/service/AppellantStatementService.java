package uk.gov.hmcts.reform.sscs.service;

import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtil.getOtherPartyIdFromTya;
import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtil.getOtherPartyNameFromTya;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.domain.wrapper.Statement;
import uk.gov.hmcts.reform.sscs.service.pdf.MyaEventActionContext;
import uk.gov.hmcts.reform.sscs.service.pdf.StoreAppellantStatementService;
import uk.gov.hmcts.reform.sscs.service.pdf.data.AppellantStatementPdfData;

@Service
public class AppellantStatementService {
    private final StoreAppellantStatementService storeAppellantStatementService;
    private final OnlineHearingService onlineHearingService;

    @Autowired
    public AppellantStatementService(
            StoreAppellantStatementService storeAppellantStatementService,
            OnlineHearingService onlineHearingService) {
        this.storeAppellantStatementService = storeAppellantStatementService;
        this.onlineHearingService = onlineHearingService;
    }

    public Optional<MyaEventActionContext> handleAppellantStatement(String identifier, Statement statement) {
        return onlineHearingService.getCcdCaseByIdentifier(identifier).map(caseDetails ->
                storeAppellantStatementService.storePdfAndUpdate(
                caseDetails.getId(),
                identifier,
                new AppellantStatementPdfData(caseDetails, statement,
                        getOtherPartyIdFromTya(caseDetails.getData(), statement.getTya()),
                        getOtherPartyNameFromTya(caseDetails.getData(), statement.getTya())
                )
        ));
    }
}
