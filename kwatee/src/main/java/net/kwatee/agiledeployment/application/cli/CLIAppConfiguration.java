/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.application.cli;

/**
 * 
 * @author mac
 * 
 */

import net.kwatee.agiledeployment.core.CoreServicesConfig;
import net.kwatee.agiledeployment.core.DataSourceConfig;
import net.kwatee.agiledeployment.core.PersistenceConfig;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({PersistenceConfig.class, DataSourceConfig.class, CoreServicesConfig.class})
public class CLIAppConfiguration {}
