"use strict";

/* Controllers */

var adminControllers = angular.module("admin.controllers", []);

//
// USERS CONTROLLER
//
adminControllers.controller("usersController",
["$scope", "$route", "$location", "kwateeService",
function($scope, $route, $location, kwateeService) {
	$scope.newUser = function() {
		$scope.ask("Create new user", "Enter name")
		.result.then(function(name) {
			var user = {"description": new Date().toDateString()}
			kwateeService.createUser(name, user)
			.then(function() {
				$location.path("#/admin/users/" + name);
			});
		});
	}
	
	$scope.deleteUser = function(name) {
		$scope.confirm("Delete user " + name + "?")
		.result.then(function() {
			kwateeService.deleteUser(name)
			.then(function() {
				$route.reload();
			});
		});
	}
	
	kwateeService.getUsers()
	.then(function(response) {
        $scope.users = response.data;
	});
}]);

//
// USER CONTROLLER
//
adminControllers.controller("userController",
["$scope", "$routeParams", "kwateeService",
function($scope, $routeParams, kwateeService) {

	$scope.$on("$locationChangeStart", $scope.changeLocation);
    
	var user = null;
	$scope.user = { "login": $routeParams.param1 };
		
	$scope.save = function() {
		if ($scope.form.$pristine)
			return;
		if ($scope.user.password != $scope.verifyPassword) {
			alert("Passwords do not match");
			return;
		}
		kwateeService.updateUser($scope.user.name, $scope.user)
		.then(function() {
			delete $scope.user.password;
			delete $scope.verifyPassword;
			user = angular.copy($scope.user);
			$scope.form.$setPristine();
		});
	}
	$scope.$on('saveEvent', $scope.save);
	
	$scope.revert = function() {
		delete $scope.verifyPassword;
		$scope.user = angular.copy(user);
		$scope.form.$setPristine();
	}

	kwateeService.getUser($scope.user.login)
	.then(function(response) {
		user = response.data;
		$scope.user = angular.copy(user);
	});
}]);

//
// VARIABLES CONTROLLER
//
adminControllers.controller("kwateeVarsController",
["$scope", "$modal", "kwateeService",
function($scope, $modal, kwateeService) {

	$scope.$on("$locationChangeStart", $scope.changeLocation);
    
	var variables = null;
    $scope.variables = [];
	$scope.editVariable = function(variable) {
		$modal.open({
			templateUrl: "ui/admin/tmpl_variable.html",
			windowClass: "askmodal",
			controller: "variableModalController",
			resolve: {
				"variable": function() { return angular.copy(variable); }
			}
		})
		.result.then(function(value) {
			variable.value = value;
			$scope.form.$setDirty();
		});
	}

	$scope.save = function() {
		if ($scope.form.$pristine)
			return;
		kwateeService.updateGlobalVariables($scope.variables)
		.then(function() {
			variables = angular.copy($scope.variables);
			$scope.form.$setPristine();
		});
	}
	$scope.$on('saveEvent', $scope.save);
	
	$scope.revert = function() {
		$scope.variables = angular.copy(variables);
		$scope.form.$setPristine();
	}

	kwateeService.getGlobalVariables()
	.then(function(response) {
		variables = response.data;
		$scope.variables = angular.copy(variables);
	});
}]);

//
// VARIABLE MODAL CONTROLLER
//
serversControllers.controller("variableModalController",
["$scope", "variable",
function($scope, variable) {
	$scope.variable = variable;
}]);

//
// PARAMETERS CONTROLLER
//
adminControllers.controller("parametersController",
["$scope", "$rootScope", "kwateeService",
function($scope, $rootScope, kwateeService) {

	$scope.$on("$locationChangeStart", $scope.changeLocation);

	var parameters = null;
	var idx;
	$scope.parameters = {"title": $rootScope.kwateeOrganisation}
	$scope.addExtension = function() {
		$scope.ask("Exclude extension", "Extension")
		.result.then(function(ext) {
			idx = $scope.parameters.excludedExtensions.indexOf(ext);
			if (idx < 0)
				$scope.parameters.excludedExtensions.push(ext);
			$scope.form.$setDirty();
		});
	}
	
	$scope.removeExtension = function(event, ext) {
		var offset = event.clientX - event.currentTarget.offsetLeft;
		if (offset < 15) {
			idx = $scope.parameters.excludedExtensions.indexOf(ext);
			$scope.parameters.excludedExtensions.splice(idx, 1);
			$scope.form.$setDirty();
		}
	}

	$scope.save = function() {
		if ($scope.form.$pristine)
			return;
		kwateeService.updateParameters($scope.parameters)
		.then(function() {
			parameters = angular.copy($scope.parameters);
			$scope.form.$setPristine();
			$scope.$parent.kwateeInfo.organization = $scope.parameters.title;
		});
	}
	$scope.$on('saveEvent', $scope.save);
	
	$scope.revert = function() {
		$scope.parameters = angular.copy(parameters);
		$scope.form.$setPristine();
	}
	
	kwateeService.getParameters()
	.then(function(response) {
		parameters = response.data;
		$scope.parameters = angular.copy(parameters);
	});
}]);

//
// MAINTENANCE CONTROLLER
//
adminControllers.controller("maintenanceController",
["$scope", "$location", "$window", "kwateeService",
function($scope, $location, $window, kwateeService) {

	$scope.upload = null;

	$scope.exportDB = function() {
		var url = kwateeService.getExportBackupUrl();
		$window.open(url, '_blank');
	}
}]);
