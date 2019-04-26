package uk.gov.hmcts.reform.sscs.service.converter;

import com.google.common.base.Preconditions;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.MrnDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.model.draft.*;

@Service
public class ConvertSscsCaseDataIntoSessionDraft implements ConvertAintoBService<SscsCaseData, SessionDraft> {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public SessionDraft convert(SscsCaseData caseData) {
        Preconditions.checkNotNull(caseData);
        Preconditions.checkNotNull(caseData.getAppeal());

        Appeal appeal = caseData.getAppeal();
        return SessionDraft.builder()
            .benefitType(buildSessionBenefitType(appeal.getBenefitType()))
            .postcode(buildSessionPostcode(appeal))
            .createAccount(buildSessionCreateAccount())
            .haveAMrn(buildHaveAMrn(appeal))
            .mrnDate(buildMrnDate(appeal))
            .checkMrn(buildCheckMrn(appeal))
            .mrnOverThirteenMonthsLate(buildMrnOverThirteenMonthsLate(appeal))
            .dwpIssuingOffice(buildDwpIssuingOffice(appeal))
            .appointee(buildAppointee(appeal))
            .appellantName(buildAppellantName(appeal))
            .appellantDOB(buildAppellantDob(appeal))
            .appellantNino(buildAppellantNino(appeal))
            .build();
    }

    private SessionMrnOverThirteenMonthsLate buildMrnOverThirteenMonthsLate(Appeal appeal) {
        if (mrnOverThirteenMonthsLate(appeal.getMrnDetails())) {
            return new SessionMrnOverThirteenMonthsLate(appeal.getMrnDetails().getMrnLateReason());
        }
        return null;
    }

    private boolean mrnOverThirteenMonthsLate(MrnDetails mrnDetails) {
        if (mrnDetails.getMrnDate() != null) {
            LocalDate mrnDate = LocalDate.parse(mrnDetails.getMrnDate(), DATE_FORMATTER);
            return mrnDate.plusMonths(13L).isBefore(LocalDate.now());
        }
        return false;
    }

    private SessionCheckMrn buildCheckMrn(Appeal appeal) {
        return StringUtils.isBlank(appeal.getMrnDetails().getMrnDate()) ? new SessionCheckMrn("no")
            : new SessionCheckMrn("yes");
    }

    private SessionMrnDate buildMrnDate(Appeal appeal) {
        MrnDetails mrnDetails = appeal.getMrnDetails();
        if (StringUtils.isNotBlank(mrnDetails.getMrnDate())) {
            LocalDate mrdDetailsDate = LocalDate.parse(mrnDetails.getMrnDate());
            String day = String.valueOf(mrdDetailsDate.getDayOfMonth());
            String month = String.valueOf(mrdDetailsDate.getMonthValue());
            String year = String.valueOf(mrdDetailsDate.getYear());
            SessionDate mrnDateDetails = new SessionDate(day, month, year);
            return new SessionMrnDate(mrnDateDetails);
        }
        return null;
    }

    private SessionHaveAMrn buildHaveAMrn(Appeal appeal) {
        String haveAMrn = StringUtils.isBlank(appeal.getMrnDetails().getMrnDate()) ? "no" : "yes";
        return new SessionHaveAMrn(haveAMrn);
    }

    private SessionCreateAccount buildSessionCreateAccount() {
        return new SessionCreateAccount("yes");
    }

    private SessionPostcodeChecker buildSessionPostcode(Appeal appeal) {
        return new SessionPostcodeChecker(appeal.getAppellant().getAddress().getPostcode());
    }

    private SessionBenefitType buildSessionBenefitType(BenefitType benefitType) {
        return new SessionBenefitType(benefitType.getDescription() + " (" + benefitType.getCode() + ")");
    }

    private SessionDwpIssuingOffice buildDwpIssuingOffice(Appeal appeal) {
        if (appeal.getMrnDetails().getDwpIssuingOffice() != null
            && StringUtils.isNotEmpty(appeal.getMrnDetails().getDwpIssuingOffice())
        ) {
            int firstBracket = appeal.getMrnDetails().getDwpIssuingOffice().indexOf("(") + 1;
            int secondBracket = appeal.getMrnDetails().getDwpIssuingOffice().lastIndexOf(")");
            return new SessionDwpIssuingOffice(
                appeal.getMrnDetails().getDwpIssuingOffice().substring(firstBracket, secondBracket)
            );
        } else {
            return null;
        }
    }

    private SessionAppointee buildAppointee(Appeal appeal) {
        if (appeal.getAppellant() == null
            || appeal.getAppellant().getAppointee() == null
            || appeal.getAppellant().getAppointee().getName() == null) {
            return new SessionAppointee("no");
        }

        return new SessionAppointee("yes");
    }

    private SessionAppellantName buildAppellantName(Appeal appeal) {
        if (appeal.getAppellant() == null
            || appeal.getAppellant().getName() == null) {
            return null;
        }

        return new SessionAppellantName(
            appeal.getAppellant().getName().getTitle(),
            appeal.getAppellant().getName().getFirstName(),
            appeal.getAppellant().getName().getLastName()
        );
    }

    private SessionAppellantDOB buildAppellantDob(Appeal appeal) {
        if (appeal.getAppellant() == null
            || appeal.getAppellant().getIdentity() == null
            || appeal.getAppellant().getIdentity().getDob() == null) {
            return null;
        }

        LocalDate mrdDetailsDate = LocalDate.parse(appeal.getAppellant().getIdentity().getDob());
        String day = String.valueOf(mrdDetailsDate.getDayOfMonth());
        String month = String.valueOf(mrdDetailsDate.getMonthValue());
        String year = String.valueOf(mrdDetailsDate.getYear());
        SessionDate mrnDateDetails = new SessionDate(day, month, year);

        return new SessionAppellantDOB(mrnDateDetails);
    }

    private SessionAppellantNino buildAppellantNino(Appeal appeal) {
        if (appeal.getAppellant() == null
            || appeal.getAppellant().getIdentity() == null) {
            return null;
        }

        return new SessionAppellantNino(appeal.getAppellant().getIdentity().getNino());
    }
}
