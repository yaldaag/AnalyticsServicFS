# Analytics Service

## What This Service Does
This service exposes a REST API for querying bounded analytics results from Google BigQuery. It validates requests, builds parameterized queries from trusted server-side metric configuration, returns the first page directly, and caches overflow pages in Redis behind opaque page tokens.

The MVP is intentionally narrow:
- Java 17, Maven, Spring Boot
- Fixed time-series row shape: `{ "date": "YYYY-MM-DD", "value": 123 }`
- Trusted tenant/member context comes from request headers
- Metric definitions live in a structured YAML configuration file and reload on a fixed interval

## How To Run The Service
### Prerequisites
- Java 17
- Maven 3.9+
- Redis available on `localhost:6379`
- Google Cloud credentials available to the BigQuery client

### Start The Application
```bash
mvn spring-boot:run
```

The default port is `8080`.

## API Overview
### Query Endpoint
`POST /analytics/query`

Required headers:
- `X-Tenant-Id`
- `X-Member-Id`

Request body:
```json
{
  "metric_name": "daily_active_users",
  "date_start": "2011-11-11",
  "date_end": "2012-12-12"
}
```

Example request:
```bash
curl -X POST "http://localhost:8080/analytics/query" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: wix" \
  -H "X-Member-Id: asaf_fs_client" \
  -d '{
    "metric_name": "daily_active_users",
    "date_start": "2011-11-11",
    "date_end": "2012-12-12"
  }'
```

Successful response when the result fits in one page:
```json
{
  "metric_name": "daily_active_users",
  "tenant_id": "wix",
  "member_id": "asaf_fs_client",
  "date_start": "2011-11-11",
  "date_end": "2012-12-12",
  "rows": [
    {
      "date": "2011-11-11",
      "value": 1234
    }
  ],
  "metadata": {
    "row_count": 1,
    "has_more": false,
    "next_page_token": null,
    "execution_time_ms": 240,
    "big_query_job_id": "bq-job-123"
  }
}
```

Successful response when the result spans multiple pages:
```json
{
  "metric_name": "daily_active_users",
  "tenant_id": "wix",
  "member_id": "asaf_fs_client",
  "date_start": "2011-11-11",
  "date_end": "2012-12-12",
  "rows": [
    {
      "date": "2011-11-11",
      "value": 1234
    }
  ],
  "metadata": {
    "row_count": 100,
    "has_more": true,
    "next_page_token": "opaque-page-token",
    "execution_time_ms": 240,
    "big_query_job_id": "bq-job-123"
  }
}
```

### Next Page Endpoint
`GET /analytics/query/pages/{page_token}`

Required headers:
- `X-Tenant-Id`
- `X-Member-Id`

Example request:
```bash
curl "http://localhost:8080/analytics/query/pages/opaque-page-token" \
  -H "X-Tenant-Id: wix" \
  -H "X-Member-Id: asaf_fs_client"
```

Example paginated response:
```json
{
  "rows": [
    {
      "date": "2011-11-12",
      "value": 1402
    }
  ],
  "metadata": {
    "row_count": 100,
    "has_more": true,
    "next_page_token": "next-opaque-page-token"
  }
}
```

Example last-page response:
```json
{
  "rows": [
    {
      "date": "2011-11-13",
      "value": 1330
    }
  ],
  "metadata": {
    "row_count": 40,
    "has_more": false,
    "next_page_token": null
  }
}
```

## Error Response Examples
All API errors use the same minimal structure:
```json
{
  "error_code": "INVALID_DATE_RANGE",
  "message": "date_start must be before or equal to date_end"
}
```

Common examples:
```json
{
  "error_code": "UNSUPPORTED_METRIC",
  "message": "Unsupported metric_name: unknown_metric"
}
```

```json
{
  "error_code": "PAGE_TOKEN_NOT_FOUND",
  "message": "The requested page token does not exist or expired"
}
```

```json
{
  "error_code": "PAGE_TOKEN_OWNERSHIP_MISMATCH",
  "message": "The requested page token does not belong to the provided tenant/member context"
}
```

## Configuration Overview
Application settings live in `src/main/resources/application.yml`.

Important properties:
- `analytics.default-page-size`: default page size, `100`
- `analytics.max-date-range-days`: maximum allowed request window
- `analytics.page-ttl`: Redis TTL for cached overflow pages
- `analytics.metric-reload-interval`: metric configuration reload interval
- `analytics.big-query-timeout`: reserved for BigQuery execution protection
- `analytics.metrics-config-location`: metric definition file
- `spring.data.redis.host` / `spring.data.redis.port`: Redis connection settings

Metric definitions live in `src/main/resources/metrics/metrics.yml`.

The metric catalog is now declarative: you can add or change many metrics by editing YAML rather than changing Java code, as long as the metric still fits the fixed `{ date, value }` response shape.

Example metric definitions:
```yaml
metrics:
  - name: daily_active_users
    source:
      table: demo.analytics.daily_active_users
      date_column: activity_date
      tenant_column: tenant_id
      member_column: member_id
    value:
      aggregation: COUNT
    predicates:
      - column: event_type
        operator: "="
        literal: login

  - name: daily_revenue
    source:
      table: demo.analytics.daily_revenue
      date_column: revenue_date
      tenant_column: tenant_id
      member_column: member_id
    value:
      aggregation: SUM
      column: revenue_amount
```

Config fields:
- `source.table`: BigQuery table or view
- `source.date_column`: date column used for filtering, grouping, and output alias `date`
- `source.tenant_column`: tenant ownership column
- `source.member_column`: member ownership column
- `value.aggregation`: one of `COUNT`, `SUM`, `AVG`
- `value.column`: required for `SUM` and `AVG` unless `value.expression` is used
- `value.expression`: optional controlled expression for aggregate inputs such as `IFNULL(amount, 0)`
- `predicates[]`: optional static filters with allowlisted operators and parameterized literals

Supported aggregation types in the MVP:
- `COUNT`
- `SUM`
- `AVG`

Current guardrails:
- the API response shape remains fixed to `date` and `value`
- request parameters are still bound as named query parameters
- raw SQL templates are not allowed in config
- predicate operators are restricted to a small allowlist
- identifiers and value expressions are validated before reload succeeds

## Testing Instructions
Run the full test suite:
```bash
mvn test
```

Run a compile-only validation:
```bash
mvn -q -DskipTests compile
```

The current suite covers:
- request validation
- metric configuration validation
- query builder behavior
- controller responses
- pagination and cached page ownership checks

## Important Assumptions And Limitations
- The service is an MVP and currently supports only fixed time-series results with `date` and `value`.
- Tenant/member identity is trusted from `X-Tenant-Id` and `X-Member-Id` headers; a real auth layer should replace or back these headers later.
- BigQuery SQL is generated only from safe, structured metric configuration. Raw SQL is intentionally not accepted from clients or metric config in this phase.
- The implementation fetches the full bounded result in the synchronous request path and uses Redis only for temporary overflow pages.
- Redis is not a durable result store and is not suitable for arbitrarily large result sets.
- For future large-scale workloads, queries should move to an async execution model that splits work by date range and stores durable results outside Redis.
