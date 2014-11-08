var main = angular.module("main");
main.controller("RegisterController", function($scope, $element, $http, $upload){
    $scope.onFileSelect = function($files) {
        $scope.avatar = $files[0];
    };

    $scope.register = function(){
        var data = {
            login: $scope.login,
            password: $scope.password,
            name: $scope.name,
            lastName: $scope.lastName
        };

        var url = window.location.origin + "//register";
        $scope.upload = $upload.upload({
            url: url,
            method: 'POST',
            file: $scope.avatar,
            data: data
        }).progress(function (evt) {
            console.log('percent: ' + parseInt(100.0 * evt.loaded / evt.total));
        }).success(function (data, status, headers, config) {
            if (!data.error) {
                alert("Success");
            } else {
                alert(data.error + " " + data.message)
        }
        }).error(function(data){
            console.error(data);
        });
    };

    Utilities.applyStylesToHtml($element);
});

