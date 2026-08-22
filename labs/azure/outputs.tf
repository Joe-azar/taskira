output "application_url" {
  description = "The single public entry point - Application Gateway's hostname."
  value       = "https://${azurerm_public_ip.appgw.domain_name_label}.${var.location}.cloudapp.azure.com"
}

output "postgres_fqdn" {
  description = "PostgreSQL Flexible Server's private FQDN - reachable only from within the VNet."
  value       = azurerm_postgresql_flexible_server.this.fqdn
}

output "resource_group_name" {
  value = azurerm_resource_group.this.name
}
