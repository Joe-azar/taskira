resource "azurerm_log_analytics_workspace" "this" {
  name                = "${var.environment_name}-logs"
  location            = azurerm_resource_group.this.location
  resource_group_name = azurerm_resource_group.this.name
  sku                 = "PerGB2018"
  retention_in_days   = 30
}

resource "azurerm_container_app_environment" "this" {
  name                       = "${var.environment_name}-env"
  location                   = azurerm_resource_group.this.location
  resource_group_name        = azurerm_resource_group.this.name
  log_analytics_workspace_id = azurerm_log_analytics_workspace.this.id
  infrastructure_subnet_id   = azurerm_subnet.container_apps.id

  # Internal-only environment: neither app gets a public IP of its own (ADR-0026) -
  # Application Gateway is the sole public entry point.
  internal_load_balancer_enabled = true
}

# Both apps pull from GHCR, not ACR (ADR-0013 already chose GHCR as the registry in
# P15; mirroring to ACR would be a redundant second registry for no real benefit).
# Container Apps requires explicit registry credentials for GHCR even though the image
# is public (ADR-0026).

resource "azurerm_container_app" "backend" {
  name                         = "${var.environment_name}-backend"
  resource_group_name          = azurerm_resource_group.this.name
  container_app_environment_id = azurerm_container_app_environment.this.id
  revision_mode                = "Single"

  registry {
    server               = "ghcr.io"
    username             = var.ghcr_username
    password_secret_name = "ghcr-pull-token"
  }

  secret {
    name  = "ghcr-pull-token"
    value = var.ghcr_pull_token
  }

  secret {
    name  = "postgres-password"
    value = var.postgres_password
  }

  template {
    container {
      name   = "backend"
      image  = var.backend_image
      cpu    = 0.5
      memory = "1Gi"

      env {
        name  = "SPRING_PROFILES_ACTIVE"
        value = "prod"
      }
      env {
        name  = "SPRING_DATASOURCE_URL"
        value = "jdbc:postgresql://${azurerm_postgresql_flexible_server.this.fqdn}:5432/${azurerm_postgresql_flexible_server_database.taskira.name}"
      }
      env {
        name  = "SPRING_DATASOURCE_USERNAME"
        value = var.postgres_admin_username
      }
      env {
        name        = "SPRING_DATASOURCE_PASSWORD"
        secret_name = "postgres-password"
      }
      env {
        name  = "SERVER_PORT"
        value = "8080"
      }
      env {
        name  = "MANAGEMENT_SERVER_PORT"
        value = "8080"
      }
      env {
        name  = "APP_CORS_ALLOWED_ORIGINS"
        value = "https://${azurerm_public_ip.appgw.domain_name_label}.${azurerm_resource_group.this.location}.cloudapp.azure.com"
      }
      # Application Gateway terminates TLS at the edge (see application_gateway.tf) -
      # traffic from Application Gateway to this internal Container App stays on the
      # private VNet, so a Secure session cookie would never be set over that internal
      # hop unless Container Apps' own ingress is also told to expect HTTPS
      # end-to-end, which it does by default. Left at the application-prod.yaml
      # default (Secure=true) deliberately, unlike the local labs' HTTP-only
      # workaround (ADR-0019) - Application Gateway's public listener is HTTPS, so the
      # browser always sees a secure origin.

      readiness_probe {
        transport = "HTTP"
        port      = 8080
        path      = "/actuator/health/readiness"
      }
      liveness_probe {
        transport = "HTTP"
        port      = 8080
        path      = "/actuator/health/liveness"
      }
    }

    min_replicas = 1
    max_replicas = 2
  }

  # Internal-only ingress: unreachable from the public internet, only from within the
  # Container Apps environment (Application Gateway reaches it over the VNet) or from
  # other apps in the same environment by name (ADR-0026, confirmed against Microsoft
  # Learn: same-environment calls resolve automatically, traffic never leaves the
  # environment).
  ingress {
    external_enabled = false
    target_port      = 8080
    transport        = "http"

    traffic_weight {
      latest_revision = true
      percentage      = 100
    }
  }
}

resource "azurerm_container_app" "frontend" {
  name                         = "${var.environment_name}-frontend"
  resource_group_name          = azurerm_resource_group.this.name
  container_app_environment_id = azurerm_container_app_environment.this.id
  revision_mode                = "Single"

  registry {
    server               = "ghcr.io"
    username             = var.ghcr_username
    password_secret_name = "ghcr-pull-token"
  }

  secret {
    name  = "ghcr-pull-token"
    value = var.ghcr_pull_token
  }

  template {
    container {
      name   = "frontend"
      image  = var.frontend_image
      cpu    = 0.25
      memory = "0.5Gi"

      readiness_probe {
        transport = "HTTP"
        port      = 8080
        path      = "/healthz"
      }
      liveness_probe {
        transport = "HTTP"
        port      = 8080
        path      = "/healthz"
      }
    }

    min_replicas = 1
    max_replicas = 2
  }

  # Also internal-only: Application Gateway routes "/*" here (application_gateway.tf).
  # The image's baked-in /api/ proxy_pass block (frontend/nginx/default.conf) never
  # gets exercised in this design - Application Gateway already splits /api/* to the
  # backend before a request ever reaches this container, so the image works
  # unmodified even though its internal proxy logic goes unused here.
  ingress {
    external_enabled = false
    target_port      = 8080
    transport        = "http"

    traffic_weight {
      latest_revision = true
      percentage      = 100
    }
  }
}
