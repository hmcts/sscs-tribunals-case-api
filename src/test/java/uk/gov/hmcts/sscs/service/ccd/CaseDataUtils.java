package uk.gov.hmcts.sscs.service.ccd;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.sscs.model.ccd.*;

public final class CaseDataUtils {
    private CaseDataUtils() {
    }

    public static CaseData buildCaseData() {
        return buildCaseData("Test");
    }

    public static CaseData buildCaseData(String surname) {
        Name name = Name.builder()
                .title("Mr")
                .firstName("User")
                .lastName(surname)
                .build();
        Address address = Address.builder()
                .postcode("L17 7AE")
                .build();
        Contact contact = Contact.builder()
                .email("mail@email.com")
                .phone("01234567890")
                .mobile("01234567890")
                .build();
        Identity identity = Identity.builder()
                .dob("1904-03-10")
                .nino("AB 22 55 66 B")
                .build();
        Appellant appellant = Appellant.builder()
                .name(name)
                .address(address)
                .contact(contact)
                .identity(identity)
                .build();
        BenefitType benefitType = BenefitType.builder()
                .code("JSA")
                .build();

        DateRange dateRange = DateRange.builder()
                .start("2018-06-30")
                .end("2018-06-30")
                .build();
        ExcludeDate excludeDate = ExcludeDate.builder()
                .value(dateRange)
                .build();

        HearingOptions hearingOptions = HearingOptions.builder()
                .wantsToAttend("Yes")
                .arrangements(Arrays.asList("disabledAccess", "hearingLoop"))
                .excludeDates(Collections.singletonList(excludeDate))
                .other("No")
                .build();

        MrnDetails mrnDetails = MrnDetails.builder()
                .mrnDate("2018-06-30")
                .dwpIssuingOffice("1")
                .build();

        Representative representative = Representative.builder()
                .hasRepresentative("Yes")
                .build();

        final Appeal appeal = Appeal.builder()
                .appellant(appellant)
                .benefitType(benefitType)
                .hearingOptions(hearingOptions)
                .mrnDetails(mrnDetails)
                .rep(representative)
                .signer("Signer")
                .build();

        Address venueAddress = Address.builder()
                .postcode("AB12 3ED")
                .build();
        Venue venue = Venue.builder()
                .name("Aberdeen")
                .address(venueAddress)
                .build();
        HearingDetails hearingDetails = HearingDetails.builder()
                .venue(venue)
                .hearingDate("2017-05-24")
                .time("10:45")
                .adjourned("Yes")
                .build();
        Hearing hearings = Hearing.builder()
                .value(hearingDetails)
                .build();
        List<Hearing> hearingsList = new ArrayList<>();
        hearingsList.add(hearings);

        DocumentDetails doc = DocumentDetails.builder()
                .dateReceived("2017-05-24")
                .evidenceType("General")
                .build();
        Document documents = Document.builder()
                .value(doc)
                .build();
        List<Document> documentsList = new ArrayList<>();
        documentsList.add(documents);
        final Evidence evidence = Evidence.builder()
                .documents(documentsList)
                .build();

        DwpTimeExtensionDetails dwpTimeExtensionDetails = DwpTimeExtensionDetails.builder()
                .requested("Yes")
                .granted("Yes")
                .build();
        DwpTimeExtension dwpTimeExtension = DwpTimeExtension.builder()
                .value(dwpTimeExtensionDetails)
                .build();
        List<DwpTimeExtension> dwpTimeExtensionList = new ArrayList<>();
        dwpTimeExtensionList.add(dwpTimeExtension);

        EventDetails eventDetails = EventDetails.builder()
                .type("appealCreated")
                .description("Appeal Created")
                .date("2001-12-14T21:59:43.10-05:00")
                .build();
        Event events = Event.builder()
                .value(eventDetails)
                .build();

        Subscription appellantSubscription = Subscription.builder()
                .tya("")
                .email("appellant@email.com")
                .mobile("")
                .subscribeEmail("Yes")
                .subscribeSms("Yes")
                .reason("")
                .build();
        Subscription supporterSubscription = Subscription.builder()
                .tya("")
                .email("supporter@email.com")
                .mobile("")
                .subscribeEmail("")
                .subscribeSms("")
                .reason("")
                .build();
        Subscriptions subscriptions = Subscriptions.builder()
                .appellantSubscription(appellantSubscription)
                .supporterSubscription(supporterSubscription)
                .build();

        return CaseData.builder()
                .caseReference("SC068/17/00013")
                .caseCreated(LocalDate.now().toString())
                .appeal(appeal)
                .hearings(hearingsList)
                .evidence(evidence)
                .dwpTimeExtension(dwpTimeExtensionList)
                .events(Collections.singletonList(events))
                .subscriptions(subscriptions)
                .build();
    }

    public static CaseDetails buildCaseDetails() {
        return CaseDetails.builder().data(buildCaseDataMap(buildCaseData())).build();
    }

    public static CaseDetails buildCaseDetails(String caseReference) {
        CaseData caseData = buildCaseData().toBuilder().caseReference(caseReference).build();
        return CaseDetails.builder().data(buildCaseDataMap(caseData)).build();
    }

    private static Map<String, Object> buildCaseDataMap(CaseData caseData) {
        ObjectMapper mapper = new ObjectMapper();

        try {
            String json = mapper.writeValueAsString(caseData);
            return mapper.readValue(json, new TypeReference<Map<String, Object>>(){});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<CaseDetails> buildCaseDetailsList() {
        return Arrays.asList(CaseDetails.builder().data(buildCaseDataMap(buildCaseData())).build());
    }
}
