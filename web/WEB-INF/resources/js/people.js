var main = angular.module("main");
main.controller("PeopleController", function($scope, $location, $element, ngDialog, $http){
    $scope.openProfile = function(user) {

    };

    $scope.addOrRemoveFriend = function(user) {
        var config = {
            params: {
                id: user.id
            }
        };
        var url = window.location.origin + (!user.isFriend ? "/addFriend" : "/removeFriend");
        $http.get(url, config).success(function(data){
            if(!data.error){
                user.isFriend = !user.isFriend;
            } else {
                console.error(data);
            }
        });
    };

    var url = window.location.origin;
    var requestType = Utilities.parseHashPath($location.hash())[0];
    if(requestType == "friends"){
        url += "//friends";
    } else {
        url += "//users"
    }

    Utilities.loadDataToScope(url, {}, $scope, $http)

    Utilities.applyStylesToHtml($element);
});