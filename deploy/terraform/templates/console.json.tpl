[
  {
    "essential": true,
    "memory": 3584,
    "name": "xtages-console",
    "cpu": 1536,
    "image": "${REPOSITORY_URL}:${TAG}",
    "workingDirectory": "/",
    "command": ["java", "-jar", "console.jar"],
    "portMappings": [
        {
            "containerPort": 8080,
            "hostPort": 0
        }
    ],
    "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
            "awslogs-create-group": "true",
            "awslogs-group" : "xtages-console",
            "awslogs-region": "us-east-1",
            "awslogs-stream-prefix": "ecs"
        }
    }
  }
]

