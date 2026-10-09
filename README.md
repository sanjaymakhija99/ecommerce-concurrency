# E-commerce Concurrency Demo (Spring Boot 3.5, Java 21, Maven)

Shows where multithreading is still needed on the service layer even though Spring Boot
already handles request-level concurrency.

## Run
    mvn spring-boot:run
    mvn test

## Endpoints
| Use case | Request |
|---|---|
| Parallel fan-out (CompletableFuture + virtual threads) | `GET /api/products/P-100/page` (~500ms) vs `/page-sequential` (~1200ms) |
| Oversell protection (AtomicInteger CAS) | `POST /api/orders` `{"productId":"P-100","quantity":1,"email":"a@b.com"}` |
| Fire-and-forget (@Async + custom pool) | watch logs after placing an order: `notify-1` threads |
| Throttling (Semaphore) | `POST /api/flash-sale/P-FLASH/buy?email=a@b.com` |
| Partitioned report (ThreadPoolTaskExecutor) | `GET /api/reports/sales` (8 regions x 600ms, ~1.2s instead of ~4.8s) |
| Scheduled job | `AbandonedCartJob` logs every 60s |

## Try the race condition
    seq 1 200 | xargs -P 50 -I{} curl -s -o /dev/null -w "%{http_code}\n" \
      -X POST localhost:8080/api/flash-sale/P-FLASH/buy?email=u{}@x.com | sort | uniq -c
    curl localhost:8080/api/inventory/P-FLASH     # never below 0

## Key ideas
- Separate pools per concern (bulkhead): `notificationExecutor`, `reportExecutor`, `ioExecutor` (virtual threads)
- Bounded queues + rejection policy
- MDC `requestId` propagated to worker threads via `TaskDecorator`
- Timeouts + graceful degradation on non-critical calls
- `@Async` runs outside the caller's transaction
