variable "product" {
  type = "string"
}

variable "component" {
  type = "string"
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
variable "ccd_idam_s2s_auth_microservice" {
  default = "sscs"
}

variable "idam_redirect_url" {
  default = "https://sscs-case-loader-sandbox.service.core-compute-sandbox.internal"
}
variable "common_tags" {
  type = "map"
}

variable "issue_further_evidence_enabled" {
  type    = "string"
  default = "false"
}
