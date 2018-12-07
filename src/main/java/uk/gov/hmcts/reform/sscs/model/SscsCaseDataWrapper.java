package uk.gov.hmcts.reform.sscs.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.transform.deserialize.SscsCaseDataWrapperDeserializer;

@Data
@Builder(toBuilder = true)
@JsonDeserialize(using = SscsCaseDataWrapperDeserializer.class)
public class SscsCaseDataWrapper {

    private SscsCaseData newSscsCaseData;
    private SscsCaseData oldSscsCaseData;
    private NotificationEventType notificationEventType;

}
