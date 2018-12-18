package uk.gov.hmcts.reform.sscs.transform.deserialize;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.AppealReason;
import uk.gov.hmcts.reform.sscs.ccd.domain.AppealReasonDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.AppealReasons;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Contact;
import uk.gov.hmcts.reform.sscs.ccd.domain.DateRange;
import uk.gov.hmcts.reform.sscs.ccd.domain.Document;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.Event;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.Evidence;
import uk.gov.hmcts.reform.sscs.ccd.domain.ExcludeDate;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.Identity;
import uk.gov.hmcts.reform.sscs.ccd.domain.MrnDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.OnlinePanel;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.Representative;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscription;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscriptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.Venue;
import uk.gov.hmcts.reform.sscs.model.NotificationEventType;
import uk.gov.hmcts.reform.sscs.model.SscsCaseDataWrapper;

@Service
public class SscsCaseDataWrapperDeserializer extends StdDeserializer<SscsCaseDataWrapper> {

    public static final String VALUE = "value";

    public SscsCaseDataWrapperDeserializer() {
        this(null);
    }

    public SscsCaseDataWrapperDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public SscsCaseDataWrapper deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        @SuppressWarnings("unchecked")
        ObjectCodec oc = jp.getCodec();
        JsonNode node = oc.readTree(jp);

        return buildSscsCaseDataWrapper(node);
    }

    public SscsCaseDataWrapper buildSscsCaseDataWrapper(JsonNode node) {
        SscsCaseData newSscsCaseData = null;
        SscsCaseData oldSscsCaseData = null;

        JsonNode caseDetailsNode = getNode(node, "case_details");
        JsonNode caseNode = getNode(caseDetailsNode, "case_data");

        if (caseNode == null) {
            caseNode = getNode(caseDetailsNode, "data");
        }

        if (caseNode != null) {
            newSscsCaseData = createSscsCaseDataFromNode(caseNode, caseDetailsNode);
        }

        if (getNode(node, "case_details_before") != null) {
            JsonNode oldCaseDetailsNode = getNode(node, "case_details_before");
            JsonNode oldCaseNode = getNode(oldCaseDetailsNode, "case_data");

            if (oldCaseNode != null) {
                oldSscsCaseData = createSscsCaseDataFromNode(oldCaseNode, oldCaseDetailsNode);
            }
        }
        NotificationEventType notificationEventType = NotificationEventType.getNotificationById(getField(node, "event_id"));

        return SscsCaseDataWrapper.builder()
                .newSscsCaseData(newSscsCaseData)
                .oldSscsCaseData(oldSscsCaseData)
                .notificationEventType(notificationEventType).build();
    }

    private SscsCaseData createSscsCaseDataFromNode(JsonNode caseNode, JsonNode caseDetailsNode) {
        SscsCaseData ccdResponse = deserializeCaseNode(caseNode);
        ccdResponse.setCcdCaseId(getField(caseDetailsNode, "id"));
        return ccdResponse;
    }

    private SscsCaseData deserializeCaseNode(JsonNode caseNode) {
        JsonNode appealNode = getNode(caseNode, "appeal");
        JsonNode subscriptionsNode = getNode(caseNode, "subscriptions");

        OnlinePanel onlinePanel = deserializeOnlinePanelJson(caseNode);
        Appeal appeal = deserializeAppealDetailsJson(appealNode);
        Subscriptions subscriptions = deserializeSubscriptionsJson(subscriptionsNode);
        List<Event> events = deserializeEventDetailsJson(caseNode);
        List<Hearing> hearings = deserializeHearingDetailsJson(caseNode);
        Evidence evidence = deserializeEvidenceDetailsJson(caseNode);
        RegionalProcessingCenter rpc = deserializeRegionalProcessingCenterJson(caseNode);
        List<SscsDocument> sscsDocument = deserializeSscsDocuments(caseNode);

        return SscsCaseData.builder()
                .caseReference(getField(caseNode, "caseReference"))
                .onlinePanel(onlinePanel)
                .appeal(appeal)
                .sscsDocument(sscsDocument)
                .subscriptions(subscriptions)
                .events(events)
                .hearings(hearings)
                .evidence(evidence)
                .regionalProcessingCenter(rpc)
                .build();
    }

    public OnlinePanel deserializeOnlinePanelJson(JsonNode caseNode) {
        JsonNode panelNode = getNode(caseNode, "onlinePanel");

        if (panelNode != null) {
            return OnlinePanel.builder()
                    .assignedTo(getField(panelNode, "assignedTo"))
                    .disabilityQualifiedMember(getField(panelNode, "disabilityQualifiedMember"))
                    .medicalMember(getField(panelNode, "medicalMember")).build();
        }
        return null;
    }

    private Appeal deserializeAppealDetailsJson(JsonNode appealNode) {

        MrnDetails mrnDetails = deserializeMrnDetailsJson(appealNode);
        Appellant appellant = deserializeAppellantDetailsJson(appealNode);
        BenefitType benefitType = deserializeBenefitDetailsJson(appealNode);
        HearingOptions hearingOptions = deserializeHearingOptionsJson(appealNode);
        AppealReasons appealReasons = deserializeAppealReasonsJson(appealNode);
        Representative rep = deserializeRepresentativeReasons(appealNode);

        return Appeal.builder()
                .mrnDetails(mrnDetails)
                .appellant(appellant)
                .benefitType(benefitType)
                .hearingOptions(hearingOptions)
                .appealReasons(appealReasons)
                .rep(rep)
                .hearingType(getField(appealNode, "hearingType"))
                .signer(getField(appealNode, "signer")).build();
    }

    public MrnDetails deserializeMrnDetailsJson(JsonNode appealNode) {
        JsonNode mrnNode = getNode(appealNode, "mrnDetails");

        String dwpIssuingOffice = getField(mrnNode, "dwpIssuingOffice");
        String mrnDate = getField(mrnNode, "mrnDate");
        String mrnLateReason = getField(mrnNode, "mrnLateReason");
        String mrnMissingReason = getField(mrnNode, "mrnMissingReason");

        return MrnDetails.builder()
                .dwpIssuingOffice(dwpIssuingOffice)
                .mrnDate(mrnDate)
                .mrnLateReason(mrnLateReason)
                .mrnMissingReason(mrnMissingReason).build();
    }

    public Appellant deserializeAppellantDetailsJson(JsonNode appealNode) {
        JsonNode appellantNode = getNode(appealNode, "appellant");

        Name name = deserializeNameJson(appellantNode);
        Address address = deserializeAddressJson(appellantNode);
        Contact contact = deserializeContactJson(appellantNode);
        Identity identity = deserializeIdentityJson(appellantNode);

        return Appellant.builder()
                .name(name).address(address).contact(contact).identity(identity).build();
    }

    private Contact deserializeContactJson(JsonNode node) {
        JsonNode contactNode = getNode(node, "contact");

        String phone = getField(contactNode, "phone") != null ? getField(contactNode, "phone") : getField(contactNode, "mobile");

        return Contact.builder()
                .email(getField(contactNode, "email"))
                .phone(phone).build();
    }

    private Identity deserializeIdentityJson(JsonNode node) {
        JsonNode nameNode = getNode(node, "identity");

        return Identity.builder()
                .dob(getField(nameNode, "dob"))
                .nino(getField(nameNode, "nino")).build();
    }

    public BenefitType deserializeBenefitDetailsJson(JsonNode appealNode) {
        JsonNode benefitTypeNode = getNode(appealNode, "benefitType");

        if (benefitTypeNode != null) {
            String benefitCode = getField(benefitTypeNode, "code");
            return BenefitType.builder().code(benefitCode).build();
        }
        return null;
    }

    public HearingOptions deserializeHearingOptionsJson(JsonNode appealNode) {
        JsonNode hearingOptionsNode = getNode(appealNode, "hearingOptions");

        String wantsToAttend = getField(hearingOptionsNode, "wantsToAttend");
        String wantsSupport = getField(hearingOptionsNode, "wantsSupport");
        String languageInterpreter = getField(hearingOptionsNode, "languageInterpreter");
        String languages = getField(hearingOptionsNode, "languages");

        List<String> arrangements = buildArrangements(hearingOptionsNode);
        String scheduleHearing = getField(hearingOptionsNode, "scheduleHearing");

        List<ExcludeDate> excludeDates = buildExcludeDates(hearingOptionsNode);

        String other = getField(hearingOptionsNode, "other");

        return HearingOptions.builder()
                .wantsToAttend(wantsToAttend)
                .wantsSupport(wantsSupport)
                .languageInterpreter(languageInterpreter)
                .languages(languages)
                .arrangements(arrangements)
                .scheduleHearing(scheduleHearing)
                .excludeDates(excludeDates)
                .other(other).build();
    }

    private List<String> buildArrangements(JsonNode hearingOptionsNode) {
        List<String> arrangements = new ArrayList<>();

        if (hearingOptionsNode != null) {
            final JsonNode arrangementsNode = hearingOptionsNode.get("arrangements");

            if (arrangementsNode != null && arrangementsNode.isArray()) {
                for (final JsonNode objNode : arrangementsNode) {
                    arrangements.add(objNode.asText());
                }
            }
        }
        return arrangements;
    }

    private List<ExcludeDate> buildExcludeDates(JsonNode hearingOptionsNode) {
        List<ExcludeDate> excludeDates = new ArrayList<>();

        if (hearingOptionsNode != null) {
            final JsonNode excludeDatesNode = hearingOptionsNode.get("excludeDates");

            if (excludeDatesNode != null && excludeDatesNode.isArray()) {
                for (final JsonNode objNode : excludeDatesNode) {
                    final JsonNode valueNode = getNode(objNode, VALUE);

                    excludeDates.add(ExcludeDate.builder().value(DateRange.builder()
                            .start(getField(valueNode, "start"))
                            .end(getField(valueNode, "end")).build()).build());
                }
            }
        }
        return excludeDates;
    }

    public AppealReasons deserializeAppealReasonsJson(JsonNode appealNode) {
        JsonNode appealReasonsNode = getNode(appealNode, "appealReasons");

        List<AppealReason> reasons = buildAppealReasons(appealReasonsNode);
        String otherReasons = getField(appealReasonsNode, "otherReasons");

        return AppealReasons.builder()
                .reasons(reasons)
                .otherReasons(otherReasons).build();
    }

    private List<AppealReason> buildAppealReasons(JsonNode appealReasonsNode) {
        List<AppealReason> appealReasons = new ArrayList<>();

        if (appealReasonsNode != null) {
            final JsonNode reasonsNode = appealReasonsNode.get("reasons");

            if (reasonsNode != null && reasonsNode.isArray()) {
                for (final JsonNode objNode : reasonsNode) {
                    final JsonNode valueNode = getNode(objNode, VALUE);

                    appealReasons.add(AppealReason.builder().value(AppealReasonDetails.builder()
                            .description(getField(valueNode, "description"))
                            .reason(getField(valueNode, "reason")).build()).build());
                }
            }
        }
        return appealReasons;
    }

    public Representative deserializeRepresentativeReasons(JsonNode appealReasonsNode) {
        final JsonNode repNode = appealReasonsNode.get("rep");

        String hasRepresentative = getField(repNode, "hasRepresentative");
        if (hasRepresentative != null && hasRepresentative.equalsIgnoreCase("yes")) {
            Name name = deserializeNameJson(repNode);
            Address address = deserializeAddressJson(repNode);
            Contact contact = deserializeContactJson(repNode);
            String organisation = getField(repNode, "organisation");

            return Representative.builder()
                    .name(name).address(address).contact(contact).organisation(organisation).build();
        }
        return null;
    }

    public Subscriptions deserializeSubscriptionsJson(JsonNode subscriptionsNode) {
        return Subscriptions.builder()
                .appellantSubscription(deserializeSubscriptionJson(
                        subscriptionsNode, "appellantSubscription"))
                .representativeSubscription(deserializeSubscriptionJson(
                        subscriptionsNode, "representativeSubscription")).build();
    }

    private Subscription deserializeSubscriptionJson(JsonNode subscriptionsNode, String subscriptionName) {
        JsonNode subscriptionNode = getNode(subscriptionsNode, subscriptionName);

        Subscription subscription = Subscription.builder().build();

        if (subscriptionNode != null) {
            subscription = deserializeSubscriberJson(subscriptionNode, subscription);
        }

        return subscription;
    }

    public List<Event> deserializeEventDetailsJson(JsonNode caseNode) {
        final JsonNode eventNode = caseNode.get("events");

        if (eventNode != null && eventNode.isArray()) {
            List<Event> events = new ArrayList<>();

            for (final JsonNode objNode : eventNode) {
                JsonNode valueNode = getNode(objNode, VALUE);

                String date = getField(valueNode, "date");
                String eventType = getField(valueNode, "type");

                events.add(Event.builder().value(EventDetails.builder().date(date).type(eventType).build()).build());

            }
            Collections.sort(events, Collections.reverseOrder());
            return events;
        }
        return Collections.emptyList();
    }

    public List<Hearing> deserializeHearingDetailsJson(JsonNode caseNode) {
        final JsonNode hearingNode = caseNode.get("hearings");

        if (hearingNode != null && hearingNode.isArray()) {
            List<Hearing> hearings = new ArrayList<>();

            for (final JsonNode objNode : hearingNode) {
                JsonNode valueNode = getNode(objNode, VALUE);
                JsonNode venueNode = getNode(valueNode, "venue");

                Hearing hearing = Hearing.builder().value(HearingDetails.builder()
                        .hearingDate(getField(valueNode, "hearingDate"))
                        .time(getField(valueNode, "time"))
                        .venue(Venue.builder()
                                .name(getField(venueNode, "name"))
                                .address(deserializeAddressJson(venueNode))
                                .googleMapLink(getField(venueNode, "googleMapLink"))
                                .build()).build()).build();

                hearings.add(hearing);
            }
            Collections.sort(hearings, Collections.reverseOrder());
            return hearings;
        }
        return Collections.emptyList();
    }

    private Evidence deserializeEvidenceDetailsJson(JsonNode caseNode) {
        JsonNode evidenceNode = getNode(caseNode, "evidence");

        if (evidenceNode != null) {
            final JsonNode documentsNode = evidenceNode.get("documents");

            List<Document> documents = new ArrayList<>();
            if (documentsNode != null && documentsNode.isArray()) {
                for (final JsonNode objNode : documentsNode) {

                    JsonNode valueNode = getNode(objNode, VALUE);

                    Document document = Document.builder().value(DocumentDetails.builder()
                            .dateReceived(getField(valueNode, "dateReceived"))
                            .evidenceType(getField(valueNode, "evidenceType"))
                            .evidenceProvidedBy(getField(valueNode, "evidenceProvidedBy")).build()).build();

                    documents.add(document);
                }
                Collections.sort(documents, Collections.reverseOrder());
            }
            return Evidence.builder().documents(documents).build();
        }
        return null;
    }

    private Address deserializeAddressJson(JsonNode node) {
        JsonNode addressNode = getNode(node, "address");

        return Address.builder().line1(getField(addressNode, "line1"))
                .line2(getField(addressNode, "line2"))
                .town(getField(addressNode, "town"))
                .county(getField(addressNode, "county"))
                .postcode(getField(addressNode, "postcode")).build();
    }

    private Name deserializeNameJson(JsonNode node) {
        JsonNode nameNode = getNode(node, "name");

        return Name.builder()
                .title(getField(nameNode, "title"))
                .firstName(getField(nameNode, "firstName"))
                .lastName(getField(nameNode, "lastName")).build();
    }

    private Subscription deserializeSubscriberJson(JsonNode node, Subscription subscription) {
        if (node != null) {
            subscription = subscription.toBuilder()
                    .tya(getField(node, "tya"))
                    .email(getField(node, "email"))
                    .mobile(getField(node, "mobile"))
                    .subscribeSms(convertEmptyToNo(getField(node, "subscribeSms")))
                    .subscribeEmail(convertEmptyToNo(getField(node, "subscribeEmail")))
                    .build();
        }

        return subscription;
    }

    private String convertEmptyToNo(String field) {
        return field == null || field.equals("") ? "No" : field;
    }

    public JsonNode getNode(JsonNode node, String field) {
        return node != null && node.has(field) && !node.get(field).getNodeType().equals(JsonNodeType.NULL) ? node.get(field) : null;
    }

    public String getField(JsonNode node, String field) {
        return node != null && !node.asText().equals("null") && node.has(field) && !node.get(field).asText().equals("null") ? node.get(field).asText() : null;
    }

    public RegionalProcessingCenter deserializeRegionalProcessingCenterJson(JsonNode rpcNode) {
        JsonNode regionalProcessingCenterNode = getNode(rpcNode, "regionalProcessingCenter");

        if (null != regionalProcessingCenterNode) {
            return RegionalProcessingCenter.builder()
                    .name(getField(regionalProcessingCenterNode, "name"))
                    .address1(getField(regionalProcessingCenterNode, "address1"))
                    .address2(getField(regionalProcessingCenterNode, "address2"))
                    .address3(getField(regionalProcessingCenterNode, "address3"))
                    .address4(getField(regionalProcessingCenterNode, "address4"))
                    .city(getField(regionalProcessingCenterNode, "city"))
                    .postcode(getField(regionalProcessingCenterNode, "postcode"))
                    .phoneNumber(getField(regionalProcessingCenterNode, "phoneNumber"))
                    .faxNumber(getField(regionalProcessingCenterNode, "faxNumber")).build();
        }
        return null;
    }

    private List<SscsDocument> deserializeSscsDocuments(JsonNode caseNode) {
        List<SscsDocument> list = new ArrayList<>();
        JsonNode sscsDocument = getNode(caseNode, "sscsDocument");

        for (int i = 0; sscsDocument != null && i < sscsDocument.size(); ++i) {
            JsonNode doc = sscsDocument.get(i);
            JsonNode value = getNode(doc, VALUE);
            JsonNode link = getNode(value, "documentLink");
            if (doc == null || value == null || link == null) {
                continue;
            }

            list.add(SscsDocument.builder()
                    .value(
                            SscsDocumentDetails.builder()
                                    .documentDateAdded(getField(value, "documentDateAdded"))
                                    .documentFileName(getField(value, "documentFileName"))
                                    .documentLink(
                                            DocumentLink.builder()
                                                    .documentUrl(getField(link, "document_url"))
                                                    .documentBinaryUrl(getField(link, "document_binary_url"))
                                                    .documentFilename(getField(link, "document_filename"))
                                                    .build()
                                    )
                                    .build()
                    )
                    .build()
            );
        }
        return list;
    }

}
