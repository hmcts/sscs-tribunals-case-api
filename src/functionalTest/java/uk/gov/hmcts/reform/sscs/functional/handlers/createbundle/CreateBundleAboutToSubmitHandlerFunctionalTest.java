//package uk.gov.hmcts.reform.sscs.functional.handlers.createbundle;
//
//import static java.util.concurrent.TimeUnit.SECONDS;
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.awaitility.Awaitility.await;
//
//import lombok.extern.slf4j.Slf4j;
//import org.junit.runner.RunWith;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.TestPropertySource;
//import org.springframework.test.context.junit4.SpringRunner;
//import uk.gov.hmcts.reform.sscs.ccd.domain.Bundle;
//import uk.gov.hmcts.reform.sscs.ccd.domain.BundleDetails;
//import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
//import uk.gov.hmcts.reform.sscs.functional.handlers.BaseHandler;
//
//
//@RunWith(SpringRunner.class)
//@TestPropertySource(locations = "classpath:config/application_functional.properties")
//@SpringBootTest
//@Slf4j
//public class CreateBundleAboutToSubmitHandlerFunctionalTest extends BaseHandler {
//
//
//
//    private void assertThatBundleStitchedSuccessfully(long caseId, int expectedBundles) {
//        await()
//            .atMost(30, SECONDS)
//            .pollInterval(1, SECONDS)
//            .untilAsserted(() -> {
//                SscsCaseDetails updatedCaseDetails = getByCaseId(caseId);
//                assertThat(updatedCaseDetails.getData().getCaseBundles())
//                    .isNotEmpty()
//                    .extracting(Bundle::getValue)
//                    .extracting(BundleDetails::getStitchStatus)
//                    .doesNotContainNull()
//                    .doesNotContain("NEW");
//            });
//
//        SscsCaseDetails updatedCaseDetails = getByCaseId(caseId);
//
//        assertThat(updatedCaseDetails.getData().getCaseBundles())
//            .hasSize(expectedBundles)
//            .extracting(Bundle::getValue)
//            .allSatisfy(bundleDetails -> {
//                assertThat(bundleDetails.getStitchedDocument()).isNotNull();
//                assertThat(bundleDetails.getStitchStatus()).isEqualTo("DONE");
//            });
//    }
//}
