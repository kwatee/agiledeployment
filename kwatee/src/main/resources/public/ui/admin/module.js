"use strict";


// Declare app level module which depends on filters, and services
angular.module("admin", [
	"admin.controllers",
])
.config(["$routeProvider", function($routeProvider) {
	$routeProvider
		.when("/admin", {
			templateUrl: "ui/admin/tmpl_users.html",
			controller: "usersController",
			label: "Admin"
		})
		.when("/admin/users", {
			templateUrl: "ui/admin/tmpl_users.html",
			controller: "usersController",
			label: "Users"
		})
		.when("/admin/users/:param1", {
			templateUrl: "ui/admin/tmpl_user.html",
			controller: "userController"
		})
		.when("/admin/variables", {
			templateUrl: "ui/admin/tmpl_variables.html",
			controller: "kwateeVarsController",
			label: "Global variables"
		})
		.when("/admin/parameters", {
			templateUrl: "ui/admin/tmpl_parameters.html",
			controller: "parametersController",
			label: "Kwatee parameters"
		})
		.when("/admin/maintenance", {
			templateUrl: "ui/admin/tmpl_maintenance.html",
			controller: "maintenanceController",
			label: "Maintenance"
		});
}]);