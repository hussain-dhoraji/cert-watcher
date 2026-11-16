variable "region" {
  description = "AWS region for the cluster."
  type        = string
  default     = "ca-central-1"
}

variable "cluster_name" {
  description = "Name of the EKS cluster and its VPC."
  type        = string
  default     = "cert-watcher"
}

variable "kubernetes_version" {
  description = "EKS Kubernetes version. Pick one in standard support; older versions incur extended-support charges."
  type        = string
}

variable "node_instance_types" {
  description = "Instance types for the managed node group."
  type        = list(string)
  default     = ["t3.medium"]
}

variable "node_min_size" {
  description = "Minimum nodes in the managed node group."
  type        = number
  default     = 1
}

variable "node_max_size" {
  description = "Maximum nodes in the managed node group."
  type        = number
  default     = 3
}

variable "node_desired_size" {
  description = "Desired nodes in the managed node group."
  type        = number
  default     = 2
}

variable "argocd_chart_version" {
  description = "Version of the argo-cd Helm chart to install."
  type        = string
  default     = "7.7.0"
}
