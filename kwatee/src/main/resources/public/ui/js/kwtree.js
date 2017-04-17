var myApp = angular.module('kwTree', []);

myApp.directive('kwTree', function() {
	return {
		template: '<ul class="kwTree"><kw-tree-node ng-repeat="node in tree"></kw-tree-node></ul>',
		replace: true,
		transclude: true,
		restrict: 'E',
		scope: {
			tree: '=ngModel',
			loadFn: '&',
			selectId: '='
		},
		controller: function($scope, $element, $attrs) {
			// this seems like an egregious hack, but it is necessary for recursively-generated
			// trees to have access to the loader function
			if ($scope.$parent.loadFn)
				$scope.loadFn = $scope.$parent.loadFn;
		}
	};
})
.directive('kwTreeNode', ['$compile', '$timeout', function($compile, $timeout) {
	return {
		restrict: 'E',
		replace: true,
		template: '<li>' +
				'<div class="node" data-node-id="{{ nodeId() }}" ng-switch="!node.dir">' +
					'<span ng-switch-when="true"><div ng-class="fileCss()" style="margin-left:20px" /></span>' +
					'<span ng-switch-when="false"><div class="icon only folderOpenIcon" ng-click="toggleNode(nodeId())" /><div class="icon only folderIcon" /></span>' +
					'<span ng-class="css()" ng-click="setSelected(node)">{{ node.name }}</span>' +
				'</div>' +
				'</li>',
		link: function(scope, elm, attrs) {
			scope.toggleNode = function(nodeId) {
				var isVisible = elm.children(".kwTree:visible").length > 0;
				var childrenTree = elm.children(".kwTree");
				var e = elm.find("div.icon:first-child");
				if (!isVisible && childrenTree.length === 0) {
					// load the children asynchronously
					var callback = function(resp) {
						scope.node.children = resp.data;
						scope.appendChildren();
						elm.find("img").remove();
						e.show();
						scope.toggleNode(); // show it
					};
					var promiseOrNodes = scope.loadFn()(scope.nodePath(scope.node), callback);
					if (promiseOrNodes && promiseOrNodes.then) {
						promiseOrNodes.then(callback, function(resp) {
							alert("Error: " + resp.status)
						});
					} else {
						$timeout(function() {
							callback(promiseOrNodes);
						}, 100);
					}
					e.parent().prepend('<img src="/img/loading.gif"');
					e.hide();
				} else {
					childrenTree.toggle(!isVisible);
					e.toggleClass("folderOpenIcon");
					e.toggleClass("folderClosedIcon");
				}
			};

			scope.appendChildren = function() {
				// Add children by $compiling and doing a new kw-tree directive
				var childrenHtml = '<kw-tree ng-model="node.children"';
				if (scope.selectId)
					childrenHtml += ' select-id="selectId"';
				childrenHtml += ' style="display: none"></kw-tree>';
				return elm.append($compile(childrenHtml)(scope));
			};

			scope.nodeId = function(node) {
				var localNode = node || scope.node;
				return localNode.id;
			};

			scope.css = function() {
				return {
					"nodeLabel": true,
					"selected": scope.selectId && scope.nodeId() === scope.selectId
				};
			};
			
			scope.fileCss = function() {
				var css = {
					"icon": true,
					"only": true
				};
				if (scope.node.layer || scope.node.hasVariables || (scope.node.properties && ! scope.node.properties.pristine)) {
					if (scope.node.layer)
						css.fileOverlayIcon = true;
					else
						css.fileSpecialIcon = true;
				} else
					css.fileIcon = true;
				return css;
			};
			
			scope.fileInfo = function() {
				scope.$emit("fileInfo", scope.node, scope.nodePath(scope.node));
			};
			
			scope.nodePath = function(node) {
				var path = "/" + node.name + (node.dir ? "/" : "");
				var s = scope.$parent;
				while (s && (s.node || s.tree)) {
					if (s.node)
						path = "/" + s.node.name + path;
					s = s.$parent;
				}
				return path.substring(1);
			};
			
			// emit an event up the scope.  Then, from the scope above this tree, a "selectNode"
			// event is expected to be broadcasted downwards to each node in the tree.
			scope.setSelected = function(node) {
				if (node.id)
					scope.$emit("nodeSelected", node, scope.nodePath(node));
			};
			scope.$on("selectNode", function(event, node) {
				scope.selectId = scope.nodeId(node);
			});
		}
	};
}]);