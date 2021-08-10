variable "env" {
  default = "production"
}

variable "aws_region" {
  default = "us-east-1"
}

variable "account_id" {
  description = "AWS account ID, defaults to our Production account"
  default     = "606626603369"
}

variable "TAG" {
  description = "TAG version used for the task definition. This is available as a environment variable"
}
