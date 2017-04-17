/*
 ${kwatee_copyright}
 */

"use strict";

angular.module("kwateeApi", ["ngCookies"])
.config(function($httpProvider) {
	delete $httpProvider.defaults.headers.common['X-Requested-With'];
	$httpProvider.defaults.withCredentials = true;
	$httpProvider.defaults.useXDomain = true;
	$httpProvider.interceptors.push('APIInterceptor');
})
.factory("kwateeJsonService", ["$http", "$q", "$cookies", function($http, $q, $cookies) {
	var service = {};
	var ROOT_URL = (window.KWATEE_API_URL || '') + 'api';

	service.login = function(userName, password) {
		var deferred = $q.defer();
		$http.post(ROOT_URL+"/authenticate/?username="+userName, password)
		.then(function(token) {
			service.getContext().then(function(response) {
				deferred.resolve(response);
			}, function(rejection) {
				deferred.reject(rejection);
			});
		}, function(rejection) {
			deferred.reject(rejection);
		});
        return deferred.promise;
	};

	service.logout = function() {
		$cookies.remove('api-token', {path: '/'});
	};

	service.getContext = function() {
        return $http.get(ROOT_URL + "/info/context.json");
	};

	service.getArtifacts = function() {
		return $http.get(ROOT_URL + "/artifacts.json");
	};

	service.createArtifact = function(artifactName, artifactOptions) {
		var json = artifactOptions ? JSON.stringify(artifactOptions) : null;
		return $http.post(ROOT_URL + "/artifacts/"+artifactName, json);
	};

	service.uploadArtifacts = function(artifactsFile) {
		var url = "/artifacts/";
		if ((typeof artifactsFile) == "string") {
			return $http.post(ROOT_URL + url, null, {params : {url: artifactsFile}});
		} else {
			var fd = new FormData();
			fd.append("file", artifactsFile);
			var config = {transformRequest: angular.identity, headers: {"Content-Type": undefined}};
			return $http.post(ROOT_URL + uri, fd, config);
		}
	};
	
	service.getArtifact = function(artifactName) {
		return $http.get(ROOT_URL + "/artifacts/"+artifactName+".json");
	};
	
	service.updateArtifact = function(artifactName, artifactOptions) {
		return $http.put(ROOT_URL + "/artifacts/"+artifactName, JSON.stringify(artifactOptions));
	};
	
	service.deleteArtifact = function(artifactName) {
		return $http.delete(ROOT_URL + "/artifacts/"+artifactName);
	};
	
	service.getVersion = function(artifactName, versionName) {
		return $http.get(ROOT_URL + "/artifacts/"+artifactName+"/"+versionName+".json");
	};

	service.createVersion = function(artifactName, versionName, versionOptions) {
		var json = versionOptions ? JSON.stringify(versionOptions) : null;
		return $http.post(ROOT_URL + "/artifacts/"+artifactName+"/"+versionName, json);
	};

	service.duplicateVersion = function(artifactName, versionName, duplicateFrom, versionOptions) {
		var json = versionOptions ? JSON.stringify(versionOptions) : null;
		return $http.post(ROOT_URL + "/artifacts/" + artifactName+"/"+versionName, json, {params : {duplicateFrom: duplicateFrom}});
	};

	service.updateVersion = function(artifactName, versionName, versionOptions) {
		return $http.put(ROOT_URL + "/artifacts/"+artifactName+"/"+versionName, JSON.stringify(versionOptions));
	};

	service.deleteVersion = function(artifactName, versionName) {
		return $http.delete(ROOT_URL + "/artifacts/"+artifactName+"/"+versionName);
	};

	service.uploadPackage = function(artifactName, versionName, packageFile, deleteOverlays) {
		var url = "/artifacts/" + artifactName + "/" + versionName + "/package";
		if (packageFile) {
			if ((typeof packageFile) == "string") {
				var params = {url: packageFile, deleteOverlays: deleteOverlays};
				return $http.post(ROOT_URL + url, null, {params : params});
			} else {
				var fd = new FormData();
				fd.append("file", packageFile);
				fd.append("deleteOverlays", deleteOverlays);
				var config = { transformRequest: angular.identity, headers: {"Content-Type": undefined}};
				return $http.post(ROOT_URL + url, fd, config);
			}
		} else {
			return $http.post(ROOT_URL + url, "");
		}
	};

	service.deletePackage = function(artifactName, versionName) {
		return $http.delete(ROOT_URL + "/artifacts/" + artifactName + "/" + versionName + "/package");
	};

	service.getPackageFiles = function(artifactName, versionName, path) {
		var url = "/artifacts/"+artifactName+"/"+versionName+"/package/files.json";
		if (path)
			return $http.get(ROOT_URL + url, {params: {path: path}});
		else
			return $http.get(ROOT_URL + url);
	};

	service.getSpecialFiles = function(artifactName, versionName) {
		return $http.get(ROOT_URL + "/artifacts/"+artifactName+"/"+versionName+"/package/specialFiles.json");
	};

	service.updateVersionFileProperties = function(artifactName, versionName, path, properties) {
		var url = "/artifacts/"+artifactName+"/"+versionName+"/package/file";
		return $http.put(ROOT_URL + url, JSON.stringify(properties), {params : {path: path}});
	};

	service.getVersionVariables = function(artifactName, versionName) {
		return $http.get(ROOT_URL + "/artifacts/"+artifactName+"/"+versionName+"/variables.json");
	};
	
	service.updateVersionVariables = function(artifactName, versionName, variables) {
		return $http.put(ROOT_URL + "/artifacts/"+artifactName+"/"+versionName+"/variables", JSON.stringify(variables));
	};
	
	service.updateVersionVariablePrefix = function(artifactName, versionName, variablePrefix) {
		return $http.put(ROOT_URL + "/artifacts/"+artifactName+"/"+versionName+"/variablePrefix", null, {params : {char: variablePrefix}});
	};

	service.exportVersionVariablesUrl = function(artifactName, versionName) {
		return ROOT_URL + "/artifacts/"+artifactName+"/"+versionName+"/variables.export";
	};
	
	service.importVersionVariables = function(artifactName, versionName, file) {
		var url = "/artifacts/" + artifactName + "/" + versionName + "/variables.import";
		if (file) {
			if ((typeof file) == "string") {
				var params = {url: file};
				return $http.post(ROOT_URL + url, null, {params : params});
			} else {
				var fd = new FormData();
				fd.append("file", file);
				var config = { transformRequest: angular.identity, headers: {"Content-Type": undefined}};
				return $http.post(ROOT_URL + url, fd, config);
			}
		}
	};

	service.uploadVersionOverlay = function(artifactName, versionName, baseDir, overlayFile) {
		var url = "/artifacts/" + artifactName + "/" + versionName + "/package/overlay";
		if ((typeof overlayFile) == "string") {
			return $http.post(ROOT_URL + url, null, {params : {url: overlayFile, path: baseDir}});
		} else {
			var fd = new FormData();
			fd.append("file", overlayFile);
			fd.append("path", baseDir);
			var config = { transformRequest: angular.identity, headers: {"Content-Type": undefined}};
			return $http.post(ROOT_URL + url, fd, config);
		}
	};
	
	service.deleteVersionOverlay = function(artifactName, versionName, overlayPath) {
		return $http.delete(ROOT_URL + "/artifacts/"+artifactName+"/"+versionName+"/package/overlay", {params : {path: overlayPath}});
	};

	service.getServers = function() {
		return $http.get(ROOT_URL + "/servers.json");
	};

	service.updateServer = function(serverName, serverOptions) {
		return $http.put(ROOT_URL + "/servers/"+serverName, JSON.stringify(serverOptions));
	};

	service.createServer = function(serverName, serverOptions) {
		var json = serverOptions ? JSON.stringify(serverOptions) : null;
		return $http.post(ROOT_URL + "/servers/"+serverName, json);
	};

	service.duplicateServer = function(serverName, duplicateFrom, serverOptions) {
		var json = serverOptions ? JSON.stringify(serverOptions) : null;
		return $http.post(ROOT_URL + "/servers/"+serverName, json, {params : {duplicateFrom: duplicateFrom}});
	};

	service.deleteServer = function(serverName) {
		return $http.delete(ROOT_URL + "/servers/"+serverName);
	};

	service.getServer = function(serverName) {
		return $http.get(ROOT_URL + "/servers/"+serverName+".json");
	};

	service.getPlatforms = function() {
		return $http.get(ROOT_URL + "/info/platforms.json");
	};

	service.getConduitTypes = function() {
		return $http.get(ROOT_URL + "/info/conduitTypes.json");
	};

	service.getPoolTypes = function() {
		return $http.get(ROOT_URL + "/info/serverPoolTypes.json");
	};

	service.testConnection = function(serverName, serverOptions) {
		var json = serverOptions ? JSON.stringify(serverOptions) : null;
		return $http.post(ROOT_URL + "/servers/"+serverName + "/testConnection", json);
	};

	service.getEnvironments = function() {
		return $http.get(ROOT_URL + "/environments.json");
	};

	service.createEnvironment = function(environmentName, environmentOptions) {
		var json = environmentOptions ? JSON.stringify(environmentOptions) : null;
		return $http.post(ROOT_URL + "/environments/"+environmentName, json);
	};

	service.duplicateEnvironment = function(environmentName, duplicateFrom, environmentOptions) {
		var json = environmentOptions ? JSON.stringify(environmentOptions) : null;
		return $http.post(ROOT_URL + "/environments/"+environmentName, json, {params : {duplicateFrom: +duplicateFrom}});
	};

	service.getEnvironment = function(environmentName) {
		return $http.get(ROOT_URL + "/environments/"+environmentName+".json");
	};
	
	service.updateEnvironment = function(environmentName, environmentOptions) {
		return $http.put(ROOT_URL + "/environments/"+environmentName, JSON.stringify(environmentOptions));
	};

	service.deleteEnvironment = function(environmentName) {
		return $http.delete(ROOT_URL + "/environments/"+environmentName);
	}

	service.getRelease = function(environmentName, releaseName) {
		return $http.get(ROOT_URL + "/environments/"+environmentName+"/"+releaseName+".json");
	};

	service.getReleaseErrors = function(environmentName, releaseName) {
		return $http.get(ROOT_URL + "/environments/"+environmentName+"/"+releaseName+"/errors.json");
	};

	service.getReleaseEffectiveArtifacts = function(environmentName, releaseName) {
		return $http.get(ROOT_URL + "/environments/"+environmentName+"/"+releaseName+"/artifacts.json");
	};
	
	service.updateRelease = function(environmentName, releaseName, releaseOptions) {
		return $http.put(ROOT_URL + "/environments/"+environmentName+"/"+releaseName, JSON.stringify(releaseOptions));
	};

	service.deleteRelease = function(environmentName, releaseName) {
		return $http.delete(ROOT_URL + "/environments/"+environmentName+"/"+releaseName);
	};

	service.tagRelease = function(environmentName, releaseName, releaseOptions) {
		var json = releaseOptions ? JSON.stringify(releaseOptions) : null;
		return $http.post(ROOT_URL + "/environments/"+environmentName+"/"+releaseName, json);
	};

	service.reeditRelease = function(environmentName, releaseName) {
		return $http.post(ROOT_URL + "/environments/"+environmentName+"/"+releaseName+"/reedit");
	};

	service.getReleasePackageFiles = function(environmentName, releaseName, artifactName, serverName, path) {
		var url = "/environments/"+environmentName+"/"+releaseName+"/package/files.json";
		var params = {artifactName: artifactName};
		if (serverName)
			params.serverName = serverName;
		if (path)
			params.path = path;
		return $http.get(ROOT_URL + url, {params: params});
	};

	service.getReleaseSpecialFiles = function(environmentName, releaseName, artifactName, serverName) {
		var url = "/environments/"+environmentName+"/"+releaseName+"/package/specialFiles.json";
		var params = {artifactName: artifactName};
		if (serverName)
			params.serverName = serverName;
		return $http.get(ROOT_URL + url, {params: params});
	};

	service.updateSnapshotFileProperties = function(environmentName, artifactName, serverName, path, properties) {
		var url = "/environments/"+environmentName+"/snapshot/package/file.json";
		var params = {artifactName: artifactName, path: path};
		if (serverName)
			params.serverName = serverName;
		return $http.put(ROOT_URL + url, JSON.stringify(properties), {params : params});
	};

	service.getReleaseVariables = function(environmentName, releaseName) {
		return $http.get(ROOT_URL + "/environments/"+environmentName+"/"+releaseName+"/variables.json");
	};

	service.updateSnapshotVariables = function(environmentName, variables) {
		return $http.put(ROOT_URL + "/environments/"+environmentName+"/snapshot/variables", JSON.stringify(variables));
	};

	service.exportReleaseVariablesUrl = function(environmentName, releaseName) {
		return ROOT_URL + "/environments/"+environmentName+"/"+releaseName+"/variables.export";
	};
	
	service.importReleaseVariables = function(environmentName, file) {
		var url = "/environments/" + environmentName + "/snapshot/variables.import";
		if (file) {
			if ((typeof file) == "string") {
				var params = {url: file};
				return $http.post(ROOT_URL + url, null, {params : params});
			} else {
				var fd = new FormData();
				fd.append("file", file);
				var config = { transformRequest: angular.identity, headers: {"Content-Type": undefined}};
				return $http.post(ROOT_URL + url, fd, config);
			}
		}
	};

	service.uploadSnapshotOverlay = function(environmentName, artifactName, serverName, baseDir, overlayFile) {
		var url = "/environments/" + environmentName + "/snapshot/package/overlay";
		if ((typeof overlayFile) == "string") {
			var params = {artifactName: artifactName, url: overlayFile, path: baseDir};
			if (serverName)
				params.serverName = serverName;
			return $http.post(ROOT_URL + url, null, {params : params});
		} else {
			var fd = new FormData();
			fd.append("artifactName", artifactName);
			if (serverName)
				fd.append("serverName", serverName);
			fd.append("file", overlayFile);
			fd.append("path", baseDir);
			var config = { transformRequest: angular.identity, headers: {"Content-Type": undefined}};
			return $http.post(ROOT_URL + url, fd, config);
		}
	};

	service.deleteSnapshotOverlay = function(environmentName, artifactName, serverName, overlayPath) {
		var url = "/environments/"+environmentName+"/snapshot/package/overlay";
		var params = {artifactName: artifactName};
		if (serverName)
			params.serverName = serverName;
		params.path = overlayPath;
		return $http.delete(ROOT_URL + url, {params : params});
	};

	service.getDeployableEnvironments = function() {
		return $http.get(ROOT_URL + "/deployments.json");
	};

	service.getDeployment = function(environmentName, releaseName) {
		return $http.get(ROOT_URL + "/deployments/"+environmentName+"/"+releaseName+".json");
	};

	service.getLightweightInstallerUrl = function(environmentName, releaseName) {
		return ROOT_URL+"/deployments/"+environmentName+"/"+releaseName+"/installer_lightweight.tar.gz";
	};

	service.getInstallerUrl = function(environmentName, releaseName) {
		return ROOT_URL+"/deployments/"+environmentName+"/"+releaseName+"/installer_cli.tar.gz";
	};

	service.manage = function(environmentName, releaseName, operation, targets, actionParams, skipIntegrityCheck) {
		var params = {};
		if (actionParams)
			params.actionParams = actionParams;
		if (skipIntegrityCheck)
			params.skipIntegrityCheck = true;
		return $http.post(ROOT_URL + "/deployments/"+environmentName+"/"+releaseName+"/"+operation, targets, {params : params});
	};

	service.getOngoingDeployment = function() {
		return $http.get(ROOT_URL + "/deployments/ongoing.json");
	};

	service.getDeploymentProgress = function(ref) {
		return $http.get(ROOT_URL + "/deployments/progress/status.json", {params: {ref: ref}});
	};
	
	service.getDeploymentProgressMessages = function(ref, artifactName, serverName) {
		var params = {ref: ref, serverName: serverName};
		if (artifactName)
			params.artifactName = artifactName;
		return $http.get(ROOT_URL + "/deployments/progress/messages.json", {params: params});
	};

	service.sendDeploymentCredentials = function(authRequest, sameForAllServers) {
		return $http.post(ROOT_URL + "/deployments/progress/credentials", JSON.stringify(authRequest), {params : {sameForAll : sameForAllServers}})
	};

	service.cancelDeployment = function(ref) {
		return $http.post(ROOT_URL + "/deployments/progress/cancel", null, {params : {ref: ref, dontClear: true}});
	};

	service.clearDeployment = function(ref) {
		return $http.post(ROOT_URL + "/deployments/progress/cancel", null, {params : {ref: ref}});
	};

	service.getUsers = function() {
		return $http.get(ROOT_URL + "/admin/users.json");
	};
	
	service.createUser = function(userName, userOptions) {
		var json = userOptions ? JSON.stringify(userOptions) : null;
		return $http.post(ROOT_URL + "/admin/users/"+userName, json);
	};
	
	service.getUser = function(userName) {
		return $http.get(ROOT_URL + "/admin/users/"+userName+".json");
	};
	
	service.updateUser = function(userName, userOptions) {
		return $http.put(ROOT_URL + "/admin/users/"+userName, JSON.stringify(userOptions));
	};

	service.deleteUser = function(userName) {
		return $http.delete(ROOT_URL + "/admin/users/"+userName);
	};

	service.getGlobalVariables = function() {
		return $http.get(ROOT_URL + "/admin/variables.json");
	};

	service.updateGlobalVariables = function(variables) {
		return $http.put(ROOT_URL + "/admin/variables", JSON.stringify(variables));
	};

	service.getParameters = function() {
		return $http.get(ROOT_URL + "/admin/parameters.json");
	};

	service.updateParameters = function(parameters) {
		return $http.put(ROOT_URL + "/admin/parameters", JSON.stringify(parameters));
	};

	service.getExportBackupUrl = function() {
		return ROOT_URL+"/admin/export";
	};

	service.getDBInfo = function() {
		return $http.get(ROOT_URL + "/db/info.json");
	};
	
	service.createDB = function(dbaPassword) {
		var data = { password: dbaPassword };
		return $http.post(ROOT_URL + "/db/create", JSON.stringify(data));
	};
	
	service.upgradeDB = function(dbaPassword) {
		var data = { password: dbaPassword };
		return $http.post(ROOT_URL + "/db/upgrade", JSON.stringify(data));
	};

	return service;
}]);
