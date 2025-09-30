# syntax=docker/dockerfile:1

FROM alpine:3.20

RUN apk add --no-cache bash coreutils

WORKDIR /app
COPY generate-traffic.sh ./
RUN chmod +x generate-traffic.sh

ENV LOG_PATH=/logs/shell-access.log
ENV SLEEP_INTERVAL=0.001

ENTRYPOINT ["/app/generate-traffic.sh"]
