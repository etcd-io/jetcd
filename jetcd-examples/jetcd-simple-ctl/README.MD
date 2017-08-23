### jetcd-simple-ctl

Download and start an etcd node from [instruction](https://github.com/coreos/etcd/blob/master/Documentation/dl_build.md)

> Note: For windows machine, run `.\mvnw.cmd` instead of `./mvnw`

Build the example jar.

`./mvnw clean package`

Put `foo` `bar` into cluster.

```bash
$ ./mvnw -q exec:java -Dexec.args="put foo bar"
21:39:06.126|INFO |CommandPut - OK
 ```
 
Put `foo` `bar2` into cluster.
 
```bash
$ ./mvnw -q exec:java -Dexec.args="put foo bar2"
21:39:06.126|INFO |CommandPut - OK
```

Get `foo`.

```bash
$ ./mvnw -q exec:java -Dexec.args="get foo"
21:41:00.265|INFO |CommandGet - foo
21:41:00.267|INFO |CommandGet - bar2
```

Get `foo` at `rev` 2.
```bash
$ ./mvnw -q exec:java -Dexec.args="get foo --rev=2"
21:42:03.371|INFO |CommandGet - foo
21:42:03.373|INFO |CommandGet - bar
```

Watch `foo` since `rev` 2.
```bash
$ ./mvnw -q exec:java -Dexec.args="watch foo --rev=2"
21:43:51.360|INFO |CommandWatch - PUT
21:43:51.361|INFO |CommandWatch - foo
21:43:51.361|INFO |CommandWatch - bar
21:43:51.361|INFO |CommandWatch - PUT
21:43:51.362|INFO |CommandWatch - foo
21:43:51.362|INFO |CommandWatch - bar2
```