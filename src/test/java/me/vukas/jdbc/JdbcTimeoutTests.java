package me.vukas.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.model.ToxicDirection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ToxiProxyConfig.class)
@ActiveProfiles("test")	//load aditionally -test.yml file and overwrite original one
public class JdbcTimeoutTests {

	private static final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);

	@Autowired
	private JdbcTemplate jdbcTemplate;
	@Autowired
	private Proxy mysqlProxy;

	@Test
	public void readTimeout(){

		executor.schedule(() -> {
			try {
				System.out.println("Adding toxic timeout...");
				mysqlProxy.toxics().timeout("mysql-timeout", ToxicDirection.DOWNSTREAM, 0);
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}, 5, TimeUnit.SECONDS);

		String x = jdbcTemplate.queryForObject("SELECT sleep(10)", String.class);
		assertThat(x).isEqualTo("0");
	}

}
