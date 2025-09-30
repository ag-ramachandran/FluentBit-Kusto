# Fluent Bit ➜ Azure Data Explorer Demo

This sample shows how to collect container logs from a .NET app and a Java app with Fluent Bit and forward them to an Azure Data Explorer (Kusto) table. Everything runs locally through Docker Compose, while Fluent Bit tails JSON logs written to a shared volume.

## Stack Overview

- **dotnet-app** – .NET 8 console worker that emits structured JSON logs to `/logs/dotnet-app.log`.
- **java-app** – Java 17 application that writes similar JSON payloads to `/logs/java-app.log`.
- **shell-app** – Lightweight Bash container that generates Apache-style access logs at `/logs/shell-access.log`.
- **fluent-bit** – Collects the log files, enriches them with pipeline metadata, prints them to stdout, and ships them to Kusto using the official `out_kusto` output.

```
┌──────────┐      JSON logs      ┌────────────┐
│ dotnet   │ ──────────────────▶ │            │         ┌─────────┐
│ log prod │                     │ shared /logs│        │ Azure   │
└──────────┘                     │   volume   │        │ Data     │
┌──────────┐      JSON logs      │            │  logs  │ Explorer │
│ java     │ ──────────────────▶ │            │ ─────▶ │ (Kusto)  │
│ log prod │                     │            │        └─────────┘
┌──────────┐  Apache access logs  │            │             ▲
│ shell    │ ──────────────────▶ │            │             │
│ log gen  │                     └────────────┘         Fluent Bit
```

## Prerequisites

- Docker Engine 24+ and Docker Compose plugin.
- Azure AD application with permissions to ingest into your Kusto cluster.
- Kusto database and table (schema should align with the JSON payload).

## Configure Environment Secrets

Create a `.env` file in the project root (Compose automatically picks it up):

```bash
KUSTO_TENANT_ID=<your-tenant-id>
KUSTO_CLIENT_ID=<azure-ad-app-id>
KUSTO_CLIENT_SECRET=<azure-ad-app-secret>
KUSTO_CLUSTER_URL=https://<cluster-name>.<region>.kusto.windows.net
KUSTO_DATABASE=<database-name>
KUSTO_TABLE=<table-name>
```

> ℹ️ Fluent Bit fails to start if any of these variables are missing. Use temporary secrets or a credentials provider such as Azure Key Vault in production scenarios.

## Provision Kusto Tables & Mappings

Run the commands in `kusto/create-log-tables.kql` against your Azure Data Explorer database before starting ingestion. The script creates a single `UnifiedLogs` table with a JSON ingestion mapping that covers all three sources (dotnet, java, shell). Fields that are not emitted by a given producer simply remain `null`, while the `log_type` column lets you pivot or filter per source. A `Raw` column captures the full JSON payload for ad-hoc exploration.

## Run the Demo

```bash
# Build application images
docker compose build

# Start all services and stream Fluent Bit output
docker compose up
```

The console displays Fluent Bit’s own logs plus the JSON payloads as they pass through the pipeline. Let it run for a few minutes, then you can run Kusto queries (for example `LogsTable | take 10`) to verify ingestion.

### Stopping the stack

```bash
docker compose down
```

Named volumes (`app-logs`, `fluent-bit-state`) keep log files and Fluent Bit tail offsets between runs. Use `docker volume rm` if you need a clean slate.

## Project Layout

```
.
├── docker-compose.yml
├── fluent-bit/
│   └── fluent-bit.conf        # Tail inputs + Kusto output config
├── dotnet-app/
│   ├── Dockerfile             # Multi-stage build for the .NET 8 worker
│   ├── DotnetLogProducer.csproj
│   └── Program.cs             # Emits random log events every 5 seconds
├── java-app/
│   ├── Dockerfile             # Gradle build + distro for Java 17 worker
│   ├── build.gradle
│   ├── settings.gradle
│   └── src/main/java/com/example/JavaLogProducer.java
└── shell-app/
    ├── Dockerfile             # Alpine + Bash script emitting Apache logs
    └── generate-traffic.sh
```

## Customization Tips

- Adjust the `LOG_INTERVAL_SECONDS` environment variable in Compose to control throughput.
- Add additional Fluent Bit filters to enrich or transform logs before shipping to Kusto (for example the `kubernetes` or `rewrite_tag` filters).
- Extend the schema in Azure Data Explorer to include new fields emitted by the sample applications.
- Tweak the shell generator’s `SLEEP_INTERVAL` or `LOG_PATH` to change the velocity and destination of the synthetic access logs.

## Troubleshooting

- **Fluent Bit auth failures** – Confirm tenant, client, and secret are correct and that the service principal has the `Ingest` permission on the target table.
- **No logs arriving** – Check the Fluent Bit container logs; the `stdout` output should echo every record it forwards.
- **Schema mismatch** – If Kusto rejects records, create the table with columns that match the JSON keys (for example `timestamp: datetime`, `level: string`, `app: string`, `message: string`, `durationMs: long`, etc.).

## Next Steps

- Secure secrets via managed identities or workload identity federation.
- Replace the file tail input with the Docker logging driver or forward input for more production-like setups.
- Add health checks and alerting by connecting Fluent Bit to Azure Monitor or Grafana Loki alongside Kusto.
