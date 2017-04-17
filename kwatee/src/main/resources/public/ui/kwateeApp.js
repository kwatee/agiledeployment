"use strict";

// Declare app level module which depends on filters, and services
var kwateeApp = angular.module("kwatee", [
        "ngCookies",
		"ngRoute",
        "ng-breadcrumbs",
        "ngStorage",
        "ui.bootstrap",
        "kwateeApi",
        "kwTree",
		"artifacts",
		"servers",
		"environments",
		"deployments",
		"admin"
]);


kwateeApp.config(["$routeProvider", function($routeProvider) {
	$routeProvider
		.when("/login", {
			templateUrl: "ui/tmpl_login.html",
			controller: "loginController",
			label: "Log in"
		})
		.when("/db/create", {
			templateUrl: "ui/tmpl_db_create.html",
			controller: "dbCreateController",
			label: "Create DB"
		})
		.when("/db/upgrade", {
			templateUrl: "ui/tmpl_db_upgrade.html",
			controller: "dbUpgradeController",
			label: "Upgrade DB"
		})
		.when("/logout", {
			templateUrl: "ui/tmpl_login.html",
			controller: "loginController",
			label: "Log out"
		})
		.otherwise({redirectTo: "/artifacts"});
	
	// Polyfill
	if (!Array.prototype.find) {
		Array.prototype.find = function(predicate) {
			if (this == null) {
				throw new TypeError('Array.prototype.find called on null or undefined');
			}
			if (typeof predicate !== 'function') {
				throw new TypeError('predicate must be a function');
			}
			var list = Object(this);
			var length = list.length >>> 0;
			var thisArg = arguments[1];
			for (var i = 0; i < length; i++) {
				var value = list[i];
				if (predicate.call(thisArg, value, i, list)) {
					return value;
				}
			}
			return undefined;
		};
	}
	if (!Array.prototype.find) {
		Array.prototype.filter = function(predicate) {
			var list = Object(this);
			var length = list.length >>> 0;
			var thisArg = arguments[1];
			var value;
			var res = [];
			for (var i = 0; i < length; i++) {
				var value = list[i];
				if (predicate.call(thisArg, value, i, list)) {
					res.push(value);
				}
			}
			return res;
		}
	}
	if (!Array.prototype.findIndex) {
		Array.prototype.findIndex = function(predicate) {
			if (this === null)
				throw new TypeError('Array.prototype.findIndex called on null or undefined');
			if (typeof predicate !== 'function')
				throw new TypeError('predicate must be a function');
			var list = Object(this);
			var length = list.length >>> 0;
			var thisArg = arguments[1];
			for (var i = 0; i < length; i++) {
				if (predicate.call(thisArg, list[i], i, list))
					return i;
			}
			return -1;
		};
	}
	if (!String.prototype.contains) {
		String.prototype.contains = function() {
			return String.prototype.indexOf.apply(this, arguments) != -1;
		};
	}

}]);

kwateeApp.directive("toolbarButton",
function() {
	return {
		// can be used as attribute or element
		restrict: "E",
		scope: {
			id: "@"
		},
		replace: true,
		template: "<button type='button' class='btn btn-default btn-xs'><div class='icon {{id}}Icon'></div></button>"
	}
})

.directive("fileModel",
["$parse",
function($parse) {
    return {
        restrict: "A",
        link: function(scope, element, attrs) {
            var model = $parse(attrs.fileModel);
            var modelSetter = model.assign;
            element.bind("change", function() {
                scope.$apply(function() {  modelSetter(scope, element[0].files[0]); });
            });
        }
    }
}])

.directive("autoFocus",
["$timeout",
function($timeout) {
    return {
        restrict: "AC",
        link: function(_scope, _element) {
            $timeout(function() { _element[0].focus(); }, 20);
        }
    }
}])

.directive("navMenu",
["$location",
function($location) {
	return function(scope, element, attrs) {
		var links = element.find("a");
		var urlMap = {};
		var subMenuOf = attrs.navMenu;
		var currentLink;

		for (var i = 0; i < links.length; i++) {
			var link = angular.element(links[i]);
			var url = link.attr("href");
			if (!$location.$$html5)
				url = url.replace(/^#[^/]*/, "");
			if (subMenuOf)
				url = url.substring(subMenuOf.length);
			var idx = url.substring(1).indexOf("/");
			if (idx > 0)
				url = url.substring(0, idx+1);

			urlMap[url] = link;
		}

		scope.$on("$routeChangeStart", function(event, newUrl, oldUrl) {
			var path = $location.path();
			if (!subMenuOf || path.indexOf(subMenuOf) == 1) {
				if (subMenuOf)
					path = path.substring(subMenuOf.length);
				var idx = path.substring(1).indexOf("/");
				if (idx > 0)
					path = path.substring(0, idx+1);
				var pathLink = urlMap[path];
				if (pathLink) {
					if (currentLink) {
						// When moving away from a tab, preserve the previous
						// link
						var oldPath = oldUrl ? oldUrl.originalPath : null;
						if (oldPath) {
							for (var p in oldUrl.pathParams)
								oldPath = oldPath.replace(":"+p, oldUrl.pathParams[p]);
							currentLink.attr("href", "#"+oldPath);
						}
					}
					if (currentLink)
						currentLink.parent().removeClass("active");
					currentLink = pathLink;
					currentLink.parent().addClass("active");
				}
			}
		});
	}
}])

.directive("fileChooser",
function($modal) {
	return {
		restrict: "A",
		require: '?ngModel',
		link: function (scope, element, attrs, ngModel) {
			if (!ngModel) return;
			var action = attrs.fileAction;
			var showFile = null;
			if (attrs.hasOwnProperty("fileShow")) {
				showFile = angular.element("<small class='text-warning' style='margin-left:10px'></small>");
				element.after(showFile);
			}
			var chooser = angular.element("<input type='file' style='display:none'></input>");
			chooser.bind("change", function(event) {
				if (showFile)
					showFile.text(chooser.val());
				scope.$apply(function() {
					ngModel.$setViewValue(event.target.files[0]);
					if (action)
						scope.$eval(action);
				});
			});
			element.bind("click", function(event) {
				if (event.shiftKey) {
					var path = window.prompt("Please enter a path or url", "");
					if (path) {
						path = path.replace("\\", "/");
						if (showFile) {
							var idx = path.lastIndexOf("/");
							var urlName = idx < 0 ? path : path.substring(idx+1);
							showFile.text(urlName);
						}
						scope.$apply(function() {
							ngModel.$setViewValue(path);
						});
						if (action)
							scope.$eval(action);
					}
				} else {
					chooser.click();
				}
			});
			element.after(chooser);
			ngModel.$render = function() {
				if (showFile)
					showFile.text("");
			};
		}
	}
})

.directive("dynamicName",
function($compile, $parse) {
	return {
		restrict: "A",
		terminal: true,
		priority: 100000,
		link: function(scope, elem) {
			var name = $parse(elem.attr("dynamic-name"))(scope);
			elem.removeAttr("dynamic-name");
			elem.attr("name", name);
			$compile(elem)(scope);
		}
	}
});

/* Controllers */

kwateeApp.controller("kwateeController",
["$scope", "$rootScope", "$location", "$modal", "breadcrumbs", "$localStorage", "kwateeJsonService",
function($scope, $rootScope, $location, $modal, breadcrumbs, $localStorage, kwateeJsonService) {
	if (!$localStorage.$kwateePrefs)
		$localStorage.$kwateePrefs = { hideDisabled: true };
	$scope.$prefs = $localStorage.$kwateePrefs;
	$scope.readOnly = false;
	$scope.breadcrumbs = breadcrumbs;
    $scope.go = function(path) {
    	if (arguments.length > 1)
	    	$location.path(path).search(arguments[1]);
	    else
	    	$location.path(path);
    }
    
    $scope.clearLastKwateeError = function() {
        delete $rootScope.lastKwateeError;
    }
    
	$scope.arrayToList = function(arr) {
		if (arr instanceof Array && arr.length > 0)
			return arr.join(", ");
		return null;
	}

	$scope.ask = function(title, what, value) {
		return $modal.open({
			templateUrl: "ui/tmpl_askmodal.html",
			windowClass: "defaultmodal",
			controller: "askModalController",
			resolve: {
				"ask": function() { return { "title": title, "what": what, "value": value }; }
			}
		});
	}

	$scope.askMultiline = function(title, what, value) {
		return $modal.open({
			templateUrl: "ui/tmpl_askmultilinemodal.html",
			windowClass: "defaultmodal",
			controller: "askModalController",
			resolve: {
				"ask": function() { return { "title": title, "what": what, "value": value }; }
			}
		});
	}

	$scope.confirm = function(question) {
		return $modal.open({
			templateUrl: "ui/tmpl_confirmmodal.html",
			windowClass: "defaultmodal",
			controller: "confirmModalController",
			resolve: {
				"question": function() { return question; }
			}
		});
	}

	$scope.overlayModal = function(path, shiftKey) {
		return $modal.open({
			templateUrl: "ui/tmpl_overlaymodal.html",
			windowClass: "defaultmodal",
			controller: "overlayModalController",
			resolve: {
				"path": function() { return path; },
				"shiftKey": function() { return shiftKey; }
			}
		});
	}

	$scope.uploadModal = function(title) {
		return $modal.open({
			templateUrl: "ui/tmpl_uploadmodal.html",
			windowClass: "defaultmodal",
			controller: "uploadModalController",
			resolve: {
				"title": function() { return title; }
			}
		});
	}

	$scope.changeLocation = function(event, next, current) {
		if (event.currentScope.form && event.currentScope.form.$dirty) {
			var answer = confirm("Are you sure you want to leave this page without saving changes?");
			if (!answer) {
				event.preventDefault();
				return false;
			}
			event.currentScope.form.$setPristine();
		}
		return true;
    }

    $scope.isDirty = function(form) {
		if (form.$dirty) {
			alert("You must save changes first");
			return true;
		}
    	return false;
    };
   
    $scope.isAdmin = function() {
    	return $location.path().substring(0, 6) === "/admin";
    };

	$scope.enabledObject = function() {
		return function(o) { return !$scope.$prefs.hideDisabled || !o.disabled; };
	};

	$(document).bind('keypress', function(e) {
		if (e.ctrlKey || e.metaKey) {
			if (e.which == 115) {
				e.preventDefault();
				$rootScope.$broadcast('saveEvent');
				return false;
			}
			if (e.which == 22) {
				e.preventDefault();
				$rootScope.$broadcast('variableEvent');
				return false;
			}
		}
	    return true;
	}); 

    kwateeJsonService.getContext()
    .then(function(response) {
    	$rootScope.kwateeInfo = response.data;
    });

}]);

kwateeApp.controller("loginController",
["$scope", "$rootScope", "$location", "$cacheFactory", "kwateeService",
function($scope, $rootScope, $location, $cacheFactory, kwateeService) {
	if ($location.path() === "/logout") {
		kwateeService.logout();
	}
	var kwateeCache = $cacheFactory.get("kwateeCache");
	if (kwateeCache)
		kwateeCache.removeAll();
	$scope.showForm = false;
    kwateeService.getContext()
    .then(function() {
    		$location.path("/");
    	}, function(rejection) {
    		if (rejection.status != 410 && rejection.status != 503) {
    			$rootScope.kwateeInfo = {};
    			if (rejection.status != 401)
    				$scope.error = "Error " + rejection.status;
    			$scope.showForm = true;
    		}
    	}
    );
	$scope.doLogin = function() {
		delete $scope.error;
		var userName = angular.element.find("#userName")[0].value; 
		var password = angular.element.find("#pwd")[0].value; 
		console.log("login");
		kwateeService.login(userName, password)
		.then(
			function(response) {
				$rootScope.kwateeInfo = response.data;
				console.log("loc: "+$rootScope.originalLocation);
				if ($rootScope.originalLocation)
					$location.path($rootScope.originalLocation);
				else
					$location.path("/artifacts");
			},
			function(rejection) {
				$scope.error = "Login failed";
			}
		);
	}
}]);

kwateeApp.controller("dbCreateController",
["$scope", "$location", "kwateeService",
function($scope, $location, kwateeService) {
	$scope.dba_user = "??";
	$scope.database = "??";
	$scope.createDB = function(dbaPassword) {
		kwateeService.createDB(dbaPassword)
		.then(
			function() {
				$location.path("/login");
			},
			function(rejection) {
				if (rejection.status == 401)
					$scope.error = "Incorrect password";
				else
					$scope.error = "Database creation failed (see logs for details)";			
			}
		);
	}
	kwateeService.getDBInfo()
	.then(function(response) {
		if (response.data.schemaVersion)
			$location.path("/");
		else {
			$scope.dba_user = response.data.jdbcUserName;
			$scope.database = response.data.jdbcUrl;
		}
	});
}]);

kwateeApp.controller("dbUpgradeController",
["$scope", "$location", "kwateeService",
function($scope, $location, kwateeService) {
	$scope.dba_user = "??";
	$scope.database = "??";
	$scope.current_schema_version = "??";
	$scope.required_schema_version = "??";
	$scope.upgradeDB = function(dbaPassword) {
		kwateeService.upgradeDB(dbaPassword)
		.then(
			function() {
				$location.path("/login");
			},
			function(rejection) {
				if (rejection.status == 401)
					$scope.error = "Incorrect password";
				else
					$scope.error = "Database upgrade failed (see logs for details)";			
			}
		);
	}
	kwateeService.getDBInfo()
	.then(function(response) {
		if (response.data.requiredSchemaVersion == response.data.schemaVersion)
			$location.path("/");
		else {
			$scope.dba_user = response.data.jdbcUserName;
			$scope.database = response.data.jdbcUrl;
			$scope.current_schema_version = response.data.schemaVersion;
			$scope.required_schema_version = response.data.requiredSchemaVersion;
		}
	});
}]);

kwateeApp.controller("askModalController",
["$scope", "ask",
function($scope, ask) {
	$scope.ask = ask;
}]);

kwateeApp.controller("confirmModalController",
["$scope", "question",
function($scope, question) {
	$scope.question = question;
}]);

kwateeApp.controller("overlayModalController",
["$scope", "$timeout", "path", "shiftKey",
function($scope, $timeout, path, shiftKey) {
	$scope.path = path;
	$timeout(function() {
		var file = angular.element(document.querySelector('#file'));
		var e = jQuery.Event("click", {"shiftKey": shiftKey});
		file.trigger(e);
	}, 50);
}]);

kwateeApp.controller("uploadModalController",
["$scope", "$timeout", "title",
function($scope, $timeout, title){
    $scope.title = title;
	$timeout(function() {
		var file = angular.element(document.querySelector('#file'));
		var e = jQuery.Event("click", {"shiftKey": false});
		file.trigger(e);
	}, 50);
}]);

/* Services */

kwateeApp.factory("kwateeService",
["$q", "$cacheFactory", "$location", "$rootScope", "kwateeJsonService",
function($q, $cacheFactory, $location, $rootScope, kwateeJsonService) {

	var kwateeCache = $cacheFactory("kwateeCache");
	var cacheable = function(httpPromise) {
		var groupKey = arguments.length > 1 ? arguments[1] : null;
		var clearKey = arguments.length > 2 ? arguments[2] : null;
		var defered = $q.defer();
		var cache = null;
		if (!clearKey && groupKey) {
			cache = kwateeCache.get(groupKey);
			if (cache)
				defered.resolve(cache);
		}
		if (!cache) {
			httpPromise.then(function(response) {
				if (clearKey)
					kwateeCache.remove(groupKey);
				else if (groupKey)
					kwateeCache.put(groupKey, response);
				defered.resolve(response);
			}, function(rejection) {
				defered.reject(rejection);
			});
		}
		return defered.promise;
	}
	
	var service = {}
	for (var attr in kwateeJsonService)
		service[attr] = kwateeJsonService[attr];
	service.getArtifacts = function() { return cacheable(kwateeJsonService.getArtifacts(), "artifacts"); }
	service.createArtifact = function(artifactName, artifactOptions) { return cacheable(kwateeJsonService.createArtifact(artifactName, artifactOptions), "artifacts", "clear"); }
	service.uploadArtifacts = function(artifactsFile) { return cacheable(kwateeJsonService.uploadArtifacts(artifactsFile), "artifacts", "clear"); }
	service.updateArtifact = function(artifactName, artifactOptions) { return cacheable(kwateeJsonService.updateArtifact(artifactName, artifactOptions), "artifacts", "clear");}
	service.deleteArtifact = function(artifactName) { return cacheable(kwateeJsonService.deleteArtifact(artifactName), "artifacts", "clear"); }
	service.getServers = function() { return cacheable(kwateeJsonService.getServers(), "servers"); }
	service.updateServer = function(serverName, serverOptions) { return cacheable(kwateeJsonService.updateServer(serverName, serverOptions), "servers", "clear"); }
	service.createServer = function(serverName, serverOptions) { return cacheable(kwateeJsonService.createServer(serverName, serverOptions), "servers", "clear"); }
	service.duplicateServer = function(serverName, duplicateName, serverOptions) { return cacheable(kwateeJsonService.duplicateServer(serverName, duplicateName, serverOptions), "servers", "clear"); }
	service.deleteServer = function(serverName) { return cacheable(kwateeJsonService.deleteServer(serverName), "servers", "clear"); }
	service.getPlatforms = function() { return cacheable(kwateeJsonService.getPlatforms(), "platforms"); }
	service.getConduitTypes = function() { return cacheable(kwateeJsonService.getConduitTypes(), "conduits"); }
	service.getPoolTypes = function() { return cacheable(kwateeJsonService.getPoolTypes(), "poolTypes"); }
	service.getEnvironments = function() { return cacheable(kwateeJsonService.getEnvironments(), "environments"); }
	service.createEnvironment = function(environmentName, environmentOptions) { return cacheable(kwateeJsonService.createEnvironment(environmentName, environmentOptions), "environments", "clear"); }
	service.duplicateEnvironment = function(environmentName, duplicateName, environmentOptions) { return cacheable(kwateeJsonService.duplicateEnvironment(environmentName, duplicateName, environmentOptions), "environments", "clear"); }
	service.updateEnvironment = function(environmentName, environmentOptions) { return cacheable(kwateeJsonService.updateEnvironment(environmentName, environmentOptions), "environments", "clear"); }
	service.deleteEnvironment = function(environmentName) { return cacheable(kwateeJsonService.deleteEnvironment(environmentName), "environments", "clear"); }
	service.getUsers = function() { return cacheable(kwateeJsonService.getUsers(), "users"); }
	service.createUser = function(userName, userOptions) { return cacheable(kwateeJsonService.createUser(userName, userOptions), "users", "clear"); }
	service.updateUser = function(userName, userOptions) { return cacheable(kwateeJsonService.updateUser(userName, userOptions), "users", "clear"); }
	service.deleteUser = function(userName) { return cacheable(kwateeJsonService.deleteUser(userName), "users", "clear"); }
	service.getGlobalVariables = function() { return cacheable(kwateeJsonService.getGlobalVariables(), "variables"); }
	service.updateGlobalVariables = function(variables) { return cacheable(kwateeJsonService.updateGlobalVariables(variables), "variables", "clear"); }
	service.getParameters = function() { return cacheable(kwateeJsonService.getParameters(), "parameters"); }
	service.updateParameters = function(parameters) { return cacheable(kwateeJsonService.updateParameters(parameters), "parameters", "clear"); }

	return service;
}]);

kwateeApp.factory('APIInterceptor', ['$q','$rootScope', '$location', '$cookies', function($q, $rootScope, $location, $cookies) {
	$rootScope.loading = 0;
    return {
    	"request": function(config) {
    		if (config.url.indexOf("api/") == 0) {
    			delete $rootScope.lastKwateeError;
    			if (!config.headers)
    				config.headers = {};
    			if (!config.headers.hasOwnProperty('Content-Type'))
    				config.headers['Content-Type'] = 'application/json';
    			var token = $cookies.get('api-token');
    			if (token && token != "null")
    				config.headers['X-API-AUTH'] = token;
    			$rootScope.loading ++;
    		}
    		return config;
    	},
    	"response": function(response) {
    		if (response.config.url.indexOf("api/") == 0) {
    			$rootScope.loading --;
    			var token = response.headers()['x-api-auth'];
    			if (!token || token == "null")
    				$cookies.remove('api-token', {path: '/'});
    			else
    				$cookies.put('api-token', token, {path: '/'});
    		}
			return response;
    	},
    	"requestError": function(config) {
    		if (config.url.indexOf("api/") == 0) {
    			$rootScope.lastKwateeError = "Request failed";
    			$rootScope.loading --;
    		}
			return $q.reject(rejection);
    	},
    	"responseError": function(rejection) {
    		if (rejection.config.url.indexOf("api/") == 0) {
				$rootScope.loading --;
				if (!rejection || !rejection.status || rejection.status == 0) {
					console.log("Kwatee server not responding");
				} else if (rejection.status == 401) {
					$cookies.remove('api-token', {path: '/'});
					var here = $location.path();
					if (here === "/login" || here === "/logout") {
					} else {
						$rootScope.originalLocation = here;
						$location.path("/login");
					}
				} else if (rejection.status == 403) {
					alert("Access denied");
					$location.path("/");
				} else if (rejection.status == 406) {
					var violations = rejection.data.violations;
					var msg = "Validation error(s). ";
					for (var v = 0; v < violations.length; v ++ ) {
						var violation = violations[v];
						msg += violation.field+ ": " + violation.message;
					}
					$rootScope.lastKwateeError = msg;
				} else if (rejection.status == 420) {
					$rootScope.lastKwateeError = rejection.data;
				} else if (rejection.status == 410) {
					$location.path("/db/create");
				} else if (rejection.status == 426) {
					$location.path("/db/upgrade");
				} else {
					console.log(rejection);
				}
    		}
    		else
    			console.log("non api rejection");
	        return $q.reject(rejection);
        }
    };
     
}]);
