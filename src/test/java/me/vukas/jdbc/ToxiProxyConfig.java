package me.vukas.jdbc;

import static eu.rekawek.toxiproxy.model.ToxicDirection.DOWNSTREAM;
import static eu.rekawek.toxiproxy.model.ToxicDirection.UPSTREAM;

import java.io.IOException;

import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;

@TestConfiguration
public class ToxiProxyConfig {

		private static final Logger logger = LoggerFactory.getLogger(ToxiProxyConfig.class);

		@Bean(initMethod = "start", destroyMethod = "stop")
		public MySQLContainer mysqlContainer() {
			return new MySQLContainer()
					.withPassword("123456");
		}

		@Bean(initMethod = "start", destroyMethod = "stop")
		@DependsOn("mysqlContainer")
		public GenericContainer toxiProxyContainer() {
			return new FixedHostPortGenericContainer("shopify/toxiproxy")
					.withFixedExposedPort(3305, 3305)
					.withFixedExposedPort(8474, 8474)
					.withExposedPorts(8474, 3305)
					.withLogConsumer(new Slf4jLogConsumer(logger))
					.waitingFor(Wait.forLogMessage(".*8474.*\\n",1));
		}

		@Bean
		@DependsOn("toxiProxyContainer")
		public ToxiproxyClient toxiproxyClient(){
			return new ToxiproxyClient("10.10.121.219", 8474);
		}

		@Bean
		public Proxy mysqlProxy() throws IOException {
			Proxy proxy = toxiproxyClient().createProxy("mysql", "0.0.0.0:3305",
					mysqlContainer().getContainerIpAddress()+":"+mysqlContainer().getMappedPort(3306));
			intoxicate(proxy);
			return proxy;
		}

		private void intoxicate(Proxy proxy) throws IOException {
			proxy.toxics().bandwidth("my-bandwidth1", DOWNSTREAM, 1);
			proxy.toxics().bandwidth("my-bandwidth2", UPSTREAM, 1);
		}

}
