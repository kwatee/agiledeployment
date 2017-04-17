"use strict";


// Declare app level module which depends on filters, and services
angular.module("environments", [
	"environments.controllers"
])
.config(["$routeProvider", function($routeProvider) {
	$routeProvider
		.when("/environments", {
			templateUrl: "ui/environments/tmpl_environments.html",
			controller: "environmentsController",
			label: "Environments"
		})
		.when("/environments/:param1", {
			templateUrl: "ui/environments/tmpl_environment.html",
			controller: "environmentController"
		})
		.when("/environments/:param1/:param2", {
			templateUrl: "ui/environments/tmpl_release.html",
			controller: "releaseController"
		})
		.when("/environments/:param1/:param2/variables", {
			templateUrl: "ui/environments/tmpl_variables.html",
			controller: "envVarsController",
			label: "Variables"
		})
		.when("/environments/:param1/:param2/:param3", {
			redirectTo: "environments/:param1/:param2"
		})
		.when("/environments/:param1/:param2/:param3/:param4", {
			redirectTo: "environments/:param1/:param2"
		})
		.when("/environments/:param1/:param2/:param3/:param4/overlays", {
			templateUrl: "ui/environments/tmpl_overlays.html",
			controller: "overlaysController",
			label: "Overlays"
		});
}]);