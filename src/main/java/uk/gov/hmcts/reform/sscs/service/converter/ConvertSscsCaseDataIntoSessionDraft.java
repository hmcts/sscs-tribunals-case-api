package uk.gov.hmcts.reform.sscs.service.converter;

import static uk.gov.hmcts.reform.sscs.model.draft.SessionMrnOverThirteenMonthsLate.mrnOverThirteenMonthsLate;

import com.google.common.base.Preconditions;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.MrnDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.model.draft.SessionBenefitType;
import uk.gov.hmcts.reform.sscs.model.draft.SessionCheckMrn;
import uk.gov.hmcts.reform.sscs.model.draft.SessionCreateAccount;
import uk.gov.hmcts.reform.sscs.model.draft.SessionDraft;
import uk.gov.hmcts.reform.sscs.model.draft.SessionHaveAMrn;
import uk.gov.hmcts.reform.sscs.model.draft.SessionMrnDate;
import uk.gov.hmcts.reform.sscs.model.draft.SessionMrnOverThirteenMonthsLate;
import uk.gov.hmcts.reform.sscs.model.draft.SessionPostcodeChecker;

@Service
public class ConvertSscsCaseDataIntoSessionDraft implements ConvertAintoBService<SscsCaseData, SessionDraft> {
    @Override
    public SessionDraft convert(SscsCaseData caseData) {
        Preconditions.checkNotNull(caseData);

        Appeal appeal = caseData.getAppeal();
        MrnDetails mrnDetails = appeal.getMrnDetails();
        boolean hasMrnDetails = StringUtils.isNotBlank(mrnDetails.getMrnDate());

        return new SessionDraft(
            new SessionBenefitType(appeal.getBenefitType()),
            new SessionPostcodeChecker(appeal.getAppellant().getAddress()),
            new SessionCreateAccount(),
            new SessionHaveAMrn(mrnDetails),
            hasMrnDetails ? new SessionMrnDate(mrnDetails) : null,
            new SessionCheckMrn(mrnDetails),
            mrnOverThirteenMonthsLate(mrnDetails) ? new SessionMrnOverThirteenMonthsLate(mrnDetails) : null
        );
    }
}
