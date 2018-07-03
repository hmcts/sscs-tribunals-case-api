package uk.gov.hmcts.sscs.service;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;
import java.util.*;
import javax.annotation.PostConstruct;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.NPOIFSFileSystem;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * Service that ingests a spreadsheet of data containing the
 * venue and regional centres for handling all post codes.
 */

@Service
public class AirLookupService {
    public String lookupRegionalCentre(String postcode) {
        return lookupData.get(postcode.toLowerCase());
    }

    private static final Logger LOG = getLogger(CcdService.class);
    private static int POSTCODE_COLUMN = 1;
    private static int REGIONAL_CENTRE_COLUMN = 3;

    private Map<String, String> lookupData;

    @PostConstruct
    public void init() {
        lookupData = new HashMap<>();

        try {
            ClassPathResource classPathResource = new ClassPathResource("reference-data/AIRLookup RC.xls");

            LOG.debug("classPathResource.isFile() " + classPathResource.isFile());

            parseSpreadSheet(classPathResource.getFile());

            LOG.debug("Successfully loaded lookup data for postcode endpoint");
        } catch (IOException e) {
            LOG.error("Unable to read in spreadsheet with post code data: reference-data/AIRLookup RC.xls", e);
        }
    }

    /**
     * Read in spreadsheet and populate the Map with postcode.
     * @param file The file containing the spreadsheet
     * @throws IOException pass up any IO errors
     */
    private void parseSpreadSheet(File file) throws IOException {
        try (NPOIFSFileSystem fs = new NPOIFSFileSystem(file);
             HSSFWorkbook wb = new HSSFWorkbook(fs.getRoot(), true)) {

            for (Sheet sheet: wb) {
                if (sheet.getSheetName().equals("AIR")) {
                    for (Row row : sheet) {
                        Cell postcodeCell = row.getCell(POSTCODE_COLUMN);
                        Cell adminGroupCell = row.getCell(REGIONAL_CENTRE_COLUMN);
                        if (postcodeCell != null && adminGroupCell != null
                                && postcodeCell.getCellTypeEnum() == CellType.STRING && adminGroupCell.getCellTypeEnum() == CellType.STRING) {
                            LOG.debug("Post code: " + postcodeCell.getRichStringCellValue().getString().toLowerCase()
                                + " Regional office: " + adminGroupCell.getRichStringCellValue().getString());
                            lookupData.put(postcodeCell.getRichStringCellValue().getString().toLowerCase(), adminGroupCell.getRichStringCellValue().getString());
                        }
                    }
                }
            }
        }
    }
}
