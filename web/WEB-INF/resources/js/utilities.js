/**
 * Created by CM on 11/1/2014.
 */

Utilities = {
    ajax_request_base_url: location.origin,
    applyStylesToHtml: function(element) {
        element = element || document;
        $(element).find( "input[type=submit], a, button" )
            .button().click(function(){
                $(this).removeClass("ui-state-focus").removeClass("ui-state-hover").button("refresh");
            });
        $(element).find(".list_item").hover(function(){
            $(this).addClass("list_item_hover");
        }, function() {
            $(this).removeClass("list_item_hover");
        })

        var emptyAvatar = location.origin + "/images/empty_avatar.png";
        $("element").find("img.avatar").attr("src", function(i, origin) {
            if(!origin){
                return emptyAvatar;
            }

            return origin;
        }).error(function() {
            $(this).attr("href", emptyAvatar);
        });

        $("element").find("form").attr("action", function(i, origin) {
            return location.origin + origin;
        })
    },
    ajax: function(params){
        var url = this.ajax_request_base_url + params.url;
        var data = params.data;
        var method = params.method;
        var onSuccess = params.success;
        var onError = params.error || function(message) {
            alert(message);
        }

        $.ajax({
            url: url,
            data: data,
            method: method,
            success: function(data){
                if(data.error){
                    onError(data.error);
                }
            },
            error: function(xhr, ajaxOptions, thrownError) {
                onError(thrownError)
            }
        });
    },
    addProperties: function(object, properties) {
        for(var i in properties){
            object[i] = properties[i];
        }
    },
    loadDataToScope: function(url, params, $scope, $http) {
        $http.get(url, {
            params: params
        }).success(function(data){
            Utilities.addProperties($scope, data);
        })
    },
    parseHashPath: function(hash) {
        return hash.split("_");
    }
}