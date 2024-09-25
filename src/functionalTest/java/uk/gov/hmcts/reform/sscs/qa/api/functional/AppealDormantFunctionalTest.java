package uk.gov.hmcts.reform.sscs.qa.api.functional;

import helper.EnvironmentProfileValueSource;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import junitparams.JUnitParamsRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.annotation.ProfileValueSourceConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.idam.client.IdamApi;
import uk.gov.hmcts.reform.sscs.TribunalsCaseApiApplication;
import uk.gov.hmcts.reform.sscs.functional.evidenceshare.AbstractFunctionalTest;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {TribunalsCaseApiApplication.class})
@ComponentScan(basePackages = {"uk.gov.hmcts.reform.sscs.idam"})
@EnableFeignClients(basePackageClasses = {IdamApi.class})
public class AppealDormantFunctionalTest {

    @Autowired
    private IdamService idamService;


    @Test
    public void testAppealDormant() throws Exception {
        String value = TestHelper.readFileContents("create-appeal.json");

        IdamTokens idamTokens = idamService.getIdamTokens();
        System.out.println("IDAM Token"+idamTokens.getIdamOauth2Token());
        System.out.println("Service Token"+idamTokens.getServiceAuthorization());
        Map<String, String> headers = Map.of("Content-Type","application/json",
                "ServiceAuthorization","Bearer "+idamTokens.getServiceAuthorization());


        Response response= RestAssured
                //.expect().that().statusCode(expectedHttpStatus.value())
                .given()
                .headers(headers)
                .baseUri("https://sscs-tribunals-api-pr-3764.preview.platform.hmcts.net")
                .body(value)
                .post("/api/appeals").then().extract().response();
        System.out.println("The value of the Response status : " + response.getStatusCode());
        System.out.println("The value of the Response status line : " + response.getStatusLine());

        System.out.println("The value of the Location : "+response.getHeaders().get("Location").getValue().split("/"));
    }
}
