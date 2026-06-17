package uk.gov.hmcts.reform.sscs.functional.evidenceshare;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.uploaddocuments.DocumentType.APPELLANT_EVIDENCE;

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
import uk.gov.hmcts.reform.ccd.document.am.model.UploadResponse;
import uk.gov.hmcts.reform.sscs.bulkscan.BaseFunctionalTest;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentSelectionDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherPartySelectionDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.Role;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;

class IssueHearingEnquiryFormHandlerFunctionalTest extends AbstractFunctionalTest {

    private static final String HENDERSONVILLE = "Hendersonville";
    private static final String JOHNNY_CASH = "Johnny Cash";
    private static final String POSTCODE = "TS1 1ST";

    @Test
    @SneakyThrows
    @EnabledIfEnvironmentVariable(named = "CM_OTHER_PARTY_CONFIDENTIALITY_ENABLED", matches = "true")
    void shouldIssueHearingEnquiryFormViaGovNotify() {
        final SscsCaseDetails caseDetails = createCaseFromEvent(Benefit.CHILD_SUPPORT, EventType.VALID_APPEAL_CREATED,
            sscsCaseData -> BaseFunctionalTest.generateRandomNino());
        assertEventuallyInState(caseDetails.getId(), "awaitOtherPartyData");
        addOtherParties(caseDetails);
        issueHearingEnquiryForm(caseDetails, null);

        assertThatPdfTextIsCorrect(getDocument(caseDetails.getId(), "issueHearingEnquiryForm"),
            getExpectedContent(caseDetails, "tyanotifications/hearingenquiryform/hearingEnquiryFormExpected.template"));

    }

    @Test
    @SneakyThrows
    @EnabledIfEnvironmentVariable(named = "CM_OTHER_PARTY_CONFIDENTIALITY_ENABLED", matches = "true")
    void shouldIssueHearingEnquiryFormViaBulkPrint() {
        final SscsCaseDetails caseDetails = createCaseFromEvent(Benefit.CHILD_SUPPORT, EventType.VALID_APPEAL_CREATED,
            sscsCaseData -> BaseFunctionalTest.generateRandomNino());
        assertEventuallyInState(caseDetails.getId(), "awaitOtherPartyData");
        addOtherParties(caseDetails);
        final UploadResponse uploadResponse = uploadDocumentAndUpdateCase(caseDetails);
        issueHearingEnquiryForm(caseDetails, uploadResponse.getDocuments().getFirst().originalDocumentName);

        assertThatPdfTextIsCorrect(getDocument(caseDetails.getId(), "issueHearingEnquiryForm"),
            getExpectedContent(caseDetails, "tyanotifications/hearingenquiryform/hearingEnquiryFormBulkPrintExpected.template"));
    }

    private static @NonNull String getExpectedContent(final SscsCaseDetails caseDetails, String path) throws IOException {
        return new ClassPathResource(path)
            .getContentAsString(StandardCharsets.UTF_8)
            .replace("${CASE_ID}", caseDetails.getId().toString())
            .replace("${TODAY_DATE}", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    }

    private @NonNull UploadResponse uploadDocumentAndUpdateCase(SscsCaseDetails caseDetails) throws IOException {
        final UploadResponse uploadResponse = uploadDocToDocMgmtStore("large-document.pdf");
        caseDetails
            .getData()
            .setSscsDocument(List.of(SscsDocument
                .builder()
                .value(SscsDocumentDetails
                    .builder()
                    .documentLink(DocumentLink
                        .builder()
                        .documentFilename(uploadResponse.getDocuments().getFirst().originalDocumentName)
                        .documentUrl(uploadResponse.getDocuments().getFirst().links.self.href)
                        .documentBinaryUrl(uploadResponse.getDocuments().getFirst().links.binary.href)
                        .build())
                    .documentType(APPELLANT_EVIDENCE.getId())
                    .documentFileName(uploadResponse.getDocuments().getFirst().originalDocumentName)
                    .build())
                .build()));
        updateCaseEvent(EventType.UPLOAD_DOCUMENT, caseDetails);
        return uploadResponse;
    }

    private void issueHearingEnquiryForm(final SscsCaseDetails caseDetails, String additionalDocumentName) {
        final String firstPartyId = caseDetails
            .getData()
            .getOtherParties()
            .stream()
            .map(CcdValue::getValue)
            .filter(otherParty -> otherParty.getName().getFullNameNoTitle().equals(JOHNNY_CASH))
            .map(OtherParty::getId)
            .findFirst()
            .orElseThrow();
        caseDetails
            .getData()
            .getAppeal()
            .getAppellant()
            .setAddress(Address.builder().line1("3 Old Street").town(HENDERSONVILLE).postcode(POSTCODE).build());
        caseDetails
            .getData()
            .setOtherPartySelection(List.of(CcdValue
                .<OtherPartySelectionDetails>builder()
                .value(OtherPartySelectionDetails
                    .builder()
                    .otherPartiesList(new DynamicList(new DynamicListItem(firstPartyId, JOHNNY_CASH),
                        List.of(new DynamicListItem(firstPartyId, JOHNNY_CASH))))
                    .build())
                .build()));

        if (nonNull(additionalDocumentName)) {
            caseDetails
                .getData()
                .setDocumentSelection(List.of(CcdValue
                    .<DocumentSelectionDetails>builder()
                    .value(DocumentSelectionDetails
                        .builder()
                        .documentsList(new DynamicList(new DynamicListItem(additionalDocumentName, additionalDocumentName),
                            List.of(new DynamicListItem(additionalDocumentName, additionalDocumentName))))
                        .build())
                    .build()));
            caseDetails.getData().setAddDocuments(YesNo.YES);
        }

        updateCaseEvent(EventType.ISSUE_HEARING_ENQUIRY_FORM, caseDetails);
    }

    private void addOtherParties(final SscsCaseDetails caseDetails) {
        caseDetails
            .getData()
            .setOtherParties(List.of(CcdValue
                .<OtherParty>builder()
                .value(OtherParty
                    .builder()
                    .role(Role.builder().name("Paying parent").build())
                    .name(Name.builder().firstName("Johnny").lastName("Cash").build())
                    .address(Address.builder().line1("1 Old Street").town(HENDERSONVILLE).postcode(POSTCODE).build())
                    .confidentialityRequired(YesNo.YES)
                    .build())
                .build(), CcdValue
                .<OtherParty>builder()
                .value(OtherParty
                    .builder()
                    .role(Role.builder().name("Other").build())
                    .name(Name.builder().firstName("June").lastName("Carter-Cash").build())
                    .address(Address.builder().line1("2 Old Street").town(HENDERSONVILLE).postcode(POSTCODE).build())
                    .confidentialityRequired(YesNo.YES)
                    .build())
                .build()));
        updateCaseEvent(EventType.UPDATE_OTHER_PARTY_DATA, caseDetails);
    }
}
