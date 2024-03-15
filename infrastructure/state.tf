terraform {
  backend "azurerm" {}

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 3.68.0"
    }
        random = {
          source = "hashicorp/random"
        }
  }
}

provider "azurerm" {
  alias           = "send-grid"
  subscription_id = var.env != "prod" ? local.sendgrid_subscription.nonprod : local.sendgrid_subscription.prod
  features {}
}
