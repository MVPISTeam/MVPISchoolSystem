# Mountain View Primary and Infant School Management System

COMP2140 group THUR_5-7_G01. A Spring Boot REST API for student records, grades, attendance, staff records, lunch orders and expenditure.

## Run it

```bash
./mvnw spring-boot:run        # macOS / Linux / Git Bash
mvnw.cmd spring-boot:run      # Windows Command Prompt or PowerShell
```

Then open http://localhost:8080/swagger-ui.html and click **Authorize**.

| Username | Role | Password |
|---|---|---|
| principal | Principal | mvpi1234 |
| viceprincipal | Vice Principal | mvpi1234 |
| admin | Administrative staff | mvpi1234 |
| teacher | Teacher | mvpi1234 |
| bursar | Bursar | mvpi1234 |
| tuckshop | Tuckshop Manager | mvpi1234 |

## Test it

```bash
./mvnw test
```

## Branches

`feature/*` -> pull request -> `dev` -> tested -> pull request -> `main`. Nobody pushes to `dev` or `main` directly.