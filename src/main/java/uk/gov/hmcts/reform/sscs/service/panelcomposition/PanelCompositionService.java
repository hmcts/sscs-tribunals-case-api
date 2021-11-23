package uk.gov.hmcts.reform.sscs.service.panelcomposition;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtil.hasDueDateSetAndOtherPartyWithoutHearingOption;

@Service
public class PanelCompositionService {

    private final CcdService ccdService;
    private final IdamService idamService;

    @Autowired
    public PanelCompositionService(CcdService ccdService, IdamService idamService) {
        this.ccdService = ccdService;
        this.idamService = idamService;
    }

    public SscsCaseDetails processCaseState(Callback<SscsCaseData> callback, SscsCaseData caseData) {
        if (caseData.getIsFqpmRequired() == null
                || hasDueDateSetAndOtherPartyWithoutHearingOption(caseData)) {
            return ccdService.updateCase(caseData, callback.getCaseDetails().getId(),
                    EventType.NOT_LISTABLE.getCcdType(), "Not listable",
                    "Update to Not Listable as it is awaiting hearing enquiry form.", idamService.getIdamTokens());
        } else {
            return ccdService.updateCase(caseData, callback.getCaseDetails().getId(),
                    EventType.READY_TO_LIST.getCcdType(), "Ready to list",
                    "Update to ready to list event as there is no further information to assist the tribunal and no dispute.", idamService.getIdamTokens());
        }
    }
}
