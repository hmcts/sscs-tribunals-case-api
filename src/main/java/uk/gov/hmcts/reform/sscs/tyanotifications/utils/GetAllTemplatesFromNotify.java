package uk.gov.hmcts.reform.sscs.tyanotifications.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.Template;
import uk.gov.service.notify.TemplateList;


public final class GetAllTemplatesFromNotify {

    public static NotificationClient testNotificationClient() {
        return new NotificationClient(testApiKey);
    }

    private GetAllTemplatesFromNotify() {

    }

    private static final String testApiKey = "";

    public static void main(String[] args) {
        String var = "benefit_name_acronym";
        NotificationClient client = testNotificationClient();
        try {
            //empty is everything, can be email sms or letter
            String type = "";
            TemplateList templates = client.getAllTemplates(type);

            System.out.println("Found " + templates.getTemplates().size() + " templates in total");

            int count = 0;

            for (Template template : templates.getTemplates()) {
                Map<String, Object> personalisation = template.getPersonalisation().orElse(new HashMap<String, Object>());

                if (personalisation.keySet().contains(var)) {
                    if (template.getName().contains("COR") || template.getName().contains("NOT USED")
                        || template.getName().contains("OLD")) {
                        System.out.println("Excluding: " + template.getName());
                        continue;
                    }
                    System.out.println("Name: " + template.getName());
                    System.out.println("ID: " + template.getId());
                    System.out.println("Type: " + template.getTemplateType());
                    Optional subject = template.getSubject();
                    subject.ifPresent(s -> System.out.println("Subject: " + s));
                    System.out.println("Body is: " + template.getBody());
                    count++;
                    continue;
                } else {
                    System.out.println("This template does not contain " + var);
                    System.out.println("Name: " + template.getName());
                    System.out.println("ID: " + template.getId());
                    System.out.println("Type: " + template.getTemplateType());
                    System.out.println("Body is: " + template.getBody());
                }

                System.out.println("************************************************************");
            }
            System.out.println("There were " + count + " templates found with key " + var);
        } catch (NotificationClientException e) {
            e.printStackTrace(System.out);
        }
    }
}
