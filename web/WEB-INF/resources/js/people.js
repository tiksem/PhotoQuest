var main = angular.module("main");
main.controller("PeopleController", function($scope, $location, $element, ngDialog, $http){
    $scope.openProfile = function(user) {

    };

    $scope.addOrRemoveFriend = function(user) {
        var config = {
            params: {
                name: $scope.createQuestName
            }
        };
        var url = window.location.origin + "/createPhotoquest";
        $http.get(url, config).success(function(){
            $scope.closeThisDialog(null);
        });
    };

    Utilities.loadDataToScope(window.location.origin + "//users", {}, $scope, $http)

    Utilities.applyStylesToHtml($element);
});