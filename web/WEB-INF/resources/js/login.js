var main = angular.module("main");
main.controller("LoginController", function($rootScope, $scope, ngDialog, $element, $http, $cookies){
    $scope.isSignin = false;

    var signin = function(login, password){
        $scope.avatar = "//:0";
        if(login == ""){
            alert("Enter login!");
            return;
        }

        if(password == ""){
            alert("Enter password");
            return;
        }

        var config = {
            params: {
                login: login,
                password: password
            }
        };
        $http.get(window.location.origin + "//login",config).success(function(data){
            if (!data.error) {
                $scope.setSignedInUser(data);
                $scope.avatar = data.avatar;
                alert("Success!");
            } else {
                var message = data.error + " " + data.message;
                alert(message);
                console.error(message);
            }
        })
    };

    $scope.signin = function() {
        var login = $scope.login;
        var password = $scope.password;
        signin(login, password);
    };

    $scope.signout = function() {
        $http.get(window.location.origin + "//logout").success(function(data){
            if (!data.error) {
                $scope.setSignedInUser(null);
                alert("Success!");
            } else {
                var message = data.error + " " + data.message;
                alert(message);
                console.error(message);
            }
        })
    };

    var login = $cookies.login;
    var password = $cookies.password;
    if(login && password){
        signin(login, password);
    }

    $scope.register = function(){
        ngDialog.open({
            template: 'html/register_dialog.html',
            className: 'ngdialog-theme-default'
        });
    };

    Utilities.applyStylesToHtml($element);
})
