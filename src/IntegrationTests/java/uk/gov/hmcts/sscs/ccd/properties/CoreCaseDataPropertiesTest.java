package uk.gov.hmcts.sscs.ccd.properties;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import uk.gov.hmcts.sscs.ccd.properties.CoreCaseDataProperties;

@RunWith(SpringRunner.class)
@SpringBootTest
public class CoreCaseDataPropertiesTest {

    @Autowired
    private CoreCaseDataProperties coreCaseDataProperties;

    @Test
    public void givenAnInstanceOfCcdProperties_shouldContainCcdValues() {
        assertNotNull(coreCaseDataProperties.getUserId());
        assertNotNull(coreCaseDataProperties.getJurisdictionId());
        assertNotNull(coreCaseDataProperties.getCaseTypeId());
    }
}
