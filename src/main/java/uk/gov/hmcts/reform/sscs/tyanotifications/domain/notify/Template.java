package uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Template {
    private final String emailTemplateId;
    private final List<String> smsTemplateId;
    private final String smsSenderTemplateId;
    private final String letterTemplateId;
    private final String docmosisTemplateId;
}
