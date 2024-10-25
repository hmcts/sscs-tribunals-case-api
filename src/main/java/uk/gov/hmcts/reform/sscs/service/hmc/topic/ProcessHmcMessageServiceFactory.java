package uk.gov.hmcts.reform.sscs.service.hmc.topic;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ProcessHmcMessageServiceFactory {

    private final Map<Boolean, ProcessHmcMessageService> processHmcMessageServiceMap;

    @Autowired
    public ProcessHmcMessageServiceFactory(List<ProcessHmcMessageService> processHmcMessageServices) {
        this.processHmcMessageServiceMap = processHmcMessageServices.stream().collect(Collectors
                .toMap(ProcessHmcMessageService::isProcessEventMessageV2Enabled, Function.identity()));
    }

    public ProcessHmcMessageService getProcessHmcMessageService(Boolean isProcessEventMessageV2Enabled) {
        return processHmcMessageServiceMap.get(isProcessEventMessageV2Enabled);
    }

}
