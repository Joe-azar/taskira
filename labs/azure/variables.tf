variable "resource_group_name" {
  description = "Name of the Azure resource group that would hold every resource in this lab."
  type        = string
  default     = "taskira-lab-rg"
}

variable "location" {
  description = "Azure region."
  type        = string
  default     = "westeurope"
}

variable "environment_name" {
  description = "Short name used as a prefix on most resources, kept distinct from other labs' names."
  type        = string
  default     = "taskira-lab"
}

variable "postgres_admin_username" {
  description = "PostgreSQL Flexible Server admin username."
  type        = string
  default     = "taskira"
}

variable "postgres_password" {
  description = <<-EOT
    PostgreSQL Flexible Server admin password. No default on purpose (ADR-0026) - the
    same discipline already applied to -PostgresPassword in scripts/up.ps1 (P17/P18)
    and to postgres.password in labs/helm/taskira/values.yaml. Never commit a real
    value; pass it with -var or a gitignored *.tfvars file.
  EOT
  type        = string
  sensitive   = true
}

variable "postgres_sku_name" {
  description = "PostgreSQL Flexible Server compute tier - Burstable, the cheapest tier, for a lab never meant for real traffic."
  type        = string
  default     = "B_Standard_B1ms"
}

variable "postgres_version" {
  description = "PostgreSQL major version - 18, matching the real stack's PostgreSQL 18.6 (ADR-0017)."
  type        = string
  default     = "18"
}

variable "backend_image" {
  description = "Backend container image - the real image published to GHCR by P15's release workflow, same image already deployed in the Kubernetes (P17) and Helm (P18) labs."
  type        = string
  default     = "ghcr.io/joe-azar/taskira-backend:v0.1.0"
}

variable "frontend_image" {
  description = "Frontend container image - same real GHCR image as the backend."
  type        = string
  default     = "ghcr.io/joe-azar/taskira-frontend:v0.1.0"
}

variable "ghcr_username" {
  description = "GitHub username Container Apps authenticates with to pull from GHCR - required even for a public image (ADR-0026, confirmed against Microsoft Learn)."
  type        = string
}

variable "ghcr_pull_token" {
  description = <<-EOT
    Classic GitHub PAT with the read:packages scope, used only to pull the (public)
    GHCR image - Container Apps requires explicit registry credentials for any non-ACR
    registry, even a public one. No default on purpose; never commit a real value.
  EOT
  type        = string
  sensitive   = true
}
