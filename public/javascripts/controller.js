"use strict";

function MasterCtrl($scope, $http) {

    $scope.pingData = {}


    $scope.socket = new WebSocket('ws://localhost:10000/ws');
    $scope.socket.onopen = function (event) {
        console.log('Hello, WebSocket')
    };

    $scope.socket.onmessage = function (event) {
        console.log(event.data)
        $scope.$apply(function () {
            var wsData = JSON.parse(event.data)
            $scope.pingData["\""+wsData.id +"\""] = wsData.state
            console.log($scope.pingData)
        })
    }

    $scope.socket.onclose = function (event) {
        console.log('closed');
    }

    $scope.getStyle = function(style) {
        return style
    }
}