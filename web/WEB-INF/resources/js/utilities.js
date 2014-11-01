/**
 * Created by CM on 11/1/2014.
 */

Utilities = {
    ajax_request_base_url: location.origin,
    applyStylesToHtml: function() {
        $( "input[type=submit], a, button" )
            .button().click(function(){
                $(this).removeClass("ui-state-focus").removeClass("ui-state-hover").button("refresh");
            });
        $(".list_item").hover(function(){
            $(this).addClass("list_item_hover");
        }, function() {
            $(this).removeClass("list_item_hover");
        })

        var emptyAvatar = location.origin + "/images/empty_avatar.png";
        $("img.avatar").attr("src", function(i, origin) {
            if(!origin){
                return emptyAvatar;
            }

            return origin;
        }).error(function() {
            $(this).attr("href", emptyAvatar);
        });

        $("form").attr("action", function(i, origin) {
            return location.origin + origin;
        })
    },
    ajax: function(params){
        var url = this.ajax_request_base_url + params.url;
        var data = params.data;
        var method = params.method;
        var onSuccess = params.success;

        $.ajax({
            url: url,
            data: data,
            method: method,
            success: onSuccess
        });
    }
}