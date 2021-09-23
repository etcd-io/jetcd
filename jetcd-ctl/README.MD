### jetcd-ctl

Download and start an etcd node from [instruction](https://github.com/etcd-io/etcd/blob/master/Documentation/dl_build.md)

Put `foo` `bar` into the cluster:

```bash
$ gradle run --args="put foo bar"
21:39:06.126|INFO |CommandPut - OK
 ```
 
Put `foo` `bar2` into the cluster:
 
```bash
$ gradle run --args="put foo bar2"
21:39:06.126|INFO |CommandPut - OK
```

Get the key `foo`:

```bash
$ gradle run --args="get foo"
21:41:00.265|INFO |CommandGet - foo
21:41:00.267|INFO |CommandGet - bar2
```

Get the key `foo` at etcd revision 2:

```bash
$ gradle run --args="get foo --rev=2"
21:42:03.371|INFO |CommandGet - foo
21:42:03.373|INFO |CommandGet - bar
```

Watch `foo` since revision 2:

```bash
$ gradle run --args="watch foo --rev=2"
21:43:51.360|INFO |CommandWatch - PUT
21:43:51.361|INFO |CommandWatch - foo
21:43:51.361|INFO |CommandWatch - bar
21:43:51.361|INFO |CommandWatch - PUT
21:43:51.362|INFO |CommandWatch - foo
21:43:51.362|INFO |CommandWatch - bar2
```
