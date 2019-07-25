#!/bin/bash -xeu

export GITHUB_TOKEN=${GITHUB_TOKEN_PSW}
export GITHUB_ORG=zeebe-io
export GITHUB_REPO=zeebe-kafka-exporter

# do github release
curl -sL https://github.com/aktau/github-release/releases/download/v0.7.2/linux-amd64-github-release.tar.bz2 | tar xjvf - --strip 3
./github-release release --user ${GITHUB_ORG} --repo ${GITHUB_REPO} --tag ${RELEASE_VERSION} --draft --name "Zeebe Kafka Exporter ${RELEASE_VERSION}" --description ""

# upload exporter
cd exporter/target

export ARTIFACT=zeebe-kafka-exporter-${RELEASE_VERSION}.jar
export CHECKSUM=${ARTIFACT}.sha1sum

export ARTIFACT_UBER=zeebe-kafka-exporter-${RELEASE_VERSION}-uber.jar
export CHECKSUM_UBER=${ARTIFACT_UBER}.sha1sum

# create checksum files
sha1sum ${ARTIFACT} > ${CHECKSUM}
sha1sum ${ARTIFACT_UBER} > ${CHECKSUM_UBER}

../../github-release upload --user ${GITHUB_ORG} --repo ${GITHUB_REPO} --tag ${RELEASE_VERSION} --name "${ARTIFACT}" --file "${ARTIFACT}"
../../github-release upload --user ${GITHUB_ORG} --repo ${GITHUB_REPO} --tag ${RELEASE_VERSION} --name "${CHECKSUM}" --file "${CHECKSUM}"
../../github-release upload --user ${GITHUB_ORG} --repo ${GITHUB_REPO} --tag ${RELEASE_VERSION} --name "${ARTIFACT_UBER}" --file "${ARTIFACT_UBER}"
../../github-release upload --user ${GITHUB_ORG} --repo ${GITHUB_REPO} --tag ${RELEASE_VERSION} --name "${CHECKSUM_UBER}" --file "${CHECKSUM_UBER}"

cd ../..

# upload samples

cd samples/target

export ARTIFACT=zeebe-kafka-exporter-samples-${RELEASE_VERSION}.jar
export CHECKSUM=${ARTIFACT}.sha1sum

export ARTIFACT_UBER=zeebe-kafka-exporter-samples-${RELEASE_VERSION}-uber.jar
export CHECKSUM_UBER=${ARTIFACT_UBER}.sha1sum

# create checksum files
sha1sum ${ARTIFACT} > ${CHECKSUM}
sha1sum ${ARTIFACT_UBER} > ${CHECKSUM_UBER}

../../github-release upload --user ${GITHUB_ORG} --repo ${GITHUB_REPO} --tag ${RELEASE_VERSION} --name "${ARTIFACT}" --file "${ARTIFACT}"
../../github-release upload --user ${GITHUB_ORG} --repo ${GITHUB_REPO} --tag ${RELEASE_VERSION} --name "${CHECKSUM}" --file "${CHECKSUM}"
../../github-release upload --user ${GITHUB_ORG} --repo ${GITHUB_REPO} --tag ${RELEASE_VERSION} --name "${ARTIFACT_UBER}" --file "${ARTIFACT_UBER}"
../../github-release upload --user ${GITHUB_ORG} --repo ${GITHUB_REPO} --tag ${RELEASE_VERSION} --name "${CHECKSUM_UBER}" --file "${CHECKSUM_UBER}"

cd ../..

# upload serde
cd serde/target

export ARTIFACT=zeebe-kafka-exporter-serde-${RELEASE_VERSION}.jar
export CHECKSUM=${ARTIFACT}.sha1sum

# create checksum files
sha1sum ${ARTIFACT} > ${CHECKSUM}

../../github-release upload --user ${GITHUB_ORG} --repo ${GITHUB_REPO} --tag ${RELEASE_VERSION} --name "${ARTIFACT}" --file "${ARTIFACT}"
../../github-release upload --user ${GITHUB_ORG} --repo ${GITHUB_REPO} --tag ${RELEASE_VERSION} --name "${CHECKSUM}" --file "${CHECKSUM}"
