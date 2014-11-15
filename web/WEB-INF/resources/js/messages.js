var main = angular.module("main");
main.controller("MessagesController", function($scope, $location, $element, ngDialog, $http){
    var updateSignedInStatus = function() {
        if(!$scope.getSignedInUser()){
            $location.hash("quests");
            return false;
        }

        return true;
    };

    if(!updateSignedInStatus()) {
        return;
    }

    $scope.$watch(
        function($scope) {
            return $scope.getSignedInUser();
        },
        updateSignedInStatus
    );


    var userId = parseInt(Utilities.parseHashPath($location.hash())[1]);

    $scope.getMessageAvatar = function(message) {
        var signedIn = $scope.getSignedInUser();
        var user = $scope.user;
        if(message.fromUserId === user.id){
            return user.avatar;
        } else {
            return signedIn.avatar;
        }
    };

    $scope.getMessageTitle = function(message) {
        var signedIn = $scope.getSignedInUser();
        var user = $scope.user;
        if(message.fromUserId !== user.id){
            user = signedIn;
        }

        return user.name + " " + user.lastName;
    };

    $scope.sendMessage = function() {
        var text = $scope.messageText;
        var user = $scope.user;

        var config = {
            params: {
                toUserId: user.id,
                message: text
            }
        }

        var url = window.location.origin + "//sendMessage"
        $http.get(url, config).success(function(data) {
            if(!data.error){
                $scope.messages.push(data);
            } else {
                console.error(data.error);
            }
        })
    };

    Utilities.loadDataToScope(window.location.origin + "//messages", {
        userId: userId
    }, $scope, $http);

    Utilities.applyStylesToHtml($element);

    $($element).find("#messages_container").perfectScrollbar();
});