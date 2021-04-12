[
  {
    "essential": true,
    "memory": 512,
    "name": "xtages-console",
    "cpu": 512,
    "image": "${REPOSITORY_URL}:1",
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

