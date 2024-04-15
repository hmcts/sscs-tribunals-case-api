terraform {
  backend "azurerm" {}

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 3.68.0"
    }
  }
}

provider "azurerm" {
  alias           = "send-grid"
  subscription_id = var.send_grid_subscription
  features {}
}