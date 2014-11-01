/**
 * Created by CM on 11/1/2014.
 */

Utilities = {
    applyStylesToHtml: function() {
        $( "input[type=submit], a, button" )
            .button()
            .click(function( event ) {
                event.preventDefault();
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
        })
    }
}