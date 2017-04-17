
#include "utils.h"
#include "descriptor.h"
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/stat.h>
#if !defined(WIN)
#include <sys/wait.h>
#endif
#include "kwateevars.h"
#include "kwateeerrors.h"
#include "untar.h"


/*************************************************************************
 *
 *************************************************************************/
void usage() {
	printf("Usage:\n");
	printf("   kwagent deploy -s server [-a artifact] [-f archiveFileOrDir] [-k kwateeDir]\n");
	printf("   kwagent undeploy -d deployment [-a artifact] [-k kwateeDir] [--noIntegrityCheck true]\n");
	printf("   kwagent check-integrity -d deployment -a artifact [--signature signature] [-k kwateeDir]\n");
	printf("   kwagent get-descriptor -d deployment -a artifact [-k kwateeDir]\n");
	printf("   kwagent --version\n");
	exit(22);
}

/*************************************************************************
 *
 *************************************************************************/
static char *extractKwateeDir(char *commandLine) {
	char *s = strrchr(commandLine, '\\');
	if (!s)
		s = strrchr(commandLine, '/');
	if (s == NULL)
		return ".";

	*s = 0;
	return commandLine;
}

/*************************************************************************
 *
 *************************************************************************/
static int doScript(const char *action, const char *actionParams, const void *descriptor, int ignoreError) {
	int exitCode = 0;
	char *script = getDescriptorAction(descriptor, action);
	if (script && *script) {
		char *runInDir = getDeployInDir(descriptor);
		char *buffer = malloc(8000);
        sprintf(buffer, "cd \"%s\" && %s %s", runInDir, script, actionParams ? actionParams : "");
		int exitCode = system(buffer);
        free(buffer);
		if (exitCode) {
			exitCode = WEXITSTATUS(exitCode);
			if (strncmp(action, "exe_", 4) == 0) {
				switch(exitCode) {
					case 243: exitWithKwateeCode("running", NULL, NULL); break;
					case 244: exitWithKwateeCode("was_running", NULL, NULL); break;
					case 245: exitWithKwateeCode("stopped", NULL, NULL); break;
					case 246: exitWithKwateeCode("was_stopped", NULL, NULL); break;
				}
			}
			if (ignoreError) {
				printf("WARNING: %s action exited with code %d", action, exitCode);
			} else {
				sprintf(buffer, "%s action exited with code %d", action, exitCode);
				exitGenericError(buffer, NULL);
			}
		}
	}
	return exitCode;
}

static void deployOnePackage(const char *kwateeDir, void *deployment, void *descriptor, char *artifact, char *artifactFile, char *serverDir, int diff) {
    /*
	 * Keep a memory copy of the existing descriptor
	 */
	void *oldDescriptor = checkPackageIntegrity(kwateeDir, deployment, artifact, NULL, 0, 0);
	void *packageDescriptor = getPackageDescriptor(serverDir, artifact, artifact);
	char *deployDir = getDeployInDir(packageDescriptor);
	char *oldSignatures = diff ? extractSignatures(oldDescriptor, deployDir) : NULL;
	char *signatures = diff ? extractSignatures(packageDescriptor, NULL) : NULL;
	
	printf("Installing %s in %s\n", artifact, deployDir);

	/*
	 * Execute pre-deploy script
	 */
	 doScript("pre_deploy", 0L, packageDescriptor, 0);
	
	/*
	 * Expand artifact archive into target deployDir
	 */
	extractFiles(artifactFile, deployDir, NULL, 0, signatures, oldSignatures);
	/*
	 * Copy overlays to target deployDir
	 */
	char *artifactTmpDir = concatenatePath(serverDir, artifact);
	char *artifactOverlaysDir = concatenatePath(artifactTmpDir, "overlays");
	copyFiles(artifactOverlaysDir, deployDir, NULL, 0, signatures, oldSignatures);

	/*
	 * Copy package descriptor file to kwateeDir
	 */
    char *deploymentDir = concatenatePath(kwateeDir, deployment);
	char *srcDescriptorPath = concatenatePath(artifactTmpDir, concatenate(artifact, ARTIFACT_EXTENSION));
	char *dstDescriptorPath = concatenatePath(deploymentDir, concatenate(artifact, ARTIFACT_EXTENSION));
	linkorcopy(srcDescriptorPath, dstDescriptorPath, 0, 0);

	char *artifactTemplateDir = concatenatePath(artifactTmpDir, "templates");
	char *kwateeTemplateDir = concatenatePath(concatenatePath(deploymentDir, "templates"), artifact);
	copyFiles(artifactTemplateDir, kwateeTemplateDir, NULL, 1, NULL, NULL);

	descriptor_permissions(deployDir, packageDescriptor, descriptor);
	/*
	 * Execute post-deploy script
	 */
	 doScript("post_deploy", 0L, packageDescriptor, 0);

	/*
	 * Remove extra files remaining from an earlier installation
	 */
	if (oldDescriptor) {
		removeObsoleteFiles(kwateeDir, deployment, artifact, oldDescriptor);
		freeDescriptor(oldDescriptor);
	}
	packageDescriptor = checkPackageIntegrity(kwateeDir, deployment, artifact, NULL, 0, 1);
	freeDescriptor(packageDescriptor);
	if (signatures)
		free(signatures);
	if (oldSignatures)
		free(oldSignatures);
}

/*************************************************************************
 *
 *************************************************************************/
static void deploy(const char *archiveDir, const char *kwateeDir, const char *server, const char *artifactName, int diff) {

	char *serverDir = concatenatePath(archiveDir, concatenate(server, ".server"));
	if (!fileExists(serverDir, 1)) {
		printf("Possible servers are: ");
		listDirsWithExtension(archiveDir, ".server");
		exitGenericError("No configuration for server %s", server);
	}

	/*
	 * Retrieve deployment descriptor (installer.deployment) and packages to install
	 */
	char *descriptorFile = concatenatePath(serverDir, "DEPLOYMENT_DESCRIPTOR");
	void *descriptor = getDeploymentDescriptorByFile(descriptorFile);
	void *deployment = getDeploymentName(descriptor);

	/*
	 * Copy deployment descriptor file to kwateeDir
	 */
	char *dstDescriptorPath = concatenatePath(concatenatePath(kwateeDir, deployment), concatenate(deployment, DEPLOYMENT_EXTENSION));
    linkorcopy(descriptorFile, dstDescriptorPath, 0, 0);

	/*
	 * Deploy each artifact in deployment for that server
	 */
	if (!artifactName)
		doScript("pre_setup", 0L, descriptor, 0);
	char *artifacts = getDeploymentArtifacts(descriptor); 
	char *artifact;
	for (artifact = artifacts; *artifact; ) {
		char *version = artifact + strlen(artifact)+1;
		if (!artifactName || strcmp(artifactName, artifact) == 0) {
			char artifactVersion[255];
			sprintf(artifactVersion, "%s[%s]%s", artifact, version, ARTIFACT_EXTENSION);
			char *artifactFile = concatenatePath(archiveDir, artifactVersion);
			deployOnePackage(kwateeDir, deployment, descriptor, artifact, artifactFile, serverDir, diff);
		}
		artifact = version + strlen(version)+1;
	}
	if (!artifactName)
		doScript("post_setup", 0L, descriptor, 0);
}

/*************************************************************************
 *
 *************************************************************************/
static void update(const char *kwateeDir, const char *deployment) {
	char *deploymentDir = concatenatePath(kwateeDir, deployment);
	char *descriptorFile = concatenatePath(deploymentDir, concatenate(deployment, DEPLOYMENT_EXTENSION));
	void *descriptor = getDeploymentDescriptorByFile(descriptorFile);

	char *variablesFile = concatenatePath(deploymentDir, "variables.xml");
	char *kwateeTemplateDir = concatenatePath(deploymentDir, "templates");
	char *artifacts = getDeploymentArtifacts(descriptor); 
	char *artifact;
	for (artifact = artifacts; *artifact; ) {
		void *packageDescriptor = checkPackageIntegrity(kwateeDir, deployment, artifact, NULL, 0, 1);
		char *deployInDir = getDeployInDir(packageDescriptor);
		char *artifactTemplates = concatenatePath(kwateeTemplateDir, artifact);
		instantiateAllFiles(artifactTemplates, deployInDir, variablesFile, artifact);
		free(artifactTemplates);
		artifact += strlen(artifact)+1;
		artifact += strlen(artifact)+1;
	}
}

/*************************************************************************
 *
 *************************************************************************/
static void unDeploy(const char *kwateeDir, const char *deployment, const char *artifactName, int noIntegrityCheck, int force) {
	char *descriptorFile = concatenatePath(concatenatePath(kwateeDir, deployment), concatenate(deployment, DEPLOYMENT_EXTENSION));
	void *descriptor = getDeploymentDescriptorByFile(descriptorFile);

	/*
	 * Undeploy each artifact in deployment
	 */
	if (!artifactName)
		doScript("pre_cleanup", 0L, descriptor, force);
	char *artifacts = getDeploymentArtifacts(descriptor); 
	char *artifact;
	for (artifact = artifacts; *artifact; ) {
		if (!artifactName || strcmp(artifactName, artifact) == 0) {
			if (!force) {
				void *packageDescriptor = checkPackageIntegrity(kwateeDir, deployment, artifact, NULL, noIntegrityCheck, 1);
				doScript("pre_undeploy", 0L, packageDescriptor, force);
				chdir(kwateeDir);
				deleteFiles(packageDescriptor);
				doScript("post_undeploy", 0L, packageDescriptor, force);
				char *deployInDir = getDeployInDir(packageDescriptor);
				if (rmdir(deployInDir) == 0)
					printf("[-] %s/\n", deployInDir);
				else
					printf("WARNING: %s/ could not be deleted (is it empty?)\n", deployInDir);
				deleteCacheFile(kwateeDir, packageDescriptor);
				freeDescriptor(packageDescriptor);
			}
			char *packageDescriptorFile = concatenatePath(concatenatePath(kwateeDir, deployment), concatenate(artifact, ARTIFACT_EXTENSION));
			if (unlink(packageDescriptorFile) != 0) {
				printf("WARNING: failed to remove descriptor %s", packageDescriptorFile);
			}
		}
		artifact += strlen(artifact)+1;
		artifact += strlen(artifact)+1;
	}
	if (!artifactName) {
		doScript("post_cleanup", 0L, descriptor, force);
		if (unlink(descriptorFile) != 0)
			printf("WARNING: failed to remove descriptor %s", descriptorFile);
	}
}

static void updateDescriptor(const char *kwateeDir, const char *deployment, const char *artifact, const char *descriptorFileName) {
	char *descriptorFile = concatenatePath(kwateeDir, descriptorFileName);
	char *destinationFile = concatenatePath(kwateeDir, deployment);
	if (artifact)
		destinationFile = concatenatePath(destinationFile, concatenate(artifact, ARTIFACT_EXTENSION));
	else
		destinationFile = concatenatePath(destinationFile, concatenate(deployment, DEPLOYMENT_EXTENSION));

	createpath(destinationFile);
	if (rename(descriptorFile, destinationFile))
		exitGenericError("Failed to update descriptor %s", artifact?artifact:deployment);
}

static char *getOption(const char *option, int argc, char *argv[]) {
	int i = 2;
	while (i < argc) {
		char *arg = argv[i];
		int isOption = arg[0] == '-' && (i != (argc-1)) && strlen(arg) > 1;
		if (!isOption)
			i ++;
		else if (strcmp(option, arg+1))
			i += 2;
		else
			return argv[i+1];
	}
	return NULL;
}

/*************************************************************************
 *
 *************************************************************************/
int main(int argc, char *argv[]) {
	if (argc >= 2) {
#if (defined(WIN))
		// patch command line interpretation bug which causes trailing '\' in command line path to escape following "
		int i;
		for (i = 1; i < argc; i ++) {
			char *s = strchr(argv[i], '"');
			if (s) {
				s[0] = '\\';
				s[1] = 0;
			}
		}
#endif

        init_debug_mode();

        char *kwateeDir = getOption("k", argc, argv);
		if (!kwateeDir)
			kwateeDir = extractKwateeDir(argv[0]);
        debug_print("kwateeDir='%s'\n", kwateeDir);
		char *command = argv[1];
	
		if (strcmp(command, "--version") == 0) {
			exitNoError(AGENT_VERSION);
		} else if (strcmp(command, "get-descriptor") == 0) {
			char *deployment = getOption("d", argc, argv);
			char *artifact = getOption("a", argc, argv);
			if (deployment && artifact) {
				void *descriptor = getPackageDescriptor(kwateeDir, deployment, artifact);
				if (!descriptor)
					exitNoDescriptorError(NULL, NULL);
				descriptor_print(kwateeDir, descriptor);
				exitNoError(NULL);
			}
		} else if (strcmp(command, "check-integrity") == 0) {
			char *deployment = getOption("d", argc, argv);
			char *artifact = getOption("a", argc, argv);
			if (deployment && artifact) {
				char *signature = getOption("-signature", argc, argv);
				checkPackageIntegrity(kwateeDir, deployment, artifact, signature, 0, 1);
				exitNoError(NULL);
			}
		} else if (strcmp(command, "action") == 0) {
			char *id = getOption("-id", argc, argv);
			char *deployment = getOption("d", argc, argv);
			if (id && deployment) {
				char *artifact = getOption("a", argc, argv);
				void *descriptor;
				if (!artifact)
					descriptor = getDeploymentDescriptor(kwateeDir, deployment);
                else {
                    char *noIntegrityCheck = getOption("-noIntegrityCheck", argc, argv);
                    descriptor = checkPackageIntegrity(kwateeDir, deployment, artifact, NULL, noIntegrityCheck && strcmp("true", noIntegrityCheck) == 0, 1);
                }
                char *actionParams = getOption("-actionParams", argc, argv);
				doScript(id, actionParams, descriptor, 0);
				exitNoError(NULL);
			}
		} else if (strcmp(command, "undeploy") == 0) {
			char *deployment = getOption("d", argc, argv);
			if (deployment) {
				char *artifact = getOption("a", argc, argv);
                char *noIntegrityCheck = getOption("-noIntegrityCheck", argc, argv);
				char *force = getOption("-force", argc, argv);
				unDeploy(kwateeDir, deployment, artifact, noIntegrityCheck && strcmp("true", noIntegrityCheck) == 0, force && strcmp("true", force) == 0);
				exitNoError(NULL);
			}
		} else if (strcmp(command, "update-descriptor") == 0)  {
			char *descriptorFileName = getOption("f", argc, argv);
			char *deployment = getOption("d", argc, argv);
			if (descriptorFileName && deployment) {
				char *artifact = getOption("a", argc, argv);
				updateDescriptor(kwateeDir, deployment, artifact, descriptorFileName);
				exitNoError(NULL);
			}
		} else if (strcmp(command, "deploy") == 0)  {
			char *server = getOption("s", argc, argv);
            debug_print("server=%s\n", server ? server : "<none>");
			char *archiveFile = getOption("f", argc, argv);
			if (!archiveFile)
				archiveFile = kwateeDir;
			if (server) {
				char *artifact = getOption("a", argc, argv);
				char *tmpDir = NULL;
				char *archiveDir = NULL;
                debug_print("artifact=%s\n", artifact ? artifact : "<none>");
				char *diff = getOption("diff", argc, argv);
				if (diff && strcmp("true", diff))
					diff = NULL;
                if (fileExists(archiveFile, 1))
					archiveDir = archiveFile;
                else {
					tmpDir = createTmpDir(kwateeDir);
                    debug_print("extract files to tmpDir='%s'\n", tmpDir);
					extractFiles(archiveFile, tmpDir, NULL, 1, NULL, NULL);
                    debug_print("extract done, ready to unlink\n", tmpDir);
					unlink(archiveFile);
					archiveDir = tmpDir;
				}
                debug_print("ready to deploy in '%s'\n", archiveDir);
				deploy(archiveDir, kwateeDir, server, artifact, diff != NULL);
                if (tmpDir) {
                    debug_print("Deploy removing dir '%s'\n", archiveDir);
					recursiveRmdir(tmpDir);
                }
				exitNoError(NULL);
			}
		} else if (strcmp(command, "update") == 0) {
			char *deployment = getOption("d", argc, argv);
			if (deployment) {
				update(kwateeDir, deployment);
				exitNoError(NULL);
			}
		}
	}
	usage();
	exit(79);
}
