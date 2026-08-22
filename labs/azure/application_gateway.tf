# The single public entry point (ADR-0026): path-based routing to two internal-only
# Container Apps under one public hostname, so the browser only ever sees one origin -
# required by the app's SameSite=Lax session cookie (ADR-0006), not just a convenience.
# Standard_v2 (the cheapest App Gateway v2 tier with path-based routing), matching the
# "never meant for real traffic" reasoning already applied to the Postgres SKU.

resource "azurerm_application_gateway" "this" {
  name                = "${var.environment_name}-appgw"
  location            = azurerm_resource_group.this.location
  resource_group_name = azurerm_resource_group.this.name

  sku {
    name     = "Standard_v2"
    tier     = "Standard_v2"
    capacity = 1
  }

  gateway_ip_configuration {
    name      = "appgw-ip-config"
    subnet_id = azurerm_subnet.appgw.id
  }

  frontend_port {
    name = "https-port"
    port = 443
  }

  frontend_ip_configuration {
    name                 = "appgw-frontend-ip"
    public_ip_address_id = azurerm_public_ip.appgw.id
  }

  # TLS termination at the edge (ADR-0026): a real deployment would attach a Key
  # Vault-backed certificate here via `ssl_certificate { key_vault_secret_id = ... }`.
  # Deliberately not modeled further - this lab stops at "architecture and structure
  # validated", not a complete secrets/PKI design (see ADR-0026, "Conséquences").

  backend_address_pool {
    name  = "backend-pool"
    fqdns = [azurerm_container_app.backend.ingress[0].fqdn]
  }

  backend_address_pool {
    name  = "frontend-pool"
    fqdns = [azurerm_container_app.frontend.ingress[0].fqdn]
  }

  probe {
    name                = "backend-probe"
    protocol            = "Https"
    path                = "/actuator/health/readiness"
    host                = azurerm_container_app.backend.ingress[0].fqdn
    interval            = 30
    timeout             = 10
    unhealthy_threshold = 3
  }

  probe {
    name                = "frontend-probe"
    protocol            = "Https"
    path                = "/healthz"
    host                = azurerm_container_app.frontend.ingress[0].fqdn
    interval            = 30
    timeout             = 10
    unhealthy_threshold = 3
  }

  backend_http_settings {
    name                  = "backend-http-settings"
    cookie_based_affinity = "Disabled"
    port                  = 443
    protocol              = "Https"
    request_timeout       = 30
    probe_name            = "backend-probe"
    host_name             = azurerm_container_app.backend.ingress[0].fqdn
  }

  backend_http_settings {
    name                  = "frontend-http-settings"
    cookie_based_affinity = "Disabled"
    port                  = 443
    protocol              = "Https"
    request_timeout       = 30
    probe_name            = "frontend-probe"
    host_name             = azurerm_container_app.frontend.ingress[0].fqdn
  }

  http_listener {
    name                           = "https-listener"
    frontend_ip_configuration_name = "appgw-frontend-ip"
    frontend_port_name             = "https-port"
    protocol                       = "Https"
    # ssl_certificate_name intentionally omitted - see the TLS termination comment
    # above. terraform validate does not require a real certificate to exist; a real
    # apply would.
  }

  url_path_map {
    name                               = "taskira-path-map"
    default_backend_address_pool_name  = "frontend-pool"
    default_backend_http_settings_name = "frontend-http-settings"

    path_rule {
      name                       = "api-path-rule"
      paths                      = ["/api/*"]
      backend_address_pool_name  = "backend-pool"
      backend_http_settings_name = "backend-http-settings"
    }
  }

  request_routing_rule {
    name               = "taskira-routing-rule"
    rule_type          = "PathBasedRouting"
    http_listener_name = "https-listener"
    url_path_map_name  = "taskira-path-map"
    priority           = 100
  }
}
