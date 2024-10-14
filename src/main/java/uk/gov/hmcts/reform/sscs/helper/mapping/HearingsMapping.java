package uk.gov.hmcts.reform.sscs.helper.mapping;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;
import static uk.gov.hmcts.reform.sscs.helper.mapping.HearingsCaseMapping.buildHearingCaseDetails;
import static uk.gov.hmcts.reform.sscs.helper.mapping.HearingsDetailsMapping.buildHearingDetails;
import static uk.gov.hmcts.reform.sscs.helper.mapping.HearingsPartiesMapping.buildHearingPartiesDetails;
import static uk.gov.hmcts.reform.sscs.helper.mapping.HearingsRequestMapping.buildHearingRequestDetails;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.EntityRoleCode.APPELLANT;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.EntityRoleCode.APPOINTEE;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.EntityRoleCode.INTERPRETER;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.EntityRoleCode.JOINT_PARTY;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.EntityRoleCode.OTHER_PARTY;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.EntityRoleCode.REPRESENTATIVE;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appointee;
import uk.gov.hmcts.reform.sscs.ccd.domain.Entity;
import uk.gov.hmcts.reform.sscs.ccd.domain.Interpreter;
import uk.gov.hmcts.reform.sscs.ccd.domain.JointParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.Representative;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.exception.ListingException;
import uk.gov.hmcts.reform.sscs.model.HearingWrapper;
import uk.gov.hmcts.reform.sscs.model.hmc.reference.EntityRoleCode;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingRequestPayload;
import uk.gov.hmcts.reform.sscs.reference.data.model.SessionCategoryMap;
import uk.gov.hmcts.reform.sscs.service.holder.ReferenceDataServiceHolder;

@Slf4j
public final class HearingsMapping {

    public static final String DWP_ID = "DWP";

    private HearingsMapping() {
    }

    public static HearingRequestPayload buildHearingPayload(HearingWrapper wrapper, ReferenceDataServiceHolder refData)
            throws ListingException {
        return HearingRequestPayload.builder()
                .requestDetails(buildHearingRequestDetails(wrapper))
                .hearingDetails(buildHearingDetails(wrapper, refData))
                .caseDetails(buildHearingCaseDetails(wrapper, refData))
                .partiesDetails(buildHearingPartiesDetails(wrapper, refData))
                .build();
    }

    public static SessionCategoryMap getSessionCaseCodeMap(SscsCaseData caseData, ReferenceDataServiceHolder refData) {
        boolean doctorSpecialistSecond = isNotBlank(caseData.getSscsIndustrialInjuriesData().getSecondPanelDoctorSpecialism());
        boolean fqpmRequired = isYes(caseData.getIsFqpmRequired());
        return refData.getSessionCategoryMaps()
                .getSessionCategory(caseData.getBenefitCode(),
                        caseData.getIssueCode(),
                        doctorSpecialistSecond,
                        fqpmRequired);
    }

    public static EntityRoleCode getEntityRoleCode(Entity entity) {
        if (entity instanceof Appellant) {
            return APPELLANT;
        }
        if (entity instanceof Appointee) {
            return APPOINTEE;
        }
        if (entity instanceof Interpreter) {
            return INTERPRETER;
        }
        if (entity instanceof Representative) {
            return REPRESENTATIVE;
        }
        if (entity instanceof JointParty) {
            return JOINT_PARTY;
        }
        return OTHER_PARTY;
    }

}
