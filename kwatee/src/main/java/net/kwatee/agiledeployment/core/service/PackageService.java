/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.core.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.kwatee.agiledeployment.common.Constants.LayerType;
import net.kwatee.agiledeployment.common.exception.InternalErrorException;
import net.kwatee.agiledeployment.common.exception.ObjectAlreadyExistsException;
import net.kwatee.agiledeployment.common.exception.ObjectNotExistException;
import net.kwatee.agiledeployment.common.utils.CryptoUtils;
import net.kwatee.agiledeployment.core.deploy.packager.PackagerService;
import net.kwatee.agiledeployment.core.variable.VarInfo;
import net.kwatee.agiledeployment.core.variable.VariableService;
import net.kwatee.agiledeployment.repository.entity.RepositoryFile;
import net.kwatee.agiledeployment.repository.entity.Version;
import net.kwatee.agiledeployment.repository.entity.VersionVariable;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 
 * @author mac
 * 
 */
@Service
public class PackageService {

	final static public String PACKAGE_FILE_NAME = "archive.kwatee";

	@Autowired
	private FileStoreService repositoryService;
	@Autowired
	private PackagerService bundleService;
	@Autowired
	private VariableService variableService;

	final static private org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(PackageService.class);

	/**
	 * @param artifactName
	 * @param versionName
	 * @return package size
	 */
	protected long getPackageSize(String artifactName, String versionName) {
		String versionPath = this.repositoryService.getVersionPath(artifactName, versionName);
		return this.repositoryService.getFileLength(versionPath, PACKAGE_FILE_NAME);
	}

	/**
	 * @param artifactName
	 * @param versionName
	 * @return input stream
	 */
	protected InputStream getPackageInputStream(String artifactName, String versionName) {
		String versionPath = this.repositoryService.getVersionPath(artifactName, versionName);
		return this.repositoryService.getFileInputStream(versionPath, PACKAGE_FILE_NAME);
	}

	/**
	 * Updates the package of a version
	 * 
	 * @param version
	 * @param packageFileName
	 * @param packageFile
	 * @param deleteOverlays
	 * @param excludedExtensions
	 */
	public String updatePackage(
			Version version,
			String packageFileName,
			File packageFile,
			boolean deleteOverlays,
			Set<String> excludedExtensions) {
		LOG.debug("updatePackage {}", version);
		String versionPath = this.repositoryService.getVersionPath(version.getArtifact().getName(), version.getName());
		if (!packageFile.exists()) {
			throw new InternalErrorException("File not found");
		}
		if (!packageFile.canRead()) {
			throw new InternalErrorException("Not enough privileges to open");
		}
		if (deleteOverlays) {
			this.repositoryService.deleteVersion(version.getArtifact().getName(), version.getName());
			deleteOverlays(version);
		} else {
			this.repositoryService.deleteFile(versionPath, PACKAGE_FILE_NAME, null);
		}
		version.setBuiltinProperties(false);
		version.setPackageUploadDate(new Date().getTime());
		try {
			OutputStream out = this.repositoryService.getFileOutputStream(versionPath, PACKAGE_FILE_NAME);
			if (packageFile.isDirectory()) {
				importDirectory(version, packageFile, out);
				version.setPackageFileName(packageFile.getAbsolutePath() + '/');
			} else {
				version.setPackageFileName(packageFileName);
				if (packageFileName.endsWith(".tar.gz") || packageFileName.endsWith(".tgz")) {
					String rootFolder = getTarRootFolder(version, new GzipCompressorInputStream(new FileInputStream(packageFile)));
					importTarFile(version, new GzipCompressorInputStream(new FileInputStream(packageFile)), out, rootFolder);
				} else if (packageFileName.endsWith(".tar.bz2")) {
					String rootFolder = getTarRootFolder(version, new BZip2CompressorInputStream(new FileInputStream(packageFile)));
					importTarFile(version, new BZip2CompressorInputStream(new FileInputStream(packageFile)), out, rootFolder);
				} else if (packageFileName.endsWith(".zip") || packageFileName.endsWith(".war")) {
					Map<String, Long> fileSizes = new java.util.HashMap<String, Long>();
					String rootFolder = getZipFileSizesAndRootFolder(new FileInputStream(packageFile), fileSizes);
					importZipFile(version, new FileInputStream(packageFile), out, rootFolder, fileSizes);
				} else if (packageFileName.endsWith(".tar")) {
					String rootFolder = getTarRootFolder(version, new FileInputStream(packageFile));
					importTarFile(version, new FileInputStream(packageFile), out, rootFolder);
				} else {
					// regular non-package file to import.
					importPlainFile(version, packageFileName, packageFile.length(), new FileInputStream(packageFile), out);
				}
			}
			long size = this.repositoryService.getFileLength(versionPath, PACKAGE_FILE_NAME);
			version.setPackageSize(size);
			if (size == 0) {
				version.setPackageFileName(null);
			}
			updateVersionFiles(version, excludedExtensions);
			return versionPath + PACKAGE_FILE_NAME;
		} catch (IOException e) {
			throw new InternalErrorException(e);
		}
	}

	/**
	 * Updates the package file of a version
	 * 
	 * @param version
	 * @param excludedExtensions
	 */
	public void updatePackage(Version version, Set<String> excludedExtensions) {
		LOG.debug("updatePackage existing {}", version);
		try {
			updateVersionFiles(version, excludedExtensions);
		} catch (IOException e) {
			throw new InternalErrorException(e);
		}
	}

	/**
	 * Updates the package file of a version
	 * 
	 * @param version
	 */
	public void removePackage(Version version) {
		LOG.debug("removePackage {}", version);
		this.repositoryService.deleteVersion(version.getArtifact().getName(), version.getName());
		deleteOverlays(version);
		version.setBuiltinProperties(false);
		version.setPackageUploadDate(0L);
		version.setPackageSize(0);
		version.setPackageFileName(null);
		version.getFiles().clear();
		version.setNeedPackageRescan(false);
	}

	private void deleteOverlays(Version version) {
		for (Iterator<RepositoryFile> it = version.getFiles().iterator(); it.hasNext();) {
			RepositoryFile f = it.next();
			if (f.getLayerType() == LayerType.ARTIFACT_OVERLAY) {
				it.remove();
			}
		}
	}

	/**
	 * @param version
	 * @param sourceDir
	 * @param package
	 * @throws IOException
	 */
	private void importDirectory(
			Version version,
			File sourceDir,
			OutputStream pkg
			) throws IOException {
		LOG.debug("importDirectory");
		TarArchiveOutputStream tos = new TarArchiveOutputStream(pkg);
		tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
		List<String> directories = new java.util.ArrayList<String>();
		directories.add(StringUtils.EMPTY);
		while (!directories.isEmpty()) {
			String basePath = directories.get(0);
			directories.remove(0);
			File[] files = new File(sourceDir, basePath).listFiles();
			if (!basePath.isEmpty() && files.length == 0) {
				TarArchiveEntry tarEntry = new TarArchiveEntry(basePath.substring(0, basePath.length()));
				tarEntry.setSize(0);
				tos.putArchiveEntry(tarEntry);
				tos.closeArchiveEntry();
			}
			for (int i = 0; i < files.length; i++) {
				File file = files[i];
				if (file.isDirectory()) {
					directories.add(basePath + file.getName() + "/");
					continue;
				}
				if (file.getName().equals(".DS_Store")) {
					continue;
				}
				TarArchiveEntry tarEntry = new TarArchiveEntry(basePath + file.getName());
				tarEntry.setModTime(file.lastModified());
				long size = file.length();
				/*
				 * Is it a symbolic link?
				 */
				if (!file.getCanonicalPath().equals(file.getAbsolutePath())) {
					String canon = file.getCanonicalPath();
					if (!canon.startsWith(sourceDir.getCanonicalPath())) {
						LOG.warn("Ignored symbolic link '" + basePath + file.getName() + "' to a file outside the package directory");
						continue; // skip link that points outside packageDir
					}
					tarEntry.setLinkName(canon.substring(sourceDir.getCanonicalPath().length()));
				} else {
					tarEntry.setSize(size);
					if (file.canExecute()) {
						tarEntry.setMode(tarEntry.getMode() | 0100); // th.mode += 0100;
					}
				}
				tos.putArchiveEntry(tarEntry);
				InputStream in = new FileInputStream(file);
				IOUtils.copy(in, tos);
				IOUtils.closeQuietly(in);
				tos.closeArchiveEntry();
			}
		}
		tos.flush();
		IOUtils.closeQuietly(tos);
	}

	/**
	 * @param version
	 * @param source
	 * @returns the root folder name if there is one, null otherwise
	 * @throws IOException
	 */
	private String getTarRootFolder(Version version, InputStream source) throws IOException {
		LOG.debug("getTarRootFolder");
		TarArchiveInputStream tis = new TarArchiveInputStream(source);
		String rootFolder = StringUtils.EMPTY;
		TarArchiveEntry entry;
		while ((entry = tis.getNextTarEntry()) != null) {
			rootFolder = extractRootFolder(rootFolder, entry.getName().replace('\\', '/'), entry.isDirectory());
		}
		IOUtils.closeQuietly(tis);
		return rootFolder;
	}

	/**
	 * @param version
	 * @param source
	 * @param packageStream
	 * @param rootFolder
	 * @throws IOException
	 */
	private void importTarFile(
			Version version,
			InputStream source,
			OutputStream packageStream,
			String rootFolder
			) throws IOException {
		LOG.debug("importTarFile");
		TarArchiveInputStream tis = new TarArchiveInputStream(source);
		TarArchiveOutputStream tos = new TarArchiveOutputStream(packageStream);
		tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
		int rootFolderLen = rootFolder == null ? 0 : rootFolder.length();
		TarArchiveEntry entry;
		while ((entry = tis.getNextTarEntry()) != null) {
			String name = entry.getName().replace('\\', '/');
			if (rootFolder != null) {
				if (name.equals(rootFolder)) {
					continue;
				}
				entry.setName(name.substring(rootFolderLen));
			}
			else {
				entry.setName(name);
			}
			tos.putArchiveEntry(entry);
			IOUtils.copy(tis, tos);
			tos.closeArchiveEntry();
			tos.flush();
		}
		IOUtils.closeQuietly(tos);
		IOUtils.closeQuietly(tis);
	}

	/**
	 * @param version
	 * @param fileName
	 * @param fileSize
	 * @param source
	 * @param packageStream
	 * @throws IOException
	 */
	private void importPlainFile(
			Version version,
			String fileName,
			long fileSize,
			InputStream source,
			OutputStream packageStream
			) throws IOException {
		LOG.debug("importPlainFile {}", fileName);
		try {
			TarArchiveOutputStream tos = new TarArchiveOutputStream(packageStream);
			tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
			TarArchiveEntry tarEntry = new TarArchiveEntry(fileName);
			tarEntry.setModTime(new Date());
			tarEntry.setSize(fileSize);
			tos.putArchiveEntry(tarEntry);
			IOUtils.copy(source, tos);
			tos.closeArchiveEntry();
			tos.flush();
			IOUtils.closeQuietly(tos);
		} finally {
			IOUtils.closeQuietly(source);
		}
	}

	/**
	 * @param version
	 * @param source
	 * @param packageStream
	 * @param rootFolder
	 * @param fileSizes
	 * @throws IOException
	 */
	private void importZipFile(
			Version version,
			InputStream source,
			OutputStream packageStream,
			String rootFolder,
			Map<String, Long> fileSizes
			) throws IOException {
		LOG.debug("importZipFile");
		ZipArchiveInputStream zis = new ZipArchiveInputStream(source);
		TarArchiveOutputStream tos = new TarArchiveOutputStream(packageStream);
		tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
		int rootFolderLen = rootFolder == null ? 0 : rootFolder.length();
		ZipArchiveEntry zipEntry;
		while ((zipEntry = zis.getNextZipEntry()) != null) {
			String name = rootFolder == null ? zipEntry.getName() : zipEntry.getName().substring(rootFolderLen);
			if (name.isEmpty()) {
				continue;
			}
			TarArchiveEntry tarEntry = new TarArchiveEntry(name.replace("\\", "/"));
			tarEntry.setModTime(zipEntry.getTime());
			tarEntry.setMode(zipEntry.getUnixMode());
			if (zipEntry.isDirectory()) {
				tarEntry.setSize(0);
				tos.putArchiveEntry(tarEntry);
			} else {
				Long size = fileSizes.get(zipEntry.getName());
				if (size == null || size < 0) {
					throw new InternalErrorException("Unknown error (zip size)");
				}
				tarEntry.setSize(size);
				tos.putArchiveEntry(tarEntry);
				IOUtils.copy(zis, tos);
			}
			tos.closeArchiveEntry();
		}
		tos.flush();
		IOUtils.closeQuietly(tos);
	}

	/**
	 * First pass computation of zip entry file sizes (when undefined only)
	 * 
	 * @param source
	 * @param fileSizes
	 * @return the rootFolder name if there is one otherwise null
	 * @throws IOException
	 */
	private String getZipFileSizesAndRootFolder(
			InputStream source,
			Map<String, Long> fileSizes
			) throws IOException {
		ZipArchiveInputStream zis = new ZipArchiveInputStream(source);
		String rootFolder = StringUtils.EMPTY;
		ZipArchiveEntry zipEntry;
		while ((zipEntry = zis.getNextZipEntry()) != null) {
			String name = zipEntry.getName().replace("\\", "/");
			rootFolder = extractRootFolder(rootFolder, name, zipEntry.isDirectory());
			if (!zipEntry.isDirectory()) {
				long size = 0;
				if (zipEntry.getSize() > 0) {
					size = zipEntry.getSize();
				} else {
					// Since the zip/war file does not define file lengths we are forced to copy the file to a temp location to calculate length of header
					byte[] buffer = new byte[8192];
					int len;
					while ((len = zis.read(buffer)) > 0) {
						size += len;
					}
				}
				fileSizes.put(name, size);
			}
		}
		IOUtils.closeQuietly(zis);
		return rootFolder;
	}

	private String extractRootFolder(String rootFolder, String path, boolean isDirectory) {
		if (rootFolder != null) {
			int idx = path.indexOf('/');
			String baseDir = idx == -1 ? (isDirectory ? path : StringUtils.EMPTY) : path.substring(0, idx + 1);
			if (rootFolder.isEmpty()) {
				return baseDir;
			}
			if (rootFolder.equals(baseDir)) {
				return rootFolder;
			}
		}
		return null;
	}

	/**
	 * Updates the version files with data extracted from package.<br/>
	 * 
	 * @param version
	 * @param excludedExtensions
	 *            list of extensions for which kwatee variables will be ignored
	 * @throws IOException
	 */
	private void updateVersionFiles(Version version, Set<String> excludedExtensions) throws IOException {
		LOG.debug("updateVersionFiles {}", version);
		String templatePath = this.repositoryService.getVersionTemplatePath(version.getArtifact().getName(), version.getName());
		FileUtils.deleteQuietly(new File(templatePath));
		Collection<RepositoryFile> files = new java.util.ArrayList<RepositoryFile>();
		if (version.getPackageFileName() != null) {
			boolean hasTemplates = extractRepositoryFiles(getPackageInputStream(version.getArtifact().getName(), version.getName()), files/* , directories */, excludedExtensions, version.getVarPrefixChar());
			if (hasTemplates) {
				extractTemplates(getPackageInputStream(version.getArtifact().getName(), version.getName()), templatePath, files);
				updateBuiltInDefaultVariables(version, files);
			}
		}
		/*
		 * Add update files
		 */
		for (RepositoryFile f : files) {
			try {
				RepositoryFile file = getVersionPackageFile(version, f.getRelativePath());
				file.setLayerType(f.getLayerType());
				file.setDirectory(f.isDirectory());
				file.setSymbolicLink(f.isSymbolicLink());
				file.setSignature(f.getSignature());
				if (f.getFileOwner() != null)
					file.setFileOwner(f.getFileOwner());
				if (f.getFileGroup() != null)
					file.setFileGroup(f.getFileGroup());
				if (f.getFileMode() != null)
					file.setFileMode(f.getFileMode());
				if (f.getDirMode() != null)
					file.setDirMode(f.getDirMode());
				file.setOriginalOwner(f.getOriginalOwner());
				file.setOriginalGroup(f.getOriginalGroup());
				file.setOriginalMode(f.getOriginalMode());
				file.setSize(f.getSize());
				file.setVariables(f.getVariables());
			} catch (InternalErrorException e) {
				version.getFiles().add(f);
			}
		}
		/*
		 * Remove obsolete files
		 */
		for (Iterator<RepositoryFile> it = version.getFiles().iterator(); it.hasNext();) {
			RepositoryFile f = it.next();
			if (f.getLayerType() != LayerType.ARTIFACT_OVERLAY && !files.contains(f)) {
				it.remove();
			}
		}
		version.setNeedPackageRescan(false);
	}

	private void updateBuiltInDefaultVariables(Version version, Collection<RepositoryFile> files) {
		for (RepositoryFile file : files) {
			if (file.isTemplatized()) {
				VarInfo varInfo = VarInfo.valueOf(file.getVariables());
				for (int i = 0; i < varInfo.size(); i++) {
					if (varInfo.getDefaultValue(i) != null) {
						VersionVariable v = findDefaultVariable(version, varInfo.getName(i));
						if (v == null) {
							v = new VersionVariable();
							v.setName(varInfo.getName(i));
							v.setVersion(version);
							version.getVariablesDefaultValues().add(v);
						}
						v.setDescription("***BUILT-IN***");
						v.setDefaultValue(varInfo.getDefaultValue(i));
					}
				}
			}
		}
	}

	private VersionVariable findDefaultVariable(Version version, String varName) {
		for (VersionVariable v : version.getVariablesDefaultValues()) {
			if (varName.equals(v.getName())) {
				return v;
			}
		}
		return null;
	}

	/*
	 * 
	 */
	private RepositoryFile getVersionPackageFile(Version version, String path) throws InternalErrorException {
		for (RepositoryFile f : version.getFiles()) {
			if ((f.getLayerType() == LayerType.ARTIFACT || f.getLayerType() == LayerType.ARTIFACT_TEMPLATE) && f.getRelativePath().equals(path))
				return f;
		}
		throw new InternalErrorException("File not found in version");
	}

	/**
	 * Extract a list of FileDto from <code>package</code> as well as the list of <code>directories</code> found
	 * in the package
	 * 
	 * @param packageStream
	 * @param files
	 * @param directories
	 * @param excludedExtensions
	 * @param varPrefixChar
	 * @return true if the package contains templates (i.e. files containing variables)
	 * @throws IOException
	 */
	private boolean extractRepositoryFiles(
			InputStream packageStream,
			Collection<RepositoryFile> files,
			Set<String> excludedExtensions,
			char varPrefixChar) throws IOException {
		// First pass scan for templatized files and signature calculation
		TarArchiveInputStream tis = new TarArchiveInputStream(packageStream);
		TarArchiveEntry entry;
		HashSet<String> dirNames = new HashSet<String>();
		boolean hasTemplates = false;
		while ((entry = tis.getNextTarEntry()) != null)
			if (!entry.getName().startsWith("./")) {
				addDirEntry(files, entry, dirNames);
				if (!entry.isDirectory()) {
					RepositoryFile file = new RepositoryFile(entry.getName());
					file.setLayerType(LayerType.ARTIFACT);
					file.setSymbolicLink(entry.isSymbolicLink());
					file.setOriginalOwner(StringUtils.isEmpty(entry.getUserName()) ? null : entry.getUserName());
					file.setOriginalGroup(StringUtils.isEmpty(entry.getGroupName()) ? null : entry.getGroupName());
					file.setOriginalMode(entry.getMode());
					file.setSize(entry.getSize());
					if (file.isSymbolicLink()) {
						LOG.debug("{} (symbolic link)", entry.getName());
						file.setSignature(CryptoUtils.computeStringSignature(entry.getName()));
					} else {
						if (!isScannable(file, excludedExtensions)) {
							computeEntrySignature(tis, file, entry.getSize());
						} else {
							scanPackageStream(tis, file, entry.getSize(), varPrefixChar);
						}
						if (file.isTemplatized()) {
							hasTemplates = true;
							LOG.debug("{} (templatized)", entry.getName());
						} else {
							LOG.debug(entry.getName());
						}
					}
					files.add(file);
				}
			}
		IOUtils.closeQuietly(tis);
		return hasTemplates;
	}

	private void addDirEntry(Collection<RepositoryFile> files, TarArchiveEntry entry, HashSet<String> dirNames) {
		String dirName = entry.getName();
		if (!entry.isDirectory()) {
			int dirIdx = dirName.lastIndexOf('/');
			if (dirIdx < 0)
				return;
			dirName = dirName.substring(0, dirIdx + 1);
			entry = null;
		}
		while (!dirNames.contains(dirName)) {
			RepositoryFile dir = new RepositoryFile(dirName, true);
			dir.setLayerType(LayerType.ARTIFACT);
			if (entry != null) {
				dir.setSymbolicLink(entry.isSymbolicLink());
				dir.setOriginalOwner(StringUtils.isEmpty(entry.getUserName()) ? null : entry.getUserName());
				dir.setOriginalGroup(StringUtils.isEmpty(entry.getGroupName()) ? null : entry.getGroupName());
				dir.setOriginalMode(entry.getMode());
			}
			entry = null;
			dir.setSignature(CryptoUtils.computeStringSignature(dirName));
			files.add(dir);
			dirNames.add(dirName);
			LOG.debug(dirName);
			int dirIdx = dirName.lastIndexOf('/', dirName.length() - 2);
			if (dirIdx == -1)
				break;
			dirName = dirName.substring(0, dirIdx + 1);
		}
	}

	private boolean isScannable(RepositoryFile file, Set<String> excludedExtensions) {
		if (!this.variableService.isScannable(file.getSize())) {
			return false;
		}

		if (excludedExtensions == null) {
			return true;
		}
		String path = file.getRelativePath();
		int idx = path.lastIndexOf('.');
		if (idx >= 0) {
			String ext = path.substring(idx + 1).toLowerCase();
			return !excludedExtensions.contains(ext);
		}
		return true;
	}

	/**
	 * Extracts the list of templatized <code>files</code> from <code>package</code>
	 * 
	 * @param packageStream
	 * @param templatePath
	 * @param files
	 * @throws IOException
	 */
	private void extractTemplates(
			InputStream packageStream,
			String templatePath,
			Collection<RepositoryFile> files
			) throws IOException {
		TarArchiveInputStream tis = new TarArchiveInputStream(packageStream);
		Iterator<RepositoryFile> it = files.iterator();
		TarArchiveEntry entry;
		while ((entry = tis.getNextTarEntry()) != null) {
			if (!entry.isDirectory()) {
				RepositoryFile file;
				do {
					file = it.next();
				} while (file.isDirectory());
				if (file.isTemplatized()) {
					OutputStream out = this.repositoryService.getFileOutputStream(templatePath, file.getRelativePath());
					IOUtils.copy(tis, out);
					IOUtils.closeQuietly(out);
				}
			}
		}
		IOUtils.closeQuietly(tis);
	}

	/**
	 * Computes the MD5 file signature of a file that is not scanned for variables
	 * 
	 * @param in
	 *            input stream of <code>file</code>
	 * @param file
	 * @param size
	 * @throws IOException
	 */
	private void computeEntrySignature(InputStream in, RepositoryFile file, long size) throws IOException {
		file.setVariables(null);
		MessageDigest md = CryptoUtils.getNewDigest(file.getRelativePath());
		byte[] buffer = new byte[8192];
		int len;
		while ((len = in.read(buffer)) > 0) {
			md.update(buffer, 0, len);
		}
		file.setSignature(CryptoUtils.getSignature(md));
	}

	/**
	 * Scans a Repositoryile, checks if it is templatized. If not, computes the MD5 file signature
	 * 
	 * @param in
	 *            input stream of <code>file</code>
	 * @param file
	 * @param size
	 * @throws IOException
	 */
	private void scanPackageStream(InputStream in, RepositoryFile file, long size, char varPrefixChar) throws IOException {
		MessageDigest md = CryptoUtils.getNewDigest(file.getRelativePath());
		String variables = this.variableService.extractStreamVariables(in, md, varPrefixChar);
		file.setSignature(CryptoUtils.getSignature(md));
		file.setVariables(variables);
		if (file.isTemplatized()) {
			file.setLayerType(LayerType.ARTIFACT_TEMPLATE);
		}
	}

	/**
	 * Extracts the package entry designated by <code>path</path> into the supplied <code>extractFile</code>
	 * 
	 * @param version
	 * @param path
	 * @param out
	 * @return the name of the extracted package entry, null if not found
	 * @throws IOException
	 */
	public String extractPackageFileContents(Version version, String path, OutputStream out) throws IOException {
		String name = null;
		TarArchiveInputStream tis = new TarArchiveInputStream(getPackageInputStream(version.getArtifact().getName(), version.getName()));
		TarArchiveEntry entry;
		while ((entry = tis.getNextTarEntry()) != null) {
			if (entry.getName().equals(path)) {
				if (entry.isDirectory()) {
					String text = "Directory entry: " + entry.getName();
					out.write(text.getBytes());
				} else if (entry.isSymbolicLink()) {
					String text = "Symbolic link: " + entry.getName();
					IOUtils.write(text, out, "UTF8");
				} else {
					IOUtils.copy(tis, out);
				}
				IOUtils.closeQuietly(out);
				int pos = path.lastIndexOf('/');
				name = path.substring(pos + 1);
				break;
			}
		}
		IOUtils.closeQuietly(tis);
		return name;
	}

	public void rescanPackage(Version version, Set<String> excludedExtensions) {
		String overlayPath = this.repositoryService.getVersionOverlayPath(version.getArtifact().getName(), version.getName());
		try {
			updateVersionFiles(version, excludedExtensions);
			boolean hasVariables = false;
			for (RepositoryFile file : version.getFiles()) {
				if (file.getLayerType() == LayerType.ARTIFACT_OVERLAY) {
					scanOverlayForVariables(file, overlayPath, version.getVarPrefixChar());
					hasVariables |= file.isTemplatized();
				}
			}
			if (hasVariables) {
				updateBuiltInDefaultVariables(version, version.getFiles());
			}
		} catch (IOException e) {
			throw new InternalErrorException(e);
		}
	}

	/**
	 * Update repository file record with overlay file if exists otherwise create new repository file entry.
	 * 
	 * @param version
	 * @param path
	 * @throws ObjectAlreadyExistsException
	 */
	public void updateVersionOverlay(Version version, String path) throws ObjectAlreadyExistsException {
		RepositoryFile file = null;
		try {
			file = version.getFile(path);
			/*
			 * The file overrides an existing artifact file. If there was already an overlay then update it.
			 */
			if (file.getLayerType() != LayerType.ARTIFACT_OVERLAY) {
				file = null;
			}
		} catch (ObjectNotExistException e) { /* it's ok, we'll create it */
		}
		if (file == null) {
			addOverlayParentDirs(path, version.getFiles());
			file = new RepositoryFile(path);
			file.setLayerType(LayerType.ARTIFACT_OVERLAY);
			version.getFiles().add(file);
		} else {
			if (file.isDirectory()) {
				throw new ObjectAlreadyExistsException("Directory");
			}
		}
		file.setSymbolicLink(false);
		file.setDirectory(path.endsWith("/"));
		if (!file.isDirectory()) {
			String overlayPath = this.repositoryService.getVersionOverlayPath(version.getArtifact().getName(), version.getName());
			file.setSize(this.repositoryService.getFileLength(overlayPath, path));
			scanOverlayForVariables(file, overlayPath, version.getVarPrefixChar());
		}
	}

	private void addOverlayParentDirs(String path, Collection<RepositoryFile> files) {
		int idx = path.length();
		while ((idx = path.lastIndexOf('/', idx - 1)) > 0) {
			RepositoryFile dir = new RepositoryFile(path.substring(0, idx + 1));
			dir.setDirectory(true);
			if (files.contains(dir)) {
				return;
			}
			dir.setLayerType(LayerType.ARTIFACT_OVERLAY);
			files.add(dir);
		}
	}

	private void scanOverlayForVariables(RepositoryFile file, String overlayPath, char varPrefixChar) {
		MessageDigest md = CryptoUtils.getNewDigest(file.getRelativePath());
		String variables = this.variableService.extractStreamVariables(this.repositoryService.getFileInputStream(overlayPath, file.getRelativePath()), md, varPrefixChar);
		file.setVariables(variables);
		file.setSignature(CryptoUtils.getSignature(md));
	}

	/**
	 * Removes a version overlay
	 * 
	 * @param version
	 * @param path
	 */
	public void removeVersionOverlay(Version version, String path) {
		for (Iterator<RepositoryFile> it = version.getFiles().iterator(); it.hasNext();) {
			RepositoryFile f = it.next();
			if (f.getLayerType() == LayerType.ARTIFACT_OVERLAY && f.getRelativePath().equals(path)) {
				it.remove();
				break;
			}
		}
	}
}
