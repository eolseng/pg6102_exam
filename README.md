> Eksamen | [PG6102 - Enterpriseprogrammering 2](https://old.kristiania.no/emnebeskrivelse-2-2/?kode=PG6102&arstall=2020&terminkode=H%C3%98ST) | [Oppgavetekst](./docs/PG6102_enterpriseprogramming2_exam_2020_fall.pdf)
# Travel Agency - Authentication, Trips & Bookings
![Travel Agency Photo - taken by Cristina Gottardi on Unsplash](./docs/travel-agency_by-cristina-gottardi.jpg)

## Modules
### Gateway
* Spring Cloud Gateway as an entrypoint that routes HTTP-requests to the other services based on path.
### Auth
* Spring Security Authentication Service.
* Uses Redis for distributed session based authentication across the services.
* Sends AMQP messages on created users.
### Trip
* Entity Service for CRUD operations on Trips.
* Sends AMQP messages on created or cancelled Trips
### Booking
* Entity Service for CRUD operations on Bookings.
* Receives AMQP messages on create or cancelled/deleted Trips and on User creation.
### Frontend
* Next.JS React frontend for graphical interaction with the API.
* Supports signup and login, view and delete trips, view, register, update and cancel bookings.
### Utils
* Utility repository for Rest-responses and AMQP configuration
### E2E-tests
* Uses Testcontainers with Docker-Compose, Awaitility and RestAssured to perform End-to-End tests on the service.
* The test `AMQP - delete trip leads to cancelled bookings` verifies that AMQP works.

## Scripts:
* `./build-and-start.sh`
    * Used for starting the whole application in "production" mode
    * Starts the project locally - rebuilds all .jar files and Docker Images
    * Exposes port 80 as entrypoint through the API Gateway Service
* `./start-dev.sh`
    * Used for local development
    * Starts all supporting services as Docker Containers
    * Exposes all ports to `localhost`
> Use `sudo bash [SCRIPT]` or `chmod +x [SCRIPT]` if permission is denied.

## Local Development
For easier development the repository can be started outside of Docker-Compose with in-memory databases, while still communicating with each other through Consul, Redis and RabbitMQ.
This allows for faster restart of individual services.

To run the services in 'dev-mode', run `[SERVICE_NAME]DevRunner.kt` located under `[MODULE]/src/test/kotlin/[PACKAGES]/` folder.
To start the frontend in 'dev mode', navigate to the module and run `npm run dev`. 
You must also start the backing services in `docker-compose-dev.yml` either through the script `./start-dev.sh` or with the command `docker-compose -f docker-compose-dev.yml up`.

### Exposed Ports:
| Service:  | Port: |
| ---       | ---   |
| Gateway   | 80    |
| Frontend  | 3000  |
| Auth      | 8081  |
| Trip      | 8082  |
| Booking   | 8083  |
|           |       |
| Consul    | 8500  |
| Redis     | 6379  |
| RabbitMQ  | 5672  |

## Notes
* All requirements, R1 through R5 and T1 through T4, are complete.
* Admin login for the service uses `admin:admin`
* The environments for the containers started with Docker-Compose are configured with the `.env` file in the project root.
* I have created direct loadbalanced HTTP calls from Booking to Trips if a Booking Request comes and the Trip does not exist in the Booking Service.
