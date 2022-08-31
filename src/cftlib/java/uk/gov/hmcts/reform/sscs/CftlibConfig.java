package uk.gov.hmcts.reform.sscs;

import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.rse.ccd.lib.api.CFTLib;
import uk.gov.hmcts.rse.ccd.lib.api.CFTLibConfigurer;

@Component
public class CftlibConfig implements CFTLibConfigurer {

    @Override
    public void configure(CFTLib lib) throws Exception {
        lib.createIdamUser("system.update@hmcts.net",
            "caseworker",
            "caseworker-sscs",
            "caseworker-sscs-superuser",
            "caseworker-sscs-clerk",
            "caseworker-sscs-systemupdate",
            "caseworker-sscs-judge",
            "caseworker-sscs-dwpresponsewriter",
            "caseworker-sscs-registrar"
        );
        lib.createIdamUser("local.test@example.com",
            "caseworker",
            "caseworker-sscs"
        );
        lib.createIdamUser("super-user@example.com",
            "caseworker",
            "caseworker-sscs",
            "caseworker-sscs-superuser"
        );
        lib.createIdamUser("sscs-citizen@hmcts.net",
            "citizen"
        );
        lib.createIdamUser("judge@example.com",
            "caseworker",
            "caseworker-sscs",
            "caseworker-sscs-judge"
        );
        lib.createIdamUser("clerk@example.com",
            "caseworker",
            "caseworker-sscs",
            "caseworker-sscs-clerk"
        );
        lib.createIdamUser("registrar@example.com",
            "caseworker",
            "caseworker-sscs",
            "caseworker-sscs-registrar"
        );
        lib.createIdamUser("dwpuser@example.com",
            "caseworker",
            "caseworker-sscs",
            "caseworker-sscs-dwpresponsewriter"
        );
        lib.createIdamUser("hmrcuser@example.com",
            "caseworker",
            "caseworker-sscs",
            "caseworker-sscs-hmrcresponsewriter"
        );
        lib.createRoles(
            "caseworker-sscs-superuser",
            "caseworker-sscs-clerk",
            "caseworker-sscs-systemupdate",
            "caseworker-sscs-judge",
            "caseworker-sscs-dwpresponsewriter",
            "caseworker-sscs-hmrcresponsewriter",
            "caseworker-sscs-registrar",
            "caseworker-sscs-callagent",
            "caseworker-sscs-teamleader",
            "caseworker-sscs-panelmember",
            "caseworker-sscs-bulkscan",
            "caseworker-sscs-anonymouscitizen",
            "caseworker-sscs-pcqextractor",
            "citizen",
            "caseworker-sscs"
        );
        var def = Files.readAllBytes(Path.of("../sscs-ccd-definitions/releases/CCD_SSCSDefinition_vdev_LOCAL.xlsx"));
        lib.importDefinition(def);
    }
}
