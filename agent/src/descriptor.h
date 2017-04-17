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

#ifndef DESCRIPTOR_H_
#define DESCRIPTOR_H_

extern void debugPrintDescriptor(const void *descriptor);
extern void *checkPackageIntegrity(const char *kwateeDir, const char *deployment, const char *package, char *signature, int noIntegrityCheck, int exitOnError);
extern void freeDescriptor(const void *descriptor);
extern void descriptor_print(const char *kwateeDir, const void *descriptor);
extern void deleteFiles(const void *descriptor);

extern char* getDeployInDir(const void *descriptor);
extern char *getDeploymentName(const void *descriptor);
extern void deleteCacheFile(const char *kwateeDir, const void *descriptor);

extern void *getDeploymentDescriptorByFile(const char *deploymentFile);
extern void *getDeploymentDescriptor(const char *kwateeDir, const char *deployment);
extern void *getPackageDescriptor(const char *kwateeDir, const char *deployment, const char *package);
extern void removeObsoleteFiles(const char *kwateeDir, const char *deployment, const char *package, void *oldDescriptor);

extern char *getDescriptorAction(const void *descriptor, const char *action);

extern char *getDeploymentArtifacts(const void *descriptor);

extern char *extractSignatures(const void *descriptor, const char *basePath);
extern int fileChanged(const char *relPath, const char *signatures, const char *oldSignatures);

extern void descriptor_permissions(const char *kwateeDir, const void *descriptor, const void *deploymentDescriptor);

#define DEPLOYMENT_EXTENSION ".deployment"
#define ARTIFACT_EXTENSION ".artifact"

#endif /* DESCRIPTOR_H_ */

