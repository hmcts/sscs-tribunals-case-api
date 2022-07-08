package uk.gov.hmcts.reform.sscs.ccd.presubmit.updateListingRequirements;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingInterpreter;
import uk.gov.hmcts.reform.sscs.ccd.domain.JudgeType;
import uk.gov.hmcts.reform.sscs.ccd.domain.OverrideFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.ReservedToMember;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.client.JudicialRefDataApi;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.model.client.JudicialMemberAppointments;
import uk.gov.hmcts.reform.sscs.model.client.JudicialRefDataUsersRequest;
import uk.gov.hmcts.reform.sscs.model.client.JudicialRefDataUsersResponse;
import uk.gov.hmcts.reform.sscs.model.client.JudicialUser;
import uk.gov.hmcts.reform.sscs.reference.data.model.Language;
import uk.gov.hmcts.reform.sscs.reference.data.service.SignLanguagesService;
import uk.gov.hmcts.reform.sscs.reference.data.service.VerbalLanguagesService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.*;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Service
@Slf4j
@RequiredArgsConstructor
public class UpdateListingRequirementsHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final IdamService idamService;
    private final SignLanguagesService signLanguagesService;
    private final VerbalLanguagesService verbalLanguagesService;
    private final JudicialRefDataApi judicialRefData;

    public static final String SERVICE_NAME = "sscs";

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        final SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();

        OverrideFields overrideFields = sscsCaseData.getSchedulingAndListingFields().getOverrideFields();

        if(isNull(overrideFields)) {
            overrideFields = new OverrideFields();
            sscsCaseData.getSchedulingAndListingFields().setOverrideFields(overrideFields);
        }

        generateInterpreterLanguageFields(overrideFields);

        generateReservedToJudgeFields(overrideFields);

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
            && (callback.getEvent() == EventType.UPDATE_LISTING_REQUIREMENTS);

    }

    private void generateInterpreterLanguageFields(OverrideFields overrideFields) {
        if(isNull(overrideFields.getAppellantInterpreter())
            ||  isNull(overrideFields.getAppellantInterpreter().getInterpreterLanguage())) {
            overrideFields.setAppellantInterpreter(HearingInterpreter.builder()
                .interpreterLanguage(new DynamicList(null, null))
                .build());
        }

        List<DynamicListItem> interpreterLanguage = generateInterpreterLanguage();

        overrideFields.getAppellantInterpreter().getInterpreterLanguage()
            .setListItems(interpreterLanguage);
    }

    private void generateReservedToJudgeFields(OverrideFields overrideFields) {
        if(isNull(overrideFields.getReservedToJudge())
            || isNull(overrideFields.getReservedToJudge().getReservedMember())) {
            overrideFields.setReservedToJudge(ReservedToMember.builder()
                .reservedMember(new DynamicList(null, null))
                .build());
        }

        List<DynamicListItem> reservedMembers = generateReservedMembers();

        overrideFields.getReservedToJudge().getReservedMember().setListItems(reservedMembers);
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();

        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(sscsCaseData);

        return response;
    }

    @NotNull
    private List<DynamicListItem> generateInterpreterLanguage() {

        return getLanguages().stream()
            .map(this::getLanguageDynamicListItem)
            .collect(Collectors.toList());
    }

    @NotNull
    private DynamicListItem getLanguageDynamicListItem(Language language) {
        String reference = language.getReference();
        String name = language.getNameEn();

        if(nonNull(language.getDialectReference())) {
            reference = String.format("%s-%s", language.getReference(), language.getDialectReference());
            name = language.getDialectEn();
        }
        return new DynamicListItem(reference, name);
    }

    private List<Language> getLanguages() {
        List<Language> signLanguages = signLanguagesService.getSignLanguages();
        List<Language> verbalLanguages = verbalLanguagesService.getVerbalLanguages();

        return Stream.concat(signLanguages.stream(), verbalLanguages.stream())
            .collect(Collectors.toList());
    }

    private List<DynamicListItem> generateReservedMembers() {
        JudicialRefDataUsersRequest request = JudicialRefDataUsersRequest.builder()
            .ccdServiceName(SERVICE_NAME)
            .build();

        JudicialRefDataUsersResponse refDataResponse = judicialRefData.getJudicialUsers(
            idamService.getIdamTokens().getIdamOauth2Token(),
            idamService.getIdamTokens().getServiceAuthorization(),
            request);

        return Optional.ofNullable(refDataResponse.getJudicialUsers())
            .orElse(Collections.emptyList())
            .stream()
            .filter(judicialUser -> judicialUser.getAppointments().stream().map(JudicialMemberAppointments::getAppointment).anyMatch(this::isValidJudgeType))
            .map(this::getJudicialMemberListItem)
            .collect(Collectors.toList());
    }

    @NotNull
    private DynamicListItem getJudicialMemberListItem(JudicialUser judicialUser) {
        String name = isNotBlank(judicialUser.getPostNominals())
            ? String.format("%s %s", judicialUser.getFullName(), judicialUser.getPostNominals())
            : judicialUser.getFullName();
        return new DynamicListItem(judicialUser.getPersonalCode(), name);
    }

    private boolean isValidJudgeType(String appointment) {
        JudgeType type = getJudgeType(appointment);
        if (JudgeType.PRESIDENT_OF_TRIBUNAL == type
            || JudgeType.TRIBUNAL_JUDGE == type
            || JudgeType.REGIONAL_TRIBUNAL_JUDGE == type) {
            return true;
        }
        return false;
    }

    private JudgeType getJudgeType(String appointment) {
        return Arrays.stream(JudgeType.values())
            .filter(x -> x.getEn().equals(appointment))
            .findFirst()
            .orElse(null);
    }

    private void extractJudges(JudicialRefDataUsersResponse refDataResponse) {

    }

}
