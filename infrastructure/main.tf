provider "vault" {
  //  # It is strongly recommended to configure this provider through the
  //  # environment variables described above, so that each user can have
  //  # separate credentials set in the environment.
  //  #
  //  # This will default to using $VAULT_ADDR
  //  # But can be set explicitly
  address = "https://vault.reform.hmcts.net:6200"
}

data "vault_generic_secret" "s2s_secret" {
  path = "secret/test/ccidam/service-auth-provider/api/microservice-keys/cmcClaimStore"
}

locals {
  aseName = "${data.terraform_remote_state.core_apps_compute.ase_name[0]}"
}


module "tribunals-case-api" {
  source   = "git@github.com:contino/moj-module-webapp?ref=master"
  product  = "${var.product}-frontend"
  location = "${var.location}"
  env      = "${var.env}"
  ilbIp    = "${var.ilbIp}"

  app_settings = {
    PORT="${var.tca_server_port}"

    AUTH_PROVIDER_SERVICE_CLIENT_KEY="${var.sscs_idam_key}"
    AUTH_PROVIDER_SERVICE_API_URL="${var.authprovider_service_api_url}"

    IDAM_API_URL="${var.idam_api_url}"
    IDAM_USER_ID="${var.idam_user_id}"
    IDAM_ROLE="${var.idam_role}"

    CCD_CASE_WORKER_ID="${var.ccd_case_worker_id}"
    CCD_SERVICE_API_URL="${var.ccd_service_api_url}"

    EMAIL_FROM="${var.appeal_email_from}"
    EMAIL_TO="${var.appeal_email_to}"
    EMAIL_SUBJECT="${var.appeal_email_subject}"
    EMAIL_MESSAGE="${var.appeal_email_message}"
    EMAIL_SERVER_HOST="${var.appeal_email_host}"
    EMAIL_SERVER_PORT="${var.appeal_email_port}"
    EMAIL_SMTP_TLS_ENABLED="${var.appeal_email_smtp_tls_enabled}"
    EMAIL_SMTP_SSL_TRUST="${var.appeal_email_smtp_ssl_trust}"

    IDAM_S2S_AUTH_TOTP_SECRET="${data.vault_generic_secret.s2s_secret.data["value"]}"
    IDAM_S2S_AUTH_MICROSERVICE="${var.idam_s2s_auth_microservice}"
    IDAM_S2S_AUTH_URL="${var.idam_s2s_auth_url}"

    PDF_API_URL="http://cmc-pdf-service-${var.env}.service.${local.aseName}.internal"
  }
}
