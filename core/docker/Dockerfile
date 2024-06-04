#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
FROM ghcr.io/airlift/jvmkill:latest AS jvmkill

FROM registry.access.redhat.com/ubi9/ubi-minimal:latest AS jdk-download
ARG JDK_DOWNLOAD_LINK
ARG JDK_VERSION
ENV JAVA_HOME="/usr/lib/jvm/jdk-${JDK_VERSION}"

RUN \
    set -xeuo pipefail && \
    microdnf install -y tar gzip && \
    # Install JDK from the provided archive link \
    echo "Downloading JDK from ${JDK_DOWNLOAD_LINK}" && \
    mkdir -p "${JAVA_HOME}" && \
    curl -#LfS "${JDK_DOWNLOAD_LINK}" | tar -zx --strip 1 -C "${JAVA_HOME}"

# Use ubi9 minimal as it's more secure
FROM registry.access.redhat.com/ubi9/ubi-minimal:latest
ARG JDK_VERSION
ENV JAVA_HOME="/usr/lib/jvm/jdk-${JDK_VERSION}"
ENV PATH=$PATH:$JAVA_HOME/bin
ENV CATALOG_MANAGEMENT=static
COPY --from=jdk-download $JAVA_HOME $JAVA_HOME

RUN \
    set -xeu && \
    microdnf update -y && \
    microdnf install -y tar less python3 shadow-utils && \
    update-alternatives --install /usr/bin/python python /usr/bin/python3 1 && \
    groupadd trino --gid 1000 && \
    useradd trino --uid 1000 --gid 1000 --create-home && \
    mkdir -p /usr/lib/trino /data/trino && \
    chown -R "trino:trino" /usr/lib/trino /data/trino

ARG TRINO_VERSION
COPY --chown=trino:trino trino-cli-${TRINO_VERSION}-executable.jar /usr/bin/trino
COPY --chown=trino:trino trino-server-${TRINO_VERSION} /usr/lib/trino
COPY --chown=trino:trino default/etc /etc/trino
COPY --chown=trino:trino --from=jvmkill /libjvmkill.so /usr/lib/trino/bin

RUN curl --fail -o /usr/lib/trino/plugin/iceberg/trino-runtime-1.56.5.jar https://tabular-repository-public.s3.amazonaws.com/releases/io/tabular/trino-runtime/1.56.5/trino-runtime-1.56.5.jar

RUN ln -s /usr/lib/trino/plugin/iceberg/trino-runtime*.jar /usr/lib/trino/plugin/iceberg/hdfs/
RUN ln -s /usr/lib/trino/plugin/iceberg/http*5-*.jar /usr/lib/trino/plugin/iceberg/hdfs/

RUN  curl --fail -o /usr/lib/trino-plugin-1.56.5-all.jar https://tabular-repository-public.s3.amazonaws.com/releases/io/tabular/trino-plugin/1.56.5/trino-plugin-1.56.5.jar


EXPOSE 8080
USER trino:trino
CMD ["/usr/lib/trino/bin/run-trino"]
HEALTHCHECK --interval=10s --timeout=5s --start-period=10s \
  CMD /usr/lib/trino/bin/health-check
