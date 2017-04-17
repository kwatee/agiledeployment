"use strict";

/* Controllers */

var environmentsControllers = angular.module("environments.controllers", []);

//
// ENVIRONMENTS CONTROLLER
//
environmentsControllers.controller("environmentsController",
function($scope, $route, kwateeService) {
	
	$scope.newEnvironment = function() {
		$scope.ask("Create new environment", "Enter name", null)
		.result.then(function(name) {
			var options = { "description": new Date().toUTCString() };
			kwateeService.createEnvironment(name, options)
			.then(function() {
				$scope.go("/environments/" + name);
			});
		});
	}

	$scope.duplicateEnvironment = function(environmentName) {
		$scope.ask("Duplicate environment " + environmentName, "Enter name")
		.result.then(function(name) {
			var options = { "description": "cloned " + new Date().toUTCString() };
			kwateeService.duplicateEnvironment(name, environmentName, options)
			.then(function() {
				$scope.go("/environments/" + name);
			});
		});
	}

	$scope.deleteEnvironment = function(name) {
		$scope.confirm("Delete environment " + name + "?")
		.result.then(function() {
			kwateeService.deleteEnvironment(name)
			.then(function() {
				$route.reload();
			});
		});
	}
	
	kwateeService.getEnvironments().then(function (response) {
		$scope.environments = response.data;
	});
	
});

//
// ENVIRONMENT CONTROLLER
//
environmentsControllers.controller("environmentController",
function($scope, $route, $routeParams, $modal, $q, kwateeService) {
	
	$scope.x = function() {
		return $(arguments[0]);
	}
	
	$scope.$on("$locationChangeStart", $scope.changeLocation);
	var environment = null;
	var artifactList = [];
	var serverList = [];
    $scope.environment = { "name": $routeParams.param1 };
	$scope.deleteRelease = function(name) {
		if (!$scope.isDirty($scope.form))
			$scope.confirm("Delete release " + name + "?")
			.result.then(function() {
				kwateeService.deleteRelease($scope.environment.name, name)
				.then(function() {
					$route.reload();
				});
			});
	}
	
	$scope.save = function() {
		if ($scope.form.$pristine)
			return;
		kwateeService.updateEnvironment($scope.environment.name, $scope.environment)
		.then(function() {
			environment = angular.copy($scope.environment);
			$scope.form.$setPristine();
		});
	}
	$scope.$on('saveEvent', $scope.save);
	
	$scope.revert = function() {
		$scope.environment = angular.copy(environment);
		$scope.form.$setPristine();
	}

	var filterChoices = function(choices, type) {
		var current = $scope.environment[type+"s"];
		return choices.filter(function(choice) {
			if (!choice.disabled)
				return !current.find(function(c) {
					return c == choice.name;
				});
			return false;
		});
	}

	$scope.artifactInfo = function(artifact) {
		return artifactList.find(function(a) { return a.name === artifact; });
	}

	$scope.addArtifact = function() {
		$modal.open({
			templateUrl: "ui/environments/tmpl_envartifacts.html",
			controller: "selectorModalController",
			windowClass: "selectormodal",
			resolve: {
				"choices": function() { return filterChoices(artifactList, "artifact"); }
			}
		}).result.then(function(artifacts) {
			Array.prototype.push.apply($scope.environment.artifacts, artifacts);
			$scope.form.$setDirty();
		});
	}

	$scope.removeArtifact = function(artifact) {
		var idx = $scope.environment.artifacts.indexOf(artifact);
		$scope.environment.artifacts.splice(idx, 1);
		$scope.form.$setDirty();
	}

	$scope.moveArtifactUp = function(artifact) {
		var idx = $scope.environment.artifacts.indexOf(artifact);
		$scope.environment.artifacts[idx] = $scope.environment.artifacts[idx-1];
		$scope.environment.artifacts[idx-1] = artifact;
		$scope.form.$setDirty();
	}
	
	$scope.moveArtifactDown = function(artifact) {
		var idx = $scope.environment.artifacts.indexOf(artifact);
		$scope.environment.artifacts[idx] = $scope.environment.artifacts[idx+1];
		$scope.environment.artifacts[idx+1] = artifact;
		$scope.form.$setDirty();
	}

	$scope.enabledEnvironmentArtifact = function() {
		return function(artifact) { return !$scope.$prefs.hideDisabled || !$scope.artifactInfo(artifact).disabled; }
	};

	$scope.serverInfo = function(server) {
		return serverList.find(function(a) { return a.name === server; });
	};

	$scope.addServer = function() {
		$modal.open({
			templateUrl: "ui/environments/tmpl_envservers.html",
			controller: "selectorModalController",
			windowClass: "selectormodal",
			resolve: {
				"choices": function() { return filterChoices(serverList, "server"); }
			}
		}).result.then(function(servers) {
			Array.prototype.push.apply($scope.environment.servers, servers);
			$scope.form.$setDirty();
		});
	}

	$scope.removeServer = function(server) {
		var idx = $scope.environment.servers.indexOf(server);
		$scope.environment.servers.splice(idx, 1);
		$scope.form.$setDirty();
	}
	
	$scope.moveServerUp = function(server) {
		var idx = $scope.environment.servers.indexOf(server);
		$scope.environment.servers[idx] = $scope.environment.servers[idx-1];
		$scope.environment.servers[idx-1] = server;
		$scope.form.$setDirty();
	}
	
	$scope.moveServerDown = function(server) {
		var idx = $scope.environment.servers.indexOf(server);
		$scope.environment.servers[idx] = $scope.environment.servers[idx+1];
		$scope.environment.servers[idx+1] = server;
		$scope.form.$setDirty();
	}

	$scope.enabledEnvironmentServer = function() {
		return function(server) { return !$scope.$prefs.hideDisabled || !$scope.serverInfo(server).disabled; }
	};

	var batch = [];
	batch.push(kwateeService.getArtifacts());
	batch.push(kwateeService.getServers());
	batch.push(kwateeService.getEnvironment($scope.environment.name));
	$q.all(batch)
	.then(function(responses) {
		artifactList = responses[0].data;
		serverList = responses[1].data;
		environment = responses[2].data;
		if (!environment.servers)
			environment.servers = [];
		if (!environment.artifacts)
			environment.artifacts = [];
		if (!environment.releases)
			environment.releases = [];
		$scope.environment = angular.copy(environment);
	});
	
});

//
// SELECTOR (ARTIFACTS/SERVERS) MODAL CONTROLLER
//
environmentsControllers.controller("selectorModalController",
function($scope, $modalInstance, choices) {
	$scope.items = [];
	choices.forEach(function(choice) {
		$scope.items.push({"selected": false, "entry": choice});
	});
	
	$scope.ok = function() {
		var selectedChoices = [];
		$scope.items.forEach(function(item) {
			if (item.selected) 
				selectedChoices.push(item.entry.name);
		});
		if (selectedChoices.length)
			$modalInstance.close(selectedChoices);
		else
			$modalInstance.dismiss();
	}
	
});

//
// RELEASE CONTROLLER
//
environmentsControllers.controller("releaseController",
function($scope, $rootScope, $routeParams, $modal, $window, kwateeService) {
	
	$scope.serverStatus = {};
	if ($routeParams.param4 && $routeParams.param4 != "*") {
		$scope.serverStatus[$routeParams.param4] = true;
	}
	$scope.$on("$locationChangeStart", $scope.changeLocation);
	
	var release = null;
	var environment = null;
	$scope.environmentName = $routeParams.param1;
    $scope.release = {
    	"name": $routeParams.param2,
    	"defaultArtifacts": []
    };
    
	$scope.$on('variableEvent', function() {
		$scope.$apply(function () {
			$scope.go('/environments/'+$scope.environmentName+'/'+$scope.release.name+'/variables');
		});
	});
    
	$scope.save = function() {
		if ($scope.form.$pristine)
			return;
		kwateeService.updateRelease($scope.environmentName, $scope.release.name, $scope.release)
		.then(function(response) {
			release = angular.copy($scope.release);
			$scope.form.$setPristine();
			getReleaseErrors();
		});
	}
	$scope.$on('saveEvent', $scope.save);
	
	$scope.revert = function() {
		$scope.release = angular.copy(release);
		$scope.form.$setPristine();
	}
	
	$scope.editActions = function() {
		$modal.open({
			templateUrl : "ui/environments/tmpl_actions.html",
			controller : "deployActionsModalController",
			resolve : {
				"actions" : function() {
					return {
						"preSetupAction" : $scope.release.preSetupAction,
						"postSetupAction" : $scope.release.postSetupAction,
						"preCleanupAction" : $scope.release.preCleanupAction,
						"postCleanupAction" : $scope.release.postCleanupAction
					};
				},
				"readOnly" : function() { return $scope.readOnly || !$scope.release.editable; },
			}
		})
		.result.then(function(actions) {
			$scope.release.preSetupAction = actions.preSetupAction;
			$scope.release.postSetupAction = actions.postSetupAction;
			$scope.release.preCleanupAction = actions.preCleanupAction;
			$scope.release.postCleanupAction = actions.postCleanupAction;
			$scope.form.$setDirty();
		});
	}

	$scope.tagSnapshot = function() {
		if (!$scope.isDirty($scope.form))
			$scope.ask("Tag snapshot", "Enter name")
			.result.then(function(name) {
			var options = { "description": new Date().toUTCString() };
			kwateeService.tagRelease($scope.environmentName, name, options)
			.then(function() {
					$scope.go("/environments/" + $scope.environmentName + "/" + name);
				});
			});
	}

	$scope.reeditRelease = function() {
		if (!$scope.isDirty($scope.form))
			$scope.confirm("Overwrite snapshot configuration and reedit release?")
			.result.then(function() {
				kwateeService.reeditRelease($scope.environmentName, $scope.release.name)
				.then(function() {
						$scope.go("/environments/" + $scope.environmentName + "/snapshot");
					});
			});
	}

	$scope.downloadInstaller = function(lightWeight) {
		var url;
		url = kwateeService.getInstallerUrl($scope.environmentName, $scope.release.name);
		$window.open(url, '_blank');
	}

	$scope.editPermissions = function() {
		$modal.open({
			templateUrl : "ui/tmpl_permissions.html",
			controller : "relPermissionsModalController",
			windowClass : "smallmodal",
			resolve : {
				"name" : function() { return $scope.release.name; },
				"permissions" : function() { return $scope.release.permissions; },
				"originalPermissions" : function() { return null; },
				"dir" : function() { return true; },
				"readOnly" : function() { return $scope.readOnly || !$scope.release.editable; },
			}
		})
		.result.then(function(permissions) {
			if (permissions) {
				$scope.release.permissions = permissions;
				$scope.form.$setDirty();
			}
		});
	}

	$scope.inheritedVersion = function(artifact) {
		for (var i = 0; i < $scope.release.defaultArtifacts.length; i ++) {
			if ($scope.release.defaultArtifacts[i].artifact === artifact)
				return $scope.release.defaultArtifacts[i].version;
		}
		return null;
	}

	$scope.changeActiveVersion = function(artifactVersion, server) {
		$modal.open({
			templateUrl: "ui/environments/tmpl_activeversion.html",
			controller: "activeVersionModalController",
			windowClass: "defaultmodal",
			resolve: {
				"artifact": function() { return artifactVersion.artifact; },
				"version": function() { return artifactVersion.version; },
				"server": function() { return server; },
			}
		}).result.then(function(version) {
			artifactVersion.version = version;
			$scope.form.$setDirty();
		});
	}
	
	$scope.addArtifact = function(evt, serverArtifact) {
		evt.stopPropagation();
		evt.preventDefault();
		var artifacts = [];
		$scope.release.defaultArtifacts.forEach(function(a) {
			if (!serverArtifact.artifacts || !serverArtifact.artifacts.find(function(b) {return b.artifact == a.artifact;}))
				artifacts.push({"name": a.artifact});
		});
		$modal.open({
			templateUrl: "ui/environments/tmpl_envartifacts.html",
			controller: "selectorModalController",
			windowClass: "selectormodal",
			resolve: {
				"choices": function() { return artifacts; }
			}
		}).result.then(function(artifacts) {
			artifacts.forEach(function(artifact) {
				if (serverArtifact.artifacts)
					serverArtifact.artifacts.push({"artifact": artifact});
				else
					serverArtifact.artifacts = [{"artifact": artifact}];
				var a = $scope.release.defaultArtifacts.find(function(a) { return a.artifact == artifact; });
				a.unused = false;
				serverArtifact.unused = false;
			});
			$scope.form.$setDirty();
		});
	}
	
	$scope.removeArtifact = function(serverArtifact, artifactVersion) {
		var idx = serverArtifact.artifacts.indexOf(artifactVersion);
		serverArtifact.artifacts.splice(idx, 1);
		if (serverArtifact.artifacts.length == 0)
			serverArtifact.unused = true;
		$scope.release.defaultArtifacts.forEach(function(artifact) {
			artifact.unused = true;
			if (serverArtifact.artifacts.find(function(a) { return a.artifact == artifact.artifact; }))
				delete artifact.unused;
		});
		$scope.form.$setDirty();
	}

	$scope.usedDefaultArtifact = function() {
		return function(defaultArtifact) { return !defaultArtifact.unused && ((!($scope.$prefs.hideDisabled || $scope.readOnly) && !defaultArtifact.disabled) || !($scope.$prefs.hideDisabled || $scope.readOnly) || !defaultArtifact.disabled); }
	};

	$scope.usedServer = function() {
		return function(serverArtifact) { return (!($scope.$prefs.hideUnusedServers || $scope.readOnly) || !serverArtifact.unused) && (!($scope.$prefs.hideDisabled || $scope.readOnly) || !serverArtifact.disabled); }
	};

	$scope.usedServerArtifact = function() {
		return function(artifactVersion) { return !$scope.$prefs.hideDisabled || !artifactVersion.disabled; }
	};
		
	kwateeService.getRelease($scope.environmentName, $scope.release.name)
	.then(function(response) {
		release = response.data;
		$scope.release = angular.copy(release);
		getReleaseErrors();
	});

	$scope.errors = {};
	function getReleaseErrors() {
		kwateeService.getReleaseErrors($scope.environmentName, $scope.release.name, $scope.release)
		.then(function(response) {
			$scope.errors.msgs = response.data;
		});
	}
});

//
// ACTIVE VERSION MODAL CONTROLLER
//
environmentsControllers.controller("activeVersionModalController",
function($scope, kwateeService, artifact, version, server) {
	
	$scope.artifact = artifact;
	$scope.versionHolder = {"version": version ? version : "" };
	$scope.server = server;
	$scope.versions = [];
	
	kwateeService.getArtifact(artifact)
	.then(function(response) {
		response.data.versions.forEach(function(v) {
			$scope.versions.push(v.name);
		});
	});

});

//
// ACTIONS DIALOG CONTROLLER
//
artifactsControllers.controller("deployActionsModalController",
function($scope, $modalInstance, actions, readOnly) {

	$scope.actions = actions;
	$scope.readOnly = readOnly;

});

//
// SPECIAL PROPERTIES MODAL CONTROLLER
//
environmentsControllers.controller("relPropertiesModalController",
function($scope, $modal, node, environmentName, releaseName, artifactName, serverName, path, readOnly) {

	$scope.node = node;
	$scope.environmentName = environmentName;
	$scope.releaseName = releaseName;
	$scope.artifactName = artifactName;
	$scope.serverName = serverName;
	var currentLayer = serverName?5:4;
	$scope.readOnly = readOnly || currentLayer != node.layer;
	$scope.path = function() {
		return encodeURIComponent(path);
	}

	$scope.editPermissions = function() {
		$modal.open({
			templateUrl : "ui/tmpl_permissions.html",
			controller : "relPermissionsModalController",
			windowClass : "smallmodal",
			resolve : {
				"name" : function() { return $scope.node.name; },
				"permissions" : function() { return $scope.node.properties && $scope.node.properties.permissions ? $scope.node.properties.permissions : null; },
				"originalPermissions" : function() { return $scope.node.properties && $scope.node.properties.originalPermissions ? $scope.node.properties.originalPermissions : null; },
				"dir" : function() { return $scope.node.dir; },
				"readOnly" : function() { return $scope.readOnly; }
			}
		})
		.result.then(function(permissions) {
			if (permissions) {
				if (!$scope.node.properties)
					$scope.node.properties = {};
				$scope.node.properties.permissions = permissions;
			}
		});
	}
});


//
//File PERMISSIONS MODAL CONTROLLER
//
artifactsControllers.controller("relPermissionsModalController", ["$scope", "name", "permissions", "originalPermissions", "dir", "readOnly",
function($scope, name, permissions, originalPermissions, dir, readOnly) {

	$scope.name = name;
	$scope.permissions = permissions ? angular.copy(permissions) : {};
	$scope.originalPermissions = originalPermissions || {};
	$scope.dir = dir;
	$scope.readOnly = readOnly;

} ]);

//
// VARIABLES CONTROLLER
//
environmentsControllers.controller("envVarsController",
function($scope, $rootScope, $routeParams, $modal, $q, kwateeService) {

	$scope.$on("$locationChangeStart", $scope.changeLocation);
	$scope.environmentName = $routeParams.param1;
	$scope.releaseName = $routeParams.param2;
	var variables = null;
    $scope.variables = [];
    $scope.readOnly = true;
	$scope.newVariable = function(variableToClone) {
		var variable = angular.copy(variableToClone);
		$scope.editVariable(variable);
	}
	$scope.deleteVariable = function(variable) {
		if (variable.references) {
			variable.value = null;
		} else {
			var idx = $scope.variables.indexOf(variable);
			$scope.variables.splice(idx, 1);			
		}
		$scope.form.$setDirty();
	}
	$scope.editVariable = function(variable) {
		$modal.open({
			templateUrl: "ui/environments/tmpl_variable.html",
			controller: "envVarModalController",
			resolve: {
				"variable": function() { return variable; },
				"servers": function() { return environment.servers; },
				"artifacts": function() { return environment.artifacts; },
				"readOnly": function() { return $scope.readOnly; }
			}
		}).result.then(function(editedVar) {
			for (var k in editedVar)
				variable[k] = editedVar[k];
			var existingVar = $scope.variables.find(function(v) {
			    return v != variable && v.name === variable.name && v.artifact == variable.artifact && v.server == variable.server;
			});
			if ($scope.variables.indexOf(variable) < 0) {
				if (existingVar) {
					for (var k in variable)
						existingVar[k]=variable[k];
				} else {
					$scope.variables.push(variable);					
				}
			}
			$scope.form.$setDirty();
		});
	}

	$scope.variableScope = function(variable) {
		if (variable.server && variable.artifact)
			return variable.artifact + "@" + variable.server;
		if (variable.server)
			return "@"+variable.server;
		if (variable.artifact)
			return variable.artifact;
		return "";
	}

	$scope.importVariables = function() {
		$scope.uploadModal("Import Variables")
		.result.then(function(file) {
			kwateeService.importReleaseVariables($scope.environmentName, file)
			.then(function(response) {
				variables = response.data.variables;
				$scope.variables = angular.copy(variables);
				$scope.missingVariables = response.data.missingVariables;
				$scope.form.$setDirty();
			});
		});
	};
	$scope.exportVariables = function() {
		window.open(kwateeService.exportReleaseVariablesUrl($scope.environmentName, $scope.releaseName));
	};

	$scope.save = function() {
		if ($scope.form.$pristine)
			return;
		kwateeService.updateSnapshotVariables($scope.environmentName, $scope.variables)
		.then(function(response) {
			variables = response.data.variables;
			if (variables)
				$scope.variables = angular.copy(variables);
			else
				$scope.variables = [];
			$scope.versionVariables = response.data.defaultVariables;
			$scope.missingVariables = response.data.missingVariables;
			$scope.form.$setPristine();
		});
	}
	$scope.$on('saveEvent', $scope.save);
	
	$scope.revert = function() {
		$scope.variables = angular.copy(variables);
		$scope.form.$setPristine();
	}
	
	var environment = { 'artifacts': [], 'servers': [] }
	var batch = [
	    kwateeService.getReleaseVariables($scope.environmentName, $scope.releaseName),
	    kwateeService.getEnvironment($scope.environmentName),
	    kwateeService.getRelease($scope.environmentName, $scope.releaseName),
	    kwateeService.getGlobalVariables()
	];
	$q.all(batch)
	.then(function(responses) {
		var response = responses[0].data;
		$scope.readOnly = $rootScope.readOnly || response.readOnly;
		variables = response.variables;
		if (variables)
			$scope.variables = angular.copy(variables);
		else
			$scope.variables = [];
		$scope.versionVariables = response.artifactVariables;
		$scope.missingVariables = response.missingVariables;
		environment = responses[1].data;
		$scope.readOnly = !responses[2].data.editable;
		$scope.globalVariables = responses[3].data;
	});
	
});

//
// VARIABLE MODAL CONTROLLER
//
environmentsControllers.controller("envVarModalController",
function($scope, variable, servers, artifacts, readOnly) {
	
	$scope.isNew = !variable.name;
	$scope.servers = servers;
	$scope.artifacts = artifacts;
	$scope.readOnly = readOnly;
	if ($scope.isNew)
		$scope.variable = {"name": "", "value": ""};
	else
		$scope.variable = angular.copy(variable);
	
});

//
// OVERLAYS CONTROLLER
//
environmentsControllers.controller("overlaysController",
function($scope, $rootScope, $route, $routeParams, $modal, $q, kwateeService) {

	$scope.environmentName = $routeParams.param1;
    $scope.releaseName = $routeParams.param2;
    $scope.artifactName = $routeParams.param3;
    $scope.serverName = $routeParams.param4 == "*" ? null : $routeParams.param4;
    $scope.selectedPath = "/";
    $scope.overlays = function(shiftKey) {
		$scope.overlayModal($scope.selectedPath, shiftKey)
		.result.then(function(overlay) {
			kwateeService.uploadSnapshotOverlay($scope.environmentName, $scope.artifactName, $scope.serverName, overlay.path, overlay.file)
			.then(function() {
				$route.reload();
			});
		});
    }

	$scope.$on("nodeSelected", function(event, node, path) {
		var idx = path.lastIndexOf("/");
		if (idx >= 0)
			$scope.selectedPath = path.substring(0, idx+1);
		else
			$scope.selectedPath = "/";
		$scope.$broadcast("selectNode", node);
		$modal.open({
			templateUrl : "ui/environments/tmpl_specialproperties.html",
			controller : "relPropertiesModalController",
			windowClass : "defaultmodal fade in",
			resolve : {
				"node" : function() { return angular.copy(node); },
				"environmentName" : function() { return $scope.environmentName; },
				"releaseName" : function() { return $scope.releaseName; },
				"artifactName" : function() { return $scope.artifactName; },
				"serverName" : function() { return $scope.serverName; },
				"path" : function() { return path; },
				"readOnly" : function() { return $scope.readOnly || !node.id; }
			}
		})
		.result.then(function(properties) {
			if (properties) {
				kwateeService.updateSnapshotFileProperties($scope.environmentName, $scope.artifactName, $scope.serverName, path, properties)
				.then(function(response) {
					var newNode = response.data;
					if (newNode.name) {
						node.properties = newNode.properties;
						node.hasVariables = newNode.hasVariables;
					} else {
						$route.reload();
					}
				});
			} else {
				kwateeService.deleteSnapshotOverlay($scope.environmentName, $scope.artifactName, $scope.serverName, path)
				.then(function() {
					$route.reload();
				});
			}
		});
	});

	$scope.done = function() {
		$scope.go("/environments/" + $scope.environmentName + "/" + $scope.releaseName);
	};

	$scope.loadChildren = function(path) {
		return kwateeService.getReleasePackageFiles($scope.environmentName, $scope.releaseName, $scope.artifactName, $scope.serverName, path);;
	}

	var batch = [
	 			kwateeService.getReleasePackageFiles($scope.environmentName, $scope.releaseName, $scope.artifactName, $scope.serverName, "/"),
	 			kwateeService.getRelease($scope.environmentName, $scope.releaseName)
	 	];
	 $q.all(batch)
	.then(function(responses) {
	 	$scope.files = responses[0].data;
	 	if (!responses[1].data.editable)
			$scope.readOnly = true;
 	});

});