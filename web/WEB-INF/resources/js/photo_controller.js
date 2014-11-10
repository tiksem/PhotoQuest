var main = angular.module("main");
main.controller("PhotoController", function($scope, ngDialog, $element, $http, $location, $upload){
    var photoId = parseInt(Utilities.parseHashPath($location.hash())[1]);
    $scope.image = window.location.origin + "/image/" + photoId;

    Utilities.loadDataToScope(window.location.origin + "//getCommentsOnPhoto", {
        photoId: photoId
    }, $scope, $http)

    $scope.putComment = function() {
        var message = $scope.message;
        if(message == ""){
            alert("Enter message");
            return;
        }

        var url = window.location.origin + "/putComment";
        var config = {
            params: {
                photoId: photoId,
                message: message
            }
        }
        $http.get(url, config).success(function(data){
            if(!data.error){
                var comments = $scope.comments = $scope.comments || [];
                comments.push(data);
            } else {
                console.error(data);
            }
        });
    };

    Utilities.applyStylesToHtml($element);
})

