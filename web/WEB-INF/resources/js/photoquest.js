var main = angular.module("main");
main.controller("PhotoQuest", function($scope, $element, $http, $location){
    $scope.contentLoaded = false;

    var questId = parseInt(Utilities.parseHashPath($location.hash())[1]);
    var scope = $scope.quest = {};
    var url = window.location.origin + "//getPhotoquestById";
    var params = {
        id: questId
    }
    Utilities.loadDataToScope(url, params, scope, $http, function(){
        $scope.contentLoaded = true;
    });
    Utilities.applyStylesToHtml($element);
})
