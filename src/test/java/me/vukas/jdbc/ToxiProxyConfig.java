package me.vukas.jdbc;

import static eu.rekawek.toxiproxy.model.ToxicDirection.DOWNSTREAM;
import static eu.rekawek.toxiproxy.model.ToxicDirection.UPSTREAM;

import java.io.IOException;

import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class ToxiProxyConfig {

		@Bean
		public ToxiproxyClient toxiproxyClient(){
			return new ToxiproxyClient("10.10.121.219", 8474);
		}

		@Bean
		public Proxy mysqlProxy() throws IOException {
			Proxy proxy = toxiproxyClient().createProxy("mysql", "0.0.0.0:3305", "10.10.121.219:3306");
			intoxicate(proxy);
			return proxy;
		}

		private void intoxicate(Proxy proxy) throws IOException {
			proxy.toxics().bandwidth("my-bandwidth1", DOWNSTREAM, 1);
			proxy.toxics().bandwidth("my-bandwidth2", UPSTREAM, 1);
		}

}
