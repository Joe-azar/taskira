# Private access only - VNet-integrated, delegated subnet, no public network access at
# all (ADR-0026: the same "db never reachable except by the backend" principle already
# applied to db_net in infra/docker-compose.prodlike.yml, P11). A Private DNS Zone is
# mandatory for Flexible Server's VNet-integrated deployment mode - Azure resolves the
# server's hostname to its private IP only within VNets linked to this zone.

resource "azurerm_private_dns_zone" "postgres" {
  name                = "${var.environment_name}.postgres.database.azure.com"
  resource_group_name = azurerm_resource_group.this.name
}

resource "azurerm_private_dns_zone_virtual_network_link" "postgres" {
  name                = "${var.environment_name}-postgres-link"
  private_dns_zone_id = azurerm_private_dns_zone.postgres.id
  virtual_network_id  = azurerm_virtual_network.this.id
}

resource "azurerm_postgresql_flexible_server" "this" {
  name                = "${var.environment_name}-postgres"
  location            = azurerm_resource_group.this.location
  resource_group_name = azurerm_resource_group.this.name

  version    = var.postgres_version
  sku_name   = var.postgres_sku_name
  storage_mb = 32768

  administrator_login    = var.postgres_admin_username
  administrator_password = var.postgres_password

  delegated_subnet_id = azurerm_subnet.postgres.id
  private_dns_zone_id = azurerm_private_dns_zone.postgres.id

  # A lab, not real production - matches the "never meant for real traffic" reasoning
  # behind the Burstable SKU above. Real production would need geo-redundant backups
  # and a genuine retention policy, deliberately out of scope here (ADR-0026).
  backup_retention_days        = 7
  geo_redundant_backup_enabled = false

  depends_on = [azurerm_private_dns_zone_virtual_network_link.postgres]
}

resource "azurerm_postgresql_flexible_server_database" "taskira" {
  name      = "taskira"
  server_id = azurerm_postgresql_flexible_server.this.id
  charset   = "UTF8"
  collation = "en_US.utf8"
}
