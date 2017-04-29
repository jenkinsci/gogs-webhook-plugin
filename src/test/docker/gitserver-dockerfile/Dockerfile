FROM gogs/gogs:0.11.4

ARG FIRST_USER=dev

COPY ./setup-gogs.sh /app/setup-gogs.sh
COPY ./app.ini /etc/templates/app.ini
COPY ./repos-to-mirror /app/repos-to-mirror

RUN echo 'sed "s#ROOT_URL.*=.*#ROOT_URL=${EXTERNAL_URL}#g" /etc/templates/app.ini > /data/gogs/conf/app.ini' \
  >> /app/gogs/docker/s6/gogs/setup \
  && bash /app/setup-gogs.sh

VOLUME "/app/data"