package uk.gov.hmcts.reform.sscs.functional.evidenceshare;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.SneakyThrows;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.ClassPathResource;
import uk.gov.hmcts.reform.sscs.bulkscan.BaseFunctionalTest;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherPartySelectionDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.Role;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;

class IssueHearingEnquiryFormHandlerFunctionalTest extends AbstractFunctionalTest {

    @Test
    @SneakyThrows
    @EnabledIfEnvironmentVariable(named = "CM_OTHER_PARTY_CONFIDENTIALITY_ENABLED", matches = "true")
    void shouldIssueHearingEnquiryForm() {
        final SscsCaseDetails caseDetails = createCaseFromEvent(Benefit.CHILD_SUPPORT, EventType.VALID_APPEAL_CREATED,
            sscsCaseData -> BaseFunctionalTest.generateRandomNino());
        assertEventuallyInState(caseDetails.getId(), "awaitOtherPartyData");
        addOtherParties(caseDetails);
        issueHearingEnquiryForm(caseDetails);

        assertThatPdfTextIsCorrect(getDocument(caseDetails.getId(), "issueHearingEnquiryForm"), getExpectedContent(caseDetails));
    }

    private static @NonNull String getExpectedContent(SscsCaseDetails caseDetails) throws IOException {
        return new ClassPathResource(
            "tyanotifications/hearingenquiryform/hearingEnquiryFormExpected.template").getContentAsString(StandardCharsets.UTF_8)
                                                                                      .replace("${CASE_ID}",
                                                                                          caseDetails.getId().toString())
                                                                                      .replace("${TODAY_DATE}", LocalDate
                                                                                          .now()
                                                                                          .format(DateTimeFormatter.ofPattern(
                                                                                              "dd/MM/yyyy")));
    }

    private void issueHearingEnquiryForm(SscsCaseDetails caseDetails) {
        final String firstPartyId = caseDetails
            .getData()
            .getOtherParties()
            .stream()
            .map(CcdValue::getValue)
            .filter(otherParty -> otherParty.getName().getFullNameNoTitle().equals("Johnny Cash"))
            .map(OtherParty::getId)
            .findFirst()
            .orElseThrow();
        caseDetails.getData().setOtherPartySelection(List.of(CcdValue.<OtherPartySelectionDetails>builder().value(
            OtherPartySelectionDetails.builder().otherPartiesList(
                new DynamicList(new DynamicListItem(firstPartyId, "Johnny Cash"),
                    List.of(new DynamicListItem(firstPartyId, "Johnny Cash")))).build()).build()));
        updateCaseEvent(EventType.ISSUE_HEARING_ENQUIRY_FORM, caseDetails);
    }

    private void addOtherParties(SscsCaseDetails caseDetails) {
        caseDetails.getData().setOtherParties(List.of(CcdValue.<OtherParty>builder().value(
            OtherParty.builder().role(Role.builder().name("Paying parent").build())
                      .name(Name.builder().firstName("Johnny").lastName("Cash").build())
                      .address(Address.builder().line1("1 Old Street").town("Hendersonville").postcode("DD11 4WR").build())
                      .confidentialityRequired(YesNo.YES).build()).build(), CcdValue.<OtherParty>builder().value(
            OtherParty.builder().role(Role.builder().name("Other").build())
                      .name(Name.builder().firstName("June").lastName("Carter-Cash").build())
                      .address(Address.builder().line1("2 Old Street").town("Hendersonville").postcode("DD12 4WR").build())
                      .confidentialityRequired(YesNo.YES).build()).build()));
        updateCaseEvent(EventType.UPDATE_OTHER_PARTY_DATA, caseDetails);
    }

}