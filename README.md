> :warning: This repository was archived automatically since no ownership was defined :warning:
>
> For details on how to claim stewardship of this repository see:
>
> [How to configure a service in OpsLevel](https://www.notion.so/pleo/How-to-configure-a-service-in-OpsLevel-f6483fcb4fdd4dcc9fc32b7dfe14c262)
>
> To learn more about the automatic process for stewardship which archived this repository see:
>
> [Automatic process for stewardship](https://www.notion.so/pleo/Automatic-process-for-stewardship-43d9def9bc9a4010aba27144ef31e0f2)

## Antaeus

Antaeus (/ænˈtiːəs/), in Greek mythology, a giant of Libya, the son of the sea god Poseidon and the Earth goddess Gaia. He compelled all strangers who were passing through the country to wrestle with him. Whenever Antaeus touched the Earth (his mother), his strength was renewed, so that even if thrown to the ground, he was invincible. Heracles, in combat with him, discovered the source of his strength and, lifting him up from Earth, crushed him to death.

Welcome to our challenge.

## The challenge

As most "Software as a Service" (SaaS) companies, Pleo needs to charge a subscription fee every month. Our database contains a few invoices for the different markets in which we operate. Your task is to build the logic that will schedule payment of those invoices on the first of the month. While this may seem simple, there is space for some decisions to be taken and you will be expected to justify them.

## Instructions

Fork this repo with your solution. Ideally, we'd like to see your progression through commits, and don't forget to update the README.md to explain your thought process.

Please let us know how long the challenge takes you. We're not looking for how speedy or lengthy you are. It's just really to give us a clearer idea of what you've produced in the time you decided to take. Feel free to go as big or as small as you want.

## Developing

Requirements:
- \>= Java 11 environment

Open the project using your favorite text editor. If you are using IntelliJ, you can open the `build.gradle.kts` file and it is gonna setup the project in the IDE for you.

### Building

```
./gradlew build
```

### Running

There are 2 options for running Anteus. You either need libsqlite3 or docker. Docker is easier but requires some docker knowledge. We do recommend docker though.

*Running Natively*

Native java with sqlite (requires libsqlite3):

If you use homebrew on MacOS `brew install sqlite`.

```
./gradlew run
```

*Running through docker*

Install docker for your platform

```
docker build -t antaeus
docker run antaeus
```

### App Structure
The code given is structured as follows. Feel free however to modify the structure to fit your needs.
```
├── buildSrc
|  | gradle build scripts and project wide dependency declarations
|  └ src/main/kotlin/utils.kt 
|      Dependencies
|
├── pleo-antaeus-app
|       main() & initialization
|
├── pleo-antaeus-core
|       This is probably where you will introduce most of your new code.
|       Pay attention to the PaymentProvider and BillingService class.
|
├── pleo-antaeus-data
|       Module interfacing with the database. Contains the database 
|       models, mappings and access layer.
|
├── pleo-antaeus-models
|       Definition of the Internal and API models used throughout the
|       application.
|
└── pleo-antaeus-rest
        Entry point for HTTP REST API. This is where the routes are defined.
```

### Main Libraries and dependencies
* [Exposed](https://github.com/JetBrains/Exposed) - DSL for type-safe SQL
* [Javalin](https://javalin.io/) - Simple web framework (for REST)
* [kotlin-logging](https://github.com/MicroUtils/kotlin-logging) - Simple logging framework for Kotlin
* [JUnit 5](https://junit.org/junit5/) - Testing framework
* [Mockk](https://mockk.io/) - Mocking library
* [Sqlite3](https://sqlite.org/index.html) - Database storage engine
* [Qaurtz](https://github.com/quartz-scheduler/quartz) - Scheduling library

Happy hacking 😁!

### Process

### Setting up Environment (1h spent)
Dive into the project, install Gradle and Docker. Starting the project in the current state, studying response models.

### Work plan (1h spent)
1. Write the logic of method for sending invoices
2. Error handling and payment retry
3. Notification of the customer about the payment error
4. Payment scheduling
5. Testing

### Core functionality (2h spent)
Implementing methods for invoices with status `InvoiceStatus.PENDING`(monthly) and `InvoiceStatus.RETRY`(daily).

When sending the request for the first time, all payments with the `InvoiceStatus.PENDING` status will be processed asynchronously using Coroutines.
If there are problems with payment (client doesn't have enough money or network errors), payments are changed to the `InvoiceStatus.RETRY` status and will be processed again in 24 hours.

A notification is sent to the client after the payment is made (about success or failure).

If serious errors have occurred (client is not found or currency does not match), the payment is transferred to the `InvoiceStatus.REJECTED` status and will not be processed anymore.

### Scheduling (2h spent)
After the research, I found 3 ways to implement scheduling:
1. [Timer](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.concurrent/java.util.-timer/schedule.html) - Standard Kotlin library
2. [Ofelia](https://github.com/mcuadros/ofelia) - Docker job scheduling
3. [Qaurtz](https://github.com/quartz-scheduler/quartz) - Scheduling library

The first approach seemed to me like brute force. I would like the scheduler to be separated from the main application flow.

Ofelia is a job scheduler which have the ability to execute commands directly on Docker containers. 
To implement this approach, it would be convenient to configure docker-compose to run the service and the scheduler together.
Also, the advantages of this approach are that I will call methods using the REST API, which decouple application flows.
I have encountered difficulties using this approach since Curl is not installed in the container. Perhaps this can be done with a workaround using wget.
In general, the solution using Ofelia seems interesting, but the tool is still not ready and a little inconvenient.

As a result, I use Quartz Scheduler to create cron jobs.

### Future improvements
1. Add Global Exception Handler (e.g. using `@ControllerAdvice` from Spring Framework) and handle all exceptions in separate place
2. Create a single method for sending invoices with the status of the request parameter + add an exception when sending a status other than Pending or Retry
3. Using `application.yml` for client messages