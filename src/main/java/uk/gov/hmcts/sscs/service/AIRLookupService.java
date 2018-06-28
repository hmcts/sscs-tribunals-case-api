package uk.gov.hmcts.sscs.service;

import com.sun.org.apache.regexp.internal.RE;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.NPOIFSFileSystem;
import org.apache.poi.ss.usermodel.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import static org.slf4j.LoggerFactory.getLogger;
import org.slf4j.Logger;
import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Service that ingests a spreadsheet of data containing the
 * venue and regional centres for handling all post codes
 */

@Service
public class AIRLookupService {
    private static final Logger LOG = getLogger(CcdService.class);
    private Map<String, String> lookupData;


    static private int POSTCODE_COLUMN = 1;
    static private int REGIONAL_CENTRE_COLUMN = 3;

    public String lookupRegionalCentre(String postcode) {
        return lookupData.get(postcode.toLowerCase());
    }

    @PostConstruct
    public void init() {
        lookupData = new HashMap<>();

        try {
            ClassPathResource classPathResource = new ClassPathResource("reference-data/AIRLookup RC.xls");

            parseSpreadSheet(classPathResource.getFile());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    /**
     * Read in spreadsheet and populate the Map with postcode
     * @param file
     * @throws IOException
     */
    private void parseSpreadSheet(File file) throws IOException{
        try (NPOIFSFileSystem fs = new NPOIFSFileSystem(file)) {
            HSSFWorkbook wb = new HSSFWorkbook(fs.getRoot(), true);

            for (Sheet sheet: wb){
                if (sheet.getSheetName().equals("AIR")) {
                    for (Row row : sheet) {
                        Cell postcodeCell = row.getCell(POSTCODE_COLUMN);
                        Cell adminGroupCell = row.getCell(REGIONAL_CENTRE_COLUMN);
                        if (postcodeCell != null && adminGroupCell != null &&
                                postcodeCell.getCellTypeEnum() == CellType.STRING && adminGroupCell.getCellTypeEnum() == CellType.STRING) {
                            lookupData.put(postcodeCell.getRichStringCellValue().getString().toLowerCase(), adminGroupCell.getRichStringCellValue().getString());
                        }
                    }
                }
            }
        }
    }
}
