package uk.gov.hmcts.reform.sscs.qa.api.functional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import io.restassured.RestAssured;
import org.springframework.util.ResourceUtils;


public class TestHelper {

    private TestHelper() {

    }

    public static String readFileContents(final String path) throws IOException {

        File file = ResourceUtils.getFile("classpath:functional/"+path);
        //File is found
        //System.out.println("File Found : "+file.exists());
        return new String(Files.readAllBytes(Paths.get(file.toURI())));

    }
}