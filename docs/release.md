# Release Process

jetcd uses Axion for versioning and JReleaser to deploy Maven artifacts. [Axion Release Plugin](https://github.com/allegro/axion-release-plugin) · [JReleaser](https://jreleaser.org/)

## Check Current Version

```bash
./gradlew currentVersion
```

## Stage Artifacts Locally

```bash
./gradlew stageArtifacts
```

Gradle recreates the local Maven repository at `build/staging-deploy` with each run.

## Verify the Published Client

```bash
bash ./.github/workflows/scripts/verify-published-client.sh
```

CI runs this check using a temporary local Maven repository. The consumer checks the published runtime dependencies and makes a loopback KV request using both Gradle module metadata and Maven POM metadata.

## Publish a Snapshot

The main-branch workflow publishes snapshots after the etcd test matrix passes. For a manual deployment, set `JRELEASER_NEXUS2_USERNAME` and `JRELEASER_NEXUS2_PASSWORD` to the two parts of a Central Portal token and run:

```bash
./gradlew publishSnapshot -Prelease.forceSnapshot
```

## Publish a Release

Create the release tag with `./gradlew release`. From the tagged commit, set `JRELEASER_MAVENCENTRAL_USERNAME` and `JRELEASER_MAVENCENTRAL_PASSWORD` to the two parts of a Central Portal token. Provide the armored signing keys and passphrase through `JRELEASER_GPG_PUBLIC_KEY`, `JRELEASER_GPG_SECRET_KEY`, and `JRELEASER_GPG_PASSPHRASE`, then run:

```bash
./gradlew publishRelease
```

## Set Next Development Version

```bash
./gradlew markNextVersion -Prelease.version=X.Y.Z
```

## Dry Run

Preview Axion's release steps and JReleaser's Maven deployment. JReleaser still requires the credential fields for validation, so use placeholder values for the dry run. A release dry run also needs usable signing keys to generate the signed bundle.

```bash
./gradlew release -Prelease.dryRun
JRELEASER_DRY_RUN=true ./gradlew jreleaserDeploy
```
