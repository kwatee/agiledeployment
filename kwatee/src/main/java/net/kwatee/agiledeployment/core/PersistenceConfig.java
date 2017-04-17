/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.core;

import java.util.Properties;

import javax.annotation.Resource;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import net.kwatee.agiledeployment.core.repository.DBPersistenceServiceImpl;
import net.kwatee.agiledeployment.core.repository.PersistenceService;
import net.kwatee.agiledeployment.core.service.DBAdminService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;
import org.springframework.orm.hibernate4.HibernateExceptionTranslator;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.persistenceunit.DefaultPersistenceUnitManager;
import org.springframework.orm.jpa.persistenceunit.PersistenceUnitManager;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaDialect;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 
 * @author mac
 * 
 */
@Configuration
@EnableTransactionManagement
public class PersistenceConfig {

	final static private Logger LOG = LoggerFactory.getLogger(PersistenceConfig.class);

	@Value("${kwatee.jdbc.url}")
	private String jdbcUrl;
	@Resource(name = "kwateeDatasource")
	private DataSource kwateeDatasource;
	@Autowired
	private EntityManagerFactory emf;

	public PersistenceConfig() {
		LOG.debug("Starting PersistenceConfig");
	}

	@Bean
	public DBAdminService dbAdminService() {
		DBAdminService service = new DBAdminService();
		LOG.debug("DBAdminService bean created");
		return service;
	}

	@Bean
	public PersistenceService dbPersistenceServiceImpl() {
		PersistenceService service = new DBPersistenceServiceImpl();
		LOG.debug("PersistenceService bean created");
		return service;
	}

	@Bean
	public HibernateExceptionTranslator hibernateExceptionTranslator() {
		HibernateExceptionTranslator translator = new HibernateExceptionTranslator();
		LOG.debug("HibernateExceptionTranslator bean created");
		return translator;
	}

	@Bean
	public PersistenceExceptionTranslationPostProcessor exceptionTranslator() {
		PersistenceExceptionTranslationPostProcessor exceptionTranslator = new PersistenceExceptionTranslationPostProcessor();
		LOG.debug("PersistenceExceptionTranslationPostProcessor bean created");
		return exceptionTranslator;
	}

	/**
	 * Define an entityManagerFactory that uses hibernate and configure it
	 */
	@Bean(name = "entityManagerFactory")
	public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
		LocalContainerEntityManagerFactoryBean emf;
		emf = new LocalContainerEntityManagerFactoryBean();
		emf.setJpaVendorAdapter(jpaVendorAdapter());
		emf.setPersistenceUnitName("kwateePU");
		Properties hibernateProperties = new Properties();
		hibernateProperties.put("hibernate.cache.use_second_level_cache", "true");
		hibernateProperties.put("hibernate.show_sql", "false");
		emf.setJpaProperties(hibernateProperties);
		emf.setJpaDialect(new HibernateJpaDialect());
		emf.setPersistenceUnitManager(persistenceUnitManager());
		LOG.debug("LocalContainerEntityManagerFactoryBean bean created");
		return emf;
	}

	@Bean
	public JpaVendorAdapter jpaVendorAdapter() {
		HibernateJpaVendorAdapter jpaVendorAdapter = new HibernateJpaVendorAdapter();
		jpaVendorAdapter.setShowSql(true);
		jpaVendorAdapter.setDatabase(db());
		LOG.debug("JpaVendorAdapter bean created");
		return jpaVendorAdapter;
	}

	private Database db() {
		String databasePlatform = this.jdbcUrl.split("\\:")[1].toUpperCase();
		Database db = Database.valueOf(databasePlatform);
		return db;
	}

	@Bean
	public PersistenceUnitManager persistenceUnitManager() {
		DefaultPersistenceUnitManager persistenceUnitManager = new DefaultPersistenceUnitManager();
		persistenceUnitManager.setDefaultDataSource(kwateeDatasource);
		LOG.debug("PersistenceUnitManager bean created");
		return persistenceUnitManager;
	}
}
