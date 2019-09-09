package uk.gov.hmcts.reform.sscs.docassembly;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.sscs.functional.ccd.CreateCaseInCcdTest;

@RunWith(SpringRunner.class)
@TestPropertySource(locations = "classpath:config/application_e2e.properties")
@ContextConfiguration(initializers = CreateCaseInCcdTest.Initializer.class)
@SpringBootTest
public class GenerateFileTest {

    @Autowired
    private GenerateFile generateFile;

    @Test
    public void canUseDocAssembly() {

        String response = generateFile.assemble();

        System.out.println(response);
        assertNotNull(response);
    }
}
