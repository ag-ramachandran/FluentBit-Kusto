# syntax=docker/dockerfile:1
FROM mcr.microsoft.com/dotnet/sdk:8.0 AS build
WORKDIR /src
COPY DotnetLogProducer.csproj ./
RUN dotnet restore
COPY . .
RUN dotnet publish -c Release -o /app/publish

FROM mcr.microsoft.com/dotnet/runtime:8.0 AS runtime
WORKDIR /app
COPY --from=build /app/publish ./
ENV DOTNET_LOG_PATH=/logs/dotnet-app.log
ENV LOG_INTERVAL_SECONDS=0.1
ENV APP_NAME=dotnet-log-producer
ENTRYPOINT ["dotnet", "DotnetLogProducer.dll"]
