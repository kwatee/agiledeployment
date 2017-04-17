/**
 * ng-breadcrumb.js - v0.1.0 - A better AngularJS service to help with breadcrumb-style navigation between views
 * Based on https://github.com/angular-app/angular-app/blob/master/client/src/common/services/breadcrumbs.js
 *
 * @author Ian Kennington Walter (http://ianvonwalter.com)
 */

/* global angular */
angular.module('ng-breadcrumbs', [])
.factory('breadcrumbs', ['$rootScope', '$location', '$route', function ($rootScope, $location, $route) {
	var BreadcrumbService = {
		breadcrumbs: [],
		get: function(options) {
			this.options = options || this.options;
			if (this.options) {
				for (var key in this.options) {
					if (this.options.hasOwnProperty(key)) {
						for (var index in this.breadcrumbs) {
							if (this.breadcrumbs.hasOwnProperty(index)) {
								var breadcrumb = this.breadcrumbs[index];
								if (breadcrumb.label === key) {
									breadcrumb.label = this.options[key];
								}
							}
						}
					}
				}
			}
			return this.breadcrumbs;
		},
		generateBreadcrumbs: function() {
			var routes = $route.routes;
			var pathElements = $location.path().split('/');
            var path = '';
            var routePath = '';
            var self = this;
            var param;
            var label;

            if (pathElements[1] === '')
            	delete pathElements[1];

            var paramIdx = 1;
            this.breadcrumbs = [];
            angular.forEach(pathElements, function(el) {
            	path += path === '/' ? el : '/' + el;
        		param = ":param"+paramIdx;
        		routePath += routePath === '/' ? param : '/' + param;
            	if (routes[routePath]) {
        			paramIdx += 1;
            		if (routes[routePath].label == "")
            			label = null;
            		else
            			label = el;
            	} else {
            		routePath = routePath.replace(param, el);
            		label = el;
            		if (routes[routePath] && routes[routePath].hasOwnProperty('label'))
            			label = routes[routePath].label;
            	}
            	if (label)
            		self.breadcrumbs.push({ label: label, path: path });
            });
		}
	};

	// We want to update breadcrumbs only when a route is actually changed
	// as $location.path() will get updated immediately (even if route change fails!)
	$rootScope.$on('$routeChangeSuccess', function() {
		BreadcrumbService.generateBreadcrumbs();
	});

	BreadcrumbService.generateBreadcrumbs();

	return BreadcrumbService;
}]);