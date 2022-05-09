module "sscs-hearings-event-queue" {
  source                = "git@github.com:hmcts/terraform-module-servicebus-queue?ref=SSCS-10510-add-flag-to-enable-queue-sessions"
  name                  = "tribunals-to-hearing-api-${var.env}"
  namespace_name        = "sscs-servicebus-${var.env}"
  resource_group_name   = "sscs-${var.env}"
  requires_session      = true
}
