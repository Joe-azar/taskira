# Three subnets, mirroring the network segmentation already applied to
# infra/docker-compose.prodlike.yml (P11, three Docker networks) and the same principle
# behind labs/kubernetes' three-network Ingress/backend/postgres separation: everything
# stays internal except the single public entry point (ADR-0026).

resource "azurerm_virtual_network" "this" {
  name                = "${var.environment_name}-vnet"
  location            = azurerm_resource_group.this.location
  resource_group_name = azurerm_resource_group.this.name
  address_space       = ["10.60.0.0/16"]
}

# Application Gateway requires a subnet dedicated to it alone - no other resource type
# may share it.
resource "azurerm_subnet" "appgw" {
  name                 = "appgw-subnet"
  resource_group_name  = azurerm_resource_group.this.name
  virtual_network_name = azurerm_virtual_network.this.name
  address_prefixes     = ["10.60.0.0/24"]
}

resource "azurerm_subnet" "container_apps" {
  name                 = "container-apps-subnet"
  resource_group_name  = azurerm_resource_group.this.name
  virtual_network_name = azurerm_virtual_network.this.name
  address_prefixes     = ["10.60.1.0/23"]

  delegation {
    name = "container-apps-delegation"
    service_delegation {
      name    = "Microsoft.App/environments"
      actions = ["Microsoft.Network/virtualNetworks/subnets/join/action"]
    }
  }
}

resource "azurerm_subnet" "postgres" {
  name                 = "postgres-subnet"
  resource_group_name  = azurerm_resource_group.this.name
  virtual_network_name = azurerm_virtual_network.this.name
  address_prefixes     = ["10.60.3.0/24"]

  delegation {
    name = "postgres-delegation"
    service_delegation {
      name    = "Microsoft.DBforPostgreSQL/flexibleServers"
      actions = ["Microsoft.Network/virtualNetworks/subnets/join/action"]
    }
  }
}

resource "azurerm_public_ip" "appgw" {
  name                = "${var.environment_name}-appgw-pip"
  location            = azurerm_resource_group.this.location
  resource_group_name = azurerm_resource_group.this.name
  allocation_method   = "Static"
  sku                 = "Standard"
  # Gives the gateway a stable *.cloudapp.azure.com hostname, referenced by
  # APP_CORS_ALLOWED_ORIGINS (container_apps.tf) - a real deployment would likely
  # attach a real custom domain instead, not modeled further here (ADR-0026).
  domain_name_label = var.environment_name
}
