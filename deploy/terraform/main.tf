locals {
  ecs_cluster_name = split("/", data.terraform_remote_state.xtages_ecs.outputs.xtages_ecs_cluster_id)[1]
}

resource "aws_ecs_task_definition" "console_task_definition" {
  family                = "xtages-console"
  container_definitions = data.template_file.console_task_definition.rendered
  task_role_arn         = aws_iam_role.ecs_console_task_role.arn
}

resource "aws_route53_record" "console_cname_record" {
  name            = "console.xtages.com"
  type            = "CNAME"
  zone_id         = data.aws_route53_zone.xtages_zone.zone_id
  ttl             = 60
  records         = [data.aws_lb.xtages_console_lb.dns_name]
  allow_overwrite = true
}

resource "aws_lb_listener" "xtages_service_secure" {
  load_balancer_arn = data.aws_lb.xtages_console_lb.arn
  port              = 443
  protocol          = "HTTPS"
  certificate_arn   = data.aws_acm_certificate.xtages_cert.id
  ssl_policy        = "ELBSecurityPolicy-2016-08"

  default_action {
    target_group_arn = aws_lb_target_group.xtages_console.id
    type             = "forward"
  }
}

resource "aws_lb_listener" "xtages_service" {
  load_balancer_arn = data.aws_lb.xtages_console_lb.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type = "redirect"

    redirect {
      port        = "443"
      protocol    = "HTTPS"
      status_code = "HTTP_301"
    }
  }
}

resource "aws_lb_target_group" "xtages_console" {
  name     = "xtages-console-tg"
  port     = 80
  protocol = "HTTP"
  deregistration_delay = 120
  vpc_id   = data.terraform_remote_state.xtages_infra.outputs.vpc_id

  health_check {
    path                = "/"
    healthy_threshold   = 3
    unhealthy_threshold = 3
    timeout             = 30
    interval            = 60
    matcher             = "200,301,302"
  }
}

resource "aws_ecs_service" "xtages_console_service" {
  name                = "xtages-console"
  cluster             = data.terraform_remote_state.xtages_ecs.outputs.xtages_ecs_cluster_id
  task_definition     = aws_ecs_task_definition.console_task_definition.arn
  desired_count       = 1
  iam_role            = data.terraform_remote_state.xtages_ecs.outputs.ecs_service_role_arn
  scheduling_strategy = "REPLICA"

  capacity_provider_strategy {
    capacity_provider = data.terraform_remote_state.xtages_ecs.outputs.ecs_capacity_provider_name
    weight            = 1
    base              = 0
  }

  ordered_placement_strategy {
    type  = "spread"
    field = "attribute:ecs.availability-zone"
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.xtages_console.id
    container_name   = "xtages-console"
    container_port   = 8080
  }
}
