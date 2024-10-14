package uk.gov.hmcts.reform.sscs.service.holder;

import static uk.gov.hmcts.reform.sscs.utility.JsonDataReader.OBJECT_MAPPER;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.Getter;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.reference.data.service.HearingDurationsService;
import uk.gov.hmcts.reform.sscs.reference.data.service.SessionCategoryMapService;
import uk.gov.hmcts.reform.sscs.reference.data.service.SignLanguagesService;
import uk.gov.hmcts.reform.sscs.reference.data.service.VerbalLanguagesService;
import uk.gov.hmcts.reform.sscs.service.RegionalProcessingCenterService;
import uk.gov.hmcts.reform.sscs.service.VenueService;


@Data
@Component
public class ReferenceDataServiceHolder {

    @Autowired
    private final HearingDurationsService hearingDurations;

    @Autowired
    private final SessionCategoryMapService sessionCategoryMaps;

    @Autowired
    private final VerbalLanguagesService verbalLanguages;

    @Autowired
    private final SignLanguagesService signLanguages;

    @Autowired
    private final RegionalProcessingCenterService regionalProcessingCenterService;

    @Autowired
    private final VenueService venueService;

    @Value("${exui.url}")
    private String exUiUrl;

    @Value("${sscs.serviceCode}")
    private String sscsServiceCode;

    @Value("${flags.adjournment.enabled}")
    private boolean adjournmentFlagEnabled; // TODO SSCS-10951

    @Value("${flags.postHearings.enabled}")
    private boolean isPostHearingsEnabled;

    @Getter(lazy = true)
    private final Map<String, List<String>> multipleHearingLocations = prepareMultipleHearingLocations();

    @SneakyThrows
    private Map<String, List<String>> prepareMultipleHearingLocations() {
        return ImmutableMap.copyOf(OBJECT_MAPPER.readValue(new ClassPathResource("multipleHearingLocations.json")
                        .getInputStream(),
                new TypeReference<Map<String, List<String>>>() {
                }));
    }
}
