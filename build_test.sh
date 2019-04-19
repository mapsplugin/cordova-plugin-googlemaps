#!/bin/bash

DO_BUILD=0

# If pull request, do it.
echo "TRAVIS_EVENT_TYPE = ${TRAVIS_EVENT_TYPE}"
if [[ ${TRAVIS_EVENT_TYPE} == "pull_request" ]]; then
  DO_BUILD=1
fi


# If commit branch is master, do the build test
echo "TRAVIS_BRANCH = ${TRAVIS_BRANCH}"
if [[ ${TRAVIS_BRANCH} == "master" ]]; then
  DO_BUILD=1
fi

if [ ${DO_BUILD} -eq 1 ]; then
  cordova-paramedic  --verbose --platform  ${CORDOVA_PLATFORM} --justBuild;
fi
