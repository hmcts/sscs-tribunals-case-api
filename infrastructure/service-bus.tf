module "sscs-hearings-event-queue" {
  source              = "git@github.com:hmcts/terraform-module-servicebus-queue?ref=master"
  name                = "tribunals-to-hearing-api-${var.env}"
  namespace_name      = "sscs-servicebus-${var.env}"
  resource_group_name = "sscs-${var.env}"
  requires_session    = true
}

resource "azurerm_key_vault_secret" "servicebus_primary_shared_access_key" {
  name         = "tribunals-servicebus-shared-access-key"
  value        = module.sscs-hearings-event-queue.primary_send_and_listen_shared_access_key
  key_vault_id = module.vault.key_vault_id
}