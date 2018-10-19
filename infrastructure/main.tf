provider "vault" {
  //  # It is strongly recommended to configure this provider through the
  //  # environment variables described above, so that each user can have
  //  # separate credentials set in the environment.
  //  #
  //  # This will default to using $VAULT_ADDR
  //  # But can be set explicitly
  address = "https://vault.reform.hmcts.net:6200"
}

resource "azurerm_resource_group" "rg" {
  name     = "${var.product}-${var.component}-${var.env}"
  location = "${var.location}"

  tags = "${merge(var.common_tags,
    map("lastUpdated", "${timestamp()}")
    )}"
}

data "azurerm_key_vault" "sscs_key_vault" {
  name                = "${local.azureVaultName}"
  resource_group_name = "${local.azureVaultName}"
}

data "azurerm_key_vault_secret" "email_mac_secret" {
  name      = "sscs-email-mac-secret-text"
  vault_uri = "${data.azurerm_key_vault.sscs_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "idam_api" {
  name      = "idam-api"
  vault_uri = "${data.azurerm_key_vault.sscs_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "idam_s2s_api" {
  name      = "idam-s2s-api"
  vault_uri = "${data.azurerm_key_vault.sscs_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "appeal_email_from" {
  name      = "appeal-email-from"
  vault_uri = "${data.azurerm_key_vault.sscs_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "appeal_email_to" {
  name      = "appeal-email-to"
  vault_uri = "${data.azurerm_key_vault.sscs_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "robotics_email_from" {
  name      = "robotics-email-from"
  vault_uri = "${data.azurerm_key_vault.sscs_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "robotics_email_to" {
  name      = "robotics-email-to"
  vault_uri = "${data.azurerm_key_vault.sscs_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "smtp_host" {
  name      = "smtp-host"
  vault_uri = "${data.azurerm_key_vault.sscs_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "smtp_port" {
  name      = "smtp-port"
  vault_uri = "${data.azurerm_key_vault.sscs_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "sscs_s2s_secret" {
  name      = "sscs-s2s-secret"
  vault_uri = "${data.azurerm_key_vault.sscs_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "idam_sscs_systemupdate_user" {
  name      = "idam-sscs-systemupdate-user"
  vault_uri = "${data.azurerm_key_vault.sscs_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "idam_sscs_systemupdate_password" {
  name      = "idam-sscs-systemupdate-password"
  vault_uri = "${data.azurerm_key_vault.sscs_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "idam_oauth2_client_secret" {
  name      = "idam-sscs-oauth2-client-secret"
  vault_uri = "${data.azurerm_key_vault.sscs_key_vault.vault_uri}"
}

locals {
  aseName       = "${data.terraform_remote_state.core_apps_compute.ase_name[0]}"
  app_full_name = "${var.product}-${var.component}"

  local_env = "${(var.env == "preview" || var.env == "spreview") ? (var.env == "preview" ) ? "aat" : "saat" : var.env}"
  local_ase = "${(var.env == "preview" || var.env == "spreview") ? (var.env == "preview" ) ? "core-compute-aat" : "core-compute-saat" : local.aseName}"

  ccdApi        = "http://ccd-data-store-api-${local.local_env}.service.${local.local_ase}.internal"
  s2sCnpUrl     = "http://rpe-service-auth-provider-${local.local_env}.service.${local.local_ase}.internal"
  pdfService    = "http://cmc-pdf-service-${local.local_env}.service.${local.local_ase}.internal"
  documentStore = "http://dm-store-${local.local_env}.service.${local.local_ase}.internal"

  azureVaultName = "sscs-${local.local_env}"

  shared_app_service_plan     = "${var.product}-${var.env}"
  non_shared_app_service_plan = "${var.product}-${var.component}-${var.env}"
  app_service_plan            = "${(var.env == "saat" || var.env == "sandbox") ?  local.shared_app_service_plan : local.non_shared_app_service_plan}"
}

module "tribunals-case-api" {
  source       = "git@github.com:hmcts/moj-module-webapp.git?ref=master"
  product      = "${local.app_full_name}"
  location     = "${var.location}"
  env          = "${var.env}"
  ilbIp        = "${var.ilbIp}"
  is_frontend  = false
  subscription = "${var.subscription}"
  capacity     = "${(var.env == "preview") ? 1 : 2}"
  common_tags  = "${var.common_tags}"
  asp_rg       = "${local.app_service_plan}"
  asp_name     = "${local.app_service_plan}"

  app_settings = {
    AUTH_PROVIDER_SERVICE_API_URL = "${local.s2sCnpUrl}"

    IDAM_API_URL = "${data.azurerm_key_vault_secret.idam_api.value}"

    CCD_SERVICE_API_URL = "${local.ccdApi}"

    EMAIL_FROM    = "${data.azurerm_key_vault_secret.appeal_email_from.value}"
    EMAIL_TO      = "${data.azurerm_key_vault_secret.appeal_email_to.value}"
    EMAIL_SUBJECT = "${var.appeal_email_subject}"
    EMAIL_MESSAGE = "${var.appeal_email_message}"

    ROBOTICS_EMAIL_FROM    = "${data.azurerm_key_vault_secret.robotics_email_from.value}"
    ROBOTICS_EMAIL_TO      = "${data.azurerm_key_vault_secret.robotics_email_to.value}"
    ROBOTICS_EMAIL_SUBJECT = "${var.robotics_email_subject}"
    ROBOTICS_EMAIL_MESSAGE = "${var.robotics_email_message}"

    EMAIL_SERVER_HOST      = "${data.azurerm_key_vault_secret.smtp_host.value}"
    EMAIL_SERVER_PORT      = "${data.azurerm_key_vault_secret.smtp_port.value}"
    EMAIL_SMTP_TLS_ENABLED = "${var.appeal_email_smtp_tls_enabled}"
    EMAIL_SMTP_SSL_TRUST   = "${var.appeal_email_smtp_ssl_trust}"

    PDF_API_URL = "${local.pdfService}"

    SUBSCRIPTIONS_MAC_SECRET = "${data.azurerm_key_vault_secret.email_mac_secret.value}"

    CORE_CASE_DATA_API_URL         = "${local.ccdApi}"
    CORE_CASE_DATA_JURISDICTION_ID = "${var.core_case_data_jurisdiction_id}"
    CORE_CASE_DATA_CASE_TYPE_ID    = "${var.core_case_data_case_type_id}"

    IDAM.S2S-AUTH.TOTP_SECRET  = "${data.azurerm_key_vault_secret.sscs_s2s_secret.value}"
    IDAM.S2S-AUTH              = "${local.s2sCnpUrl}"
    IDAM.S2S-AUTH.MICROSERVICE = "${var.ccd_idam_s2s_auth_microservice}"

    IDAM_SSCS_SYSTEMUPDATE_USER     = "${data.azurerm_key_vault_secret.idam_sscs_systemupdate_user.value}"
    IDAM_SSCS_SYSTEMUPDATE_PASSWORD = "${data.azurerm_key_vault_secret.idam_sscs_systemupdate_password.value}"

    IDAM_OAUTH2_CLIENT_ID     = "${var.idam_oauth2_client_id}"
    IDAM_OAUTH2_CLIENT_SECRET = "${data.azurerm_key_vault_secret.idam_oauth2_client_secret.value}"
    IDAM_OAUTH2_REDIRECT_URL  = "${var.idam_redirect_url}"

    DOCUMENT_MANAGEMENT_URL = "${local.documentStore}"

    MAX_FILE_SIZE    = "${var.max_file_size}"
    MAX_REQUEST_SIZE = "${var.max_request_size}"
  }
}
