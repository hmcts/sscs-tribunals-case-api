package uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.reissueartifact;

import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appointee;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.Representative;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;

public abstract class ReissueArtifactHandlerTest {
    public CcdValue<OtherParty> buildOtherPartyWithAppointeeAndRep(String id, String appointeeId, String repId) {
        return CcdValue.<OtherParty>builder()
                .value(OtherParty.builder()
                        .id(id)
                        .name(Name.builder().firstName("Peter").lastName("Parker").build())
                        .isAppointee(StringUtils.isBlank(appointeeId) ? YesNo.NO.getValue() : YES.getValue())
                        .appointee(StringUtils.isBlank(appointeeId) ? null : Appointee.builder().id(appointeeId).name(Name.builder().firstName("Otto").lastName("Octavius").build()).build())
                        .rep(Representative.builder().id(repId).name(Name.builder().firstName("Harry").lastName("Osbourne").build()).hasRepresentative(YES.getValue()).build())
                        .build())
                .build();
    }
}
