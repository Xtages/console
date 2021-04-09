variable "env" {
  default = "production"
}

variable "aws_region" {
  default = "us-east-1"
}

variable "TAG" {
  description = "TAG version used for the task definition. This is available as a environment variable"
}
