var main = angular.module("main");
main.controller("PeopleController", function($scope, $location, $element, ngDialog, $http){
    $scope.openProfile = function(user) {
        $location.hash("profile_" + user.id);
    };

    $scope.addOrRemoveFriend = function(user) {
        var config = {
            params: {
                id: user.id
            }
        };

        var removeFriend = user.relation == "friend" || user.relation == "request_sent";
        var url = window.location.origin +
            (removeFriend ? "/removeFriend" : "/addFriend");

        $http.get(url, config).success(function(data){
            if(!data.error){
                if(removeFriend){
                    delete user.relation;
                } else {
                    if(user.relation == "request_received"){
                        user.relation = "friend";
                    } else {
                        user.relation = "request_sent";
                    }
                }
            } else {
                console.error(data);
            }
        });
    };

    $scope.getToggleFriendStatusButtonName = function(user) {
        var relation = user.relation;
        if(relation == "friend"){
            return "Remove friend";
        } else if(relation == "request_sent") {
            return "Cancel friend request"
        } else if(relation == "request_received") {
            return "Accept friend request"
        } else {
            return "Add friend";
        }
    };

    $scope.writeMessage = function(user) {
        $location.hash("messages_" + user.id);
    }

    var loadData = function() {
        var url = window.location.origin;
        var requestType = Utilities.parseHashPath($location.hash())[0];
        if(requestType == "friends"){
            url += "//friends";
        } else {
            url += "//users"
        }

        Utilities.loadDataToScope(url, {}, $scope, $http);
    };

    // reload data, when user sign out/sign in
    $scope.$watch(
        function($scope) {
            return $scope.getSignedInUser();
        },
        loadData
    );

    loadData();
    Utilities.applyStylesToHtml($element);
});