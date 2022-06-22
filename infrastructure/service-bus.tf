module "sscs-hearings-event-queue" {
  source              = "git@github.com:hmcts/terraform-module-servicebus-queue?ref=master"
  name                = "tribunals-to-hearing-api-${var.env}"
  namespace_name      = "sscs-servicebus-${var.env}"
  resource_group_name = "sscs-${var.env}"
  requires_session    = true
}

output "sb_primary_send_and_listen_shared_access_key" {
  value     = module.sscs-hearings-event-queue.primary_send_and_listen_shared_access_key
  sensitive = true
}

resource "azurerm_key_vault_secret" "servicebus_primary_shared_access_key" {
  name         = "tribunals-hearing-queue-shared-access-key"
  value        = module.sscs-hearings-event-queue.primary_send_and_listen_shared_access_key
  key_vault_id = data.azurerm_key_vault.sscs_key_vault.id
}