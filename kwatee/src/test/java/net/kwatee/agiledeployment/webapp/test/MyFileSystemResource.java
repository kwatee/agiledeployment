package net.kwatee.agiledeployment.webapp.test;

import java.io.File;

import org.springframework.core.io.FileSystemResource;

public class MyFileSystemResource extends FileSystemResource {

	final private String originalName;

	public MyFileSystemResource(File file, String originalName) {
		super(file);
		this.originalName = originalName;
	}

	@Override
	public String getFilename() {
		return this.originalName;
	}
}
