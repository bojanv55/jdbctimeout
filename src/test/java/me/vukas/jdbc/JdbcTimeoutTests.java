package me.vukas.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.model.ToxicDirection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ToxiProxyConfig.class)
@ActiveProfiles("test")	//load aditionally -test.yml file and overwrite original one
public class JdbcTimeoutTests {

	private static final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
	private static final ExecutorService writableExecutor = Executors.newSingleThreadExecutor();
	private static final Logger logger = LoggerFactory.getLogger(JdbcTimeoutTests.class);

	@Autowired
	private JdbcTemplate jdbcTemplate;
	@Autowired
	private Proxy mysqlProxy;

	@Before
	public void removeTimeout() {
		try {
			mysqlProxy.toxics().get("mysql-timeout").remove();
		}
		catch (IOException e){
			logger.info("Toxic mysql-timeout already removed");
		}
	}

	private String buildString() {
		Random r = new Random();
		return IntStream.range(0, 1_000_000)
				.mapToObj(x -> String.valueOf(r.nextInt(10_000_000)))
				.collect(Collectors.joining(","));
	}

	private void addToxic(){
		executor.schedule(() -> {
			try {
				System.out.println("Adding toxic timeout...");
				mysqlProxy.toxics().timeout("mysql-timeout", ToxicDirection.DOWNSTREAM, 0);
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}, 5, TimeUnit.SECONDS);
	}

//	@Test(timeout = 120_000, expected = RecoverableDataAccessException.class)
//	public void readTimeout(){
//		addToxic();
//		String x = jdbcTemplate.queryForObject("SELECT sleep(10)", String.class);
//		assertThat(x).isEqualTo("0");
//	}

//	@Test(timeout = 120_000, expected = RecoverableDataAccessException.class)
//	public void writeTimeout(){
//		String builtString = buildString();
//		addToxic();
//		String x = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM mysql.db WHERE NULL IN (" + builtString +  ")", String.class);
//		assertThat(x).isEqualTo("0");
//	}

		@Test(timeout = 120_000, expected = RecoverableDataAccessException.class)
		public void writeTimeout(){
			String builtString = buildString();
			addToxic();
			Future<String> queryFuture = writableExecutor.submit(() ->
					jdbcTemplate.queryForObject("SELECT COUNT(*) FROM mysql.db WHERE NULL IN (" + builtString +  ")", String.class));
			String x;
			try {
				x = queryFuture.get(30, TimeUnit.SECONDS);
			}
			catch (InterruptedException | ExecutionException | TimeoutException e) {
				throw new RecoverableDataAccessException("Write timeout!");
			}
			finally {
				queryFuture.cancel(true);
			}
			assertThat(x).isEqualTo("0");
		}

}
