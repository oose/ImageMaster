"use strict";

function MasterCtrl($scope, $http, $window) {
	
	$scope.pingData = {}

    $scope.getStyle = function(style) {
        return style
    }

	// hack to obtain relative ws url
	var wsUrl = window.location.href.replace(/^http(s?:\/\/.*)\/.*$/, 'ws$1/ws')
	
    // configure WebSocket
    $scope.socket = new WebSocket(wsUrl);
    $scope.socket.onopen = function (event) {
        console.log('opened websocket')
    };

    $scope.socket.onclose = function (event) {
        console.log('closed websocket')
    };
    
    // close the websocket before the page is unloaded
    $window.onbeforeunload = function() {
    	$scope.socket.close()
    }
    
    $scope.socket.onmessage = function (event) {
        $scope.$apply(function () {
            var wsData = JSON.parse(event.data)
            $scope.pingData["\""+wsData.id +"\""] = wsData.state
            console.log($scope.pingData)
        })
    }

    $scope.socket.onclose = function (event) {
        console.log('closed websocket');
    }
}