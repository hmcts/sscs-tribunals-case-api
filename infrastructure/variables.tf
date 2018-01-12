variable "product" {
  type    = "string"
  default = "sscs-tribunals"
}

variable "location" {
  type    = "string"
  default = "UK South"
}

variable "env" {
  type = "string"
}

variable "infrastructure_env" {
  default     = "dev"
  description = "Infrastructure environment to point to"
}

variable "tca_server_port" {}

variable "sscs_idam_key" {}

variable "authprovider_service_api_url" {}

variable "idam_api_url" {}

variable "idam_user_id" {}

variable "idam_role" {}

variable "ccd_case_worker_id" {}

variable "ccd_service_api_url" {}

variable "appeal_email_from" {}

variable "appeal_email_to" {}

variable "appeal_email_subject" {}

variable "appeal_email_message" {}

variable "appeal_email_host" {}

variable "appeal_email_port" {}

variable "appeal_email_smtp_tls_enabled" {}

variable "appeal_email_smtp_ssl_trust" {}

variable "idam_s2s_auth_totp_secret" {}

variable "idam_s2s_auth_microservice" {}

variable "idam_s2s_auth_url" {}

variable "pdf_api_url" {}
