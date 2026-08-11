package uk.gov.hmcts.reform.sscs.evidenceshare.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class EmailSenderProvider {

    @Autowired
    @Qualifier("mtaMailSender")
    private final JavaMailSender mtaMailSender;

    public JavaMailSender getMailSender() {
        return  mtaMailSender;
    }
}
