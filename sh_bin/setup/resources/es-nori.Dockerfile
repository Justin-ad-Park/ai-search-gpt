ARG ES_VERSION=8.13.4
FROM docker.elastic.co/elasticsearch/elasticsearch:${ES_VERSION}
ARG ES_VERSION

USER root
COPY analysis-nori-${ES_VERSION}.zip /tmp/analysis-nori.zip
RUN /usr/share/elasticsearch/bin/elasticsearch-plugin install --batch file:///tmp/analysis-nori.zip \
    && rm -f /tmp/analysis-nori.zip \
    && chown -R elasticsearch:root /usr/share/elasticsearch/plugins

USER 1000
