
output "vaultUri" {
  value = "${module.tribunals-case-api-key-vault.key_vault_uri}"
}

output "vaultName" {
  value = "${local.vaultName}"
}

output "microserviceName" {
  value = "${var.component}"
}

output "sscs-output" {
  value = "sscs-output"
}
