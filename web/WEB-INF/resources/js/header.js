var main = angular.module("main");
main.controller("HeaderController", function($scope, $location, $element, ngDialog, $http){
    Utilities.applyStylesToHtml($element);
});