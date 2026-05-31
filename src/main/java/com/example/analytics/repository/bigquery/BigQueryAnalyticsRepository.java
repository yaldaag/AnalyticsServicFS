package com.example.analytics.repository.bigquery;

import com.example.analytics.domain.AnalyticsRow;
import com.example.analytics.domain.QueryExecutionResult;
import com.example.analytics.repository.AnalyticsRepository;
import com.example.analytics.service.query.BuiltQuery;
import com.example.analytics.web.ApiException;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.Job;
import com.google.cloud.bigquery.JobId;
import com.google.cloud.bigquery.TableResult;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

@Repository
public class BigQueryAnalyticsRepository implements AnalyticsRepository {

    private static final Logger log = LoggerFactory.getLogger(BigQueryAnalyticsRepository.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final BigQuery bigQuery;

    public BigQueryAnalyticsRepository(BigQuery bigQuery) {
        this.bigQuery = bigQuery;
    }

    @Override
    public QueryExecutionResult execute(BuiltQuery query) {
        try {
            log.info("Executing BigQuery query");
            long start = System.currentTimeMillis();
            JobId jobId = JobId.of(UUID.randomUUID().toString());
            Job job = bigQuery.create(com.google.cloud.bigquery.JobInfo.newBuilder(query.queryConfiguration()).setJobId(jobId).build());
            Job completedJob = job.waitFor();
            if (completedJob == null) {
                throw new ApiException(HttpStatus.GATEWAY_TIMEOUT, "BIGQUERY_TIMEOUT", "BigQuery job did not complete");
            }
            if (completedJob.getStatus().getError() != null) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "BIGQUERY_QUERY_FAILED", "BigQuery query execution failed");
            }

            TableResult response = completedJob.getQueryResults();
            List<AnalyticsRow> rows = new ArrayList<>();
            for (FieldValueList row : response.iterateAll()) {
                LocalDate date = LocalDate.parse(row.get("date").getStringValue(), DATE_FORMATTER);
                long value = Math.round(row.get("value").getNumericValue().doubleValue());
                rows.add(new AnalyticsRow(date, value));
            }

            long executionTimeMs = System.currentTimeMillis() - start;
            log.info("BigQuery query completed jobId={} rowCount={} executionTimeMs={}",
                    jobId.getJob(), rows.size(), executionTimeMs);
            return new QueryExecutionResult(rows, executionTimeMs, jobId.getJob());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ApiException(HttpStatus.GATEWAY_TIMEOUT, "BIGQUERY_TIMEOUT", "BigQuery query timed out");
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("BigQuery query execution failed", exception);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "BIGQUERY_QUERY_FAILED", "BigQuery query execution failed");
        }
    }
}
