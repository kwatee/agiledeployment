/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.webapp.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import net.kwatee.agiledeployment.common.exception.InternalErrorException;

import org.apache.commons.io.IOUtils;
import org.springframework.web.multipart.MultipartFile;

class FileUpload {

	private String uploadUrl;
	private URL url;
	private MultipartFile uploadMultipart;
	private String name;
	private InputStream stream;
	private File tmpFile;

	FileUpload(String fileUrl, MultipartFile multipart) {
		this(fileUrl, multipart, false);
	}

	FileUpload(String uploadUrl, MultipartFile uploadMultipart, boolean needFileObject) {
		if (uploadUrl == null && uploadMultipart == null) {
			throw new InternalErrorException("Missing file");
		}
		if (uploadUrl != null && uploadMultipart != null) {
			throw new InternalErrorException("Cannot specify both url and multipart file");
		}
		try {
			if (uploadUrl != null) {
				this.uploadUrl = uploadUrl;
				File localFile = new File(uploadUrl);
				if (localFile.exists()) {
					this.name = localFile.getName();
				} else {
					this.url = new URL(this.uploadUrl);
					String s = this.url.getPath();
					int pos = s.lastIndexOf('/');
					this.name = pos < 0 ? s : s.substring(pos + 1);
					if (needFileObject) {
						ReadableByteChannel rbc = Channels.newChannel(url.openStream());
						this.tmpFile = File.createTempFile("kwatee", null);
						FileOutputStream fos = new FileOutputStream(this.tmpFile);
						fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
						IOUtils.closeQuietly(fos);
					}
				}
			} else {
				this.uploadMultipart = uploadMultipart;
				this.name = uploadMultipart.getOriginalFilename();
				if (needFileObject) {
					this.tmpFile = File.createTempFile("kwatee", null);
					uploadMultipart.transferTo(this.tmpFile);
				}

			}
		} catch (IOException e) {
			throw new InternalErrorException(e);
		}
	}

	InputStream getInputStream() throws IOException {
		if (this.stream != null) {
			throw new InternalErrorException("Inputstream already open");
		}
		if (this.tmpFile != null) {
			this.stream = new FileInputStream(this.tmpFile);
		} else if (this.uploadMultipart != null) {
			this.stream = this.uploadMultipart.getInputStream();
		} else if (this.url == null) {
			File localFile = new File(this.uploadUrl);
			this.stream = new FileInputStream(localFile);
		} else {
			this.stream = this.url.openStream();
		}
		return this.stream;
	}

	File getFile() {
		if (this.tmpFile != null) {
			return this.tmpFile;
		}
		if (this.uploadUrl != null && this.url == null) {
			return new File(this.uploadUrl);
		}
		throw new InternalErrorException("No file object");
	}

	String getName() {
		return this.name;
	}

	void release() {
		IOUtils.closeQuietly(this.stream);
		this.stream = null;
		if (this.tmpFile != null) {
			this.tmpFile.delete();
		}
	}

	static void releaseQuietly(FileUpload upload) {
		if (upload != null)
			upload.release();
	}

}
