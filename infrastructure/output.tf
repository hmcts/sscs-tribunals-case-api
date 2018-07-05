
output "vaultUri" {
  value = "${module.sscs-tca-key-vault.key_vault_uri}"
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
