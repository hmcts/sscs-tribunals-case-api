package uk.gov.hmcts.sscs.hld;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.hmcts.befta.BeftaMain;
import uk.gov.hmcts.befta.dse.ccd.CcdEnvironment;
import uk.gov.hmcts.befta.dse.ccd.CcdRoleConfig;
import uk.gov.hmcts.befta.dse.ccd.DataLoaderToDefinitionStore;
import uk.gov.hmcts.befta.exception.ImportException;
import uk.gov.hmcts.befta.util.BeftaUtils;

import javax.crypto.AEADBadTagException;
import javax.net.ssl.SSLException;
import java.util.List;
import java.util.Locale;

public class HighLevelDataSetupApp extends DataLoaderToDefinitionStore {

    private static final Logger logger = LoggerFactory.getLogger(HighLevelDataSetupApp.class);

    private static final CcdRoleConfig[] CCD_ROLES = {
        new CcdRoleConfig("citizen", "PUBLIC"),
        new CcdRoleConfig("caseworker", "PUBLIC"),
        new CcdRoleConfig("caseworker-sscs", "PUBLIC"),
        new CcdRoleConfig("caseworker-sscs-systemupdate", "PUBLIC"),
        new CcdRoleConfig("caseworker-sscs-anonymouscitizen", "PUBLIC"),
        new CcdRoleConfig("caseworker-sscs-callagent", "PUBLIC"),
        new CcdRoleConfig("caseworker-sscs-judge", "PUBLIC"),
        new CcdRoleConfig("caseworker-sscs-judge-salaried", "PUBLIC"),
        new CcdRoleConfig("caseworker-sscs-clerk", "PUBLIC"),
        new CcdRoleConfig("caseworker-sscs-dwpresponsewriter", "PUBLIC"),
        new CcdRoleConfig("caseworker-sscs-registrar", "PUBLIC"),
        new CcdRoleConfig("caseworker-sscs-superuser", "PUBLIC"),
        new CcdRoleConfig("caseworker-sscs-teamleader", "PUBLIC"),
        new CcdRoleConfig("caseworker-sscs-panelmember", "PUBLIC"),
        new CcdRoleConfig("caseworker-sscs-bulkscan", "PUBLIC"),
        new CcdRoleConfig("caseworker-sscs-pcqextractor", "PUBLIC"),
        new CcdRoleConfig("caseworker-sscs-hmrcresponsewriter", "PUBLIC"),
        new CcdRoleConfig("caseworker-sscs-ibcaresponsewriter", "PUBLIC"),
        new CcdRoleConfig("caseworker-wa-task-configuration", "PUBLIC"),
        new CcdRoleConfig("caseworker-ras-validation", "PUBLIC"),
        new CcdRoleConfig("GS_profile", "PUBLIC")
    };

    private final CcdEnvironment environment;

    public HighLevelDataSetupApp(CcdEnvironment dataSetupEnvironment) {
        super(dataSetupEnvironment);
        environment = dataSetupEnvironment;
    }

    public static void main(String[] args) throws Throwable {
        main(HighLevelDataSetupApp.class, args);
    }

    @Override
    public void addCcdRoles() {
        for (CcdRoleConfig roleConfig : CCD_ROLES) {
            try {
                logger.info("\n\nAdding CCD Role {}.", roleConfig);
                addCcdRole(roleConfig);
                logger.info("\n\nAdded CCD Role {}.", roleConfig);
            } catch (Exception e) {
                logger.error("\n\nCouldn't add CCD Role {} - Exception: {}.\n\n", roleConfig, e);
                if (!shouldTolerateDataSetupFailure()) {
                    throw e;
                }
            }
        }
    }

    @Override
    protected List<String> getAllDefinitionFilesToLoadAt(String definitionsPath) {
        String environmentName = environment.name().toLowerCase(Locale.UK);
        return List.of(
            String.format("definitions/benefit/ccd-release-config/civil-ccd-%s.xlsx", environmentName),
            String.format("definitions/bulk/ccd-release-config/civil-ccd-%s.xlsx", environmentName)
        );
    }

    @Override
    public void createRoleAssignments() {
        // Do not create role assignments.
        BeftaUtils.defaultLog("Will NOT create role assignments!");
    }

    @Override
    protected boolean shouldTolerateDataSetupFailure(){
        if(BeftaMain.getConfig().getDefinitionStoreUrl().contains(".preview.")){
            return true;
        }
        return false;
    }

    @Override
    protected boolean shouldTolerateDataSetupFailure(Throwable e) {
        int httpStatusCode504 = 504;
        if (e instanceof ImportException) {
            ImportException importException = (ImportException) e;
            return importException.getHttpStatusCode() == httpStatusCode504;
        }
        if(e instanceof SSLException){
            return true;
        }
        if(e instanceof AEADBadTagException){
            return true;
        }
        return shouldTolerateDataSetupFailure();
    }
}
