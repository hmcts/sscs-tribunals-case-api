package uk.gov.hmcts.reform.sscs.util;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingInterpreter;
import uk.gov.hmcts.reform.sscs.ccd.domain.OverrideFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.ReservedToMember;
import uk.gov.hmcts.reform.sscs.client.JudicialRefDataApi;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.model.client.JudicialMemberAppointments;
import uk.gov.hmcts.reform.sscs.model.client.JudicialRefDataUsersRequest;
import uk.gov.hmcts.reform.sscs.model.client.JudicialUser;
import uk.gov.hmcts.reform.sscs.reference.data.model.Language;
import uk.gov.hmcts.reform.sscs.reference.data.service.SignLanguagesService;
import uk.gov.hmcts.reform.sscs.reference.data.service.VerbalLanguagesService;

@RunWith(MockitoJUnitRunner.class)
public class UpdateListingRequirementsUtilTest {

    private static final String IDAM_OAUTH2_TOKEN = "TestOauthToken";
    private static final String SERVICE_AUTHORIZATION = "TestServiceAuthorization";

    @Mock
    private IdamService idamService;
    @Mock
    private SignLanguagesService signLanguagesService;
    @Mock
    private VerbalLanguagesService verbalLanguagesService;
    @Mock
    private JudicialRefDataApi judicialRefData;
    @InjectMocks
    private UpdateListingRequirementsUtil updateListingRequirementsUtil;
    private List<JudicialUser> response;


    @Before
    public void setup() {
        response = newArrayList(JudicialUser.builder()
            .personalCode("1234")
            .fullName("Test Person1")
            .postNominals("Judge")
            .appointments(List.of(JudicialMemberAppointments.builder()
                .appointment("Tribunal Judge")
                .build()))
            .build());
    }

    @Test
    public void generateInterpreterLanguageFields() {
        Language signLanguage = new Language("sign-mkn", "Makaton", null, null, null);
        given(signLanguagesService.getSignLanguages()).willReturn(List.of(signLanguage));

        Language verbalLanguage = new Language("fre", "French", null, null, null);
        given(verbalLanguagesService.getVerbalLanguages()).willReturn(List.of(verbalLanguage));

        OverrideFields overrideFields = OverrideFields.builder()
            .appellantInterpreter(HearingInterpreter.builder()
                .interpreterLanguage(new DynamicList(null,null))
                .build())
            .build();

        updateListingRequirementsUtil.generateInterpreterLanguageFields(overrideFields);

        List<DynamicListItem> result = overrideFields.getAppellantInterpreter().getInterpreterLanguage().getListItems();

        assertThat(result)
            .hasSize(2)
            .extracting("code","label")
            .containsExactlyInAnyOrder(tuple("sign-mkn","Makaton"), tuple("fre","French"));
    }

    @Test
    public void generateInterpreterLanguageFieldsInterpreterLanguageNull() {
        Language signLanguage = new Language("sign-mkn", "Makaton", null, null, null);
        given(signLanguagesService.getSignLanguages()).willReturn(List.of(signLanguage));

        Language verbalLanguage = new Language("fre", "French", null, null, null);
        given(verbalLanguagesService.getVerbalLanguages()).willReturn(List.of(verbalLanguage));

        OverrideFields overrideFields = OverrideFields.builder()
            .appellantInterpreter(HearingInterpreter.builder()
                .interpreterLanguage(null)
                .build())
            .build();

        updateListingRequirementsUtil.generateInterpreterLanguageFields(overrideFields);

        List<DynamicListItem> result = overrideFields.getAppellantInterpreter().getInterpreterLanguage().getListItems();

        assertThat(result)
            .hasSize(2)
            .extracting("code","label")
            .containsExactlyInAnyOrder(tuple("sign-mkn","Makaton"), tuple("fre","French"));
    }

    @Test
    public void generateInterpreterLanguageFieldsAppellantInterpreterNull() {
        Language signLanguage = new Language("sign-mkn", "Makaton", null, null, null);
        given(signLanguagesService.getSignLanguages()).willReturn(List.of(signLanguage));

        Language verbalLanguage = new Language("fre", "French", null, null, null);
        given(verbalLanguagesService.getVerbalLanguages()).willReturn(List.of(verbalLanguage));

        OverrideFields overrideFields = OverrideFields.builder()
            .appellantInterpreter(null)
            .build();

        updateListingRequirementsUtil.generateInterpreterLanguageFields(overrideFields);

        List<DynamicListItem> result = overrideFields.getAppellantInterpreter().getInterpreterLanguage().getListItems();

        assertThat(result)
            .hasSize(2)
            .extracting("code","label")
            .containsExactlyInAnyOrder(tuple("sign-mkn","Makaton"), tuple("fre","French"));
    }

    @Test
    public void generateReservedToJudgeFields() {
        given(idamService.getIdamTokens()).willReturn(IdamTokens.builder()
            .idamOauth2Token(IDAM_OAUTH2_TOKEN)
            .serviceAuthorization(SERVICE_AUTHORIZATION)
            .build());

        given(judicialRefData.getJudicialUsers(
            eq(IDAM_OAUTH2_TOKEN),
            eq(SERVICE_AUTHORIZATION),
            any(JudicialRefDataUsersRequest.class)))
            .willReturn(response);

        OverrideFields overrideFields = OverrideFields.builder()
            .reservedToJudge(ReservedToMember.builder()
                .reservedMember(new DynamicList(null,null))
                .build())
            .build();

        updateListingRequirementsUtil.generateReservedToJudgeFields(overrideFields);

        List<DynamicListItem> result = overrideFields.getReservedToJudge().getReservedMember().getListItems();

        assertThat(result)
            .hasSize(1)
            .extracting("code","label")
            .containsExactlyInAnyOrder(tuple("1234|84","Test Person1 Judge"));
    }

    @Test
    public void generateReservedToJudgeFieldsReservedToMemberNull() {
        given(idamService.getIdamTokens()).willReturn(IdamTokens.builder()
            .idamOauth2Token(IDAM_OAUTH2_TOKEN)
            .serviceAuthorization(SERVICE_AUTHORIZATION)
            .build());

        given(judicialRefData.getJudicialUsers(
            eq(IDAM_OAUTH2_TOKEN),
            eq(SERVICE_AUTHORIZATION),
            any(JudicialRefDataUsersRequest.class)))
            .willReturn(response);

        OverrideFields overrideFields = OverrideFields.builder()
            .reservedToJudge(ReservedToMember.builder()
                .reservedMember(null)
                .build())
            .build();

        updateListingRequirementsUtil.generateReservedToJudgeFields(overrideFields);

        List<DynamicListItem> result = overrideFields.getReservedToJudge().getReservedMember().getListItems();

        assertThat(result)
            .hasSize(1)
            .extracting("code","label")
            .containsExactlyInAnyOrder(tuple("1234|84","Test Person1 Judge"));
    }

    @Test
    public void generateReservedToJudgeFieldsAllAppointments() {
        given(idamService.getIdamTokens()).willReturn(IdamTokens.builder()
            .idamOauth2Token(IDAM_OAUTH2_TOKEN)
            .serviceAuthorization(SERVICE_AUTHORIZATION)
            .build());

        response.addAll(List.of(JudicialUser.builder()
            .personalCode("abcd")
            .fullName("Test Person2")
            .postNominals("Judge")
            .appointments(List.of(JudicialMemberAppointments.builder()
                .appointment("President of Tribunal")
                .build()))
            .build(),
            JudicialUser.builder()
                .personalCode("efgh")
                .fullName("Test Person3")
                .postNominals("Judge")
                .appointments(List.of(JudicialMemberAppointments.builder()
                    .appointment("Regional Tribunal Judge")
                    .build()))
                .build()));

        given(judicialRefData.getJudicialUsers(
            eq(IDAM_OAUTH2_TOKEN),
            eq(SERVICE_AUTHORIZATION),
            any(JudicialRefDataUsersRequest.class)))
            .willReturn(response);

        OverrideFields overrideFields = OverrideFields.builder()
            .reservedToJudge(ReservedToMember.builder()
                .reservedMember(null)
                .build())
            .build();

        updateListingRequirementsUtil.generateReservedToJudgeFields(overrideFields);

        List<DynamicListItem> result = overrideFields.getReservedToJudge().getReservedMember().getListItems();

        assertThat(result)
            .hasSize(3)
            .extracting("code","label")
            .containsExactlyInAnyOrder(
                tuple("1234|84", "Test Person1 Judge"),
                tuple("abcd|65", "Test Person2 Judge"),
                tuple("efgh|74", "Test Person3 Judge"));
    }
}
