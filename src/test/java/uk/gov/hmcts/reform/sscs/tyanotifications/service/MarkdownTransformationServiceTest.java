package uk.gov.hmcts.reform.sscs.tyanotifications.service;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import uk.gov.hmcts.reform.sscs.notifications.gov.notify.service.MarkdownTransformationService;

public class MarkdownTransformationServiceTest {

    private MarkdownTransformationService markdownTransformationService = new MarkdownTransformationService();

    @Test
    public void toHtml() {
        String html = markdownTransformationService.toHtml("test");

        assertEquals("<p>test</p>\n", html);
    }
}
