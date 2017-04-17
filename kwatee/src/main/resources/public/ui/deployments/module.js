"use strict";


// Declare app level module which depends on filters, and services
angular.module("deployments", [
	"deployments.controllers"
])
.config(["$routeProvider", function($routeProvider) {
	$routeProvider
		.when("/deployments", {
			templateUrl: "ui/deployments/tmpl_deployments.html",
			controller: "deploymentsController",
			label: "Deployments"
		})
		.when("/deployments/:param1", {
			redirectTo: "/deployments"
		})
		.when("/deployments/:param1/:param2", {
			templateUrl: "ui/deployments/tmpl_deployment.html",
			controller: "deploymentController",
		})
		.when("/deployments/:param1/:param2/progress", {
			templateUrl: "ui/deployments/tmpl_progress.html",
			controller: "progressController",
			label: "Progress"
		});
}]);