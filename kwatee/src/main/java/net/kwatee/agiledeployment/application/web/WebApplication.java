package net.kwatee.agiledeployment.application.web;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(WebAppConfiguration.class)
public class WebApplication extends SpringBootServletInitializer {

	public static void main(String[] args) throws Exception {
		System.setProperty("spring.config.name", "kwatee");
		SpringApplication app = new SpringApplication(WebApplication.class);
		String pid = getPid();
		if (pid != null)
			writePid(pid);
		app.run(args);

	}

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application
				.sources(WebApplication.class);
	}

	private static String getPid() {
		try {
			String jvmName = ManagementFactory.getRuntimeMXBean().getName();
			return jvmName.split("@")[0];
		} catch (Throwable ex) {
			return null;
		}
	}

	private static void writePid(String pid) {
		String pidFile = System.getProperty("pidfile");
		if (pidFile == null)
			pidFile = new File(System.getProperty("java.io.tmpdir"), "kwatee.pid").getAbsolutePath();
		File file = new File(pidFile);
		File parent = file.getParentFile();
		if (parent != null) {
			parent.mkdirs();
		}
		try (FileWriter writer = new FileWriter(file)) {
			writer.append(pid);
		} catch (IOException e) {
			System.err.println("Impossible to write kwatee pid " + pid);
		}
		file.deleteOnExit();
	}
}