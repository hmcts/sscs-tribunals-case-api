package uk.gov.hmcts.reform.sscs.notifications.gov.notify.service.docmosis;

import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.IBCA_URL;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderUtility.getPostponementRequestStatus;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.ADDRESS_NAME;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.PersonalisationMappingConstants.POSTPONEMENT_REQUEST;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.notify.NotificationEventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.personalisation.Personalisation.translateToWelshDate;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.service.LetterUtils.LetterType.DOCMOSIS;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.service.LetterUtils.addBlankPageAtTheEndIfOddPage;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.service.LetterUtils.buildBundledLetter;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.service.LetterUtils.getAddressPlaceholders;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.service.LetterUtils.getAddressToUseForLetter;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.service.LetterUtils.getNameToUseForLetter;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.LanguagePreference;
import uk.gov.hmcts.reform.sscs.service.conversion.LocalDateToWelshStringConverter;
import uk.gov.hmcts.reform.sscs.thirdparty.pdfservice.DocmosisPdfService;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.DocmosisTemplatesConfig;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.properties.EvidenceProperties;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.SubscriptionWithType;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.docmosis.PdfCoverSheet;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.notify.Notification;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.exception.NotificationClientRuntimeException;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.factory.NotificationWrapper;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.service.LetterUtils;

@Service
@Slf4j
public class PdfLetterService {
    private static final String SSCS_URL_LITERAL = "sscs_url";
    private static final String SSCS_URL = "www.gov.uk/appeal-benefit-decision";
    protected static final String GENERATED_DATE_LITERAL = "generated_date";
    protected static final String WELSH_GENERATED_DATE_LITERAL = "welsh_generated_date";
    private static final List<NotificationEventType> REQUIRES_TWO_COVERSHEET =
        Collections.singletonList(APPEAL_RECEIVED);

    private final DocmosisPdfService docmosisPdfService;
    private final DocmosisTemplatesConfig docmosisTemplatesConfig;
    private final EvidenceProperties evidenceProperties;

    @Autowired
    public PdfLetterService(DocmosisPdfService docmosisPdfService, DocmosisTemplatesConfig docmosisTemplatesConfig, EvidenceProperties evidenceProperties) {
        this.docmosisPdfService = docmosisPdfService;
        this.docmosisTemplatesConfig = docmosisTemplatesConfig;
        this.evidenceProperties = evidenceProperties;
    }

    public byte[] buildCoversheet(NotificationWrapper wrapper, SubscriptionWithType subscriptionWithType) {
        try {
            byte[] coversheet = generateCoversheet(wrapper, subscriptionWithType);
            if (REQUIRES_TWO_COVERSHEET.contains(wrapper.getNotificationType())
                && ArrayUtils.isNotEmpty(coversheet)) {
                return buildBundledLetter(addBlankPageAtTheEndIfOddPage(coversheet), coversheet);
            }
            return coversheet;
        } catch (IOException e) {
            String message = String.format("Cannot '%s' generate evidence coversheet to %s.",
                wrapper.getNotificationType().getId(),
                subscriptionWithType.getSubscriptionType().name());
            log.error(message, e);
            throw new NotificationClientRuntimeException(message);
        }
    }

    private byte[] generateCoversheet(NotificationWrapper wrapper, SubscriptionWithType subscriptionWithType) {
        Address addressToUse = getAddressToUseForLetter(wrapper, subscriptionWithType);
        var lines = LetterUtils.lines(addressToUse);
        for (int i = lines.size(); i < 5; i++) {
            lines.add("");
        }

        String name = getNameToUseForLetter(wrapper, subscriptionWithType);
        int lineNum = 0;

        PdfCoverSheet pdfCoverSheet = new PdfCoverSheet(
            wrapper.getCaseId(),
            name,
            lines.get(lineNum++),
            lines.get(lineNum++),
            lines.get(lineNum++),
            lines.get(lineNum++),
            lines.get(lineNum),
            evidenceProperties.getAddress().getLine2(wrapper.getNewSscsCaseData()),
            evidenceProperties.getAddress().getLine3(wrapper.getNewSscsCaseData()),
            evidenceProperties.getAddress().getTown(),
            evidenceProperties.getAddress().getPostcode(wrapper.getNewSscsCaseData()),
            docmosisTemplatesConfig.getHmctsImgVal(),
            docmosisTemplatesConfig.getHmctsWelshImgVal());

        LanguagePreference languagePreference =
            wrapper.getSscsCaseDataWrapper().getNewSscsCaseData().getLanguagePreference();

        String templatePath = docmosisTemplatesConfig.getCoversheets().get(languagePreference)
            .get(wrapper.getNotificationType().getId());
        if (StringUtils.isBlank(templatePath)) {
            log.info("There is no template for notificationType " + wrapper.getNotificationType().getId());
            return null;
        }

        return docmosisPdfService.createPdf(pdfCoverSheet, templatePath);
    }


    public byte[] generateLetter(NotificationWrapper wrapper, Notification notification,
                                 SubscriptionWithType subscriptionWithType) {
        if (StringUtils.isNotBlank(notification.getDocmosisLetterTemplate())) {

            Map<String, Object> placeholders = new HashMap<>(notification.getPlaceholders());
            placeholders.put(SSCS_URL_LITERAL, wrapper.getNewSscsCaseData().isIbcCase() ? IBCA_URL : SSCS_URL);
            placeholders.put(GENERATED_DATE_LITERAL, LocalDateTime.now().toLocalDate().toString());

            translateToWelshDate(LocalDateTime.now().toLocalDate(), wrapper.getNewSscsCaseData(), value -> placeholders.put(WELSH_GENERATED_DATE_LITERAL, value));
            placeholders.put(ADDRESS_NAME, truncateAddressLine(getNameToUseForLetter(wrapper, subscriptionWithType)));

            Address addressToUse = getAddressToUseForLetter(wrapper, subscriptionWithType);

            placeholders.putAll(getAddressPlaceholders(addressToUse, true, DOCMOSIS));

            placeholders.put(docmosisTemplatesConfig.getHmctsImgKey(), docmosisTemplatesConfig.getHmctsImgVal());
            placeholders.put(docmosisTemplatesConfig.getHmctsWelshImgKey(), docmosisTemplatesConfig.getHmctsWelshImgVal());

            if (wrapper.getNewSscsCaseData().isLanguagePreferenceWelsh()) {
                placeholders.put(docmosisTemplatesConfig.getHmctsWelshImgKey(),
                    docmosisTemplatesConfig.getHmctsWelshImgVal());
                placeholders.put(WELSH_GENERATED_DATE_LITERAL, LocalDateToWelshStringConverter.convert(LocalDate.now()));
            }

            placeholders.put(POSTPONEMENT_REQUEST,  getPostponementRequestStatus(wrapper.getNewSscsCaseData()));

            return docmosisPdfService.createPdfFromMap(placeholders, notification.getDocmosisLetterTemplate());
        }
        return new byte[0];
    }

    private static String defaultToEmptyStringIfNull(String value) {
        return (value == null) ? StringUtils.EMPTY : value;
    }

    private static String truncateAddressLine(String addressLine) {
        return addressLine != null && addressLine.length() > 45 ? addressLine.substring(0, 45) : addressLine;
    }

}
