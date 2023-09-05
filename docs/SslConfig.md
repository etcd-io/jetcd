# How to Build Jectd Client for One TLS Secured Etcd Cluster

# prepare certification files

If your etcd cluster is installed using [etcdadm](https://github.com/kubernetes-sigs/etcdadm), you are likely to find
certification files in the path `/etc/etcd/pki/`: `ca.crt`, `etcdctl-etcd-client.key`, `etcdctl-etcd-client.crt`.

If your etcd cluster is the builtin etcd in one kubernetes cluster(
using [kubeadm](https://kubernetes.io/docs/setup/production-environment/tools/kubeadm/setup-ha-etcd-with-kubeadm/)),

you can find the same files in `/etc/kubernetes/pki/etcd/`: `ca.crt`, `healthcheck-client.crt`, `healthcheck-client.key`.

Because `SslContextBuilder` only support a PKCS#8 private key file in PEM format, convert  `etcdctl-etcd-client.key`
to `etcdctl-etcd-client.key.pem` according
to [netty SslContextBuilder doc](https://netty.io/wiki/sslcontextbuilder-and-private-key.html).

# build jetcd client

```
        File cert = new File("ca.crt");
        File keyCertChainFile = new File("etcdctl-etcd-client.crt");
        File keyFile = new File("etcdctl-etcd-client.key.pem");
        SslContext context = GrpcSslContexts.forClient()
                .trustManager(cert)
                .keyManager(keyCertChainFile, keyFile)
                .build();
        Client client = Client.builder()
                .endpoints("https://10.168.168.66:2379")
                .sslContext(context)
                .build();

        client.getClusterClient().listMember().get().getMembers().forEach(member -> {
            logger.info("member: {}", member);
        });

```
