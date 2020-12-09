> Eksamen | [PG6102 - Enterpriseprogrammering 2](https://old.kristiania.no/emnebeskrivelse-2-2/?kode=PG6102&arstall=2020&terminkode=H%C3%98ST) | [Oppgavetekst](./docs/PG6102_enterpriseprogramming2_exam_2020_fall.pdf)
# Travel Agency
![Travel Agenct Photo - taken by Cristina Gottardi on Unsplash](./docs/travel-agency_by-cristina-gottardi.jpg)

## Scripts:
* `./build-and-start.sh`
    * Used for starting the whole application in "production" mode
    * Starts the project locally - rebuilds all .jar files and Docker Images
    * Exposes port 80 as entrypoint through the API Gateway Service
* `./start-dev.sh`
    * Used for local development
    * Starts all supporting services as Docker Containers
    * Exposes all ports to `localhost`
> Use `chmod +x [SCRIPT]` or `sudo bash [SCRIPT]` if permission is denied.

## Modules

## Local Development
The modules can be executed in a non-Docker environment by running their respective `[SERVICE_NAME]DevRunner.kt` located under `[MODULE]/src/test/kotlin/[PACKAGE]/` folder.

This requires the backing services (Redis, RabbitMQ and Consul) to be started as Docker Containers either via the script `./start-dev.sh` or with the command `docker-compose -f docker-compose-dev.yml up`.

### Exposed Ports:
| Service:  | Port: |
| ---       | ---   |
| Gateway   | 80    |
| Auth      | 8081  |
| Trip      | 8082  |
| Booking   | 8083  |
|           |       |
| Consul    | 8500  |
| Redis     | 6379  |
| RabbitMQ  | 5672  |
Only exposed when profile 'dev' is active.
