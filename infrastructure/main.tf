provider "azurerm" {
  features {}
}

data "azurerm_key_vault" "sscs_key_vault" {
  name                = local.azureVaultName
  resource_group_name = local.azureVaultName
}

locals {
  azureVaultName = "sscs-${var.env}"
}

data "azurerm_key_vault" "send_grid" {
  provider            = azurerm.send-grid
  name                = var.env != "prod" ? "sendgridnonprod" : "sendgridprod"
  resource_group_name = var.env != "prod" ? "SendGrid-nonprod" : "SendGrid-prod"
}

data "azurerm_key_vault_secret" "send_grid_api_key" {
  provider     = azurerm.send-grid
  key_vault_id = data.azurerm_key_vault.send_grid.id
  name         = "sscs-api-key"
}

resource "azurerm_key_vault_secret" "sendgrid_api_key" {
  key_vault_id = data.azurerm_key_vault.sscs_key_vault.id
  name         = "sscs-sendgrid-api-key"
  value        = data.azurerm_key_vault_secret.send_grid_api_key.value

  content_type = "secret"
  tags = merge(var.common_tags, {
    "source" : "Vault ${data.azurerm_key_vault.sscs_key_vault.name}"
  })
}