"use strict";


// Declare app level module which depends on filters, and services
var serverModule = angular.module("servers", [
	"servers.controllers"
]);

serverModule.config(["$routeProvider", function($routeProvider) {
	$routeProvider
		.when("/servers", {
			templateUrl: "ui/servers/tmpl_servers.html",
			controller: "serversController",
			label: "Servers"
		})
		.when("/servers/:param1", {
			templateUrl: "ui/servers/tmpl_server.html",
			controller: "serverController"
		});
}]);

serverModule.directive("serverPoolManual", function() {
	return {
		restrict: "E",
		scope: {
			"properties": "="
		},
		replace: true,
		templateUrl: "ui/servers/tmpl_pool_manual.html",
	    link: function(scope, elem, attrs) {
	    	var first = true;
			scope.instances = [];
			for (var i = 0; i < 10000; i ++) {
				if (!scope.properties["instance"+i])
					break;
				var instance = scope.properties["instance"+i].split(",");
				scope.instances.push({
					"name": instance[0],
					"ip_address": instance[1],
					"port": instance[2]
				});
			}
			scope.newInstance = function() {
				scope.instances.push({
					"name": "instance"+(scope.instances.length+1),
					"ip_address": "0.0.0.0",
					"port": 22
				});
			}
			scope.removeInstance = function(instance) {
				var idx = scope.instances.indexOf(instance);
				scope.instances.splice(idx, 1);
			}

			scope.$watch('instances', function(instance) {
				if (first) {
					first = false;
					return;
				}
				for (var i = 0; i < scope.instances.length; i ++) {
					instance = scope.instances[i];
					scope.properties["instance"+i] = instance.name+","+instance.ip_address+","+instance.port;
				}
				delete scope.properties["instance"+scope.instances.length];
				scope.$parent.form.$setDirty(true);
			}, true);
		}
	};
});

serverModule.directive("serverPoolEc2", function() {
	return {
		restrict: "E",
		scope: {
			properties: "="
		},
		replace: true,
		templateUrl: "ui/servers/tmpl_pool_ec2.html",
	    link: function(scope, elem, attrs) {
		}
	};
});

serverModule.directive("serverPoolGeneric", function() {
	return {
		restrict: "E",
		scope: {
			properties: "=",
			descriptors: "&"
		},
		replace: true,
		templateUrl: "ui/servers/tmpl_pool_generic.html",
	    link: function(scope, elem, attrs) {
	    	scope.test = function(f) {
	    	var s = f;
	    	var t = s;
	    	}
		}
	};
});