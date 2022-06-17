provider "azurerm" {
  features {}
}

data "azurerm_key_vault" "sscs_key_vault" {
  name                = local.azureVaultName
  resource_group_name = local.azureVaultName
}

# locals {     
#   azureVaultName = "sscs-${var.env}"
# }



### FOLLOWING FILES ARE TEMPORARY AND SHOULD BE REMOVED ONCE PERFEST TFSTATE CAN PERFORM A TERRAFORM INIT 
resource "azurerm_resource_group" "rg" {
  name     = "${var.product}-${var.component}-${var.env}"
  location = "${var.location}"

  tags = "${var.common_tags}"
}

# data "azurerm_key_vault" "sscs_key_vault" {
#   name                = "${local.azureVaultName}"
#   resource_group_name = "${local.azureVaultName}"
# }

data "azurerm_key_vault_secret" "email_mac_secret" {
  name      = "sscs-email-mac-secret-text"
  key_vault_id = "${data.azurerm_key_vault.sscs_key_vault.id}"
}

data "azurerm_key_vault_secret" "idam_api" {
  name      = "idam-api"
  key_vault_id = "${data.azurerm_key_vault.sscs_key_vault.id}"
}

data "azurerm_key_vault_secret" "idam_s2s_api" {
  name      = "idam-s2s-api"
  key_vault_id = "${data.azurerm_key_vault.sscs_key_vault.id}"
}

data "azurerm_key_vault_secret" "appeal_email_from" {
  name      = "appeal-email-from"
  key_vault_id = "${data.azurerm_key_vault.sscs_key_vault.id}"
}

data "azurerm_key_vault_secret" "appeal_email_to" {
  name      = "appeal-email-to"
  key_vault_id = "${data.azurerm_key_vault.sscs_key_vault.id}"
}

data "azurerm_key_vault_secret" "robotics_email_from" {
  name      = "robotics-email-from"
  key_vault_id = "${data.azurerm_key_vault.sscs_key_vault.id}"
}

data "azurerm_key_vault_secret" "robotics_email_to" {
  name      = "robotics-email-to"
  key_vault_id = "${data.azurerm_key_vault.sscs_key_vault.id}"
}

data "azurerm_key_vault_secret" "robotics_email_scottish_to" {
  name      = "robotics-email-scottish-to"
  key_vault_id = "${data.azurerm_key_vault.sscs_key_vault.id}"
}

data "azurerm_key_vault_secret" "smtp_host" {
  name      = "smtp-host"
  key_vault_id = "${data.azurerm_key_vault.sscs_key_vault.id}"
}

data "azurerm_key_vault_secret" "smtp_port" {
  name      = "smtp-port"
  key_vault_id = "${data.azurerm_key_vault.sscs_key_vault.id}"
}

data "azurerm_key_vault_secret" "sscs_s2s_secret" {
  name      = "sscs-s2s-secret"
  key_vault_id = "${data.azurerm_key_vault.sscs_key_vault.id}"
}

data "azurerm_key_vault_secret" "idam_sscs_systemupdate_user" {
  name      = "idam-sscs-systemupdate-user"
  key_vault_id = "${data.azurerm_key_vault.sscs_key_vault.id}"
}

data "azurerm_key_vault_secret" "idam_sscs_systemupdate_password" {
  name      = "idam-sscs-systemupdate-password"
  key_vault_id = "${data.azurerm_key_vault.sscs_key_vault.id}"
}

data "azurerm_key_vault_secret" "idam_oauth2_client_secret" {
  name      = "idam-sscs-oauth2-client-secret"
  key_vault_id = "${data.azurerm_key_vault.sscs_key_vault.id}"
}

data "azurerm_key_vault_secret" "pdf_service_base_url" {
  name      = "docmosis-endpoint"
  key_vault_id = "${data.azurerm_key_vault.sscs_key_vault.id}"
}

data "azurerm_key_vault_secret" "pdf_service_access_key" {
  name      = "docmosis-api-key"
  key_vault_id = "${data.azurerm_key_vault.sscs_key_vault.id}"
}

data "azurerm_key_vault_secret" "appinsights_instrumentation_key" {
  name      = "AppInsightsInstrumentationKey"
  key_vault_id = "${data.azurerm_key_vault.sscs_key_vault.id}"
}

locals {
  local_ase = "${data.terraform_remote_state.core_apps_compute.ase_name[0]}"

  ccdApi        = "http://ccd-data-store-api-${var.env}.service.${local.local_ase}.internal"
  s2sCnpUrl     = "http://rpe-service-auth-provider-${var.env}.service.${local.local_ase}.internal"
  pdfService    = "http://cmc-pdf-service-${var.env}.service.${local.local_ase}.internal"
  documentStore = "http://dm-store-${var.env}.service.${local.local_ase}.internal"
  docAssembly   = "http://dg-docassembly-${var.env}.service.${local.local_ase}.internal"

  azureVaultName = "sscs-${var.env}"

  shared_app_service_plan     = "${var.product}-${var.env}"
  non_shared_app_service_plan = "${var.product}-${var.component}-${var.env}"
  app_service_plan            = "${(var.env == "saat" || var.env == "sandbox") ?  local.shared_app_service_plan : local.non_shared_app_service_plan}"
}