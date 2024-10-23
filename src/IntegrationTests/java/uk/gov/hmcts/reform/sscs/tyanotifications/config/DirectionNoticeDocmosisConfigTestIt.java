package uk.gov.hmcts.reform.sscs.tyanotifications.config;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.PIP;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.SubscriptionType.*;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.DIRECTION_ISSUED;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.DIRECTION_ISSUED_WELSH;

import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.NotificationSscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.Template;
import uk.gov.hmcts.reform.sscs.tyanotifications.factory.CcdNotificationWrapper;
import uk.gov.hmcts.reform.sscs.tyanotifications.factory.NotificationWrapper;
import uk.gov.hmcts.reform.sscs.tyanotifications.personalisation.Personalisation;

public class DirectionNoticeDocmosisConfigTestIt extends AbstractNotificationConfigTest {

    @Test
    @Parameters({
        "APPEAL_TO_PROCEED, TB-SCS-GNO-ENG-00551.docx, TB-SCS-GNO-ENG-00551.docx, TB-SCS-GNO-ENG-00551.docx",
        "PROVIDE_INFORMATION, TB-SCS-GNO-ENG-00067.docx, TB-SCS-GNO-ENG-00089.docx, TB-SCS-GNO-ENG-00067.docx",
        "GRANT_EXTENSION, TB-SCS-GNO-ENG-00556.docx, TB-SCS-GNO-ENG-00556.docx, TB-SCS-GNO-ENG-00556.docx",
        "REFUSE_EXTENSION, TB-SCS-GNO-ENG-00557.docx, TB-SCS-GNO-ENG-00557.docx, TB-SCS-GNO-ENG-00557.docx",
        "GRANT_REINSTATEMENT, TB-SCS-GNO-ENG-00584.docx, TB-SCS-GNO-ENG-00584.docx, null",
        "REFUSE_REINSTATEMENT, TB-SCS-GNO-ENG-00585.docx, TB-SCS-GNO-ENG-00585.docx, null",
        "REFUSE_HEARING_RECORDING_REQUEST, TB-SCS-GNO-ENG-00067.docx, TB-SCS-GNO-ENG-00089.docx, TB-SCS-GNO-ENG-00067.docx"
    })
    public void shouldGiveCorrectDocmosisIdForDirectionIssued(DirectionType directionType,
                                                              String configAppellantOrAppointee,
                                                              String configRep,
                                                              @Nullable String configJointParty) {
        NotificationWrapper wrapper = new CcdNotificationWrapper(NotificationSscsCaseDataWrapper.builder()
                .newSscsCaseData(SscsCaseData.builder()
                        .directionTypeDl(new DynamicList(directionType.toString()))
                        .appeal(Appeal.builder()
                                .hearingType(HearingType.ONLINE.getValue())
                                .build())
                        .build())
                .notificationEventType(DIRECTION_ISSUED)
                .build());

        Personalisation personalisation = new Personalisation();
        ReflectionTestUtils.setField(personalisation, "config", notificationConfig);

        Template templateAppellant = personalisation.getTemplate(wrapper, PIP, APPELLANT);

        assertThat(templateAppellant.getEmailTemplateId()).isNull();
        assertThat(templateAppellant.getSmsTemplateId()).isEmpty();
        assertThat(templateAppellant.getLetterTemplateId()).isNull();
        assertThat(templateAppellant.getDocmosisTemplateId()).isEqualTo(configAppellantOrAppointee);

        Template templateAppointee = personalisation.getTemplate(wrapper, PIP, APPOINTEE);

        assertThat(templateAppointee.getDocmosisTemplateId()).isEqualTo(configAppellantOrAppointee);

        Template templateRep = personalisation.getTemplate(wrapper, PIP, REPRESENTATIVE);

        assertThat(templateRep.getDocmosisTemplateId()).isEqualTo(configRep);

        Template templateJointParty = personalisation.getTemplate(wrapper, PIP, JOINT_PARTY);

        assertThat(templateJointParty.getDocmosisTemplateId()).isEqualTo(configJointParty);
    }

    @Test
    @Parameters({
        "GRANT_EXTENSION, TB-SCS-GNO-WEL-00591.docx, TB-SCS-GNO-WEL-00591.docx, TB-SCS-GNO-WEL-00591.docx",
        "REFUSE_EXTENSION, TB-SCS-GNO-WEL-00592.docx, TB-SCS-GNO-WEL-00592.docx, TB-SCS-GNO-WEL-00592.docx",
        "APPEAL_TO_PROCEED, TB-SCS-GNO-WEL-00590.docx, TB-SCS-GNO-WEL-00590.docx, TB-SCS-GNO-WEL-00590.docx",
        "PROVIDE_INFORMATION, TB-SCS-GNO-WEL-00468.docx, TB-SCS-GNO-WEL-00472.docx, TB-SCS-GNO-WEL-00468.docx",
        "GRANT_REINSTATEMENT, TB-SCS-GNO-WEL-00586.docx, TB-SCS-GNO-WEL-00586.docx, null",
        "REFUSE_REINSTATEMENT, TB-SCS-GNO-WEL-00587.docx, TB-SCS-GNO-WEL-00587.docx, null",
        "REFUSE_HEARING_RECORDING_REQUEST, TB-SCS-GNO-WEL-00468.docx, TB-SCS-GNO-WEL-00472.docx, TB-SCS-GNO-WEL-00468.docx"
    })
    public void shouldGiveCorrectDocmosisIdForDirectionIssuedWelsh(DirectionType directionType,
                                                                   String configAppellantOrAppointee,
                                                                   String configRep,
                                                                   @Nullable String configJointParty) {

        NotificationWrapper wrapper = new CcdNotificationWrapper(NotificationSscsCaseDataWrapper.builder()
                .newSscsCaseData(SscsCaseData.builder()
                        .languagePreferenceWelsh(YES.getValue())
                        .directionTypeDl(new DynamicList(directionType.toString()))
                        .appeal(Appeal.builder()
                                .hearingType(HearingType.ONLINE.getValue())
                                .build())
                        .build())
                .notificationEventType(DIRECTION_ISSUED_WELSH)
                .build());

        Personalisation personalisation = new Personalisation();
        ReflectionTestUtils.setField(personalisation, "config", notificationConfig);

        Template templateAppellant = personalisation.getTemplate(wrapper, PIP, APPELLANT);

        assertThat(templateAppellant.getEmailTemplateId()).isNull();
        assertThat(templateAppellant.getSmsTemplateId()).isEmpty();
        assertThat(templateAppellant.getLetterTemplateId()).isNull();
        assertThat(templateAppellant.getDocmosisTemplateId()).isEqualTo(configAppellantOrAppointee);

        Template templateAppointee = personalisation.getTemplate(wrapper, PIP, APPOINTEE);

        assertThat(templateAppointee.getDocmosisTemplateId()).isEqualTo(configAppellantOrAppointee);

        Template templateRep = personalisation.getTemplate(wrapper, PIP, REPRESENTATIVE);

        assertThat(templateRep.getDocmosisTemplateId()).isEqualTo(configRep);

        Template templateJointParty = personalisation.getTemplate(wrapper, PIP, JOINT_PARTY);

        assertThat(templateJointParty.getDocmosisTemplateId()).isEqualTo(configJointParty);
    }

}
