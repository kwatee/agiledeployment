"use strict";

/* Controllers */

var deploymentsControllers = angular.module("deployments.controllers", []);

//
// DEPLOYMENTS CONTROLLER
//
deploymentsControllers.controller("deploymentsController",
["$scope", "$routeParams", "kwateeService",
function($scope, $routeParams, kwateeService) {

	$scope.status = {};
	if ($routeParams.param1)
		$scope.status[$routeParams.param1] = true;
	kwateeService.getDeployableEnvironments()
	.then(function(response) {
        $scope.deployments = response.data;
	});

}]);

//
// DEPLOYMENT CONTROLLER
//
deploymentsControllers.controller("deploymentController",
["$scope", "$routeParams", "$q", "$modal", "kwateeService",
function($scope, $routeParams, $q, $modal, kwateeService) {

	$scope.environmentName = $routeParams.param1;
	$scope.release = { "name": $routeParams.param2 };

	var manage = function(operation, skipIntegrityCheck) {
//		var evt = args.length > 0 ? args[0] : null;
//		if (evt) {
//			evt.stopPropagation();
//			evt.preventDefault();
//		}
		if ($scope.release.servers.length >= 3) {
			// count number of selected active servers
			var count = 0;
			$scope.release.servers.forEach(function(s) { if (s.selected && !s.disabled) count ++; } );
			if (count >= 3 && !confirm("Perform '" + operation + "' on all "+ count + " servers?"))
				return;
		}
		var targets = null;
		if (!$scope.selectAll) {
			targets = [];
			$scope.release.servers.forEach(function(s) {
				if (s.selected) {
					var selectedServer = {"server": s.server};
					targets.push(selectedServer);
				}
			});
		}
		
		kwateeService.manage($scope.environmentName, $scope.release.name, operation, targets, null, skipIntegrityCheck)
		.then(
			function(response) {
				$scope.go("/deployments/"+$scope.environmentName+"/"+$scope.release.name+"/progress",{"ref":response.data.ref});
			},
			function(error) {
				if (error.status == 417) {
					console.log(error);
					$modal.open({
						templateUrl: "ui/deployments/tmpl_authmodal.html",
						windowClass: "authmodal",
						controller: "authModalController",
						resolve: {
							"kwateeService": function() { return kwateeService; },
							"authRequest": function() { return error.data; }
						}
					})
					.result.then(function() {
						manage(args, operation);	// retry the operation
					});
				} else if (error.status == 409) {
					// operation in progress
					$scope.confirm("Impossible to perform this operation because a another operation is still ongoing. View ongoing progress?")
					.result.then(function() {
						$scope.go("/deployments/"+$scope.environmentName+"/"+$scope.release.name+"/progress",{"ref":error.data.ref});
					});
				} else {
					alert("error " + error.status)
				}
			});
	};
	
	$scope.updateSelected = function() {
		if (arguments.length == 1) {
			var selected = arguments[0];
			$scope.release.servers.forEach(function(s) { s.selected = selected; } );
		} else {
			var unselected = $scope.release.servers.find(function(s) { return !s.selected && !s.unused && (!s.disabled); });
			$scope.selectAll = unselected == null;
		}
		$scope.anySelected = $scope.release.servers.find(function(s) { return s.selected && !s.unused && (!s.disabled); }) != null;
	};

	$scope.manageDeploy = function(skipIntegrityCheck) { manage("deploy", skipIntegrityCheck); };
	$scope.manageUndeploy = function(skipIntegrityCheck) { manage("undeploy", skipIntegrityCheck); };
	$scope.manageCheck = function() { manage("check", true); };
	$scope.manageStart = function(skipIntegrityCheck) { manage("start", skipIntegrityCheck); };
	$scope.manageStop = function(skipIntegrityCheck) { manage("stop", skipIntegrityCheck); };
	$scope.manageStatus = function(skipIntegrityCheck) { manage("status", skipIntegrityCheck); };

	$scope.usedServer = function() {
		return function(serverArtifact) { return !serverArtifact.unused && (!$scope.$prefs.hideDisabled || !serverArtifact.disabled); }
	};

	$q.all([
		kwateeService.getDeployment($scope.environmentName, $routeParams.param2)
		.then(function(response) {
			$scope.selectAll = true;
			$scope.anySelected = true;
			response.data.servers.forEach(function(s) { s.selected = true; } );
			$scope.release = response.data;
		}),
		kwateeService.getOngoingDeployment()
		.then(function(response) {
			if (response.data.ref)
				$scope.go("/deployments/"+$scope.environmentName+"/"+$scope.release.name+"/progress",{"ref":response.data.ref});
		})
	]);

}]);


//
// DEPLOYMENT PROGRESS CONTROLLER
//
deploymentsControllers.controller("progressController",
["$scope", "$routeParams", "$modal", "$timeout", "kwateeService",
function($scope, $routeParams, $modal, $timeout, kwateeService) {

	$scope.environmentName = $routeParams.param1;
	$scope.releaseName = $routeParams.param2;
	var ref = $routeParams.ref;
	if (!ref) {
		$scope.go("/deployments/"+$scope.environmentName+"/"+$scope.releaseName);
		return;
	}
		
	$scope.progress = { "operation": "", "status": "inexistant"};
	$scope.canceling = false;

	$scope.showStatus = function(serverName, artifactName) {
		kwateeService.getDeploymentProgressMessages(ref, artifactName, serverName)
		.then(function(response) {
			$modal.open({
				templateUrl: "ui/deployments/tmpl_messages.html",
				windowClass: "messagesmodal",
				controller: "messagesModalController",
				resolve: {
					"messages": function() { return response.data.messages; }
				}
			});
		});
	}

	$scope.cancelOperation = function() {
		kwateeService.cancelDeployment(ref)
		.then(function() {
			$scope.canceling = true;
		});
	}
	
	$scope.clearOperation = function() {
		kwateeService.clearDeployment(ref)
		.then(function() {
			$scope.go("/deployments/"+$scope.environmentName+"/"+$scope.releaseName);
		});
	}

	var refresh = {
		"action": function() {
			kwateeService.getDeploymentProgress(ref)
			.then(function(response) {
				if (refresh.action) {
					$scope.progress = response.data;
					if (!"inexistant,done,interrupted,failed".contains($scope.progress.status)) {
						refresh.timer = $timeout(refresh.action, 1000);
						return;
					}
					if ($scope.progress.status == "done") {
						var globalSuccess = true;
						for (var s = 0; s < $scope.progress.servers.length; s ++) {
							if ($scope.progress.servers[s].status == "failed") {
								globalSuccess = false;
								break;
							}
						}
						if (globalSuccess)
							$scope.success = true;
						else
							$scope.failure = true;
					}
				} 
				delete refresh.timer;
				delete refresh.action;
			},
			function() {
				delete refresh.timer;
				delete refresh.action;
				$scope.go("/deployments/"+$scope.environmentName+"/"+$scope.releaseName);
			});
		}
	};
	refresh.timeout = $timeout(refresh.action);
	$scope.$on("$destroy", function() {
		if (refresh.timer) {
			delete refresh.action;
			$timeout.cancel(refresh.timer);
			delete refresh.timer;
		}
		if ("done,interrupted".contains($scope.progress.status))
			kwateeService.clearDeployment(ref);
	});
	$scope.refresh = refresh;

}]);

//
// DEPLOYMENT AUTHENTICATION MODAL CONTROLLER
//
deploymentsControllers.controller("authModalController",
["$scope", "kwateeService", "authRequest",
function($scope, kwateeService, authRequest) {
	$scope.authRequest = authRequest;
	$scope.authRequest.password = "";
	$scope.sameForAllServers = false;
	
	$scope.doAuth = function() {
		kwateeService.sendDeploymentCredentials($scope.authRequest, $scope.sameForAllServers)
		.then(
			function() {
				$scope.$close();
			},
			function(code) {
				if (code == 420)
					$scope.authRequest.message = "Bad login or password";
				else
					$scope.authRequest.message = "Error " + code;
			}
		);
		
//		CheckUserCredentialsAction action = new CheckUserCredentialsAction(
//				this.environmentName,
//				this.serverName,
//				this.login.getText(),
//				this.password.getText(),
//				this.samePasswordForAll.getValue());
//		$scope.$close(authRequest);
	}

}]);

//
// STATUS MESSAGES MODAL CONTROLLER
//
deploymentsControllers.controller("messagesModalController",
["$scope", "messages",
function($scope, messages) {

	$scope.messages = messages;

}]);
