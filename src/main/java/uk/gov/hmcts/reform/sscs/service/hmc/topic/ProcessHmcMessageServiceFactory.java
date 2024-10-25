package uk.gov.hmcts.reform.sscs.service.hmc.topic;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ProcessHmcMessageServiceFactory {

    private final ProcessHmcMessageService processHmcMessageService;

    @Autowired
    public ProcessHmcMessageServiceFactory(List<ProcessHmcMessageService> processHmcMessageServices) {
        this.processHmcMessageService = processHmcMessageServices.stream().findFirst().get();
    }

    public ProcessHmcMessageService getProcessHmcMessageService() {
        return processHmcMessageService;
    }
}
