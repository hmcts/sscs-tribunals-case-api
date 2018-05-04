package uk.gov.hmcts.sscs.model.ccd;

import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals;

import java.io.File;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.sscs.service.ccd.CaseDataUtils;

public class CaseDataContentToJsonTest {

    @Test
    public void givenACaseDataContent_ShouldBeTransformedToJson() throws Exception {
        // given
        CaseDataContent caseDataContent = getCaseDataContent();

        // should
        File caseDataContentFile = new File("src/test/resources/json/CaseDataContent.json");

        String expectedCaseDataContentJson = FileUtils.readFileToString(caseDataContentFile,
            StandardCharsets.UTF_8.name());

        assertJsonEquals(expectedCaseDataContentJson, caseDataContent);
    }

    private CaseDataContent getCaseDataContent() {
        return CaseDataContent.builder()
            .eventToken("user token")
            .event(Event.builder()
                .id("appealReceived_SYA")
                .summary("SSCS - appeal received event")
                .description("Created SSCS case with token")
                .build())
            .data(CaseDataUtils.buildCaseData())
            .build();
    }


}
