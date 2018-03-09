provider "vault" {
  address = "https://vault.reform.hmcts.net:6200"
}

data "vault_generic_secret" "cmc_s2s_secret" {
  path = "secret/${var.infrastructure_env}/ccidam/service-auth-provider/api/microservice-keys/cmc"
}

data "vault_generic_secret" "email_mac_secret" {
  path = "secret/${var.infrastructure_env}/sscs/sscs_email_mac_secret_text"
}

data "vault_generic_secret" "sscs_tribunals_case_secret" {
  path = "secret/${var.infrastructure_env}/ccidam/service-auth-provider/api/microservice-keys/sscs-tribunals-case"
}

data "vault_generic_secret" "idam_api" {
  path = "secret/${var.infrastructure_env}/sscs/idam_api"
}

data "vault_generic_secret" "idam_s2s_api" {
  path = "secret/${var.infrastructure_env}/sscs/idam_s2s_api"
}

data "vault_generic_secret" "idam_uid" {
  path = "secret/${var.infrastructure_env}/sscs/idam_uid"
}

data "vault_generic_secret" "idam_key_sscs" {
  path = "secret/${var.infrastructure_env}/sscs/idam_key_sscs"
}

data "vault_generic_secret" "idam_role" {
  path = "secret/${var.infrastructure_env}/sscs/idam_role"
}

data "vault_generic_secret" "ccd_api" {
  path = "secret/${var.infrastructure_env}/sscs/ccd_api"
}

data "vault_generic_secret" "ccd_case_worker_id" {
  path = "secret/${var.infrastructure_env}/sscs/ccd_case_worker_id"
}

data "vault_generic_secret" "appeal_email_from" {
  path = "secret/${var.infrastructure_env}/sscs/appeal_email_from"
}

data "vault_generic_secret" "appeal_email_to" {
  path = "secret/${var.infrastructure_env}/sscs/appeal_email_to"
}

data "vault_generic_secret" "smtp_host" {
  path = "secret/${var.infrastructure_env}/sscs/smtp_host"
}

data "vault_generic_secret" "smtp_port" {
  path = "secret/${var.infrastructure_env}/sscs/smtp_port"
}

data "vault_generic_secret" "sscs_s2s_secret" {
  path = "secret/${var.infrastructure_env}/ccidam/service-auth-provider/api/microservice-keys/sscs"
}

data "vault_generic_secret" "idam_sscs_systemupdate_user" {
  path = "secret/${var.infrastructure_env}/ccidam/idam-api/sscs/systemupdate/user"
}

data "vault_generic_secret" "idam_sscs_systemupdate_password" {
  path = "secret/${var.infrastructure_env}/ccidam/idam-api/sscs/systemupdate/password"
}

data "vault_generic_secret" "idam_oauth2_client_secret" {
  path = "secret/${var.infrastructure_env}/ccidam/idam-api/oauth2/client-secrets/sscs"
}


locals {
  aseName = "${data.terraform_remote_state.core_apps_compute.ase_name[0]}"
}


module "tribunals-case-api" {
  source       = "git@github.com:contino/moj-module-webapp?ref=master"
  product      = "${var.product}-api"
  location     = "${var.location}"
  env          = "${var.env}"
  ilbIp        = "${var.ilbIp}"
  is_frontend  = false
  subscription = "${var.subscription}"

  app_settings = {
    AUTH_PROVIDER_SERVICE_CLIENT_KEY="${data.vault_generic_secret.sscs_tribunals_case_secret.data["value"]}"
    AUTH_PROVIDER_SERVICE_API_URL="${data.vault_generic_secret.idam_s2s_api.data["value"]}"

    IDAM_API_URL="${data.vault_generic_secret.idam_api.data["value"]}"
    IDAM_USER_ID="${data.vault_generic_secret.idam_uid.data["value"]}"
    IDAM_ROLE="${data.vault_generic_secret.idam_role.data["value"]}"

    CCD_CASE_WORKER_ID="${data.vault_generic_secret.ccd_case_worker_id.data["value"]}"
    CCD_SERVICE_API_URL="http://ccd-data-store-api-${var.env}.service.${local.aseName}.internal"

    EMAIL_FROM="${data.vault_generic_secret.appeal_email_from.data["value"]}"
    EMAIL_TO="${data.vault_generic_secret.appeal_email_to.data["value"]}"
    EMAIL_SUBJECT="${var.appeal_email_subject}"
    EMAIL_MESSAGE="${var.appeal_email_message}"
    EMAIL_SERVER_HOST="${data.vault_generic_secret.smtp_host.data["value"]}"
    EMAIL_SERVER_PORT="${data.vault_generic_secret.smtp_port.data["value"]}"
    EMAIL_SMTP_TLS_ENABLED="${var.appeal_email_smtp_tls_enabled}"
    EMAIL_SMTP_SSL_TRUST="${var.appeal_email_smtp_ssl_trust}"

    IDAM_S2S_AUTH_TOTP_SECRET="${data.vault_generic_secret.cmc_s2s_secret.data["value"]}"
    IDAM_S2S_AUTH_MICROSERVICE="${var.idam_s2s_auth_microservice}"
    IDAM_S2S_AUTH_URL="${data.vault_generic_secret.idam_s2s_api.data["value"]}"

    PDF_API_URL="http://cmc-pdf-service-${var.env}.service.${local.aseName}.internal"

    SUBSCRIPTIONS_MAC_SECRET="${data.vault_generic_secret.email_mac_secret.data["value"]}"

    CORE_CASE_DATA_API_URL = "http://ccd-data-store-api-${var.env}.service.${local.aseName}.internal"
    CORE_CASE_DATA_USER_ID = "${var.core_case_data_user_id}"
    CORE_CASE_DATA_JURISDICTION_ID = "${var.core_case_data_jurisdiction_id}"
    CORE_CASE_DATA_CASE_TYPE_ID = "${var.core_case_data_case_type_id}"
    CORE_CASE_DATA_EVENT_ID = "${var.core_case_data_event_id}"

    IDAM_URL = "${data.vault_generic_secret.idam_api.data["value"]}"

    IDAM.S2S-AUTH.TOTP_SECRET ="${data.vault_generic_secret.sscs_s2s_secret.data["value"]}"
    IDAM.S2S-AUTH = "${data.vault_generic_secret.idam_s2s_api.data["value"]}"
    IDAM.S2S-AUTH.MICROSERVICE = "${var.ccd_idam_s2s_auth_microservice}"

    IDAM_SSCS_SYSTEMUPDATE_USER = "${data.vault_generic_secret.idam_sscs_systemupdate_user.data["value"]}"
    IDAM_SSCS_SYSTEMUPDATE_PASSWORD = "${data.vault_generic_secret.idam_sscs_systemupdate_password.data["value"]}"

    IDAM_OAUTH2_CLIENT_ID = "${var.idam_oauth2_client_id}"
    IDAM_OAUTH2_CLIENT_SECRET = "${data.vault_generic_secret.idam_oauth2_client_secret.data["value"]}"
    IDAM_OAUTH2_REDIRECT_URL = "https://sscs-tribunals-api-${var.env}.service.${local.aseName}.internal"

  }
}
