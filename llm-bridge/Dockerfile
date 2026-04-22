FROM python:3.12-slim

WORKDIR /app

COPY server.py /app/server.py
COPY docker-entrypoint.sh /app/docker-entrypoint.sh

RUN chmod +x /app/docker-entrypoint.sh

EXPOSE 8090

CMD ["/app/docker-entrypoint.sh"]
