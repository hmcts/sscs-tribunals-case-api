
output "vaultUri" {
  value = "${module.sscs-tribunals-case-api-key-vault.key_vault_uri}"
}

output "vaultName" {
  value = "${local.vaultName}"
}

output "sscs-output" {
  value = "sscs-output"
}
