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

variable "subscription" {
  type = "string"
}

variable "ilbIp"{}

variable "sscs_idam_key" {
  type    = "string"
  default = "idamkey"
}

variable "authprovider_service_api_url" {
  type    = "string"
  default = "http://betaDevBccidamS2SLB.reform.hmcts.net"
}

variable "idam_api_url" {
  type    = "string"
  default = "http://betaDevBccidamAppLB.reform.hmcts.net"
}

variable "idam_user_id" {
  type    = "string"
  default = "352"
}

variable "idam_role" {
  type    = "string"
  default = "caseworker-sscs"
}

variable "ccd_case_worker_id" {
  type    = "string"
  default = "352"
}

variable "ccd_service_api_url" {
  type    = "string"
  default = "https://case-data-app.test.ccd.reform.hmcts.net:4481"
}

variable "appeal_email_from" {
  type    = "string"
  default = "sscs@hmcts.net"
}

variable "appeal_email_to" {
  type    = "string"
  default = "receiver@hmcts.net"
}

variable "appeal_email_subject" {
  type    = "string"
  default = "Your appeal"
}

variable "appeal_email_message" {
  type    = "string"
  default = "Your appeal has been created. Please do not respond to this email"
}

variable "appeal_email_host" {
  type    = "string"
  default = "mta.reform.hmcts.net"
}

variable "appeal_email_port" {
  type    = "string"
  default = "25"
}

variable "appeal_email_smtp_tls_enabled" {
  type    = "string"
  default = "true"
}

variable "appeal_email_smtp_ssl_trust" {
  type    = "string"
  default = "*"
}

variable "idam_s2s_auth_totp_secret" {
  type    = "string"
  default = "secret"
}

variable "idam_s2s_auth_microservice" {
  type    = "string"
  default = "sscsTribunalsCase"
}

variable "idam_s2s_auth_url" {
  type    = "string"
  default = "http://betaDevBccidamS2SLB.reform.hmcts.net"
}

variable "pdf_api_url" {
  type    = "string"
  default = "http://localhost:5500"
}
