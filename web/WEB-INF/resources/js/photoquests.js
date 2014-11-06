var main = angular.module("main");
main.controller("PhotoQuests", function($scope, $location, $element, ngDialog, $http){
    $scope.openCreatePhotoquestDialog = function() {
        ngDialog.open({
            template: 'HTML/create_photo_quest_dialog.html',
            className: 'ngdialog-theme-default',
            controller: 'PhotoQuests'
        });
    };

    $scope.createPhotoquest = function() {
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

    $scope.openQuest = function(id) {
        $location.hash("quest_" + id);
    }

    Utilities.loadDataToScope(window.location.origin + "//getPhotoquests", {}, $scope, $http)

    Utilities.applyStylesToHtml($element);
});