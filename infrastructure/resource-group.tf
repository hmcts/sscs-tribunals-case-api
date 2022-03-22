resource "azurerm_resource_group" "rg" {
  name     = join("-", [var.product, var.env])
  location = var.location
}
