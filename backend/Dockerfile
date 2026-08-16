FROM eclipse-temurin:24.0.2_12-jdk@sha256:7493205ffe6caa8074fa8a06a276bb1c5ac41d3dd0fd43a0db66d7f776e80b3e AS build
WORKDIR /app

RUN apt-get update \
    && apt-get install --yes --no-install-recommends unzip \
    && rm -rf /var/lib/apt/lists/*

COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline

COPY src ./src
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:24.0.2_12-jre@sha256:8cb2387a28af84cf0db0948d9c67d4480192f4e567027a3963f145d218e8b4f2
WORKDIR /app

# Explicit user/group rather than relying on the incidental "ubuntu" (uid 1000) user
# present in this base image - an implementation detail of the base image, not a
# contractual guarantee, that could disappear in a future base image update. 10001 is
# used (not 1000): confirmed by a real build failure ("GID 1000 already exists") that
# 1000 is already taken by that same incidental ubuntu group.
RUN groupadd --gid 10001 taskira \
    && useradd --uid 10001 --gid taskira --shell /usr/sbin/nologin --no-create-home taskira

COPY --from=build --chown=taskira:taskira /app/target/*.jar app.jar

# Matches application.yaml's default app.attachments.storage-path (./data/attachments,
# relative to WORKDIR /app). Pre-created (not left to LocalFileSystemStorage's
# @PostConstruct) so a named volume later mounted at this path inherits taskira:taskira
# ownership via Docker's volume copy-up behavior - mounting an empty named volume over an
# image directory copies that directory's existing content and permissions into the
# volume on first use, but only if it already exists with the right owner at build time.
RUN mkdir -p /app/data/attachments \
    && chown -R taskira:taskira /app/data

USER taskira

EXPOSE 8080
EXPOSE 9091

ENTRYPOINT ["java", "-jar", "app.jar"]
