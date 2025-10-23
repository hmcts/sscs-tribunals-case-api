package uk.gov.hmcts.reform.sscs.helper.mapping;

import static uk.gov.hmcts.reform.sscs.helper.mapping.HearingsPartiesMapping.buildHearingPartiesDetails;
import static uk.gov.hmcts.reform.sscs.helper.mapping.HearingsRequestMapping.buildHearingRequestDetails;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.EntityRoleCode.APPELLANT;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.EntityRoleCode.APPOINTEE;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.EntityRoleCode.INTERPRETER;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.EntityRoleCode.JOINT_PARTY;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.EntityRoleCode.OTHER_PARTY;
import static uk.gov.hmcts.reform.sscs.model.hmc.reference.EntityRoleCode.REPRESENTATIVE;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appointee;
import uk.gov.hmcts.reform.sscs.ccd.domain.Entity;
import uk.gov.hmcts.reform.sscs.ccd.domain.Interpreter;
import uk.gov.hmcts.reform.sscs.ccd.domain.JointParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.Representative;
import uk.gov.hmcts.reform.sscs.exception.ListingException;
import uk.gov.hmcts.reform.sscs.model.HearingWrapper;
import uk.gov.hmcts.reform.sscs.model.hmc.reference.EntityRoleCode;
import uk.gov.hmcts.reform.sscs.model.single.hearing.HearingRequestPayload;
import uk.gov.hmcts.reform.sscs.service.holder.ReferenceDataServiceHolder;

@Slf4j
@Component
public final class HearingsMapping {

    private final HearingsDetailsMapping hearingsDetailsMapping;
    private final HearingsCaseMapping hearingsCaseMapping;

    HearingsMapping(HearingsDetailsMapping hearingsDetailsMapping, HearingsCaseMapping hearingsCaseMapping) {
        this.hearingsDetailsMapping = hearingsDetailsMapping;
        this.hearingsCaseMapping = hearingsCaseMapping;
    }

    public HearingRequestPayload buildHearingPayload(HearingWrapper wrapper, ReferenceDataServiceHolder refData)
            throws ListingException {
        return HearingRequestPayload.builder()
                .requestDetails(buildHearingRequestDetails(wrapper))
                .hearingDetails(hearingsDetailsMapping.buildHearingDetails(wrapper, refData))
                .caseDetails(hearingsCaseMapping.buildHearingCaseDetails(wrapper, refData))
                .partiesDetails(buildHearingPartiesDetails(wrapper, refData))
                .build();
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
