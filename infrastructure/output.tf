output "microserviceName" {
  value = "${var.component}"
}

output "vaultUri" {
  value = "${module.sscs-tribunals-api-vault.key_vault_uri}"
}

output "vaultName" {
  value = "${local.vaultName}"
}
output "sscs-output" {
  value = "sscs-output"
}
