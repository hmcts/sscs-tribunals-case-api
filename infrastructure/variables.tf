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

variable "appeal_email_subject" {
  type    = "string"
  default = "Your appeal"
}

variable "appeal_email_message" {
  type    = "string"
  default = "Your appeal has been created. Please do not respond to this email"
}

variable "appeal_email_smtp_tls_enabled" {
  type    = "string"
  default = "true"
}

variable "appeal_email_smtp_ssl_trust" {
  type    = "string"
  default = "*"
}

variable "idam_s2s_auth_microservice" {
  type    = "string"
  default = "cmc"
}
