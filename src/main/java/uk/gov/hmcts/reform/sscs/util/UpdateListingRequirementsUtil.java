package uk.gov.hmcts.reform.sscs.util;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.apache.tika.utils.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingInterpreter;
import uk.gov.hmcts.reform.sscs.ccd.domain.OverrideFields;
import uk.gov.hmcts.reform.sscs.client.JudicialRefDataApi;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.model.client.JudicialMemberAppointments;
import uk.gov.hmcts.reform.sscs.model.client.JudicialRefDataUsersRequest;
import uk.gov.hmcts.reform.sscs.model.client.JudicialUser;
import uk.gov.hmcts.reform.sscs.reference.data.model.JudicialMemberType;
import uk.gov.hmcts.reform.sscs.reference.data.model.Language;
import uk.gov.hmcts.reform.sscs.reference.data.service.SignLanguagesService;
import uk.gov.hmcts.reform.sscs.reference.data.service.VerbalLanguagesService;

@Component
@RequiredArgsConstructor
public class UpdateListingRequirementsUtil {

    private final IdamService idamService;
    private final SignLanguagesService signLanguagesService;
    private final VerbalLanguagesService verbalLanguagesService;
    private final JudicialRefDataApi judicialRefData;

    public static final String SERVICE_NAME = "sscs";

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

    @NotNull
    private List<DynamicListItem> generateInterpreterLanguage() {
        List<Language> signLanguages = signLanguagesService.getSignLanguages();
        List<Language> verbalLanguages = verbalLanguagesService.getVerbalLanguages();

        return Stream.concat(signLanguages.stream(), verbalLanguages.stream())
            .map(this::getLanguageDynamicListItem)
            .sorted(Comparator.comparing(DynamicListItem::getLabel))
            .collect(Collectors.toList());
    }

    @NotNull
    private DynamicListItem getLanguageDynamicListItem(Language language) {
        String reference = language.getReference();
        String name = language.getNameEn();

        if (nonNull(language.getDialectReference())) {
            reference = String.format("%s-%s", language.getReference(), language.getDialectReference());
            name = language.getDialectEn();
        }
        return new DynamicListItem(reference, name);
    }

    private List<DynamicListItem> generateReservedMembers() {
        JudicialRefDataUsersRequest request = JudicialRefDataUsersRequest.builder()
            .ccdServiceName(SERVICE_NAME)
            .build();

        List<JudicialUser> judicialUsers = judicialRefData.getJudicialUsers(
            idamService.getIdamTokens().getIdamOauth2Token(),
            idamService.getIdamTokens().getServiceAuthorization(),
            request);

        return Optional.ofNullable(judicialUsers)
            .orElse(Collections.emptyList())
            .stream()
            .filter(Objects::nonNull)
            .filter(judicialUser -> isNotEmpty(judicialUser.getAppointments()))
            .filter(judicialUser -> judicialUser.getAppointments().stream()
                .map(JudicialMemberAppointments::getAppointment)
                .anyMatch(this::isValidJudicialMemberType))
            .map(this::getJudicialMemberListItem)
            .sorted(Comparator.comparing(DynamicListItem::getLabel))
            .collect(Collectors.toList());
    }

    private boolean isValidJudicialMemberType(String appointment) {
        if (appointment == null) {
            return false;
        }

        JudicialMemberType type = getJudicialMemberType(appointment);
        return JudicialMemberType.TRIBUNAL_PRESIDENT == type
            || JudicialMemberType.TRIBUNAL_JUDGE == type
            || JudicialMemberType.REGIONAL_TRIBUNAL_JUDGE == type;
    }

    private static JudicialMemberType getJudicialMemberType(String appointment) {
        return Arrays.stream(JudicialMemberType.values())
            .filter(x -> x.getDescriptionEn().equals(appointment))
            .findFirst()
            .orElse(null);
    }

    @NotNull
    private DynamicListItem getJudicialMemberListItem(JudicialUser judicialUser) {
        String referenceCodes = String.format("%s|%s", judicialUser.getPersonalCode(), extractHmcReferenceCode(judicialUser));

        String name = isNotBlank(judicialUser.getPostNominals())
            ? String.format("%s %s", judicialUser.getFullName(), judicialUser.getPostNominals())
            : judicialUser.getFullName();
        return new DynamicListItem(referenceCodes, name);
    }

    private String extractHmcReferenceCode(JudicialUser judicialUser) {
        if (judicialUser.getAppointments() != null) {
            JudicialMemberType judicialMemberType = judicialUser.getAppointments().stream()
                .map(JudicialMemberAppointments::getAppointment)
                .map(UpdateListingRequirementsUtil::getJudicialMemberType)
                .findFirst()
                .orElse(null);
            if (judicialMemberType != null) {
                return judicialMemberType.getHmcReference();
            }

        }
        return StringUtils.EMPTY;
    }
}
