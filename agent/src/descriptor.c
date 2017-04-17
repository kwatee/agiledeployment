/* Copyright (c) 2011, Kwatee Ltd
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * Neither the name of Kwatee Ltd nor the names of its
 * contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL KWATEE LIMITED,
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "md5.h"
#include "ezxml.h"
#include "hashmap.h"
#include "utils.h"
#include <unistd.h>
#include <errno.h>
#include <string.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <stdio.h>
#include "kwateeerrors.h"
#include "descriptor.h"

static ezxml_t new_files;

static my_map_t s_cache = NULL;
typedef struct cache_item_s {
	char file_path[MAX_PATH_LENGTH];
	long time_stamp;
} cache_item_t;

#if !defined(WIN)
#include <pwd.h>
#include <grp.h>
typedef struct perm {
	uid_t	uid;
	gid_t	gid;
	int		fMode;
	int		dMode;
} FILE_PERM;
#else
typedef void FILE_PERM;
#endif

typedef enum {
	CHECK,
	DELETE,
	DELETE_OBSOLETE,
	PRINT,
	PERMISSIONS
} OpCode;


/*************************************************************************
 * Load the contents of the cache into a static hashmap "s_cache"
 *************************************************************************/
static void loadCache(const char *cacheFile) {
	s_cache = hashmap_new();
	FILE *f = fopen(cacheFile, "r");
	if (f != NULL) {
		char line[MAX_PATH_LENGTH];
		while (fgets(line, sizeof(line), f)) {
			char *sep = strchr(line, '=');
			if (sep != NULL) {
				cache_item_t *item = malloc(sizeof(cache_item_t));
				*sep ++ = 0;
				strcpy(item->file_path, line);
				item->time_stamp = atol(sep);
				hashmap_put(s_cache, item->file_path, item);
			}
		}
		fclose(f);
	}
}

/*************************************************************************
 *
 *************************************************************************/
static int PFIterator(FILE *f, cache_item_t *cacheItem) {
	fprintf(f, "%s=%ld\n", cacheItem->file_path, cacheItem->time_stamp);
	free(cacheItem);
	return MAP_OK;
}

/*************************************************************************
 * Save the content of the static cach hashmap into a file
 *************************************************************************/
static void saveCache(const char *cacheFile) {
	if (s_cache != NULL) {
		FILE *f = fopen(cacheFile, "w");
		if (f != NULL) {
			hashmap_iterate(s_cache, (PFany)PFIterator, f);
			fclose(f);
		}
		hashmap_free(s_cache);
		s_cache = NULL;
	}
}

/*************************************************************************
 * Return the name of the cache file corresponding to the artifact descriptor
 *************************************************************************/
static char *getCacheFileName(const char *kwateeDir, ezxml_t descriptor) {
	const char *deployment = ezxml_attr(descriptor, "deployment_name");
	const char *package = strdup(ezxml_attr(descriptor, "artifact_name"));
	*strchr(package, '[') = 0;	// cut out version info
	char *path = concatenatePath(concatenatePath(kwateeDir, deployment), concatenate(package, ".cache"));
	return path;
}

/*************************************************************************
 * Delete the cache file
 *************************************************************************/
void deleteCacheFile(const char *kwateeDir, const void *descriptor) {
	char *cacheFile = getCacheFileName(kwateeDir, (ezxml_t)descriptor);
	unlink(cacheFile);
}

/*************************************************************************
 *
 *************************************************************************/
static int hasChanged(const char *deployInDir, const char *filePath) {
	cache_item_t *cacheItem = NULL;
	if (s_cache != NULL && hashmap_get(s_cache, (char *)filePath, (any_t)&cacheItem) == MAP_OK) {
		struct stat fileInfo;
		char *path = concatenatePath(deployInDir, filePath);
		int s = stat(path, &fileInfo);
		free(path);
		if (s == 0) {
#if defined(_DARWIN_C_SOURCE)
			long timeStamp = fileInfo.st_mtimespec.tv_sec;
#else
			long timeStamp = fileInfo.st_mtime;
#endif
			return cacheItem->time_stamp != timeStamp;
		}
	}
	return 1;
}

/*************************************************************************
 * 
 *************************************************************************/
static void updateCacheEntry(const char *deployInDir, const char *filePath) {
	if (s_cache != NULL) {
		// Get file info
		struct stat fileInfo;
		char *path = concatenatePath(deployInDir, filePath);
		int s = stat(path, &fileInfo);
		free(path);
		if (s == 0) {
			long timeStamp = 0;
			cache_item_t *item = NULL;
#if defined(_DARWIN_C_SOURCE)
			timeStamp = fileInfo.st_mtimespec.tv_sec;
#else
			timeStamp = fileInfo.st_mtime;
#endif
			if (hashmap_get(s_cache, (char *)filePath, (any_t)&item) == MAP_OK)
				item->time_stamp = timeStamp;
			else {
				item = malloc(sizeof(cache_item_t));
				strcpy(item->file_path, filePath);
				item->time_stamp = timeStamp;
				hashmap_put(s_cache, (char *)filePath, item);
			}
		}
	}
}


static int computeFileSignature(const char *deployInDir, const char *filePath, char *signature) {
	char *path = concatenatePath(deployInDir, filePath);
	FILE *f = fopen(path, "rb");
	free(path);
	if (f) {
		char buffer[MAX_PATH_LENGTH];
		strcpy(buffer, filePath);
		int count = strlen(buffer);
		int i;
		for (i = 0; i < count; i ++) {
			if (buffer[i] == '\\')
				buffer[i] = '/';
		}
		md5_init();
		md5_update((char *)buffer, count);
		while ((count = fread(buffer, 1, sizeof(buffer), f)) > 0)
			md5_update(buffer, count);
		fclose(f);
		md5_finalize();
		strcpy(signature, md5_hexdigest());
		return 1;
	}
	return 0;
}


/*************************************************************************
 * 
 *************************************************************************/
static int checkFileSignature(const char *deployInDir, const char *filePath, const char *signature, int silent) {
	if (!hasChanged(deployInDir, filePath) || *signature == '\0')
		return 1;
	char computedSignature[64];
	if (computeFileSignature(deployInDir, filePath, computedSignature)) {
		if (strcmp(signature, computedSignature) == 0) {
			updateCacheEntry(deployInDir, filePath);
			return 1;
		}
		if (!silent)
			printf("***file %s has been tampered with\n", filePath);
	} else {
		if (!silent)
			printf("***could not verify signature of file %s\n", filePath);
	}
	return 0;
}

/*************************************************************************
 * If filePath is not present in old_descriptor it is obsolete
 *************************************************************************/
static int isObsolete(const char *name, const ezxml_t parent) {
	ezxml_t f;
	for (f = ezxml_child(parent, "file"); f; f = ezxml_next(f)) {
		if (strcmp(name, ezxml_attr(f, "name")) == 0)
			return 0;
	}
	for (f = ezxml_child(parent, "directory"); f; f = ezxml_next(f)) {
		if (!isObsolete(name, f))
			return 0;
	}
	return 1;
}

#if !defined(WIN)
static void getPermissions(FILE_PERM *permissions, const char *fileOwner, const char *fileGroup, const char *fileMode, const char *dirMode) {
	permissions->uid = -1;
	permissions->gid = -1;
	permissions->fMode = 0;
	permissions->dMode = 0;
	if (fileOwner && *fileOwner) {
		struct passwd *pwd = getpwnam(fileOwner);
		if (pwd)
			permissions->uid = pwd->pw_uid;
	}
	if (fileGroup && *fileGroup) {
		struct group * grp = getgrnam(fileGroup);
		if (grp)
			permissions->gid = grp->gr_gid;
	}
	if (fileMode && *fileMode)
		permissions->fMode = strtol(fileMode, NULL, 8) & 0x0000FFFF;
	if (dirMode && *dirMode)
		permissions->dMode = strtol(dirMode, NULL, 8) & 0x0000FFFF;
}
#endif

/*************************************************************************
 *
 *************************************************************************/
static int recurseFiles(const ezxml_t parent, const char *deployInDir, const char *dir, FILE_PERM *permissions, OpCode op, int silent) {

	int valid = 1;
	char *path;
	ezxml_t f;
#if !defined(WIN)
	FILE_PERM p;
#endif
	for (f = ezxml_child(parent, "file"); f; f = ezxml_next(f)) {
		const char *name = ezxml_attr(f, "name");
		const char *signature = ezxml_attr(f, "signature");
		const char *is_sym_link = ezxml_attr(f, "symbolic_link");
		const char *dont_delete = ezxml_attr(f, "dont_delete");
		char filePath[MAX_PATH_LENGTH];

		strcpy(filePath, dir);
		strcat(filePath, name);

		switch (op) {
		case DELETE:
		case DELETE_OBSOLETE:
			path = concatenatePath(deployInDir, filePath);
			if (fileExists(path, 0)) {
				if (dont_delete)
					printf("%s untouched (don't delete flag set)", filePath);
				else {
					if (op == DELETE || isObsolete(name, new_files)) {
						if (unlink(path) == 0) {
							if (!silent)
								printf("  [-] %s\n", filePath);
						} else {
							if (!silent)
								printf("*** failed to delete %s\n", filePath);
							valid = 0;
						}
					}
				}
			}
			free(path);
			break;
		case CHECK:
			path = concatenatePath(deployInDir, filePath);
			if (*signature != '\0' && !fileExists(path, 0)) {
				if (!silent)
					printf("*** %s not found\n", filePath);
				valid = 0;
			} else if (!is_sym_link && !checkFileSignature(deployInDir, filePath, signature, 0))
				valid = 0;
			free(path);
			break;
		case PRINT:
			printf("<file name=\"%s\" ", name);
			path = concatenatePath(deployInDir, filePath);
			if (!fileExists(path, 0) || (!is_sym_link && !checkFileSignature(deployInDir, filePath, signature, 1)))
				signature = "!!!!!";
			if (is_sym_link)
				printf(" symbolic_link=\"true\" ");
			printf("signature=\"%s\" />\n", signature);
            free(path);
			break;
		case PERMISSIONS:
#if !defined(WIN)
			getPermissions(&p, ezxml_attr(f, "file_owner"), ezxml_attr(f, "file_group"), ezxml_attr(f, "file_mode"), NULL);
			if (permissions) {
				if (p.uid == -1)
					p.uid = permissions->uid;
				if (p.gid == -1)
					p.gid = permissions->gid;
				if (p.fMode == 0)
					p.fMode = permissions->fMode;
				if (p.dMode == 0)
					p.dMode = permissions->dMode ;
			}
			if (p.uid != -1 || p.gid != -1 || p.fMode != 0) {
				path = concatenatePath(deployInDir, filePath);
				if (p.uid != -1 || p.gid != -1) {
                    if (chown(path, p.uid, p.gid)) {
                        printf("***chown uid=%d gid=%d on '%s' failed with errno=%d\n", p.uid, p.gid, path, errno);
                        exitGenericError("chown failed", 0L);
                    }
				}
                if (p.fMode != 0) {
                    if (chmod(path, p.fMode | 0600)) {
                        printf("***chmod %o on '%s' failed with errno=%d\n", p.fMode, path, errno);
                        exitGenericError("chmod failed", 0L);
                    }
                }
				free(path);
			}
#endif
			break;
		}
	}

	for (f = ezxml_child(parent, "directory"); f; f = ezxml_next(f)) {
		const char *name = ezxml_attr(f, "name");
		const char *signature = ezxml_attr(f, "signature");
		char dirPath[MAX_PATH_LENGTH];

		strcpy(dirPath, dir);
		strcat(dirPath, name);
#if (defined(WIN))
		strcat(dirPath, "\\");
#else
		strcat(dirPath, "/");
#endif
		switch (op) {
		case DELETE:
		case DELETE_OBSOLETE:
			if (!recurseFiles(f, deployInDir, dirPath, NULL, op, silent))
				valid = 0;
			path = concatenatePath(deployInDir, dirPath);
			if (rmdir(path) == 0) {
				if (!silent)
					printf("  [-] %s\n", dirPath);
			} else {
				if (op == DELETE && !silent)
					printf("*** failed to delete %s (is it empty?)\n", dirPath);
			}
			free(path);
			break;
		case CHECK:
			if (!recurseFiles(f, deployInDir, dirPath, NULL, CHECK, silent))
				valid = 0;
			break;
		case PRINT:
			if (signature)
				printf("<directory name=\"%s/\" signature=\"%s\" />\n", name, signature);	// empty dir
			else {
				printf("<directory name=\"%s\" >\n", name);
				recurseFiles(f, deployInDir, dirPath, NULL, PRINT, 0);
				printf("</directory>\n");
			}
			break;
		case PERMISSIONS:
#if !defined(WIN)
            if (ezxml_attr(f, "signature")) {
                getPermissions(&p, ezxml_attr(f, "file_owner"), ezxml_attr(f, "file_group"), ezxml_attr(f, "file_mode"), ezxml_attr(f, "dir_mode"));
                if (permissions) {
                    if (p.uid == -1)
                        p.uid = permissions->uid;
                    if (p.gid == -1)
                        p.gid = permissions->gid;
                    if (p.fMode == 0)
                        p.fMode = permissions->fMode;
                    if (p.dMode == 0)
                        p.dMode = permissions->dMode;
                }
                if (p.uid != -1 || p.gid != -1 || p.dMode != 0) {
                    path = concatenatePath(deployInDir, dirPath);
                    if (p.uid != -1 || p.gid != -1) {
                        if (chown(path, p.uid, p.gid)) {
                            printf("***chown uid=%d gid=%d on '%s' failed with errno=%d\n", p.uid, p.gid, path, errno);
                            exitGenericError("chown failed", 0L);
                        }
printf("DIR chown uid=%d gid=%d on '%s'\n", p.uid, p.gid, dirPath);
                    }
                    if (p.dMode != 0 && chmod(path, p.dMode | 0700)) {
                        printf("***chmod %o on '%s' failed with errno=%d\n", p.dMode, path, errno);
                        exitGenericError("chmod failed", 0L);
                    }
                    free(path);
                }
                recurseFiles(ezxml_next(f), deployInDir, dirPath, &p, PERMISSIONS, 0);
            }
#endif
			break;
		}
	}
	return valid;
}

void debugPrintDescriptor(const void *descriptor) {
	recurseFiles(ezxml_child((ezxml_t)descriptor, "package"), NULL, "", NULL, PRINT, 0);
}

/*************************************************************************
 *
 *************************************************************************/
static void *checkPackageExistence(const char *kwateeDir, const char *deployment, const char *package, int isError)  {
	char *descriptorFile = concatenatePath(concatenatePath(kwateeDir, deployment), concatenate(package, ARTIFACT_EXTENSION));
	if (!fileExists(descriptorFile, 0)) {
		if (isError)
			exitNoDescriptorError(package, NULL);
		else
			exitNoError("No kwatee package installed");
	}
	return ezxml_parse_file(descriptorFile);
}

/*************************************************************************
 *
 *************************************************************************/
static char *checkDescriptorSignature(const char *descriptorFile) {
	char *buffer;
	struct stat fileInfo;
	FILE *f = NULL;

	if (stat(descriptorFile, &fileInfo) == 0)
		f = fopen(descriptorFile, "r");
	if (!f)
		exitGenericError("could not open descriptor file %s", descriptorFile);
	buffer = malloc(fileInfo.st_size+1);
	buffer[fileInfo.st_size] = 0;
	if (fread(buffer, 1, fileInfo.st_size, f) != fileInfo.st_size) {
		fclose(f);
		exitGenericError("could not read descriptor file %s", descriptorFile);
	}
	fclose(f);

	char *sigp = strstr(buffer, "signature=\"");
	char *quote = NULL;
	if (sigp != NULL)
		quote = strchr(sigp+11, '\"');
	if (quote != NULL) {
		static char signature[33];
		strncpy(signature, sigp+11, sizeof(signature)-1);
		signature[32] = 0;
		memmove(sigp+11, quote, strlen(quote)+1);
		md5_init();
		md5_update(buffer, strlen(buffer));
		md5_finalize();
		free(buffer);
		if (strcmp(md5_hexdigest(), signature) == 0)
			return signature;
		exitTamperingError("file as been tampered with - bad signature", NULL);
	} else
		exitTamperingError("descriptor file as been tampered with - bad format", NULL);
	return NULL;
}

/*************************************************************************
 *
 *************************************************************************/
void *checkPackageIntegrity(const char *kwateeDir, const char *deployment, const char *package, char *refSignature, int noIntegrityCheck, int exitOnError) {
	char *descriptorFile = concatenatePath(concatenatePath(kwateeDir, deployment), concatenate(package, ARTIFACT_EXTENSION));
	if (!fileExists(descriptorFile, 0)) {
		if (exitOnError)
			exitNoDescriptorError(NULL, NULL);
		return NULL;
	}
	char *signature = checkDescriptorSignature(descriptorFile);
	if (refSignature && *refSignature && strcmp(refSignature, signature)) {
		if (exitOnError)
			exitOutOfDateError(NULL, NULL);
		printf("!Tampering detected");
		return NULL;
	}
	void *descriptor = checkPackageExistence(kwateeDir, deployment, package, 1);
	if (descriptor == NULL)
		exitTamperingError("bad descriptor", NULL);
    if (noIntegrityCheck)
        return (void *)descriptor;
	const char *deployInDir = ezxml_attr((ezxml_t)descriptor, "deploy_in");
	char *cacheFile = getCacheFileName(kwateeDir, (ezxml_t)descriptor);
	loadCache(cacheFile);
	if (!recurseFiles(ezxml_child((ezxml_t)descriptor, "package"), deployInDir, "", NULL, CHECK, !exitOnError)) {
		if (exitOnError)
			exitTamperingError(NULL, NULL);
	} else {
		saveCache(cacheFile);
	}
	return (void *)descriptor;
}

/*************************************************************************
 *
 *************************************************************************/
char *getDeployInDir(const void *descriptor) {
	return concatenate(ezxml_attr((ezxml_t)descriptor, "deploy_in"), "");
}

/*************************************************************************
 *
 *************************************************************************/
void deleteFiles(const void *descriptor) {
	char *deployInDir = getDeployInDir(descriptor);
	if (!recurseFiles(ezxml_child((ezxml_t)descriptor, "package"), deployInDir, "", NULL, DELETE, 0))
		exitGenericError(NULL, NULL);
}

/*************************************************************************
 *
 *************************************************************************/
void freeDescriptor(const void *descriptor) {
	if (descriptor)
		ezxml_free((ezxml_t)descriptor);
}

/*************************************************************************
 *
 *************************************************************************/
void descriptor_print(const char *kwateeDir, const void *descriptor) {
	if (descriptor == NULL)
		exitTamperingError("bad descriptor", NULL);
	const char *deployInDir = ezxml_attr((ezxml_t)descriptor, "deploy_in");
	const char *artifactName = ezxml_attr((ezxml_t)descriptor, "artifact_name");
	printf("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
	printf("<kwatee artifact_name=\"%s\" >\n", artifactName);
	char *cacheFile = NULL;
	if (kwateeDir) {
		cacheFile = getCacheFileName(kwateeDir, (ezxml_t)descriptor);
		loadCache(cacheFile);
	}
	recurseFiles(ezxml_child((ezxml_t)descriptor, "package"), deployInDir, "", NULL, PRINT, 0);
	if (kwateeDir)
		saveCache(cacheFile);
	printf("</kwatee>\n");
}

/*************************************************************************
 *
 *************************************************************************/
void *getDeploymentDescriptorByFile(const char *descriptorFile) {
	if (!fileExists(descriptorFile, 0))
		exitNoDescriptorError("No kwatee deployment found", "");
	checkDescriptorSignature(descriptorFile);
	return (void *)ezxml_parse_file(descriptorFile);
}

/*************************************************************************
 *
 *************************************************************************/
void *getDeploymentDescriptor(const char *kwateeDir, const char *deployment) {
	char *descriptorFile = concatenatePath(concatenatePath(kwateeDir, deployment), concatenate(deployment, DEPLOYMENT_EXTENSION));
	return getDeploymentDescriptorByFile((const char *)descriptorFile);
}

/*************************************************************************
 *
 *************************************************************************/
char *getDeploymentName(const void *descriptor) {
	return (char *)ezxml_attr((ezxml_t)descriptor, "deployment");
}

/*************************************************************************
 *
 *************************************************************************/
void *getPackageDescriptor(const char *kwateeDir, const char *deployment, const char *package) {
	char *descriptorPath = concatenatePath(concatenatePath(kwateeDir, deployment), concatenate(package, ARTIFACT_EXTENSION));
	if (!fileExists(descriptorPath, 0))
		return (void *)NULL;
	void *descriptor = ezxml_parse_file(descriptorPath);
	if (descriptor == NULL)
		exitGenericError("bad descriptor", NULL);
	return descriptor;
}

/*************************************************************************
 *
 *************************************************************************/
void removeObsoleteFiles(const char *kwateeDir, const char *deployment, const char *package, void *oldDescriptor) {
	char deployInDir[MAX_PATH_LENGTH];
	void *descriptor = checkPackageExistence(kwateeDir, deployment, package, 1);
	if (descriptor == NULL)
		exitGenericError("bad descriptor", NULL);
	strcpy(deployInDir, ezxml_attr((ezxml_t)descriptor, "deploy_in"));
	chdir(kwateeDir);
	new_files = ezxml_child((ezxml_t)descriptor, "package");
	int error = recurseFiles(ezxml_child(oldDescriptor, "package"), deployInDir, "", NULL, DELETE_OBSOLETE, 0);
	new_files = NULL;
	ezxml_free((ezxml_t)descriptor);
	if (!error)
		exitGenericError(NULL, NULL);
}

/*************************************************************************
 *
 *************************************************************************/
char *getDescriptorAction(const void *descriptor, const char *action) {
	ezxml_t actions = ezxml_child((ezxml_t)descriptor, "actions");
	ezxml_t a;
	for (a = ezxml_child(actions, "action"); a; a = ezxml_next(a)) {
		if (strcmp(ezxml_attr(a, "type"), action) == 0) {
			return ezxml_txt(a);
		}
	}
	return NULL;
}

/*************************************************************************
 *
 *************************************************************************/
char *getDeploymentArtifacts(const void *descriptor) {
	int len = 0;
	ezxml_t a;
	ezxml_t artifactsNode = ezxml_child((ezxml_t)descriptor, "artifacts");
	for (a = ezxml_child(artifactsNode, "artifact"); a; a = ezxml_next(a)) {
		len += strlen(ezxml_attr(a, "name")) + strlen(ezxml_attr(a, "version")) + 2;
	}
	char *artifacts = (char *)malloc(len+1);
	char *p = artifacts;
	for (a = ezxml_child(artifactsNode, "artifact"); a; a = ezxml_next(a)) {
		strcpy(p, ezxml_attr(a, "name"));
		p += strlen(p)+1;
		strcpy(p, ezxml_attr(a, "version"));
		p += strlen(p)+1;
		*p = 0;
	}
	return artifacts;
}

/*************************************************************************
 *
 *************************************************************************/
static char *recurseExtractSignatures(const ezxml_t parent, const char *dir, const char *basePath) {
	char *buffer = NULL;
	size_t bufferLen = 1;
	ezxml_t f;

	for (f = ezxml_child(parent, "file"); f; f = ezxml_next(f)) {
		char entry[MAX_PATH_LENGTH+64];
		char sig[64];
		strcpy(entry, dir);
		strcat(entry, ezxml_attr(f, "name"));
		
		char *signature = (char *)ezxml_attr(f, "signature");
		if (basePath) {
			// compute the real signature
			const char *is_sym_link = ezxml_attr(f, "symbolic_link");
			char *path = concatenatePath(basePath, entry);
			if (!fileExists(path, 0)) {
				signature = "***";
			} else if (!is_sym_link) {
				if (computeFileSignature(basePath, entry, sig))
					signature = sig;
				else
					signature = "***";
			}
			free(path);
		}

		strcat(entry, "=");
		strcat(entry, signature);
		strcat(entry, "\n");
		size_t entryLen = (size_t)strlen(entry);
		buffer = realloc(buffer, bufferLen + entryLen);
		memmove(buffer + bufferLen - 1L, entry, entryLen + 1);
		bufferLen += entryLen;
	}

	for (f = ezxml_child(parent, "directory"); f; f = ezxml_next(f)) {
		const char *name = ezxml_attr(f, "name");
		char dirPath[MAX_PATH_LENGTH];
		strcpy(dirPath, dir);
		strcat(dirPath, name);
#if (defined(WIN))
		strcat(dirPath, "\\");
#else
		strcat(dirPath, "/");
#endif
		char *signatures =	recurseExtractSignatures(f, dirPath, basePath);
		if (signatures && *signatures) {
			size_t sigLen = (size_t)strlen(signatures);
			buffer = realloc(buffer, bufferLen + sigLen);
			memmove(buffer + bufferLen -1L, signatures, sigLen + 1);
			bufferLen += sigLen;
			free(signatures);
		}
	}
	return buffer;
}

/*************************************************************************
 *
 *************************************************************************/
char *extractSignatures(const void *descriptor, const char *basePath) {
	char *signatures = recurseExtractSignatures(ezxml_child((ezxml_t)descriptor, "package"), "", basePath);
	return signatures;
}

/*************************************************************************
 *
 *************************************************************************/
static char *getDescriptorFileSignature(const char *signatures, const char *relPath) {
	if (!signatures)
		return NULL;

	char *entry = (char *)signatures;
	char *eol;
	while ((eol = strchr(entry, '\n'))) {
		*eol = 0;
		char *eop = strrchr(entry, '=');
		char *signature = NULL;
		if (eop) {
			*eop = 0;
			if (strcmp(relPath, entry) == 0) {
				signature = strdup(eop+1);
			}
			*eop = '=';
		}
		*eol = '\n';
		if (signature)
			return signature;
		entry = eol + 1;
	}
	return NULL;
}

/*************************************************************************
 *
 *************************************************************************/
int fileChanged(const char *relPath, const char *signatures, const char *oldSignatures) {
	char *signature = getDescriptorFileSignature(signatures, relPath);
	char *oldSignature = getDescriptorFileSignature(oldSignatures, relPath);
	int retVal = (!signature || !oldSignature || strcmp(signature, oldSignature));
	if (signature)
		free(signature);
	if (oldSignature)
		free(oldSignature);
	return retVal;
}

/*************************************************************************
 *
 *************************************************************************/
void descriptor_permissions(const char *deployDir, const void *descriptor, const void *deploymentDescriptor) {
#if !defined(WIN)
	ezxml_t desc = ezxml_child((ezxml_t)descriptor, "package");
	const char *fileOwner = ezxml_attr(desc, "file_owner");
	const char *fileGroup = ezxml_attr(desc, "file_group");
	const char *fileMode = ezxml_attr(desc, "file_mode");
	const char *dirMode = ezxml_attr(desc, "dir_mode");
	FILE_PERM permissions;
	getPermissions(&permissions, fileOwner, fileGroup, fileMode, dirMode);
	if (deploymentDescriptor && (permissions.uid == -1 || permissions.gid == -1 || permissions.fMode == 0 || permissions.dMode == 0)) {
		fileOwner = ezxml_attr((ezxml_t)deploymentDescriptor, "file_owner");
		fileGroup = ezxml_attr((ezxml_t)deploymentDescriptor, "file_group");
		fileMode = ezxml_attr((ezxml_t)deploymentDescriptor, "file_mode");
		dirMode = ezxml_attr((ezxml_t)deploymentDescriptor, "dir_mode");
		FILE_PERM deploymentPermissions;
		getPermissions(&deploymentPermissions, fileOwner, fileGroup, fileMode, dirMode);
		if (permissions.uid == -1)
			permissions.uid = deploymentPermissions.uid;
		if (permissions.gid == -1)
			permissions.gid = deploymentPermissions.gid;
		if (permissions.fMode == 0)
			permissions.fMode = deploymentPermissions.fMode;
		if (permissions.dMode == -1)
			permissions.dMode = deploymentPermissions.dMode;
	}
    if (permissions.uid == -1) {
        char * sudoUid = getenv("SUDO_UID");
        if (sudoUid)
            permissions.uid = atoi(sudoUid);
    }
    if (permissions.gid == -1) {
        char * sudoGid = getenv("SUDO_GID");
        if (sudoGid)
            permissions.gid = atoi(sudoGid);
    }
	recurseFiles(desc, deployDir, "", &permissions, PERMISSIONS, 0);
#endif
}

