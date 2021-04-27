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
    ]
  }
]

