FROM folioci/alpine-jre-openjdk17:latest

# Install latest patch versions of packages: https://pythonspeed.com/articles/security-updates-in-docker/
USER root
RUN apk upgrade --no-cache && apk add openjdk17-jdk --no-cache

COPY run-java.sh ${JAVA_APP_DIR}/
RUN chmod 755 ${JAVA_APP_DIR}/run-java.sh

USER folio

ENV VERTICLE_FILE mod-users-bl-fat.jar

# Set the location of the verticles
ENV VERTICLE_HOME /usr/verticles

# Copy your fat jar to the container
COPY target/${VERTICLE_FILE} ${VERTICLE_HOME}/${VERTICLE_FILE}

# Expose this port locally in the container.
EXPOSE 8081
