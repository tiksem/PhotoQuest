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

    var scope = $scope.photo = {};
    var url = window.location.origin + "//getPhotoById";
    var params = {
        id: photoId
    };

    Utilities.loadDataToScope(url, params, scope, $http, function(){
        var unlike = function(item) {
            var url = window.location.origin + "//unlike";
            var config = {
                params: {
                    id: item.yourLike.id
                }
            };
            $http.get(url, config).success(function (data) {
                if (!data.error) {
                    item.yourLike = null;
                    item.likesCount--;
                    console.log(data);
                } else {
                    console.error(data);
                }
            })
        }

        var like = function(item, params) {
            var url = window.location.origin + "//like";
            var config = {
                params: params
            };
            $http.get(url, config).success(function (data) {
                if (!data.error) {
                    item.yourLike = data;
                    item.likesCount++;
                    console.log(data);
                } else {
                    console.error(data);
                }
            })
        };

        var toggleLikeState = function(item, params) {
            if (item.yourLike) {
                unlike(item);
            } else {
                like(item, params);
            }
        };

        $scope.likePhoto = function() {
            var params = {
                photoId: photoId
            };

            toggleLikeState($scope.photo, params);
        };

        $scope.likeComment = function(comment) {
            var params = {
                commentId: comment.id
            };

            toggleLikeState(comment, params);
        }
    });

    Utilities.applyStylesToHtml($element);
})

