FROM eclipse-temurin:21.0.11_10-jdk@sha256:57865c22b954cf920cb05a610af81d577e89783282514ba071e99c7357f6c769 AS build
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

FROM eclipse-temurin:21.0.11_10-jre@sha256:371da296b8cb74c7e53fbe7083d5374befc0011b493231d97d45fa789915e434
WORKDIR /app

# Explicit user/group rather than relying on the incidental "ubuntu" (uid 1000) user
# present in this base image - an implementation detail of the base image, not a
# contractual guarantee, that could disappear in a future base image update. 10001 is
# used (not 1000): confirmed by a real build failure ("GID 1000 already exists") that
# 1000 is already taken by that same incidental ubuntu group.
RUN groupadd --gid 10001 taskira \
    && useradd --uid 10001 --gid taskira --shell /usr/sbin/nologin --no-create-home taskira

COPY --from=build --chown=taskira:taskira /app/target/*.jar app.jar

# Pre-created (not left to LocalFileSystemStorage's @PostConstruct) so a named volume
# later mounted at this path inherits taskira:taskira ownership via Docker's volume
# copy-up behavior - mounting an empty named volume over an image directory copies that
# directory's existing content and permissions into the volume on first use, but only if
# it already exists with the right owner at image build time.
RUN mkdir -p /var/lib/taskira/attachments \
    && chown -R taskira:taskira /var/lib/taskira

USER taskira

EXPOSE 8080
EXPOSE 9091

ENTRYPOINT ["java", "-jar", "app.jar"]
