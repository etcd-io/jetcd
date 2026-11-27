#!/usr/bin/env bash
set -euo pipefail

workspace=$(mktemp -d)
trap 'rm -rf -- "$workspace"' EXIT
repository="$workspace/repository"

./gradlew "$@" "-Dmaven.repo.local=$repository" \
    :jetcd-core:publishToMavenLocal :jetcd-api:publishToMavenLocal \
    :jetcd-common:publishToMavenLocal :jetcd-grpc:publishToMavenLocal

publications=("$repository"/io/etcd/jetcd-core/*/jetcd-core-*.pom)
if [[ ${#publications[@]} -ne 1 || ! -f "${publications[0]}" ]]; then
    echo "Expected one published jetcd-core POM" >&2
    exit 1
fi
version_dir=${publications[0]%/*}
version=${version_dir##*/}

cp -R etc/published-client "$workspace/consumer"
consumer_args=(-p "$workspace/consumer" "-PjetcdRepository=$repository" "-PjetcdVersion=$version")

echo "Checking the published client through Gradle module metadata"
./gradlew "$@" "${consumer_args[@]}" verifyPublishedClient
echo "Checking the published client through Maven POM metadata"
./gradlew "$@" "${consumer_args[@]}" -PpomOnly verifyPublishedClient
