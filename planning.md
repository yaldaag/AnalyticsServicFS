# Analytics Service Implementation Notes

## Purpose
This file records the current implementation decisions for the Analytics Service MVP and serves as the workspace-owned copy of the plan summary required by the prompt.

## Current Implemented Decisions
- The service is a greenfield Maven + Spring Boot Java 17 application.
- Trusted caller context is carried by `X-Tenant-Id` and `X-Member-Id` headers on both endpoints.
- The POST request body accepts:
  - `metric_name`
  - `date_start`
  - `date_end`
- The response row shape is fixed to:
  - `date`
  - `value`
- Metric definitions are stored in `src/main/resources/metrics/metrics.yml`.
- Metric query behavior is driven by a structured YAML schema with `source`, `value`, and optional static `predicates` blocks.
- Metric configuration is loaded at startup and reloaded on a fixed interval.
- BigQuery access is isolated behind `AnalyticsRepository`.
- Redis page caching is isolated behind `ResultPageCache`.
- Overflow pages are stored with:
  - `result_set_id`
  - cached rows
  - `tenant_id`
  - `member_id`
  - `has_more`
  - `next_page_token`
- Page tokens are opaque random values and do not expose Redis keys.
- Error responses contain only:
  - `error_code`
  - `message`

## Change Log
### 2026-05-31
- Created the initial Spring Boot project scaffold from scratch.
- Implemented the REST controller, service orchestration, validation flow, BigQuery query builder, BigQuery repository adapter, Redis cache adapter, and global error handling.
- Chose request headers as the trusted transport for tenant/member ownership validation.
- Locked the MVP to fixed time-series metric responses with `{ date, value }`.
- Added unit and controller tests for validation, configuration, query generation, pagination, and token ownership behavior.
- Added `README.md` with run instructions, API examples, response contracts, configuration details, testing, and limitations.
- Refactored metric definitions into a richer declarative schema so common metric additions and query changes can be made in `metrics.yml` without Java code changes.
- Added validation and query builder support for nested metric `source`, aggregated `value`, optional aggregate expressions, and static configured predicates.

## Future Extension Notes
- Replace trusted headers with authenticated identity claims from an upstream auth layer.
- Add a durable async execution model for very large result sets.
- Consider exposing configured dimensions and filters in a later API version if needed.
