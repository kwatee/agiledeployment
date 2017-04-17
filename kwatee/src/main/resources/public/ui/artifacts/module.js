"use strict";


// Declare app level module which depends on filters, and services
angular.module("artifacts", [
	"artifacts.controllers"
])
.config(["$routeProvider", function($routeProvider) {
	$routeProvider
		.when("/artifacts", {
			templateUrl: "ui/artifacts/tmpl_artifacts.html",
			controller: "artifactsController",
			label: "Artifacts"
		})
		.when("/artifacts/:param1", {
			templateUrl: "ui/artifacts/tmpl_artifact.html",
			controller: "artifactController"
		})
		.when("/artifacts/:param1/:param2", {
			templateUrl: "ui/artifacts/tmpl_version.html",
			controller: "versionController",
		})
		.when("/artifacts/:param1/:param2/package", {
			templateUrl: "ui/artifacts/tmpl_package.html",
			controller: "packageController",
			label: "Package"
		})
		.when("/artifacts/:param1/:param2/variables", {
			templateUrl: "ui/artifacts/tmpl_variables.html",
			controller: "variablesController",
			label: "Variables"
		});
}]);