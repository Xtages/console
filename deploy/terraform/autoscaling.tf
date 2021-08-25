resource "aws_appautoscaling_target" "ecs_console_autoscaling" {
  max_capacity       = 20
  min_capacity       = 1
  resource_id        = "service/${local.ecs_cluster_name}/${aws_ecs_service.xtages_console_service.name}"
  scalable_dimension = "ecs:service:DesiredCount"
  service_namespace  = "ecs"
}

resource "aws_appautoscaling_policy" "ecs_policy" {
  name               = "console-scale-out"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.ecs_console_autoscaling.resource_id
  scalable_dimension = aws_appautoscaling_target.ecs_console_autoscaling.scalable_dimension
  service_namespace  = aws_appautoscaling_target.ecs_console_autoscaling.service_namespace

  target_tracking_scaling_policy_configuration {

    predefined_metric_specification {
      predefined_metric_type = "ECSServiceAverageCPUUtilization"
    }

    target_value       = 80
    scale_in_cooldown  = 300
    scale_out_cooldown = 120
  }
}

resource "aws_appautoscaling_policy" "console_ecs_policy_memory" {
  name               = "console-scale-out-memory"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.ecs_console_autoscaling.resource_id
  scalable_dimension = aws_appautoscaling_target.ecs_console_autoscaling.scalable_dimension
  service_namespace  = aws_appautoscaling_target.ecs_console_autoscaling.service_namespace

  target_tracking_scaling_policy_configuration {

    predefined_metric_specification {
      predefined_metric_type = "ECSServiceAverageMemoryUtilization"
    }

    target_value       = 80
    scale_in_cooldown  = 300
    scale_out_cooldown = 120
  }
}

