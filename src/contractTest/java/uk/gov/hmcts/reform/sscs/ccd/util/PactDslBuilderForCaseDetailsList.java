package uk.gov.hmcts.reform.sscs.ccd.util;

import static au.com.dius.pact.consumer.dsl.PactDslJsonRootValue.stringMatcher;
import static io.pactfoundation.consumer.dsl.LambdaDsl.newJsonArray;
import static io.pactfoundation.consumer.dsl.LambdaDsl.newJsonBody;

import au.com.dius.pact.consumer.dsl.DslPart;
import io.pactfoundation.consumer.dsl.LambdaDslObject;

public final class PactDslBuilderForCaseDetailsList {

    private PactDslBuilderForCaseDetailsList() {

    }

    public static final String REGEX_DATE = "^((19|2[0-9])[0-9]{2})-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$";
    private static final String ALPHABETIC_REGEX = "[/^[A-Za-z_]+$/]+";

    public static DslPart buildStartEventReponse(String eventId) {
        return newJsonBody((o) -> {
            o.stringType("event_id", eventId)
                .stringType("token", null)
                .object("case_details", (cd) -> {
                    cd.numberType("id", 2000);
                    cd.stringValue("jurisdiction", "SSCS");
                    cd.object("case_data", PactDslBuilderForCaseDetailsList::getCaseData);
                });
        }).build();
    }

    public static DslPart buildStartEventResponseWithEmptyCaseDetails(String eventId) {
        return newJsonBody((o) -> {
            o.stringType("event_id", eventId)
                .stringType("token", null)
                .object("case_details", (cd) -> {
                    cd.numberType("id", null);
                    cd.stringMatcher("jurisdiction", ALPHABETIC_REGEX, "SSCS");
                    cd.stringType("callback_response_status", null);
                    cd.stringMatcher("case_type_id", ALPHABETIC_REGEX, "Benefit");
                    cd.object("case_data", data -> {
                    });
                });
        }).build();
    }

    private static void getCaseData(final LambdaDslObject dataMap) {
        dataMap
            .stringType("caseReference", "SC123/11/11111")
            .stringMatcher("caseCreated", REGEX_DATE, "2005-06-07")
            .object("appeal", a -> a
                .object("rep",
                    r -> r.object("name",
                        n -> getNameLambdaDslObject(n))
                        .object("contact", c -> c
                            .stringType("email", "tanesis@gmail.com")
                            .stringType("phone", "07982989348")
                            .stringType("mobile", "07982989348"))
                        .stringType("organisation", "org")
                )
                .object("mrnDetails", m -> m
                    .stringMatcher("mrnDate", REGEX_DATE, "2010-05-01")
                )
                .object("appellant", ap -> ap
                    .object("name", n -> getNameLambdaDslObject(n))
                    .object("address", add -> getAddressLambdaDslObject(add))
                    .object("identity", i -> getIdentityLambdaDslObject(i))
                    .object("appointee", apt -> apt
                        .object("name", n -> getNameLambdaDslObject(n))
                        .object("address", apAd -> getAddressLambdaDslObject(apAd))
                        .object("contact", c -> c
                            .stringType("email", "appointee@test.com")
                            .stringType("mobile", "07982989348"))
                        .object("identity", o -> getIdentityLambdaDslObject(o))
                    )
                    .stringMatcher("isAppointee", "Yes|No", "Yes")
                    .stringMatcher("isAddressSameAsAppointee", "Yes|No", "Yes")
                )
                .object("benefitType", b -> b
                    .stringMatcher("code", "PIP")
                )
                .stringType("hearingType", "oral")
                .object("hearingOptions", ho -> ho
                    .stringMatcher("wantsToAttend", "Yes|No", "Yes")
                    .stringMatcher("wantsSupport", "Yes|No", "Yes")
                    .stringMatcher("languageInterpreter", "Yes|No", "Yes")
                    .stringType("languages", "Arabic")
                    .minArrayLike("arrangements", 2,
                        stringMatcher("signLanguageInterpreter|hearingLoop", "signLanguageInterpreter"), 2)
                    .stringMatcher("scheduleHearing", "Yes|No", "Yes")
                    .minArrayLike("excludeDates", 3, cd -> cd
                        .object("value", v -> v
                            .stringMatcher("start", REGEX_DATE, "2018-04-04")
                            .stringMatcher("end", REGEX_DATE, "2018-04-04")
                        )
                    )
                    .stringType("other", "Yes, this...")
                )
                .object("appealReasons", ar -> ar
                    .minArrayLike("reasons", 3, rs -> rs
                        .object("value", v -> v
                            .stringType("reason", "aaaaaaaaaa")
                            .stringType("description", "aaaaaaaaaa")
                        )
                    )
                    .stringType("otherReasons", "Another reason")
                )
                .stringType("signer", "Joe Bloggs")
            )
            .object("subscriptions", s -> s
                .object("appellantSubscription", sub -> getSubscriptionLambdaDslObject(sub))
                .object("appointeeSubscription", sub -> getSubscriptionLambdaDslObject(sub))
                .object("representativeSubscription", sub -> getSubscriptionLambdaDslObject(sub))
                .object("jointPartySubscription", sub -> getSubscriptionLambdaDslObject(sub))
            )
            .stringType("generatedNino", "AB877533C")
            .stringType("generatedSurname", "Bloggs")
            .stringType("generatedEmail", "joe@bloggs.com")
            .stringType("generatedMobile", "07411222222")
            //.stringType("generatedDob", "1990-02-26")
            .stringMatcher("evidencePresent", "Yes|No", "No")
            .stringMatcher("evidenceHandled", "Yes|No", "No")
            .stringMatcher("isAppellantDeceased", "Yes|No", "No");;

    }

    private static LambdaDslObject getSubscriptionLambdaDslObject(LambdaDslObject sub) {
        return sub
            .stringMatcher("wantSmsNotifications", "Yes|No", "Yes")
            .stringType("tya", "ew2OCHXOL5")
            .stringType("email", "joe@bloggs.com")
            .stringType("mobile", "07411333333")
            .stringMatcher("subscribeEmail", "Yes|No", "Yes")
            .stringMatcher("subscribeSms", "Yes|No", "Yes");
    }

    private static LambdaDslObject getIdentityLambdaDslObject(LambdaDslObject i) {
        return i
            .stringType("dob", "1990-02-26")
            .stringType("nino", "AB877533C");
    }

    private static LambdaDslObject getAddressLambdaDslObject(LambdaDslObject add) {
        return add
            .stringType("town", "Pinner")
            .stringType("line1", "12 West End Avenue")
            .stringType("line2", "Harrow")
            .stringType("county", "Middlesex")
            .stringType("postcode", "HA5 1BJ");
    }

    private static LambdaDslObject getNameLambdaDslObject(LambdaDslObject n) {
        return n
            .stringType("title", "Mrs")
            .stringType("lastName", "strickland")
            .stringType("firstName", "leah");
    }

    public static DslPart buildCaseDetailsDsl(Long caseId) {
        return newJsonBody((o) -> {
            o.numberType("id", caseId)
                .stringType("jurisdiction", "SSCS")
                .stringMatcher("state", "validAppeal|draft", "validAppeal")
                .stringValue("case_type_id", "Benefit")
                .object("case_data", (dataMap) -> {
                    getCaseData(dataMap);
                });
        }).build();
    }

    public static DslPart buildSearchResultDsl() {
        return newJsonBody((o) -> {
            o.numberType("total", 1)
                .minArrayLike("cases", 1, (cd) -> {
                    cd.numberType("id", 200);
                    cd.stringType("jurisdiction", "SSCS");
                    cd.stringType("callback_response_status", null);
                    cd.object("case_data", (dataMap) -> {
                        getCaseData(dataMap);
                    });
                });
        }).build();
    }

    public static DslPart buildNewListOfCaseDetailsDsl(Long caseId) {
        return newJsonArray((rootArray) -> {
            rootArray.object((dataMap) ->
                dataMap.stringValue("case_type_id", "Benefit")
                    .object("case_data", (caseData) -> {
                        getCaseData(caseData);
                    }));
        }).build();
    }
}



