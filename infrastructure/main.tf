resource "azurerm_resource_group" "rg" {
  name     = "${var.product}-${var.component}-${var.env}"
  location = "${var.location}"

  tags = "${var.common_tags}"
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

data "azurerm_key_vault_secret" "robotics_email_scottish_to" {
  name      = "robotics-email-scottish-to"
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
  azureVaultName = "sscs-${var.env}"
}
