package uk.gov.hmcts.reform.sscs.ccd.presubmit.updatelistingrequirements;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingInterpreter;
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
import uk.gov.hmcts.reform.sscs.reference.data.model.JudicialMemberType;
import uk.gov.hmcts.reform.sscs.reference.data.model.Language;
import uk.gov.hmcts.reform.sscs.reference.data.service.SignLanguagesService;
import uk.gov.hmcts.reform.sscs.reference.data.service.VerbalLanguagesService;

@Service
@Slf4j
@RequiredArgsConstructor
public class UpdateListingRequirementsHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Value("${feature.snl.enabled}")
    private boolean isScheduleListingEnabled;

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

        if (isScheduleListingEnabled) {
            OverrideFields overrideFields = sscsCaseData.getSchedulingAndListingFields().getOverrideFields();

            if (isNull(overrideFields)) {
                overrideFields = new OverrideFields();
                sscsCaseData.getSchedulingAndListingFields().setOverrideFields(overrideFields);
            }

            generateInterpreterLanguageFields(overrideFields);

            generateReservedToJudgeFields(overrideFields);
        }

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
            && (callback.getEvent() == EventType.UPDATE_LISTING_REQUIREMENTS);

    }

    public void generateInterpreterLanguageFields(OverrideFields overrideFields) {
        if (isNull(overrideFields.getAppellantInterpreter())
            || isNull(overrideFields.getAppellantInterpreter().getInterpreterLanguage())) {
            overrideFields.setAppellantInterpreter(HearingInterpreter.builder()
                .interpreterLanguage(new DynamicList(null, null))
                .build());
        }

        List<DynamicListItem> interpreterLanguage = generateInterpreterLanguage();

        overrideFields.getAppellantInterpreter().getInterpreterLanguage()
            .setListItems(interpreterLanguage);
    }

    public void generateReservedToJudgeFields(OverrideFields overrideFields) {
        if (isNull(overrideFields.getReservedToJudge())
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
    public List<DynamicListItem> generateInterpreterLanguage() {

        return getLanguages().stream()
            .map(this::getLanguageDynamicListItem)
            .collect(Collectors.toList());
    }

    @NotNull
    public DynamicListItem getLanguageDynamicListItem(Language language) {
        String reference = language.getReference();
        String name = language.getNameEn();

        if (nonNull(language.getDialectReference())) {
            reference = String.format("%s-%s", language.getReference(), language.getDialectReference());
            name = language.getDialectEn();
        }
        return new DynamicListItem(reference, name);
    }

    public List<Language> getLanguages() {
        List<Language> signLanguages = signLanguagesService.getSignLanguages();
        List<Language> verbalLanguages = verbalLanguagesService.getVerbalLanguages();

        return Stream.concat(signLanguages.stream(), verbalLanguages.stream())
            .collect(Collectors.toList());
    }

    public List<DynamicListItem> generateReservedMembers() {
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
            .filter(Objects::nonNull)
            .filter(judicialUser -> isNotEmpty(judicialUser.getAppointments()))
            .filter(judicialUser -> judicialUser.getAppointments().stream().map(JudicialMemberAppointments::getAppointment).anyMatch(this::isValidJudicialMemberType))
            .map(this::getJudicialMemberListItem)
            .collect(Collectors.toList());
    }

    @NotNull
    public DynamicListItem getJudicialMemberListItem(JudicialUser judicialUser) {
        String referenceCodes = String.format("%s|%s", judicialUser.getPersonalCode(), getHmcReferenceCode(judicialUser));

        String name = isNotBlank(judicialUser.getPostNominals())
            ? String.format("%s %s", judicialUser.getFullName(), judicialUser.getPostNominals())
            : judicialUser.getFullName();
        return new DynamicListItem(referenceCodes, name);
    }

    private String getHmcReferenceCode(JudicialUser judicialUser) {
        return judicialUser.getAppointments().stream()
           .map(JudicialMemberAppointments::getAppointment)
           .map(this::getJudicialMemberType)
           .findFirst()
           .orElse(null)
           .getHmcReference();

    }

    public boolean isValidJudicialMemberType(String appointment) {
        JudicialMemberType type = getJudicialMemberType(appointment);
        return JudicialMemberType.TRIBUNAL_PRESIDENT == type
            || JudicialMemberType.TRIBUNAL_JUDGE == type
            || JudicialMemberType.REGIONAL_TRIBUNAL_JUDGE == type;
    }

    public JudicialMemberType getJudicialMemberType(String appointment) {
        return Arrays.stream(JudicialMemberType.values())
            .filter(x -> x.getDescriptionEn().equals(appointment))
            .findFirst()
            .orElse(null);
    }
}
