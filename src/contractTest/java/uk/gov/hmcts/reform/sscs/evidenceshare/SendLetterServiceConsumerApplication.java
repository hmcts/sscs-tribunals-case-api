package uk.gov.hmcts.reform.sscs.evidenceshare;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.idam.client.IdamApi;
import uk.gov.hmcts.reform.sendletter.api.proxy.SendLetterApiProxy;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@EnableFeignClients(clients = {
    SendLetterApiProxy.class
})
public class SendLetterServiceConsumerApplication {
    @MockBean
    RestTemplate restTemplate;

    @MockBean
    IdamApi idamApi;
}
