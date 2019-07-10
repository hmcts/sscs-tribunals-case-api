output "vaultUri" {
  value = "${data.azurerm_key_vault.sscs_key_vault.vault_uri}"
}

output "vaultName" {
  value = "${local.azureVaultName}"
}

output "sscs-output" {
  value = "sscs-output"
}

output "issue_further_evidence_enabled" {
  value = "${var.issue_further_evidence_enabled}"
}
