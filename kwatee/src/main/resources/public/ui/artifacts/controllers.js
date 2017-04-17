"use strict";

/* Controllers */

var artifactsControllers = angular.module("artifacts.controllers", []);

//
// ARTIFACTS CONTROLLER
//
artifactsControllers.controller("artifactsController", ["$scope", "$modal", "$route", "kwateeService",
function($scope, $modal, $route, kwateeService) {

	$scope.newArtifact = function() {
		$scope.ask("Create new artifact", "Enter name")
		.result.then(function(name) {
			var options = {
				"description" : new Date().toUTCString()
			};
			kwateeService.createArtifact(name, options)
			.then(function() {
				$scope.go("/artifacts/" + name);
			});
		});
	}
	$scope.deleteArtifact = function(name) {
		$scope.confirm("Delete artifact " + name + "?")
		.result.then(function() {
			kwateeService.deleteArtifact(name)
			.then(function() {
				$route.reload();
			});
		});
	}
	$scope.importArtifacts = function() {
		var artifactsFile = $scope.upload;
		$scope.upload = null;
		kwateeService.uploadArtifacts(artifactsFile)
		.then(function() {
			$route.reload();
		});
	}

	kwateeService.getArtifacts().then(function(response) {
		$scope.artifacts = response.data;
	});

} ]);

//
// ARTIFACT CONTROLLER
//
artifactsControllers.controller("artifactController", ["$scope", "$route", "$routeParams", "kwateeService",
function($scope, $route, $routeParams, kwateeService) {

	$scope.$on("$locationChangeStart", $scope.changeLocation);

	var artifact = null;
	$scope.artifact = {
		"name" : $routeParams.param1
	};
	$scope.newVersion = function() {
		if (!$scope.isDirty($scope.form)) {
			$scope.ask("Create new version", "Enter name")
			.result.then(function(name) {
				var options = {
					"description" : "Created "
							+ new Date()
									.toUTCString()
				};
				kwateeService.createVersion($scope.artifact.name, name, options)
				.then(function() {
					$scope.go("/artifacts/" + $scope.artifact.name + "/" + name);
				});
			});
		}
	}

	$scope.duplicateVersion = function(duplicateName) {
		if (!$scope.isDirty($scope.form)) {
			$scope.ask("Duplicate version " + duplicateName, "Enter name")
			.result.then(function(versionName) {
				var options = {
					"description" : "cloned " + new Date().toUTCString()
				};
				kwateeService.duplicateVersion($scope.artifact.name, versionName, duplicateName, options)
				.then(function() {
					$scope.go("/artifacts/" + $scope.artifact.name + "/" + versionName);
				});
			});
		}
	}

	$scope.deleteVersion = function(name) {
		if (!$scope.isDirty($scope.form))
			$scope.confirm("Delete version " + name + "?")
			.result.then(function() {
				kwateeService.deleteVersion($scope.artifact.name, name)
				.then(function() {
					$route.reload();
				});
			});
	};

	$scope.save = function() {
		if ($scope.form.$pristine)
			return;
		kwateeService.updateArtifact($scope.artifact.name, $scope.artifact)
		.then(function() {
			artifact = angular.copy($scope.artifact);
			$scope.form.$setPristine();
		});
	};
	$scope.$on('saveEvent', $scope.save);

	$scope.revert = function() {
		$scope.artifact = angular.copy(artifact);
		$scope.form.$setPristine();
	};

	kwateeService.getArtifact($scope.artifact.name)
	.then(function(response) {
		artifact = response.data;
		$scope.artifact = angular.copy(artifact);
	});

} ]);

//
// VERSION CONTROLLER
//
artifactsControllers.controller("versionController", ["$scope", "$rootScope", "$routeParams", "$modal", "$q", "kwateeService",
function($scope, $rootScope, $routeParams, $modal, $q, kwateeService) {

	$scope.$on("$locationChangeStart", $scope.changeLocation);
	var version = null;
	$scope.artifactName = $routeParams.param1;
	$scope.version = {
		"name" : $routeParams.param2,
		"platforms" : []
	};
	$scope.platforms = [];
	$scope.upload = null;
	
	$scope.$on('variableEvent', function() {
		$scope.$apply(function () {
			$scope.go('/environments/'+$scope.environmentName+'/'+$scope.release.name+'/variables');
		});
	});

	$scope.save = function() {
		if ($scope.form.$pristine)
			return;
		kwateeService.updateVersion($scope.artifactName, $scope.version.name, $scope.version)
		.then(function(response) {
			$scope.errors = response.data.errors;
			version = angular.copy($scope.version);
			if ($scope.upload) {
				var fileToUpload = $scope.upload;
				kwateeService.uploadPackage($scope.artifactName, $scope.version.name, fileToUpload, false)
				.then(function(response) {
					$scope.version.packageInfo = response.data.packageInfo;
					version.packageInfo = $scope.version.packageInfo;
					$scope.upload = null;
					$scope.form.$setPristine();
				});
			} else {
				$scope.form.$setPristine();
			}
		});
	}
	$scope.$on('saveEvent', $scope.save);

	$scope.revert = function() {
		$scope.upload = null;
		$scope.version = angular.copy(version);
		$scope.form.$setPristine();
	}

	$scope.hasPlatform = function(platformId) {
		return $scope.version.platforms && $scope.version.platforms.indexOf(platformId) >= 0;
	}

	$scope.togglePlatform = function(platformId) {
		if (!$scope.version.platforms)
			$scope.version.platforms = [];
		var idx = $scope.version.platforms.indexOf(platformId);
		if (idx >= 0) { // is currently selected
			$scope.version.platforms.splice(idx, 1);
		} else { // is newly selected
			$scope.version.platforms.push(platformId);
		}
		$scope.form.$setDirty();
	}

	$scope.examine = function(shiftKey) {
		if (shiftKey) {
			kwateeService.uploadPackage($scope.artifactName, $scope.versionName, null, false)
			.then(function() {
				alert("Package successfully rescanned");
			});
		} else
			$scope.go("/artifacts/"+$scope.artifactName+"/"+$scope.version.name+"/package");
	}

	$scope.newExecutable = function() {
		$scope.editExecutable({});
	}

	$scope.deleteExecutable = function(exe) {
		var idx = $scope.version.executables.indexOf(exe);
		$scope.version.executables.splice(idx, 1);
		$scope.form.$setDirty();
	}

	$scope.editExecutable = function(executable) {
		$modal.open({
			templateUrl : "ui/artifacts/tmpl_executable.html",
			controller : "executableModalController",
			resolve : {
				"executable" : function() { return executable; },
				"readOnly" : function() { return $scope.readOnly || $scope.version.frozen; },
			}
		})
		.result.then(function(executable) {
			if (!$scope.version.executables)
				$scope.version.executables = [];
			var result = $scope.version.executables.filter(function(exe) {
				return exe.name === executable.name;
			});
			if (result.length > 0) {
				var exe = result[0];
				for ( var k in executable)
					exe[k] = executable[k];
			} else {
				$scope.version.executables.push(executable);
			}
			$scope.form.$setDirty();
		});
	}

	$scope.deletePackage = function() {
		$scope.confirm("Delete package and special properties?")
		.result.then(function() {
			kwateeService.deletePackage($scope.artifactName, $scope.version.name)
			.then(function() {
				delete $scope.version.packageInfo;
				delete $scope.upload;
			});
		});
	}

	$scope.editActions = function() {
		$modal.open({
			templateUrl : "ui/artifacts/tmpl_actions.html",
			controller : "versionActionsModalController",
			resolve : {
				"actions" : function() {
					return {
						"preDeployAction" : $scope.version.preDeployAction,
						"postDeployAction" : $scope.version.postDeployAction,
						"preUndeployAction" : $scope.version.preUndeployAction,
						"postUndeployAction" : $scope.version.postUndeployAction
					};
				},
				"readOnly" : function() { return $scope.readOnly || $scope.version.frozen; },
			}
		})
		.result.then(function(actions) {
			$scope.version.preDeployAction = actions.preDeployAction;
			$scope.version.postDeployAction = actions.postDeployAction;
			$scope.version.preUndeployAction = actions.preUndeployAction;
			$scope.version.postUndeployAction = actions.postUndeployAction;
			$scope.form.$setDirty();
		});
	}

	var batch = [];
	batch.push(kwateeService.getPlatforms());
	batch.push(kwateeService.getVersion($scope.artifactName, $scope.version.name));
	$q.all(batch)
	.then(function(responses) {
		$scope.platforms = responses[0].data;
		version = responses[1].data;
		if (!version.packageInfo)
			version.packageInfo = {};
		$scope.version = angular.copy(version);
	});

} ]);

//
// EXECUTABLE DIALOG CONTROLLER
//
artifactsControllers.controller("executableModalController", [ "$scope", "executable", "readOnly",
function($scope, executable, readOnly) {

	$scope.readOnly = readOnly;
	$scope.isNew = !executable.name;
	if ($scope.isNew)
		$scope.executable = {};
	else
		$scope.executable = angular.copy(executable);

} ]);

//
// ACTIONS DIALOG CONTROLLER
//
artifactsControllers.controller("versionActionsModalController", [ "$scope", "$modalInstance", "actions", "readOnly",
function($scope, $modalInstance, actions, readOnly) {

	$scope.actions = actions;
	$scope.readOnly = readOnly;

} ]);

//
// PACKAGE CONTROLLER
//
artifactsControllers.controller("packageController", [ "$scope", "$route", "$routeParams", "$modal", "$q", "kwateeService",
function($scope, $route, $routeParams, $modal, $q, kwateeService) {

	var version = {};
	$scope.artifactName = $routeParams.param1;
	$scope.versionName = $routeParams.param2;
	$scope.files = [];
	$scope.selectedPath = "/";

	$scope.specialFiles = function() {
		kwateeService.getSpecialFiles($scope.artifactName, $scope.versionName)
		.then(function(response) {
			$modal.open({
				templateUrl : "ui/artifacts/tmpl_special.html",
				controller : "specialModalController",
				windowClass : "specialmodal",
				resolve : {
					"files" : function() { return response.data; },
					"artifactName": function() { return $scope.artifactName; },
					"versionName": function() { return $scope.versionName; },
					"readOnly" : function() { return $scope.readOnly; }
				}
			}).result.then(function() {
				$route.reload();
			});
		});
	}

	$scope.rescanPackage = function() {
		kwateeService.uploadPackage($scope.artifactName, $scope.versionName, null, false)
		.then(function() {
			$route.reload();
		});
	}

	$scope.overlays = function(shiftKey) {
		$scope.overlayModal($scope.selectedPath, shiftKey)
		.result.then(function(overlay) {
			kwateeService.uploadVersionOverlay($scope.artifactName, $scope.versionName, overlay.path, overlay.file)
			.then(function() {
				$route.reload();
			});
		});
	}

	$scope.editVariablePrefixChar = function() {
		$modal.open({
			templateUrl : "ui/artifacts/tmpl_varprefix.html",
			windowClass : "smallmodal",
		}).result.then(function(prefixChar) {
			kwateeService.updateVersionVariablePrefix($scope.artifactName, $scope.versionName, prefixChar);
		});
	}

	$scope.editPermissions = function() {
		$modal.open({
			templateUrl : "ui/tmpl_permissions.html",
			controller : "permissionsModalController",
			windowClass : "smallmodal",
			resolve : {
				"name" : function() { return $scope.artifactName; },
				"permissions" : function() { return version.permissions; },
				"originalPermissions" : function() { return null; },
				"dir" : function() { return true; },
				"readOnly" : function() { return $scope.readOnly; }
			}
		})
		.result.then(function(permissions) {
			if (permissions) {
				version.permissions = permissions;
				kwateeService.updateVersion($scope.artifactName, $scope.versionName, version)
				.then(function(response) {
					$scope.errors = response.data.errors;
				});
			}
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
			templateUrl : "ui/artifacts/tmpl_specialproperties.html",
			controller : "propertiesModalController",
			windowClass : "defaultmodal fade in",
			resolve : {
				"node" : function() { return angular.copy(node); },
				"artifactName" : function() { return $scope.artifactName; },
				"versionName" : function() { return $scope.versionName; },
				"path" : function() { return path; },
				"readOnly" : function() { return $scope.readOnly || $scope.packageRescanIsNeeded || !node.id; }
			}
		})
		.result.then(function(properties) {
			if (properties) {
				kwateeService.updateVersionFileProperties($scope.artifactName, $scope.versionName, path, properties)
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
				kwateeService.deleteVersionOverlay($scope.artifactName, $scope.versionName, path)
				.then(function() {
					$route.reload();
				});
			}
		});
	});

	$scope.loadChildren = function(path) {
		return kwateeService.getPackageFiles($scope.artifactName, $scope.versionName, path);
	}

	var batch = [
			kwateeService.getPackageFiles($scope.artifactName, $scope.versionName, "/"),
			kwateeService.getVersion($scope.artifactName, $scope.versionName)
	];
	$q.all(batch)
	.then(function(responses) {
		$scope.files = responses[0].data;
		version = responses[1].data;
		$scope.packageRescanIsNeeded = version.packageRescanIsNeeded;
		$scope.variablePrefixChar = version.variablePrefixChar;
		if (version.frozen)
			$scope.readOnly = true;
	});

} ]);

//
// SPECIAL FILES MODAL CONTROLLER
//
artifactsControllers.controller("specialModalController", [ "$scope", "$modal", "$route", "kwateeService", "files", "artifactName", "versionName", "readOnly",
function($scope, $modal, $route, kwateeService, files, artifactName, versionName, readOnly) {

	$scope.edited = false;
	$scope.files = files;
	$scope.readOnly = readOnly;
	
	$scope.editProperties = function(file) {
		$modal.open({
			templateUrl : "ui/artifacts/tmpl_specialproperties.html",
			controller : "propertiesModalController",
			windowClass : "defaultmodal fade in",
			resolve : {
				"node" : function() { return angular.copy(file); },
				"artifactName" : function() { return artifactName; },
				"versionName" : function() { return versionName; },
				"path" : function() { return file.path; },
				"readOnly" : function() { return $scope.readOnly; }
			}
		})
		.result.then(function(properties) {
			$scope.edited = true;
			if (properties) {
				kwateeService.updateVersionFileProperties(artifactName, versionName, file.path, properties)
				.then(function(response) {
					var newNode = response.data;
					if (newNode.name) {
						file.properties = newNode.properties;
						file.hasVariables = newNode.hasVariables;
					} else {
						$route.reload();
					}
				});
			} else {	
				kwateeService.deleteVersionOverlay(artifactName, versionName, file.path)
				.then(function() {
					$route.reload();
				});
			}
		});
	}

}]);

//
// SPECIAL PROPERTIES MODAL CONTROLLER
//
artifactsControllers.controller("propertiesModalController", ["$scope", "$modal", "node", "artifactName", "versionName", "path", "readOnly",
function($scope, $modal, node, artifactName, versionName, path, readOnly) {
	$scope.node = node;
	$scope.artifactName = artifactName;
	$scope.versionName = versionName;
	$scope.readOnly = readOnly;
	$scope.path = function() {
		return encodeURIComponent(path);
	}
	
	$scope.editPermissions = function() {
		$modal.open({
			templateUrl : "ui/tmpl_permissions.html",
			controller : "permissionsModalController",
			windowClass : "smallmodal",
			resolve : {
				"name" : function() { return node.name; },
				"permissions" : function() { return $scope.node.properties && $scope.node.properties.permissions ? $scope.node.properties.permissions : null; },
				"originalPermissions" : function() { return $scope.node.properties && $scope.node.properties.originalPermissions ? $scope.node.properties.originalPermissions : null; },
				"dir" : function() { return $scope.node.dir; },
				"readOnly" : function() { return readOnly; }
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

} ]);

//
// File PERMISSIONS MODAL CONTROLLER
//
artifactsControllers.controller("permissionsModalController", ["$scope", "name", "permissions", "originalPermissions", "dir", "readOnly",
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
artifactsControllers.controller("variablesController", ["$scope", "$rootScope", "$routeParams", "$modal", "$q", "kwateeService",
function($scope, $rootScope, $routeParams, $modal, $q, kwateeService) {

	$scope.$on("$locationChangeStart", $scope.changeLocation);
	$scope.artifactName = $routeParams.param1;
	$scope.versionName = $routeParams.param2;
	var variables = null;
	$scope.variables = [];
	$scope.newVariable = function() {
		$scope.editVariable({});
	};
	
	$scope.deleteVariable = function(variable) {
		if (variable.references) {
			variable.value = null;
		} else {
			var idx = $scope.variables.indexOf(variable);
			$scope.variables.splice(idx, 1);
		}
		$scope.form.$setDirty();
	};

	$scope.editVariable = function(variable) {
		$modal.open({
			templateUrl : "ui/artifacts/tmpl_variable.html",
			controller : "versionVarModalController",
			resolve : {
				"variable" : function() { return variable; },
				"readOnly" : function() { return $scope.readOnly; },
			}
		})
		.result.then(function(editedVar) {
			for ( var k in editedVar)
				variable[k] = editedVar[k];
			var existingVar = $scope.variables.find(function(v) {
				return v != variable && v.name === variable.name;
			});
			if ($scope.variables.indexOf(variable) < 0) {
				if (existingVar) {
					for ( var k in variable)
						existingVar[k] = variable[k];
				} else {
					$scope.variables.push(variable);
				}
			}
			$scope.form.$setDirty();
		});
	};

	$scope.importVariables = function() {
		$scope.uploadModal("Import Variables")
		.result.then(function(file) {
			kwateeService.importVersionVariables($scope.artifactName, $scope.versionName, file)
			.then(function() {
				variables = response.data.defaultVariableValues;
				$scope.variables = angular.copy(variables);
				$scope.form.$setDirty();
			});
		});
	};

	$scope.exportVariables = function() {
		window.open(kwateeService.exportVersionVariablesUrl($scope.artifactName, $scope.versionName));
	};

	$scope.references = [];
	$scope.filteredReference = null;
	$scope.filterByReference = function(value) {
		if (!$scope.filteredReference)
			return true;
		if (!value.references)
			return false;
		return value.references.find(function (ref) { return ref.location == $scope.filteredReference } ) != undefined;
	};

	$scope.save = function() {
		if (!$scope.form.$dirty)
			return;
		kwateeService.updateVersionVariables($scope.artifactName, $scope.versionName, $scope.variables)
		.then(function(response) {
			variables = response.data.defaultVariableValues;
			$scope.variables = angular.copy(variables);
			$scope.form.$setPristine();
		});
	};
	$scope.$on('saveEvent', $scope.save);

	$scope.revert = function() {
		$scope.variables = angular.copy(variables);
		$scope.form.$setPristine();
	};

	kwateeService.getVersionVariables($scope.artifactName, $scope.versionName)
	.then(function(response) {
		$scope.form.filter.$pristine = false;
		variables = response.data.defaultVariableValues;
		if (variables) {
			$scope.variables = angular.copy(variables);
			var references = {};
			for (var v = 0; v < variables.length; v ++) {
				var variable = variables[v];
				if (variable.references) {
					for (var r = 0; r < variable.references.length; r ++)
						references[variable.references[r].location] = true;
				}
			}
			for (var ref in references)
				$scope.references.push(ref);
		} else
			$scope.variables = variables;
		if (response.data.frozen)
			$scope.readOnly = true;
	});

} ]);

//
// VARIABLE MODAL CONTROLLER
//
artifactsControllers.controller("versionVarModalController", ["$scope", "variable", "readOnly",
function($scope, variable, readOnly) {

	$scope.readOnly = readOnly;
	$scope.isNew = !variable.name;
	if ($scope.isNew)
		$scope.variable = { "name" : "", "value" : "" };
	else
		$scope.variable = angular.copy(variable);
} ]);
