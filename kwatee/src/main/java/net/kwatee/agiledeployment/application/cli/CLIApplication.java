/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.application.cli;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;

import javax.xml.parsers.ParserConfigurationException;

import net.kwatee.agiledeployment.common.exception.KwateeException;
import net.kwatee.agiledeployment.core.conduit.impl.FileSystemConduitFactory;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.AuthenticationException;
import org.xml.sax.SAXException;

@Import(CLIAppConfiguration.class)
public class CLIApplication implements CommandLineRunner {

	public static void main(String[] args) throws KwateeException, IOException, ParserConfigurationException,
			SAXException {
		System.setProperty("spring.config.name", "kwatee");
		SpringApplication app = new SpringApplication(CLIApplication.class);
		app.setApplicationContextClass(AnnotationConfigApplicationContext.class);
		SpringApplication.run(CLIApplication.class, args);
	}

	public void run(String... args) throws Exception {
		Options options = prepareOptions();
		CommandLineParser parser = new BasicParser();
		try {
			CommandLine line = parser.parse(options, args);
			args = line.getArgs();
			if (args.length != 1) {
				throw new ParseException("Missing command: deploy | undeploy | check | start | stop | status | contents");
			}
			String command = args[0];

			String forcedConduitType = null;
			String serverName = line.getOptionValue("server");
			if (serverName == null) {
				serverName = line.getOptionValue("server-local");
				if (serverName != null) {
					forcedConduitType = FileSystemConduitFactory.getId();
				}
			}
			String artifactName = line.getOptionValue("artifact");
			String repoDirPath = line.getOptionValue("repo");
			String configDirPath = line.getOptionValue("config");
			if (repoDirPath == null)
				throw new ParseException("You must specify the bundle directory (--repo)");
			if (configDirPath == null)
				configDirPath = repoDirPath;
			File bundleFile = new File(repoDirPath, "bundle.xml");
			File deploymentFile = new File(configDirPath, "deployment.xml");
			if (!bundleFile.exists()) {
				System.err.println("Invalid bundle dir: " + bundleFile.getAbsolutePath() + " not found");
				return;
			}
			if (!deploymentFile.exists()) {
				System.err.println("Invalid deployment dir: " + deploymentFile.getAbsolutePath() + " not found");
				return;
			}

			String bundleXml = FileUtils.readFileToString(bundleFile);
			String configurationXml = FileUtils.readFileToString(deploymentFile);
			System.setProperty("kwatee.installer.bundle.dir", bundleFile.getParentFile().getAbsolutePath());
			System.setProperty("kwatee.installer.configuration.dir", deploymentFile.getParentFile().getAbsolutePath());

			/*
			Deployment deployment = deployer.loadDeployment(bundleXml, configurationXml, serverName, artifactName,
			forcedConduitType);
			if (command.equals("deploy")) {
			deployer.deploy(deployment);
			} else if (command.equals("undeploy")) {
			deployer.undeploy(deployment, false);
			} else if (command.equals("undeploy-forced")) {
			deployer.undeploy(deployment, true);
			} else if (command.equals("check")) {
			deployer.check(deployment);
			} else if (command.equals("start")) {
			deployer.start(deployment);
			} else if (command.equals("stop")) {
			deployer.stop(deployment);
			} else if (command.equals("status")) {
			deployer.status(deployment);
			} else if (command.equals("contents")) {
			deployer.showContents(deployment);
			} else
			*/
			throw new ParseException("Unknown command: " + command);

		} catch (AuthenticationException e) {
			System.err.println("Authentication failed");
		} catch (ParseException e) {
			System.out.println(e.getMessage());
			HelpFormatter help = new HelpFormatter();
			help.setArgName("qwe");
			help.setWidth(80);
			help.setOptionComparator(new Comparator<OrderedOption>() {

				@Override
				public int compare(OrderedOption option0, OrderedOption option1) {
					return option0.index == option1.index ? 0 : (option0.index < option1.index ? -1 : 1);
				}
			});
			help.printHelp(
					"kwdeploy deploy|undeploy|undeploy-force|check|start|stop|status|contents"
							+ "--repo <path>\n"
							+ "[--config <path>]\n"
							+ "[--server <name>] [--server-local <name>]\n"
							+ "[--artifact <name>]\n"
					, options);
			System.exit(1);
		}
	}

	static private Options prepareOptions() {
		Options options = new Options();
		options.addOption(new OrderedOption("repository", "path", "repository directory"));
		options.addOption(new OrderedOption("d", "path", "deployment directory"));
		options.addOption(new OrderedOption("server", "name", "server name"));
		options.addOption(new OrderedOption("server-local", "name", "server name"));
		options.addOption(new OrderedOption("artifact", "name", "artifact name"));
		return options;
	}

	@SuppressWarnings("serial")
	static public class OrderedOption extends Option {

		static public int counter = 0;

		int index = counter++;

		public OrderedOption(String opt, String argName, String description) throws IllegalArgumentException {
			super(opt.length() > 1 ? null : opt, opt.length() == 1 ? null : opt, argName != null, description);
			if (argName != null)
				super.setArgName(argName);
		}
	}
}
