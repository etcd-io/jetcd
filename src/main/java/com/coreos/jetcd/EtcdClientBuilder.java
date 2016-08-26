package com.coreos.jetcd;

import com.coreos.jetcd.exception.AuthFailedException;
import com.coreos.jetcd.exception.ConnectException;
import com.coreos.jetcd.resolver.AbstractEtcdNameResolverFactory;
import com.coreos.jetcd.resolver.SimpleEtcdNameResolverFactory;
import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import static com.google.common.base.Preconditions.*;


/**
 * ClientBuilder knows how to create an EtcdClient instance.
 */
public class EtcdClientBuilder {

    private List<String> endpoints = Lists.newArrayList();
    private ByteString name;
    private ByteString password;
    private AbstractEtcdNameResolverFactory nameResolverFactory;

    private EtcdClientBuilder() {
    }

    public static EtcdClientBuilder newBuilder() {
        return new EtcdClientBuilder();
    }

    /**
     * gets the endpoints for the builder
     *
     * @return the list of endpoints configured for the builder
     * @throws IllegalArgumentException if endpoints is empty
     */
    public List<String> endpoints() {
        return this.endpoints;
    }

    /**
     * configure etcd server endpoints.
     *
     * @param endpoints etcd server endpoints, at least one
     * @return this builder to train
     * @throws NullPointerException     if endpoints is null or one of endpoint is null
     * @throws IllegalArgumentException if endpoints is empty or some endpoint is invalid
     */
    public EtcdClientBuilder endpoints(String... endpoints) {
        checkNotNull(endpoints, "endpoints can't be null");
        checkArgument(endpoints.length > 0, "please configure at lease one endpoint ");

        // TODO: check endpoint is in host:port format
        for (String endpoint : endpoints) {
            checkNotNull(endpoint, "endpoint can't be null");
            final String trimmedEndpoint = endpoint.trim();
            checkArgument(trimmedEndpoint.length() > 0, "invalid endpoint: endpoint=" + endpoint);
            this.endpoints.add(trimmedEndpoint);
        }
        return this;
    }

    public ByteString getName() {
        return name;
    }

    /**
     * config etcd auth name
     *
     * @param name etcd auth name
     * @return this builder
     * @throws NullPointerException if name is null
     */
    public EtcdClientBuilder setName(ByteString name) {
        checkNotNull(name, "name can't be null");
        this.name = name;
        return this;
    }

    public ByteString getPassword() {
        return password;
    }

    /**
     * config etcd auth password
     *
     * @param password etcd auth password
     * @return this builder
     * @throws NullPointerException if password is null
     */
    public EtcdClientBuilder setPassword(ByteString password) {
        checkNotNull(name, "password can't be null");
        this.password = password;
        return this;
    }

    /**
     * config etcd auth password
     *
     * @param nameResolverFactory etcd nameResolverFactory
     * @return this builder
     * @throws NullPointerException if password is null
     */
    public EtcdClientBuilder setNameResolverFactory(AbstractEtcdNameResolverFactory nameResolverFactory) {
        checkNotNull(nameResolverFactory);
        this.nameResolverFactory = nameResolverFactory;
        return this;
    }

    /**
     * get nameResolverFactory for etcd client
     *
     * @return nameResolverFactory
     */
    public AbstractEtcdNameResolverFactory getNameResolverFactory() {
        return nameResolverFactory;
    }

    private AbstractEtcdNameResolverFactory getSimpleNameResolveFactory(List<String> endpoints) {
        URI[] uris = new URI[endpoints.size()];
        for (int i = 0; i < endpoints.size(); ++i) {
            try {
                String endpoint = endpoints.get(i);
                if (!endpoint.startsWith("http://")) {
                    endpoint = "http://" + endpoint;
                }
                uris[i] = new URI(endpoint);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
        return new SimpleEtcdNameResolverFactory(uris);
    }

    /**
     * build a new EtcdClient.
     *
     * @return EtcdClient instance.
     * @throws ConnectException    As network reason, wrong address
     * @throws AuthFailedException This may be caused as wrong username or password
     */
    public EtcdClient build() throws ConnectException, AuthFailedException {
        checkState(!endpoints.isEmpty() || nameResolverFactory != null, "please configure ectd serve endpoints or nameResolverFactory before build.");
        if (nameResolverFactory == null) {
            //If no nameResolverFactory was set, use SimpleEtcdNameResolver
            this.nameResolverFactory = getSimpleNameResolveFactory(this.endpoints);
        }
        return new EtcdClient(null, new EtcdConfig(this.endpoints, this.name, this.password, this.nameResolverFactory));
    }
}
