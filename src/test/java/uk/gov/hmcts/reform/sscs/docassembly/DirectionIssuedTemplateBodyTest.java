package uk.gov.hmcts.reform.sscs.docassembly;

import static org.junit.Assert.assertEquals;

import java.time.LocalDate;
import org.junit.Test;

public class DirectionIssuedTemplateBodyTest {

    @Test
    public void canBuildATemplateBody() {
        String now = LocalDate.now().toString();
        DirectionIssuedTemplateBody body = DirectionIssuedTemplateBody.builder()
                .appellantFullName("Appellant")
                .userRole("userRole")
                .userName("userName")
                .noticeBody("noticeBody")
                .nino("nino")
                .caseId("123")
                .noticeType("noticeType")
                .dateAdded(now)
                .generatedDate(now)
                .build();

        assertEquals("Appellant", body.getAppellantFullName());
        assertEquals("userRole", body.getUserRole());
        assertEquals("userName", body.getUserName());
        assertEquals("nino", body.getNino());
        assertEquals("123", body.getCaseId());
        assertEquals("noticeType", body.getNoticeType());
        assertEquals(now, body.getDateAdded());
        assertEquals(now, body.getGeneratedDate());

    }
}
