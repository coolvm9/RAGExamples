# YugabyteDB Setup and Installation Guide

## Prerequisites

- Ensure Docker is installed and running on your machine. You can download and install Docker from [Docker's official website](https://www.docker.com/get-started).

## Step 1: Pull YugabyteDB Docker Image

To install YugabyteDB locally, first pull the official YugabyteDB Docker image by running:

```bash
docker pull yugabytedb/yugabyte:latest
```

This will pull the latest version of YugabyteDB.

## Step 2: Run YugabyteDB Container

Once the image is downloaded, start the YugabyteDB container using the following command:

```bash
docker run -d --name yugabytedb \
    -p 5433:5433 \
    -p 9042:9042 \
    -p 7000:7000 \
    yugabytedb/yugabyte:latest bin/yugabyted start --daemon=false
```

## Step 3: Access YugabyteDB

After starting the container, you can access YugabyteDB in the following ways:

### 1. Access YSQL (PostgreSQL-compatible):

- **Host:** `localhost`
- **Port:** `5433`
- **Default Database:** `yugabyte`
- **Default User:** `yugabyte`

To connect using YSQL’s command-line tool, run:

```bash
docker exec -it yugabytedb bin/ysqlsh -h localhost -p 5433 -U yugabyte
```

### 2. Access the YugabyteDB Admin Console:

Open your web browser and navigate to [http://localhost:7000](http://localhost:7000) to access the Admin Console.

## Step 4: Stopping and Restarting the YugabyteDB Container

To stop the YugabyteDB container, run:

```bash
docker stop yugabytedb
```

To start it again:

```bash
docker start yugabytedb
```

## Step 5: Remove the YugabyteDB Container

If you need to remove the YugabyteDB container, run:

```bash
docker rm -f yugabytedb
```

## Step 6: Access the Docker Container Logs

To check the logs of the running YugabyteDB container, you can use the following command:

```bash
docker logs yugabytedb
```

## Step 7: Connecting to YugabyteDB from PgAdmin

1. **Launch PgAdmin:** Open your installed PgAdmin tool.
2. **Create New Server Connection:**
    - Right-click on "Servers" in the left-hand panel, then click "Create" > "Server".
3. **Fill in the Connection Details:**
    - **Name:** `YugabyteDB`
    - **Host:** `localhost`
    - **Port:** `5433`
    - **Username:** `yugabyte`
    - **Password:** `yugabyte`
    - **Database:** `yugabyte`
4. **Save and Connect:** Click "Save" to establish the connection.

## Step 8: Additional Configuration and Documentation

For further details, please refer to the [YugabyteDB Documentation](https://docs.yugabyte.com/).