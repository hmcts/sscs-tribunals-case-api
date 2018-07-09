package uk.gov.hmcts.sscs.service;

import static org.slf4j.LoggerFactory.getLogger;

import com.opencsv.CSVReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import javax.annotation.PostConstruct;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.NPOIFSFileSystem;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * Service that ingests a spreadsheet and a csv file containing the
 * venues and regional centres for handling all post codes.
 */

@Service
public class AirLookupService {
    public String lookupRegionalCentre(String postcode) {
        //full post code
        if (postcode.length() >= 5) {
            int index = postcode.length() - 3;
            //trim last 3 chars to leave the outcode
            String outcode = postcode.toLowerCase().substring(0, index).trim();
            return lookupRegionalCentreByPostCode.get(outcode);
        } else {
            //try it as the outcode
            return lookupRegionalCentreByPostCode.get(postcode.toLowerCase().trim());
        }
    }

    private static final Logger LOG = getLogger(AirLookupService.class);
    private static int POSTCODE_COLUMN = 1;
    private static int REGIONAL_CENTRE_COLUMN = 3;
    private static int VENUE_COLUMN = 2;

    private static String DEFAULT_VENUE = "Birmingham";
    private static int DEFAULT_VENUE_ID = 24;

    private static final String CSV_FILE_PATH = "reference-data/airLookupVenueIds.csv";

    private Map<String, String> lookupRegionalCentreByPostCode;
    private Map<String, String> lookupAirVenueNameByPostCode;
    Map<String, Integer> lookupVenueIdByAirVenueName;

    /**
     * Read in the AIRLookup RC spreadsheet and the venue id csv file.
     */
    @PostConstruct
    public void init() {
        lookupRegionalCentreByPostCode = new HashMap<>();
        lookupAirVenueNameByPostCode = new HashMap<>();
        lookupVenueIdByAirVenueName = new HashMap<>();

        try {
            ClassPathResource classPathResource = new ClassPathResource("reference-data/AIRLookup RC.xls");

            parseSpreadSheet(classPathResource);

            LOG.debug("Successfully loaded lookup data for postcode endpoint");
            parseSpreadSheet(classPathResource);

            LOG.debug("Regional centre data has " + lookupRegionalCentreByPostCode.keySet().size() + " post codes");
            LOG.debug("Air Venue data has " + lookupAirVenueNameByPostCode.keySet().size() + " post codes");

        } catch (IOException e) {
            LOG.error("Unable to read in spreadsheet with post code data: reference-data/AIRLookup RC.xls", e);
        }
        try {
            ClassPathResource classPathResource = new ClassPathResource(CSV_FILE_PATH);

            readVenueCsv(classPathResource);

            LOG.debug("Venue map has " + lookupAirVenueNameByPostCode.keySet().size() + " venue ids");
        } catch (IOException e) {
            LOG.error("Unable to read in csv with post code - venue id data: reference-data/airLookupVenueIds.csv", e);
        }
    }

    /**
     * Read in spreadsheet and populate the Map with postcode.
     * @param classPathResource The file containing the spreadsheet
     * @throws IOException pass up any IO errors
     */
    private void parseSpreadSheet(ClassPathResource classPathResource) throws IOException {
        try (NPOIFSFileSystem fs = new NPOIFSFileSystem(classPathResource.getInputStream());
             HSSFWorkbook wb = new HSSFWorkbook(fs.getRoot(), true)) {

            for (Sheet sheet: wb) {
                if (sheet.getSheetName().equals("AIR")) {
                    for (Row row : sheet) {
                        Cell postcodeCell = row.getCell(POSTCODE_COLUMN);
                        Cell adminGroupCell = row.getCell(REGIONAL_CENTRE_COLUMN);
                        Cell venueCell = row.getCell(VENUE_COLUMN);
                        if (postcodeCell != null && adminGroupCell != null
                                && postcodeCell.getCellTypeEnum() == CellType.STRING && adminGroupCell.getCellTypeEnum() == CellType.STRING) {
                            LOG.debug("Post code: " + postcodeCell.getRichStringCellValue().getString().toLowerCase()
                                + " Regional office: " + adminGroupCell.getRichStringCellValue().getString());
                            lookupRegionalCentreByPostCode.put(postcodeCell.getRichStringCellValue().getString().toLowerCase(), adminGroupCell.getRichStringCellValue().getString());
                        }
                        if (postcodeCell != null && venueCell != null
                                && postcodeCell.getCellTypeEnum() == CellType.STRING && venueCell.getCellTypeEnum() == CellType.STRING) {
                            if (hasPip(venueCell.getRichStringCellValue().getString())) {
                                String venueName = venueCell.getRichStringCellValue().getString();

                                String venueNameSplitChar = "-";
                                if (venueName.indexOf(venueNameSplitChar) != -1) {
                                    lookupAirVenueNameByPostCode.put(postcodeCell.getRichStringCellValue().getString().toLowerCase(),
                                            venueName.substring(0, venueName.indexOf(venueNameSplitChar)).trim());
                                } else {
                                    LOG.error("Unknown venue name type" + venueName);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Work out whether a string value has PIP in it
     * e.g.Northampton - 03 - PIP/DLA
     * @param cellWithPip String from spreadsheet that describes venue
     * @return
     */
    public boolean hasPip(String cellWithPip) {
        return cellWithPip.contains("PIP");
    }

    /**
     * Read in csv file with mapping values between AIRLookup and GAPS.
     * @param classPathResource the csv file on classpath
     * @throws IOException errors from reading in file
     */
    public void readVenueCsv(ClassPathResource classPathResource) throws IOException {

        try (CSVReader reader = new CSVReader(new InputStreamReader(classPathResource.getInputStream()))) {

            //read the headers in
            reader.readNext();

            List<String[]> linesList = reader.readAll();
            linesList.forEach(line ->
                    lookupVenueIdByAirVenueName.put(line[0], Integer.parseInt(line[1]))
            );
        }
    }

    /**
     * Get the map with the first half of post code as the key and
     * the Regional Centre as the value.
     * @return
     */
    protected Map<String, String> getLookupRegionalCentreByPostCode() {
        return lookupRegionalCentreByPostCode;
    }

    /**
     * Get the map with the first half of the post code as the key
     * and the venue name as the value.
     * @return
     */
    protected Map<String, String> getLookupAirVenueNameByPostCode() {
        return lookupAirVenueNameByPostCode;
    }

    /**
     * Get the map with the air venue name as the key and the venue id
     * as the value.
     * @return
     */
    protected Map<String, Integer> getLookupVenueIdByAirVenueName() {
        return lookupVenueIdByAirVenueName;
    }

    /**
     * Return the venue name in the AirLookup spreadsheet for the given post code.
     * @param postcode The first half of a post code
     * @return
     */
    protected String lookupAirVenueNameByPostCode(String postcode) {
        String value = getLookupAirVenueNameByPostCode().get(postcode.toLowerCase());
        if (value == null) {
            return DEFAULT_VENUE;
        }
        return value;
    }

    /**
     * Method to find a venue ID from the postcode.
     * @param postcode first half of post code
     * @return
     */
    public int lookupVenueId(String postcode) {
        String venueName = lookupAirVenueNameByPostCode(postcode);
        Integer venueId = getLookupVenueIdByAirVenueName().get(venueName);
        if (venueId == null) {
            return DEFAULT_VENUE_ID;
        }
        return venueId;
    }
}
