package uk.gov.hmcts.reform.sscs.util;

import static java.time.LocalDateTime.now;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static uk.gov.hmcts.reform.sscs.service.pdf.data.UploadedEvidence.pdf;

import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.domain.wrapper.*;
import uk.gov.hmcts.reform.sscs.domain.wrapper.pdf.PdfAppealDetails;
import uk.gov.hmcts.reform.sscs.domain.wrapper.pdf.PdfQuestion;
import uk.gov.hmcts.reform.sscs.domain.wrapper.pdf.PdfQuestionRound;
import uk.gov.hmcts.reform.sscs.domain.wrapper.pdf.PdfSummary;
import uk.gov.hmcts.reform.sscs.service.pdf.CohEventActionContext;

public class DataFixtures {
    private DataFixtures() {

    }

    public static OnlineHearing someOnlineHearing() {
        return someOnlineHearing(123456789L);
    }

    public static OnlineHearing someOnlineHearing(long caseId) {
        AddressDetails addressDetails = new AddressDetails("line1", "line2", "town", "country", "postcode");
        AppellantDetails appellantDetails = new AppellantDetails(addressDetails, "email", "phone", "mobile");
        return new OnlineHearing("someOnlineHearingId", "someAppellantName", "someCaseReference", caseId, null, new FinalDecision("final decision"), true, appellantDetails, someAppealDetails());
    }

    public static AppealDetails someAppealDetails() {
        return new AppealDetails("12-12-2019", "11-11-2019", "PIP");
    }

    public static OnlineHearing someOnlineHearingWithDecision() {
        AddressDetails addressDetails = new AddressDetails("line1", "line2", "town", "country", "postcode");
        AppellantDetails appellantDetails = new AppellantDetails(addressDetails, "email", "phone", "mobile");
        Decision decision = new Decision("decision_issued", now().format(ISO_LOCAL_DATE_TIME), null, null, "startDate", "endDate", null, "decisionReason", null);
        return new OnlineHearing("someOnlineHearingId", "someAppellantName", "someCaseReference", 123456789L, decision, new FinalDecision("final decision"), true, appellantDetails, someAppealDetails());
    }

    public static OnlineHearing someOnlineHearingWithDecisionAndAppellentReply() {
        AddressDetails addressDetails = new AddressDetails("line1", "line2", "town", "country", "postcode");
        AppellantDetails appellantDetails = new AppellantDetails(addressDetails, "email", "phone", "mobile");
        Decision decision = new Decision("decision_issued", now().format(ISO_LOCAL_DATE_TIME), "decision_accepted", now().format(ISO_LOCAL_DATE_TIME), "startDate", "endDate", null, "decisionReason", null);
        return new OnlineHearing("someOnlineHearingId", "someAppellantName", "someCaseReference", 123456789L, decision, new FinalDecision("final decision"), true, appellantDetails, someAppealDetails());
    }

    public static Evidence someEvidence() {
        return new Evidence("http://example.com/document/1", "someFilename.txt", "2018-10-24'T'12:11:21Z");
    }

    public static PdfSummary somePdfSummary() {
        return new PdfSummary(somePdfAppealDetails(),
                "relisting reason",
                singletonList(
                        new PdfQuestionRound(singletonList(
                                new PdfQuestion("title", "body", "answer", AnswerState.submitted, "issueDate", "submittedDate")
                        ))
                )
        );
    }

    public static CohEventActionContext someStorePdfResult() {
        return new CohEventActionContext(
                pdf(new byte[]{2, 4, 6, 0, 1}, "pdfName.pdf"),
                SscsCaseDetails.builder()
                        .data(SscsCaseData.builder()
                                .caseReference("caseReference")
                                .build())
                        .build()
        );
    }

    public static PdfAppealDetails somePdfAppealDetails() {
        return new PdfAppealDetails("someTitle", "someFirstName", "someSurname", "someNino", "someCaseRef", "someDate");
    }

    public static Decision someDecision() {
        return new Decision(
                "decisionsState",
                "decisionsStartDateTime",
                "appellantReply",
                "appellantReplyDateTime",
                "2017-04-01",
                "2018-12-11",
                new DecisionRates(Rate.noAward, Rate.enhancedRate, ComparedRate.Higher),
                "There was a reason!",
                new Activities(
                        asList(new Activity("mobilityActivity1", "2.1"), new Activity("mobilityActivity2", "7.5")), asList(new Activity("dailyActivity1", "3.2"), new Activity("dailyActivity2", "4"))
                )
        );
    }

    public static Statement someStatement() {
        return new Statement("Some Statement body", "someTyaCode");
    }

}
