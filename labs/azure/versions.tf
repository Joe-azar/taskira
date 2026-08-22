# Terraform architecture lab for Taskira on Azure (P19, ADR-0026). Never applied
# automatically - see ADR-0026 and the top-level README.md in this directory. Validated
# only with `terraform validate` / `terraform fmt -check`, which run entirely offline
# and need no Azure credentials.

terraform {
  required_version = ">= 1.15.0"

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 5.1"
    }
  }
}

provider "azurerm" {
  features {}
}
