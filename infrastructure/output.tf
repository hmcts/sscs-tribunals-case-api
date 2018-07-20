output "vaultUri" {
  value = "${module.sscs-tca-key-vault.key_vault_uri}"
}

output "vaultName" {
  value = "${local.vaultName}"
}

output "sscs-output" {
  value = "sscs-output"
}
