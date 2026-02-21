# Schlierelacht Admin

[![Java CI with Maven](https://github.com/sjucker/schlierelacht-admin/actions/workflows/maven.yml/badge.svg?branch=main)](https://github.com/sjucker/schlierelacht-admin/actions/workflows/maven.yml)
[![Quality gate](https://sonarcloud.io/api/project_badges/quality_gate?project=sjucker_schlierelacht-admin)](https://sonarcloud.io/summary/new_code?id=sjucker_schlierelacht-admin)

## Development

* Start DB in Docker container:  
  `docker compose -p schlierelacht -f src/main/docker/postgres.yml down && docker compose -p schlierelacht -f src/main/docker/postgres.yml up --build`

* Generate the jOOQ-code by running the following command (make sure Docker is running):  
  `mvn clean test-compile -Djooq-codegen-skip=false`
  Or use the run configuration `generate jOOQ code`.
