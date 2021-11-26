### jetcd-ctl

Download and start an etcd node from [instruction](https://github.com/etcd-io/etcd/releases/latest)

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
21:35:09.162|INFO |CommandWatch - type=PUT, key=foo, value=bar
21:35:09.164|INFO |CommandWatch - type=PUT, key=foo, value=bar2
```
