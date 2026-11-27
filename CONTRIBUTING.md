# How to Contribute

jetcd is Apache 2.0 licensed and accepts contributions via GitHub pull requests.

## Contact

- Email: [etcd-dev](https://groups.google.com/g/etcd-dev)

## Getting Started

1. Fork the repository on GitHub
2. Read [docs/development.md](docs/development.md) for code style and testing guidelines
3. Read [docs/design.md](docs/design.md) for architecture decisions

## Reporting Bugs

See the etcd [bug reporting guide](https://github.com/etcd-io/etcd/blob/main/Documentation/reporting_bugs.md).

## Contribution Flow

1. Create a topic branch from `main`
2. Make commits of logical units
3. Run quality checks: `./gradlew check`
4. Fix formatting: `./gradlew spotlessApply`
5. Push to your fork
6. Submit a pull request to etcd-io/jetcd
7. PR requires LGTM from a maintainer in [OWNERS](OWNERS)

## Commit Message Format

```
<subsystem>: <what changed>

<why this change was made>

Fixes #123
```

- Subject line: max 70 characters
- Body: wrap at 80 characters

Example:

```
kv: add prefix delete support

Enables deleting all keys with a given prefix in a single operation.

Fixes #38
```
