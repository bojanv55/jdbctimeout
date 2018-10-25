package me.vukas;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;

//@Component
public class CmdRun implements CommandLineRunner {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Override
	public void run(String... args) throws Exception {
		String x = jdbcTemplate.queryForObject("SELECT sleep(10)", String.class);
		System.out.println(x);
	}
}
