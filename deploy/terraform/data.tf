data "terraform_remote_state" "xtages_infra" {
  backend = "s3"
  config = {
    bucket = "xtages-tfstate"
    key    = "tfstate/us-east-1/production"
    region = "us-east-1"
  }
}

data "terraform_remote_state" "xtages_ecs" {
  backend = "s3"
  config = {
    bucket = "xtages-tfstate"
    key    = "tfstate/us-east-1/production/ecs"
    region = "us-east-1"
  }
}

data "terraform_remote_state" "xtages_lb" {
  backend = "s3"
  config = {
    bucket = "xtages-tfstate"
    key    = "tfstate/us-east-1/production/lbs/lb-tfstate"
    region = "us-east-1"
  }
}

data "aws_route53_zone" "xtages_zone" {
  name         = "xtages.com"
  private_zone = false
}

data "aws_lb" "xtages_console_lb" {
  arn = data.terraform_remote_state.xtages_lb.outputs.xtages_console_alb_arn
}

data "aws_acm_certificate" "xtages_cert" {
  domain   = "xtages.com"
  statuses = ["ISSUED"]
}

data "aws_ecr_repository" "xtages_console_repo" {
  name = "xtages-console"
}

data "template_file" "console_task_definition" {
  template = file("${path.root}/templates/console.json.tpl")
  vars = {
    REPOSITORY_URL = replace(data.aws_ecr_repository.xtages_console_repo.repository_url, "https://", "")
    TAG            = var.TAG
  }
}
