"use strict";

/* Controllers */

var serversControllers = angular.module("servers.controllers", []);

//
// SERVERS CONTROLLER
//
serversControllers.controller("serversController",
["$scope", "$modal", "$route", "kwateeService",
function($scope, $modal, $route, kwateeService) {
	
	$scope.newServer = function() {
		$scope.ask("Create new server", "Enter name")
		.result.then(function(name) {
			var options = { "description": new Date().toUTCString() };
			kwateeService.createServer(name, options)
			.then(function() {
				$scope.go("/servers/" + name);
			});
		});
	}
	
	$scope.newServerPool = function() {
		$modal.open({
			templateUrl: "ui/servers/tmpl_newpool.html",
			windowClass: "defaultmodal",
			controller: "newPoolModalController"
		})
		.result.then(function(nameAndType) {
			var options = { "poolType": nameAndType.type, "description": new Date().toUTCString() };
			kwateeService.createServer(nameAndType.name, options)
			.then(function() {
				$scope.go("/servers/" + nameAndType.name);
			});
		});
	}

	$scope.duplicateServer = function(serverName) {
		$scope.ask("Duplicate server " + serverName, "Enter name")
		.result.then(function(name) {
			var options = { "description": "cloned " + new Date().toUTCString() };
			kwateeService.duplicateServer(serverName, name, options)
			.then(function() {
				$scope.go("/servers/" + name);
			});
		});
	}

	$scope.deleteServer = function(name) {
		$scope.confirm("Delete server " + name + "?")
		.result.then(function() {
			kwateeService.deleteServer(name)
			.then(function() {
				$route.reload();
			});
		});
	}
	
    $scope.servers = [];
    kwateeService.getServers().then(function(response) {
		$scope.servers = response.data;
    });
    
}]);

//
// NEW SERVER POOL CONTROLLER
//
serversControllers.controller("newPoolModalController",
["$scope", "kwateeService",
function($scope, kwateeService) {
	
	$scope.type = "manual";
	kwateeService.getPoolTypes()
	.then(function(response) {
		$scope.poolTypes = response.data;
	});
	
}]);

//
// SERVER CONTROLLER
//
serversControllers.controller("serverController",
["$scope", "$routeParams", "$modal", "$q", "kwateeService",
function($scope, $routeParams, $modal, $q, kwateeService) {
	
	var poolTypes = [];
    var server = null;
    var authType = "password";

	$scope.$on("$locationChangeStart", $scope.changeLocation);
    $scope.server = { "name": $routeParams.param1 };
    $scope.platforms = [];
    $scope.authType = authType;
	$scope.save = function() {
		if ($scope.form.$pristine)
			return;
		kwateeService.updateServer($scope.server.name, $scope.server)
		.then(function() {
			authType = $scope.authType;
			server = angular.copy($scope.server);
			$scope.form.$setPristine();
		});
	}
	$scope.$on('saveEvent', $scope.save);
	
	$scope.revert = function() {
		$scope.authType = authType;
		$scope.server = angular.copy(server);
		$scope.form.$setPristine();
	}
	
	$scope.testConnection = function() {
		var serverOptions = angular.copy($scope.server);
		if (serverOptions.credentials.promptPassword) {
			serverOptions.credentials.password = prompt("Enter password", "");
			if (serverOptions.credentials.password == null)
				return;
		}
		kwateeService.testConnection($scope.server.name, serverOptions)
		.then(function(response) {
			$modal.open({
				templateUrl: "ui/servers/tmpl_testConnection.html",
				controller: "testModalController",
				resolve: {
					"capabilities": function() { return response.data; }
				}
			});
		});
	}
	
	$scope.isTelnet = function() {
		return $scope.server.conduitType === "telnetftp";
	}

	$scope.toggleAuth = function() {
		if ($scope.isTelnet())
			$scope.authType = "password";
		if ($scope.authType === "password")
			$scope.server.credentials.pem = "";
	}

	$scope.editTelnetOptions = function() {
		$modal.open({
			templateUrl: "ui/servers/tmpl_telnetOptions.html",
			controller: "telnetModalController",
			resolve: {
				"properties": function() { return $scope.server.properties; }
			}
		}).result.then(function(props) {
			for (var k in props)
				$scope.server.properties[k] = props[k];
			$scope.form.$setDirty();
		});
	}
	
	$scope.editPrivateKey = function() {
		$modal.open({
			templateUrl: "ui/servers/tmpl_privatekey.html",
			controller: "privateKeyModalController",
			resolve: {
				"pem": function() { return $scope.server.credentials.pem; }
			}
		})
		.result.then(function(key) {
			$scope.server.credentials.pem = key;
			$scope.form.$setDirty();
		});
	}

	$scope.poolName = function(poolId) {
		if (poolId && poolTypes.length) {
			var p = poolTypes.find(function(p) { return p.id === poolId; });
			if (p)
				return p.name;
		}
		return null;
	}

	var batch = [];
	batch.push(kwateeService.getPlatforms());
	batch.push(kwateeService.getConduitTypes());
	batch.push(kwateeService.getPoolTypes());
	batch.push(kwateeService.getServer($scope.server.name));
	$q.all(batch)
	.then(function(responses) {
		$scope.platforms = responses[0].data;
		$scope.conduitTypes = responses[1].data;
		poolTypes = responses[2].data;
        server = responses[3].data;
        if (!server.properties)
        	server.properties = {};
        if (!server.poolProperties)
        	server.poolProperties = {};
		$scope.server = angular.copy(server);
		authType = (!$scope.isTelnet() && server.credentials && server.credentials.pem) ? "privateKey" : "password";
	    $scope.authType = authType;
 	});
	
}]);

//
// TELNET OPTIONS DIALOG CONTROLLER
//
serversControllers.controller("telnetModalController",
["$scope", "properties",
function($scope, properties) {
	
	$scope.properties = angular.copy(properties);
	$scope.properties.ftpPort = properties.ftpPort || 21;
	
}]);

serversControllers.controller("privateKeyModalController",
["$scope", "pem",
 function($scope, pem) {
	$scope.pem = '';
	$scope.path = '';
	if (pem && pem.indexOf("-----BEGIN") == 0) {
		$scope.pemType = 'text';
		$scope.pem = pem;
	} else if (pem && pem == "*") {
		$scope.pemType = 'text';
		$scope.pem = '';
	} else {
		$scope.pemType = 'path';
		$scope.path = '' || pem;
	}

}]);

//
// TEST CONNECTION MODAL CONTROLLER
//
serversControllers.controller("testModalController",
["$scope", "capabilities",
function($scope, capabilities) {
	$scope.title = capabilities[0];
	$scope.capabilities = capabilities.slice(1);
}]);
