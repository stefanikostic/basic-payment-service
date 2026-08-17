# Basic Payment Service

A basic money transfer service: move funds between accounts, and keep a
record of every transfer.

- Java 21, Spring Boot 4.1.0, Spring MVC + Spring Data JPA
- H2 database

## Running the project

```bash
./mvnw spring-boot:run
```

The service listens on `http://localhost:8080`. 
The schema is seeded from `src/main/resources/data.sql`:

## API

### `POST /api/transfers`

Moves money between two accounts. Returns `201 Created`.

```bash
curl -X POST http://localhost:8080/api/transfers \
  -H 'Content-Type: application/json' \
  -d '{"sourceAccountId":"ACC-1","destinationAccountId":"ACC-2","amount":50}'
```

Request rules:
- `sourceAccountId`       required, non-blank                                         
- `destinationAccountId`  required, non-blank, must differ from the source            
- `amount`                required, at least `0.01`, at most 2 decimal places         

### `GET /api/accounts/{accountId}`

Returns an account entity record by the given accountId.

```bash
curl http://localhost:8080/api/accounts/ACC-1
```

### `GET /api/accounts/{accountId}/transactions`

Every transfer that includes given accountId, either as source or destination.
Returns `200` with an empty array when the account exists but there are no transfers.

```bash
curl http://localhost:8080/api/accounts/ACC-1/transactions
```


### Error Handling

#### HTTP Status
- `400`   Request validation failed    
- `404`   Source or destination account does not exist                         
- `422`   Source account has insufficient funds                                

Example:
```json
{ "errorMessage": "Validation failed due to invalid fields: amount: amount must have at most 2 decimal places" }
```

## Tests

```bash
./mvnw test
```
