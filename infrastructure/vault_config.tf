resource "azurerm_key_vault_secret" "idam-url" {
  name      = "idam-url"
  value     = "${data.vault_generic_secret.idam_api.data["value"]}"
  vault_uri = "${module.sscs-tribunals-case-api-vault.key_vault_uri}"
}

resource "azurerm_key_vault_secret" "s2s-api" {
  name      = "s2s-api"
  value     = "${local.s2sCnpUrl}"
  vault_uri = "${module.sscs-tribunals-case-api-vault.key_vault_uri}"
}

resource "azurerm_key_vault_secret" "s2s-auth" {
  name      = "s2s-auth"
  value     = "${data.vault_generic_secret.sscs_s2s_secret.data["value"]}"
  vault_uri = "${module.sscs-tribunals-case-api-vault.key_vault_uri}"
}

resource "azurerm_key_vault_secret" "sysupdate-user" {
  name      = "sysupdate-user"
  value     = "${data.vault_generic_secret.idam_sscs_systemupdate_user.data["value"]}"
  vault_uri = "${module.sscs-tribunals-case-api-vault.key_vault_uri}"
}

resource "azurerm_key_vault_secret" "sysupdate-pass" {
  name      = "sysupdate-pass"
  value     = "${data.vault_generic_secret.idam_sscs_systemupdate_password.data["value"]}"
  vault_uri = "${module.sscs-tribunals-case-api-vault.key_vault_uri}"
}

resource "azurerm_key_vault_secret" "mac-secret" {
  name      = "mac-secret"
  value     = "${data.vault_generic_secret.email_mac_secret.data["value"]}"
  vault_uri = "${module.sscs-tribunals-case-api-vault.key_vault_uri}"
}

resource "azurerm_key_vault_secret" "idam-oauth-secret" {
  name      = "idam-oauth-secret"
  value     = "${data.vault_generic_secret.idam_oauth2_client_secret.data["value"]}"
  vault_uri = "${module.sscs-tribunals-case-api-vault.key_vault_uri}"
}

resource "azurerm_key_vault_secret" "idam-oauth-user" {
  name      = "idam-oauth-user"
  value     = "${var.idam_oauth2_client_id}"
  vault_uri = "${module.sscs-tribunals-case-api-vault.key_vault_uri}"
}

resource "azurerm_key_vault_secret" "ccd-api" {
  name      = "ccd-api"
  value     = "${local.ccdApi}"
  vault_uri = "${module.sscs-tribunals-case-api-vault.key_vault_uri}"
}

resource "azurerm_key_vault_secret" "ccd-jid" {
  name      = "ccd-jid"
  value     = "${var.core_case_data_jurisdiction_id}"
  vault_uri = "${module.sscs-tribunals-case-api-vault.key_vault_uri}"
}

resource "azurerm_key_vault_secret" "ccd-tid" {
  name      = "ccd-tid"
  value     = "${var.core_case_data_case_type_id}"
  vault_uri = "${module.sscs-tribunals-case-api-vault.key_vault_uri}"
}

resource "azurerm_key_vault_secret" "s2s-micro" {
  name      = "s2s-micro"
  value     = "${var.idam_s2s_auth_microservice}"
  vault_uri = "${module.sscs-tribunals-case-api-vault.key_vault_uri}"
}

resource "azurerm_key_vault_secret" "idam-redirect" {
  name      = "idam-redirect"
  value     = "${var.idam_redirect_url}"
  vault_uri = "${module.sscs-tribunals-case-api-vault.key_vault_uri}"
}