variable "product" {
  type    = string
  default = "sscs"
}

variable "component" {}

variable "location" {
  default = "UK South"
}

variable "env" {}

variable "infrastructure_env" {
  default     = "dev"
  description = "Infrastructure environment to point to"
}

variable "subscription" {}


variable "tenant_id" {}

variable "jenkins_AAD_objectId" {
  description = "(Required) The Azure AD object ID of a user, service principal or security group in the Azure Active Directory tenant for the vault. The object ID must be unique for the list of access policies."
}

variable "common_tags" {
  type = map(string)
}

variable "appinsights_instrumentation_key" {
  description = "Instrumentation key of the App Insights instance this webapp should use. Module will create own App Insights resource if this is not provided"
  default     = ""
}

variable "send_grid_subscription" {
  default = "1c4f0704-a29e-403d-b719-b90c34ef14c9"
}