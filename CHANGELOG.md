# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).


## [Unreleased]
### Added
- [Launcher](https://github.com/etcd-io/jetcd#launcher) ([#384](https://github.com/etcd-io/jetcd/issues/384))
- _...TODO..._


## [0.0.2] - 2018-03-27
### Added
- lock support ([#259](https://github.com/etcd-io/jetcd/issues/259))
- nested txn support ([#265](https://github.com/etcd-io/jetcd/issues/265))
- move leader support ([#235](https://github.com/etcd-io/jetcd/issues/235))
- hashkv support ([#234](https://github.com/etcd-io/jetcd/issues/234))
- OSGi support ([#269](https://github.com/etcd-io/jetcd/issues/269))

### Changed
- `Client` extends AutoCloseable ([#244](https://github.com/etcd-io/jetcd/issues/244))
- improved integration test framework ([#295](https://github.com/etcd-io/jetcd/issues/295))

### Fixed
- TXN getter ([#237](https://github.com/etcd-io/jetcd/issues/237))

### Doc
- TLS support ([#245](https://github.com/etcd-io/jetcd/issues/245))


## [0.0.1] - 2017-08-23
### Added
- Initial Release

### Known Limitations
- Nested transactions are not currently supported ([#143](https://github.com/etcd-io/jetcd/issues/143))
- HashKV is not currently supported ([#222](https://github.com/etcd-io/jetcd/issues/222))
