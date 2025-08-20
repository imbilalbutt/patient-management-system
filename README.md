
---

# Patient Management System

A modern, scalable, and containerized Patient Management System built with microservices architecture. This system handles core healthcare operations like patient registration, record management, and appointment scheduling, leveraging cutting-edge technologies for reliability and performance.

![Java](https://img.shields.io/badge/Java-17-blue?style=flat&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.1-green?style=flat&logo=springboot)
![Docker](https://img.shields.io/badge/Docker-✓-blue?style=flat&logo=docker)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue?style=flat&logo=postgresql)
![Apache Kafka](https://img.shields.io/badge/Apache%20Kafka-✓-black?style=flat&logo=apachekafka)
![CI/CD](https://img.shields.io/badge/CI/CD-Jenkins-red?style=flat&logo=jenkins)

## 🚀 Features

| Component            | Description                                                                                       |
| -------------------- | ------------------------------------------------------------------------------------------------- |
| REST API             | Exposes patient management endpoints using Spring MVC and Protobuf for request/response modeling. |
| Data Storage         | Uses PostgreSQL via Spring Data JPA for resilient persistence.                                    |
| Event Messaging      | Publishes and consumes patient-related events with Kafka producers and consumers.                 |
| Containerization     | Dockerizes the Spring Boot application and PostgreSQL; orchestrated via `docker-compose.yml`.     |
| CI/CD Automation     | Jenkins pipelines automate building, testing, Docker image creation, and deployment workflows.    |
| Protobuf Integration | Supports efficient and strongly-typed API messaging by using `.proto` definitions.                |

---

## 🏗️ System Architecture

The application follows a microservices-based architecture:
1.  **API Layer**: Spring Boot REST controllers expose endpoints.
2.  **Business Logic Layer**: Services contain the core application logic.
3.  **Data Access Layer**: Repositories interact with the PostgreSQL database.
4.  **Messaging Layer**: Kafka producers and consumers handle asynchronous events.
5.  **Infrastructure**: Docker containers managed via Docker Compose.

## 📦 Tech Stack and Highlights

* **Java 17, Spring Boot 3.1** – Backend framework for API development
* **API Protocol**: REST, Protocol Buffers (Protobuf)
* **Protobuf** – Efficient data serialization
* **Docker & Docker Compose** – Containerized deployment orchestration
* **Jenkins** – Automated CI/CD pipeline execution
* **Apache Kafka** – Event-driven communication through producer and consumer services
* **PostgreSQL** – Relational database for data persistence


## 🔧 Prerequisites

Before running this application, ensure you have the following installed on your machine:
- **Java JDK 17**
- **Docker** and **Docker Compose**
- **Maven** (if not using the Dockerized build)
- (Optional) A Kafka client for testing messages (e.g., `kafkacat`)

## 🚀 Getting Started

Follow these steps to get the project running on your local machine.

### 1. Clone the Repository
```bash
git clone https://github.com/imbilalbutt/patient-management-system.git
cd patient-management-system
```

### 2. Run with Docker Compose (Recommended)
The easiest way to run the entire ecosystem (app, database, Kafka) is using Docker Compose.

```bash
# Start all services in the background
cd ./billing-service
docker compose up

cd  ./patient-service
docker compose up

cd  cd ./analytics-service
docker-compose up

# To view logs, run:
docker-compose logs -f
```

This command will start:
- The Spring Boot application
- A PostgreSQL database
- A Kafka broker
- (Optional) Kafka UI for monitoring (TODO)

### 3. Access the Application
- The Spring Boot application will be available at: 
  - Patient service: `http://localhost:4000`
  - Billing service: `http://localhost:4001`
  - Billing GRPC service: `http://localhost:9001`
  - Analytics service: `http://localhost:4002`
- Use tools like **Postman** or **curl** to interact with the APIs.

### 4. (Alternative) Run without Docker
If you want to run the application natively:

1.  **Start PostgreSQL and Kafka** (You can use the Docker Compose file just for these dependencies):
    ```bash
    docker-compose up -d
    ```
2.  **Build the application**:
    ```bash
    mvn clean package
    ```
3.  **Run the JAR**:
    ```bash
    java -jar target/patient-service.jar
    java -jar target/billing-service.jar
    java -jar target/analytics-service.jar
    ```

## 📡 API Documentation

Once the application is running, you can explore the API in several ways:

- **RESTful Endpoints**: Available at `http://localhost:4000/patients`
- **Protobuf Endpoints**: Endpoints using Protobuf are typically available on a separate port or path. Check your configuration.
- **Swagger/OpenAPI UI**: If integrated, it would be at `http://localhost:4000/swagger-ui.html`
- **Actuator Endpoints**: For health checks and metrics: `http://localhost:4000/actuator/health`

### Example API Calls

**Create a New Patient (JSON):**
```bash
curl -X POST http://localhost:4000/patients \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "dateOfBirth": "1985-07-15",
    "email": "john.doe@example.com"
  }'
```

**Retrieve a Patient by ID:**
```bash
curl -X GET http://localhost:4000/patients/1
```

## 🔄 CI/CD Pipeline

This project uses a Jenkins pipeline for automation. The typical pipeline stages include:
1.  **Checkout**: Pulls the latest code from the `main` or `master` branch.
2.  **Build**: Compiles the code and runs unit tests (`mvn clean package`).
3.  **Docker Build**: Builds a new Docker image for the application.
4.  **Deploy**: Deploys the new image to a target environment (e.g., staging/production).

The `Jenkinsfile` at the root of the repository defines this process.

## 📁 Project Structure

```
analytics-service/
├── src/main/java/
│   └── com/imbilalbutt/analyticsservice/
│       └── kafka/            # Kafka consumer
│   └── proto/                # .proto schema definitions
├── src/main/resources/
│   ├── application.properties       # Main configuration
│   └── proto/                # .proto schema definitions
│        └──patient-event.proto  
├── Dockerfile               # Instructions to build the app image
├── docker-compose.yml       # Defines multi-container setup
├── Jenkinsfile              # CI/CD pipeline definition
└── pom.xml                  # Maven dependencies and build config


billing-service/
├── src/main/java/
│   └── com/imbilalbutt/billingservice/
│       ├── grpc/       
│       └── proto/           # Spring configuration (Kafka, Protobuf, etc.)
│           └── billing-service.proto          # Kafka producers and consumers
├── src/main/resources/
│   └── application.properties       # Main configuration
├── Dockerfile               # Instructions to build the app image
├── docker-compose.yml       # Defines multi-container setup
├── Jenkinsfile              # CI/CD pipeline definition
└── pom.xml                  # Maven dependencies and build config


patient-service/
├── src/main/java/
│   └── com/yourcompany/patientservice/
│       ├── controller/       # REST API Controllers
│       ├── dto/       # Data transfer objects
│           └── validators/
│       ├── exceptions/           
│       ├── grpc/          
│       ├── service/          # Business logic layer
│       ├── repository/       # Data access layer (JPA)
│       ├── model/            # Entity and Protobuf models
│       ├── mapper/           
│       └── kafka/            # Kafka producers and consumers
│   └── proto/                # .proto schema definitions
├── src/main/resources/
│   ├── application.properties       # Main configuration
├── Dockerfile               # Instructions to build the app image
├── docker-compose.yml       # Defines multi-container setup
├── Jenkinsfile              # CI/CD pipeline definition
└── pom.xml                  # Maven dependencies and build config               # Maven dependencies and build config



```

## 🤝 Contributing

1.  Fork the Project
2.  Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3.  Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4.  Push to the Branch (`git push origin feature/AmazingFeature`)
5.  Open a Pull Request

## 📜 License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details.

## 👏 Acknowledgments

- Spring Boot and the Spring ecosystem.
- Apache Kafka community.
- Docker community.

---