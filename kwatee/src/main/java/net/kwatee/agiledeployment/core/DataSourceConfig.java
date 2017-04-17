/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.core;

import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * 
 * @author mac
 * 
 */
@Configuration
public class DataSourceConfig {

	final private static Logger LOG = LoggerFactory.getLogger(DataSourceConfig.class);

	@Autowired
	protected Environment environment;

	@Value("${kwatee.jdbc.driver}")
	private String jdbcDriver;
	@Value("${kwatee.jdbc.url}")
	private String jdbcUrl;
	@Value("${kwatee.jdbc.user}")
	private String jdbcUserName;
	@Value("${kwatee.jdbc.password}")
	private String jdbcPassword;
	@Value("${kwatee.repository.path}")
	private String repositoryPath;

	@Bean(name = "kwateeDatasource")
	public DataSource getKwateeDatasource() {
		BasicDataSource ds = new BasicDataSource();
		ds.setDriverClassName(this.jdbcDriver);
		ds.setUrl(this.jdbcUrl);
		ds.setUsername(this.jdbcUserName);
		ds.setPassword(this.jdbcPassword);
		ds.setMaxActive(20);
		ds.setInitialSize(1);
		LOG.info("DataSource: {}", ds.getUrl());
		return ds;
	}
}
