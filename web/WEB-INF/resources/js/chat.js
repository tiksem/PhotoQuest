/**
 * Created by CM on 11/14/2014.
 */

tiksem = tiksem || {};
tiksem.createChat = function(params, $http) {
    var requestInterval = params.requestInterval || 1000;

    var getMessagesCount = params.getMessagesCount;
    if(typeof getMessagesCount != "function"){
        throw new Error("params.getMessagesCount is not a function, specify getMessagesCount");
    }

    var getSendRequestUrlAndParams = params.getRequestUrlAndParams;
    if(typeof getSendRequestUrlAndParams != "function"){
        throw new Error("params.getRequestUrl is not a function, specify getSendRequestUrlAndParams");
    }

    var getReceiveRequestUrlAndParams = params.getReceiveRequestUrlAndParams;
    if(typeof getReceiveRequestUrlAndParams != "function"){
        throw new Error("params.getRequestUrl is not a function, specify getSendRequestUrlAndParams");
    }

    var getMessages = params.getMessages;
    if(typeof getMessages != "function"){
        throw new Error("params.getMessages is not a function, specify getMessages");
    }

    var onMessageReceived = params.onMessageReceived;
    var onMessageSent = params.onMessageSent;
    var intervalHandle;
    var lastMessagesCount = 0;

    return {
        sendMessage: function(text) {
            var config = getSendRequestUrlAndParams(text);

            $http.get(config.url, {
                params: config.params
            }).success(function(data){
                if (!data.error) {
                    console.log("Message delivered!")
                    console.log(data);
                    if(onMessageSent){
                        onMessageSent(data);
                    }
                } else {
                    console.error("Message delivering error!")
                    console.error(data);
                }
            }).error(function(){
                console.error("Message has not been delivered, unknown error");
            });
        },
        startMessageReceiving: function() {
            intervalHandle = setTimeout(function(){
                var config = getSendRequestUrlAndParams(text);

                $http.get(config.url, {
                    params: config.params
                }).success(function(data){
                    if (!data.error) {
                        var messagesCount = getMessagesCount(data);
                        if(messagesCount > lastMessagesCount){
                            if(onMessageReceived){
                                var messages = getMessages();
                                onMessageReceived(messages[messages.length - 1]);
                            }
                            lastMessagesCount = messagesCount;
                        }
                    } else {
                        console.error("Message receiving error!")
                        console.error(data);
                    }
                }).error(function(){
                    console.error("Message receiving error!");
                });
            }, requestInterval);
        },
        stopMessageReceiving: function() {
            clearTimeout(intervalHandle);
        }
    };
};