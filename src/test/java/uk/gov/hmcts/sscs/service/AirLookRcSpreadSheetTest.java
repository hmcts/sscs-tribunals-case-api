package uk.gov.hmcts.sscs.service;

import static org.junit.Assert.assertTrue;

import java.util.*;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests to look at the integrity of the data in a business spreadsheet
 * and our list of venues.
 */
public class AirLookRcSpreadSheetTest {
    AirLookupService airLookupService;
    Map<String, String> lookupData;
    Map<String, String> venueData;
    Map<String, Integer> lookupVenueIdByAirLookupName;

    @Before
    public void setUp() {
        airLookupService = new AirLookupService();
        airLookupService.init();

        lookupData = airLookupService.getLookupRegionalCentreByPostCode();
        venueData = airLookupService.getLookupAirVenueNameByPostCode();
        lookupVenueIdByAirLookupName = airLookupService.getLookupVenueIdByAirVenueName();
    }

    /*
    TODO there are some postcodes that do not have a matching PIP venue
    known issue been referred to business waiting for reply, see Josh
    */
    static Set<String> realPostcodesWithNoVenue = new HashSet<>(Arrays.asList("ec1m", "ec3m", "ec4a",
            "nw1w", "bt82", "bl11", "bl78", "wc1v", "s31", "s30"));

    //These are not real post codes so can be ignored
    static Set<String> notRealPostcodes = new HashSet<>(Arrays.asList("cw28",
            "cw29", "cw26", "cw27", "cw24", "cw25", "cw22", "cw23", "cw20", "cw21", "cw19", "cw17", "cw18",
            "cw15", "cw16", "cw13", "cw14", "cw48", "cw49", "cw46", "cw47", "cw44", "cw45", "cw42", "cw43",
            "cw40", "cw41", "cw39", "cw37", "cw38", "cw35", "cw36", "cw33", "cw34", "cw31", "cw32", "cw30",
            "cw60", "cw61", "cw68", "cw69", "cw66", "cw67", "cw64", "cw65", "cw62", "cw63", "cw50",
            "cw59", "cw57", "cw58", "cw55", "cw56", "cw53", "cw54", "cw51", "cw52", "cw82", "cw83", "cw80",
            "cw81", "cw88", "cw89", "cw86", "cw87", "cw84", "cw85", "cw71", "cw72", "cw70", "cf53", "cw79",
            "cw77", "cw78", "cw75", "cw76", "cw73", "cw74", "cw93", "cw94", "cw91", "cw92", "cw90", "cw99",
            "cw97", "cw95", "cw96", "bl15", "bl16", "bl13", "bl14", "bl12", "bl10", "bl19",
            "bl17", "bl18", "bl37", "bl38", "bl35", "bl36", "bl33", "bl34", "bl31", "bl32", "bl30", "bl39",
            "bl26", "bl27", "bl24", "bl25", "bl22", "bl23", "bl20", "bl21", "bl28", "bl29", "bl59", "bl57",
            "bl58", "bl55", "bl56", "bl53", "bl54", "bl51", "bl52", "bl50", "bl48", "bl49", "bl46", "bl47",
            "bl44", "bl45", "bl42", "bl43", "bl40", "bl41", "bl79", "bl77", "bl75", "bl76", "bl73",
            "bl74", "bl71", "bl72", "bl70", "bl68", "bl69", "bl66", "bl67", "bl64", "bl65", "bl62", "bl63",
            "bl60", "bl61", "bl99", "bl97", "bl98", "bl95", "bl96", "bl93", "bl94", "bl91", "bl92", "bl90",
            "bl88", "bl89", "bl86", "bl87", "bl84", "bl85", "bl82", "bl83", "bl80", "bl81", "fy10", "fy11",
            "fy12", "fy13", "fy14", "fy15", "fy16", "fy17", "fy18", "fy19", "fy30", "fy31", "fy32", "fy33",
            "fy34", "fy35", "fy36", "fy37", "fy38", "fy39", "fy20", "fy21", "fy22", "fy23", "fy24", "fy25",
            "fy26", "fy27", "fy28", "fy29", "fy52", "fy53", "fy54", "fy55", "fy56", "fy57", "fy58", "fy59",
            "fy50", "fy51", "fy41", "fy42", "fy43", "fy44", "fy45", "fy46", "fy47", "fy48", "fy49", "fy40",
            "fy74", "fy75", "fy76", "fy77", "fy78", "fy79", "fy70", "fy71", "fy72", "fy73", "fy63", "fy64",
            "fy65", "fy66", "fy67", "fy68", "fy69", "fy60", "fy61", "fy62", "fy96", "fy97", "fy98", "fy99",
            "fy90", "fy91", "fy92", "fy93", "fy94", "fy95", "fy85", "fy86", "fy87", "fy88", "fy89", "fy80",
            "fy81", "fy82", "fy83", "fy84", "np17", "np41", "np40", "np34", "np33", "np32", "np31", "np38",
            "np37", "np36", "np35", "np39", "np30", "np21", "np27", "np29", "np28", "np63", "np62", "np61",
            "np60", "np56", "np55", "np54", "np53", "np59", "np58", "np57", "np52", "np51", "np50", "np45",
            "np43", "np42", "np49", "np48", "np47", "np46", "np81", "np80", "np85", "np84", "np83", "np82",
            "np78", "np77", "np76", "np75", "np79", "np70", "np74", "np73", "np72", "np71", "np67", "np66",
            "np65", "np64", "np69", "np68", "wn9", "np99", "np98", "np97", "np92", "np91", "np90", "np96",
            "np95", "np94", "np93", "np89", "np88", "np87", "np86", "fy9", "nk15",
            "wn11", "wn10", "wn19", "wn18", "wn17", "wn16", "wn15", "wn14", "wn13", "wn12", "item", "wn33",
            "wn32", "wn31", "wn30", "wn39", "wn38", "wn37", "wn36", "wn35", "wn34", "wn22", "wn21", "wn20",
            "wn29", "wn28", "wn27", "wn26", "wn25", "wn24", "wn23", "wn55", "wn54", "wn53", "wn52", "wn51",
            "wn50", "wn59", "wn58", "wn57", "wn56", "wn44", "wn43", "wn42", "wn41", "wn40", "wn49", "wn48",
            "wn47", "wn46", "wn45", "wn77", "wn76", "wn75", "wn74", "wn73", "wn72", "wn71", "wn70", "wn79",
            "wn78", "wn66", "wn65", "wn64", "wn63", "wn62", "wn61", "wn60", "wn69", "wn68", "wn67", "wn99",
            "wn98", "wn97", "wn96", "wn95", "wn94", "wn93", "wn92", "wn91", "wn90", "wn88", "wn87", "wn86",
            "wn85", "wn84", "wn83", "wn82", "wn81", "wn89", "wn80", "sk30", "sk32", "sk31", "sk38", "sk37",
            "sk39", "sk34", "sk33", "sk36", "sk35", "sk29", "sk28", "sk52", "sk51", "sk54", "sk53", "sk50",
            "sk59", "sk56", "sk55", "sk58", "sk57", "sk41", "sk40", "sk43", "sk42", "sk49", "sk48", "sk45",
            "sk44", "sk47", "sk46", "sk74", "sk73", "sk76", "sk75", "sk70", "sk72", "sk71", "sk78", "sk77",
            "sk79", "sk63", "sk62", "sk65", "sk64", "sk61", "sk60", "sk67", "sk66", "sk69", "sk68", "sk96",
            "sk95", "sk98", "sk97", "sk92", "sk91", "sk94", "sk93", "sk99", "sk90", "sk85", "sk84", "sk87",
            "sk86", "sk81", "sk80", "sk83", "sk82", "sk89", "sk88"));

    @Test
    public void testAllPostcodesHavePip() {
        List<String> missingPipVenuePostcodes = new ArrayList<>();

        Set<String> rcKeys = lookupData.keySet();
        Iterator<String> iterator = rcKeys.iterator();
        while (iterator.hasNext()) {
            String postcode = iterator.next();
            if (!venueData.keySet().contains(postcode) && !lookupData.get(postcode).equals("Glasgow")) {
                if (!realPostcodesWithNoVenue.contains(postcode)  && !notRealPostcodes.contains(postcode)) {
                    missingPipVenuePostcodes.add(postcode);
                }
            }
        }

        assertTrue(missingPipVenuePostcodes + " of " + lookupData.size()
                        + " post codes do not have a PIP entry: "
                        + Arrays.toString(missingPipVenuePostcodes.toArray()),
                missingPipVenuePostcodes.size() == 0);
    }

    //TODO venues that are in spreadsheet but not in our list of venues
    static Set<String> venuesThatAreNotInSpreadsheet = new HashSet<>(Arrays.asList("Teeside Justice Centre", "Dover", "Gateshead Law Courts"));

    //TODO venues that the business are coming back to us as to how to process
    static Set<String> venuesWithIssues = new HashSet<>(Arrays.asList("St.Helens(if WA3 3** use Wigan)", "Liverpool (if Banks area use Wigan)"));

    @Test
    public void testAllPipsMapToVenueId() {
        Set<String> postCodesForVenueNames = venueData.keySet();

        Set<String> missingAirLookupNames = new HashSet<>();
        Set<String> workingAirLookupNames = new HashSet<>();

        Iterator postCodeIterator = postCodesForVenueNames.iterator();
        while (postCodeIterator.hasNext()) {
            Object postCode = postCodeIterator.next();
            String airLookupName = venueData.get(postCode);
            Integer venueId = lookupVenueIdByAirLookupName.get(airLookupName);

            if (venueId == null || venueId.intValue() == 0) {
                if (venuesThatAreNotInSpreadsheet.contains(airLookupName)
                        || venuesWithIssues.contains(airLookupName)) {
                    //ignore
                } else {
                    missingAirLookupNames.add(airLookupName);
                }
            } else {
                workingAirLookupNames.add(airLookupName);
            }
        }

        assertTrue(missingAirLookupNames.size() + " airLookupNames don't map to a venueId"
                        + "\nMissing: " + Arrays.toString(missingAirLookupNames.toArray())
                        + "\nWorking: " + Arrays.toString(workingAirLookupNames.toArray()),
                missingAirLookupNames.size() == 0);
    }
}
