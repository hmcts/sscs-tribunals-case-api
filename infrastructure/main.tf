provider "azurerm" {
  version = "1.19.0"
}

# Make sure the resource group exists
resource "azurerm_resource_group" "rg" {
  name     = "${var.product}-${var.component}-${var.env}"
  location = "${var.location_app}"
}

locals {
  ase_name  = "core-compute-${var.env}"
  local_env = "${(var.env == "preview" || var.env == "spreview") ? (var.env == "preview" ) ? "aat" : "saat" : var.env}"

  s2sCnpUrl   = "http://rpe-service-auth-provider-${local.local_env}.service.core-compute-${local.local_env}.internal"
  ccdApi = "http://ccd-data-store-api-${local.local_env}.service.core-compute-${local.local_env}.internal"
  documentStore = "http://dm-store-${local.local_env}.service.core-compute-${local.local_env}.internal"
  pdfService    = "http://cmc-pdf-service-${local.local_env}.service.${local.local_env}.internal"

  vaultName = "sscs-${local.local_env}"

  # URI of vault that stores long-term secrets. It's the app's own Key Vault, except for (s)preview,
  # where vaults are short-lived and can only store secrets generated during deployment
  preview_vault_name = "https://${var.raw_product}-aat.vault.azure.net/"
  permanent_vault_uri = "${var.env != "preview" ? module.sscs-tribunals-api-vault.key_vault_uri : local.preview_vault_name}"
}

module "tribunals-case-api" {
  source              = "git@github.com:hmcts/moj-module-webapp?ref=master"
  product             = "${var.product}-${var.component}"
  location            = "${var.location_app}"
  env                 = "${var.env}"
  ilbIp               = "${var.ilbIp}"
  resource_group_name = "${azurerm_resource_group.rg.name}"
  subscription        = "${var.subscription}"
  capacity            = "${var.capacity}"
  common_tags         = "${var.common_tags}"

  app_settings = {
    IDAM_API_URL = "${var.idam_url}"

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

module "sscs-tribunals-api-vault" {
  source                  = "git@github.com:hmcts/moj-module-key-vault?ref=master"
  name                    = "${local.vaultName}"
  product                 = "${var.product}"
  env                     = "${var.env}"
  tenant_id               = "${var.tenant_id}"
  object_id               = "${var.jenkins_AAD_objectId}"
  resource_group_name     = "${azurerm_resource_group.rg.name}"
  product_group_object_id = "70de400b-4f47-4f25-a4f0-45e1ee4e4ae3"
}

data "azurerm_key_vault_secret" "email_mac_secret" {
  name      = "sscs-email-mac-secret-text"
  vault_uri = "${local.permanent_vault_uri}"
}

data "azurerm_key_vault_secret" "idam_api" {
  name      = "idam-api"
  vault_uri = "${local.permanent_vault_uri}"
}

data "azurerm_key_vault_secret" "idam_s2s_api" {
  name      = "idam-s2s-api"
  vault_uri = "${local.permanent_vault_uri}"
}

data "azurerm_key_vault_secret" "appeal_email_from" {
  name      = "appeal-email-from"
  vault_uri = "${local.permanent_vault_uri}"
}

data "azurerm_key_vault_secret" "appeal_email_to" {
  name      = "appeal-email-to"
  vault_uri = "${local.permanent_vault_uri}"
}

data "azurerm_key_vault_secret" "robotics_email_from" {
  name      = "robotics-email-from"
  vault_uri = "${local.permanent_vault_uri}"
}

data "azurerm_key_vault_secret" "robotics_email_to" {
  name      = "robotics-email-to"
  vault_uri = "${local.permanent_vault_uri}"
}

data "azurerm_key_vault_secret" "smtp_host" {
  name      = "smtp-host"
  vault_uri = "${local.permanent_vault_uri}"
}

data "azurerm_key_vault_secret" "smtp_port" {
  name      = "smtp-port"
  vault_uri = "${local.permanent_vault_uri}"
}

data "azurerm_key_vault_secret" "sscs_s2s_secret" {
  name      = "sscs-s2s-secret"
  vault_uri = "${local.permanent_vault_uri}"
}

data "azurerm_key_vault_secret" "idam_sscs_systemupdate_user" {
  name      = "idam-sscs-systemupdate-user"
  vault_uri = "${local.permanent_vault_uri}"
}

data "azurerm_key_vault_secret" "idam_sscs_systemupdate_password" {
  name      = "idam-sscs-systemupdate-password"
  vault_uri = "${local.permanent_vault_uri}"
}

data "azurerm_key_vault_secret" "idam_oauth2_client_secret" {
  name      = "idam-sscs-oauth2-client-secret"
  vault_uri = "${local.permanent_vault_uri}"
}



