I want to build an analytics service from scratch.

The service should expose a REST API that allows clients to query analytics data from Google BigQuery.

Technology requirements:

* Maven
* Java 17
* Spring Boot
* REST API
* Google BigQuery
* Redis for cached paginated query results
* Use SOLID design principles
* Keep the implementation simple enough for an MVP

Before writing code, please create an implementation plan only. Do not implement yet.

Create a file named `planning.md`.
Every time the plan changes, extends, or removes a decision, update `planning.md` and document the change clearly.

Also create a `README.md` file that explains:

* What the service does
* How to run the service
* API request examples
* API response examples
* Error response examples
* Configuration overview
* Testing instructions
* Important assumptions and limitations

Functional requirements:

* Expose a REST endpoint for analytics queries.
* The API should accept a JSON analytics request in this format:

```json
{
  "metric_name": "daily_active_users",
  "tenant_id": "wix",
  "member_id": "asaf_fs_client",
  "date_start": "2011-11-11",
  "date_end": "2012-12-12"
}
```

Field meanings:

* `metric_name`: the analytics metric to query.
* `tenant_id`: the company requesting the analytics data.
* `member_id`: the user inside that company.
* `date_start`: the start date for the query, in `YYYY-MM-DD` format.
* `date_end`: the end date for the query, in `YYYY-MM-DD` format.

Response requirements:

* The API should return a consistent JSON response.
* The API should not return unbounded result sets.
* The response page size should be 100 rows by default and configurable.
* If the query returns the configured page size or fewer, return all rows directly with `has_more = false`.
* If the query returns more than the configured page size:

  * return only the first page in the initial response
  * cache the remaining result pages in Redis
  * split the cached result into chunks using the configured page size
  * generate a unique result set ID for the full query result
  * generate a unique opaque page token for each cached page
  * associate each cached page with the `tenant_id` and `member_id`
  * return `has_more = true`
  * return the page token for the next page
* The page token should not expose internal Redis keys or sensitive tenant/member information.
* The service should add a second REST endpoint to fetch the next page by page token.
* When fetching the next page, the service should validate that the page token belongs to the correct tenant/member context.
* If the requested page has another page after it, return `has_more = true` and the next page token.
* If the requested page is the last page, return `has_more = false` and `next_page_token = null`.

Successful response format when the result has the configured page size or fewer rows:

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

Successful response format when the result has more than the configured page size:

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
    "next_page_token": "page_token_abc123",
    "execution_time_ms": 240,
    "big_query_job_id": "bq-job-123"
  }
}
```

Next page endpoint:

```http
GET /analytics/query/pages/{page_token}
```

Next page response format:

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
    "next_page_token": "page_token_def456"
  }
}
```

Last page response format:

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

Error response format:

```json
{
  "error_code": "INVALID_DATE_RANGE",
  "message": "date_start must be before or equal to date_end"
}
```

Error response rules:

* Error responses should contain only:

  * `error_code`
  * `message`
* Do not include stack traces in the API response.
* Do not include internal exception details in the API response.
* Log internal exception details separately for debugging and observability.

Validation requirements:

* Reject any request with missing required fields.
* Reject unsupported `metric_name` values.
* Reject invalid date ranges:

  * `date_start` after `date_end`
  * dates in the future
  * invalid date format
* Use `YYYY-MM-DD` as the required date format.
* Reject missing or blank `tenant_id`.
* Reject missing or blank `member_id`.
* Return clear HTTP error responses for invalid input.

Metric configuration requirements:

* Metric names should be configurable in a server-side configuration file.
* The service should allow only `metric_name` values that exist in this configuration.
* The configuration should be reloaded every 1 hour by default.
* The reload interval should be configurable.
* Each configured metric should define how to build the BigQuery SQL query.
* The client must not be allowed to send raw SQL.
* SQL should be created only from trusted server-side configuration.
* Prefer a safe structured metric configuration, for example:

  * table name
  * date column
  * aggregation type
  * target column
  * allowed filters
  * allowed dimensions, if needed
* Avoid arbitrary raw SQL in configuration where possible.

Architecture requirements:

Separate responsibilities clearly:

* Controller: handles REST requests and responses only.
* Service: handles business logic, validation flow, pagination decision, cache coordination, and execution flow.
* Metric configuration provider: loads, validates, caches, and reloads metric configuration.
* Query builder: converts a valid analytics request and metric configuration into a BigQuery query.
* Repository/interface: represents the analytics data provider.
* BigQuery adapter: implements the repository and connects to Google BigQuery.
* Result page cache interface: represents cached paginated results.
* Redis result page cache adapter: implements result page caching using Redis.

The BigQuery adapter should be hidden behind an interface so it can be replaced later with another provider, for example Snowflake, with minimal changes.

The Redis cache should also be hidden behind an interface so it can be replaced later with another cache or result store with minimal changes.

BigQuery requirements:

* Use parameterized queries for values such as dates, `tenant_id`, and `member_id`.
* Do not use `SELECT *`.
* Always use date filters.
* Add cost/performance protection where reasonable, such as:

  * required date filters
  * maximum allowed date range
  * configurable maximum query size or timeout where possible
* Keep BigQuery-specific code isolated inside the BigQuery adapter.
* Log query execution metadata, but do not log sensitive data or full raw user input.

Redis/cache requirements:

* Use Redis to cache paginated query results when the BigQuery result contains more than the configured page size.
* Store cached result pages as chunks of the configured page size.
* Generate one unique result set ID for the complete query result.
* Generate a unique opaque page token for each page.
* Store enough metadata with each cached page to support:

  * tenant/member ownership validation
  * `has_more`
  * `next_page_token`
  * page expiration
* The page token returned to the client must not expose the internal Redis key.
* If a page token does not exist or expired, return a clear error response.
* Do not use Redis as a permanent result store.
* For very large result sets, Redis should not be used as the main storage for all results. The plan should describe a future async job model where large queries run in the background and results are stored in durable storage, such as object storage, a temporary BigQuery table, or a database table.

Pagination/cache flow:

* Client sends `POST /analytics/query`.
* Service validates the request.
* Service builds and executes the BigQuery query.
* If the result has the configured page size or fewer rows:

  * return all rows directly
  * set `has_more = false`
  * set `next_page_token = null`
* If the result has more than the configured page size:

  * return the first page directly
  * split the remaining rows into chunks using the configured page size
  * cache each chunk in Redis
  * associate all chunks with the same result set ID
  * assign each chunk a unique opaque page token
  * store tenant/member ownership metadata with each page
  * return `has_more = true`
  * return the token for the next page
* Client sends `GET /analytics/query/pages/{page_token}` to fetch the next page.
* Service loads the page from Redis.
* Service validates that the page belongs to the correct tenant/member context.
* Service returns the cached rows and metadata.
* If another page exists, return `has_more = true` and the next page token.
* If no more pages exist, return `has_more = false` and `next_page_token = null`.

Scalability requirements:

* The analytics service should be stateless so multiple instances can run behind a load balancer.
* Design the service so it can later act as a worker in a larger analytics execution system.
* For larger-scale execution, prefer splitting large analytics work by date ranges using `date_start` and `date_end`.
* The design should support a future orchestrator service that can split one large date range into smaller date-range tasks.

Example of one worker processing one date range:

```json
{
  "metric_name": "daily_active_users",
  "tenant_id": "wix",
  "member_id": "asaf_fs_client",
  "date_start": "2011-11-11",
  "date_end": "2011-12-31"
}
```

Example of another worker processing a different date range:

```json
{
  "metric_name": "daily_active_users",
  "tenant_id": "wix",
  "member_id": "asaf_fs_client",
  "date_start": "2012-01-01",
  "date_end": "2012-03-31"
}
```

Scalability reasoning:

* BigQuery tables are commonly partitioned by date.
* Partitioning large tables can improve query performance and control cost by reducing the amount of data read.
* Splitting by date range works well with date-partitioned BigQuery tables.
* Deep row-based pagination, for example using large offsets, can be inefficient because the query may still need to calculate, sort, and skip earlier rows before returning the requested range.
* For this reason, date-range splitting is preferred for large-scale execution.
* Redis page caching helps avoid returning very large HTTP responses, but it should only be used for temporary cached pages, not as a permanent result store.

Testing requirements:

* Add unit tests for request validation.
* Add unit tests for metric configuration validation.
* Add unit tests for query-building logic.
* Add tests for the REST controller.
* Add tests for unsupported metrics.
* Add tests for invalid dates and invalid date ranges.
* Add tests for response pagination behavior.
* Add tests for Redis cache behavior using a mocked cache interface.
* Add tests for expired or missing page tokens.
* Make the BigQuery adapter mockable so tests do not require real BigQuery credentials.
* Make the Redis cache adapter mockable so tests do not require a real Redis instance.

Logging and observability requirements:

* Add logs for critical steps:

  * incoming HTTP request
  * request validation result
  * selected metric
  * metric configuration reload success/failure
  * query execution start/end
  * BigQuery errors
  * Redis cache write/read
  * page token generation
  * expired or missing page token
* Handle exceptions with clear error responses.
* Log errors and exceptions separately and clearly for future use with Loki and Grafana.
* Use structured logs where possible.

Please create a step-by-step implementation plan including:

1. Proposed project structure
2. Main classes and responsibilities
3. API contract
4. Successful and error response contracts
5. Metric configuration design
6. Configuration reload approach
7. Query-building approach
8. Pagination and Redis caching approach
9. Error handling approach
10. Testing plan
11. Observability/logging plan
12. Scalability plan, including horizontal scaling, date-range task splitting, Redis result-page caching, and the limitations of deep row-based pagination
13. README.md structure and content
14. Assumptions and open questions
15.  If anything in this request is unclear, missing, contradictory, or could be interpreted in more than one way, stop and ask me clarifying questions before making decisions or writing code.  

Do not write code yet. First give me the plan and save it to `planning.md`.
