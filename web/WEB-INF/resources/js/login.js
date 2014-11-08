var main = angular.module("main");
main.controller("LoginController", function($scope, ngDialog, $element, $http){
    $scope.isSignin = false;
    $scope.signin = function(){
        var login = $scope.login;
        var password = $scope.password;
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
        }
        $http.get(window.location.origin + "//login",config).success(function(){
            $scope.isSignin = true;
            alert("Success!");
        })
    }

    $scope.register = function(){
        ngDialog.open({
            template: 'html/register_dialog.html',
            className: 'ngdialog-theme-default'
        });
    };

    Utilities.applyStylesToHtml($element);
})
