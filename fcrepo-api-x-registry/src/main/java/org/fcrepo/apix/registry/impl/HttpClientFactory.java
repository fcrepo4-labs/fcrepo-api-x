
package org.fcrepo.apix.registry.impl;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

public class HttpClientFactory {

    private int connectTimeout = 1000;

    private int socketTimeout = 1000;

    public void setConnectTimeout(int timeout) {
        this.connectTimeout = timeout;
    }

    public void setSocketTimeout(int timeout) {
        this.socketTimeout = timeout;
    }

    public CloseableHttpClient getClient() {
        final RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(connectTimeout)
                .setSocketTimeout(socketTimeout).build();

        return HttpClientBuilder.create().setDefaultRequestConfig(config).build();
    }
}
