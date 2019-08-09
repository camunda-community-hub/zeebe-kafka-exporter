#!/bin/bash
set -ef -o pipefail

PROPERTIES=("-DskipTests -Dcheckstyle.skip")

# Assume if CHANGE_ID defined that we're building a PR
if [ ! -z "${CHANGE_ID}" ]; then
  PROPERTIES+=("-Dsonar.pullrequest.key=${CHANGE_ID}")

  if [ ! -z "${CHANGE_BRANCH}" ]; then
    PROPERTIES+=("-Dsonar.pullrequest.branch=${CHANGE_BRANCH}")
  fi

  if [ ! -z "${CHANGE_TARGET}" ]; then
    PROPERTIES+=("-Dsonar.pullrequest.base=${CHANGE_TARGET}")
    git fetch --no-tags "${GIT_URL}" "+refs/heads/${CHANGE_TARGET}:refs/remotes/origin/${CHANGE_TARGET}"
  fi
else
  if [ ! -z "${GIT_BRANCH}" ]; then
    PROPERTIES+=("-Dsonar.branch.name=${GIT_BRANCH}")
  fi

  git fetch --no-tags "${GIT_URL}" +refs/heads/master:refs/remotes/origin/master
fi

mvn -s .ci/settings.xml -P sonar sonar:sonar ${PROPERTIES[@]}
