/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.etcd;

import java.io.File;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;

import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultPollingEndpoint;
import org.apache.camel.spi.UriParam;

import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
//import org.apache.http.conn.ssl.SSLSocketFactory;
//import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;

import com.justinsb.etcd.EtcdClient;

public class EtcdEndpoint extends DefaultPollingEndpoint{
	
	
	@UriParam
	private EtcdConfiguration configuration;
	
	
	private EtcdClient etcdClient;
	
	public EtcdEndpoint(String uri, EtcdComponent component, EtcdConfiguration configuration) {
		super(uri, component);
		this.configuration = configuration;
	}
	
	@Override
	protected void doStart() throws Exception {
                if ((configuration.getTrustSelfsigned() == true) || (configuration.getCaFile() != null) || (configuration.getKeyFile() != null)) {
			// Need to create a custom httpclient since we need to change the SSL information.
			SSLContextBuilder builder = new SSLContextBuilder();
                        if (configuration.getTrustSelfsigned() == true) {
				// Don't need to look at the CA file since we are going to trust anyhow.
				final TrustStrategy acceptingTrustStrategy = new TrustStrategy() {
     					@Override
					public boolean isTrusted(X509Certificate[] certificate, String authType) {
						return true;
					}
				};
				builder.loadTrustMaterial(acceptingTrustStrategy);
			} else {
				if (configuration.getCaFile() != null) {
					builder.loadTrustMaterial(new File(configuration.getCaFile()));
				}
			}
			// Now check if there are any private keys.
			if (configuration.getKeyFile() != null) {
				builder.loadKeyMaterial(new File(configuration.getKeyFile()),null,null);
			}
			//SSLSocketFactory socketfactory = SSLSocketFactory(builder.build());
			final CloseableHttpAsyncClient httpClient = HttpAsyncClients.custom().setSSLContext(builder.build()).build();
			etcdClient = new EtcdClient(configuration.makeURI());
		} else {
			etcdClient = new EtcdClient(configuration.makeURI());
		}
	}
	

	@Override
	public Producer createProducer() throws Exception {
		
		EtcdProducer producer = new EtcdProducer(this, etcdClient, this.configuration);
		
		return producer;
	}
	
	
	@Override
	public Consumer createConsumer(Processor processor) throws Exception {
		EtcdConsumer etcdConsumer = new EtcdConsumer(this, processor, this.configuration, etcdClient);
		
		
		return etcdConsumer;
	}
	
	

	@Override
	public boolean isSingleton() {
		// TODO Auto-generated method stub
		return false;
	}

}
