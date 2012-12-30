var ajax = {
    cometListeners: {
        partialUpdate: function(jsArray){
            $("."+jsArray["fragId"]).first().before(jsArray["contents"]).end().remove()
        },
        partialDiff: function(jsArray){
            var current = $("."+jsArray["fragId"])

            var currentText = ""
            for(var i = 0; i < current.length; i++){
                currentText = currentText + current[i].outerHTML
            }

            var dmp = new diff_match_patch()
            var x = dmp.patch_apply(dmp.patch_fromText(jsArray['diffs']), currentText)[0]
            jsArray['contents'] = x
            ajax.cometListeners.partialUpdate(jsArray)
        },
        doNothing: function(jsArray){}
    },
    lastMsg: "",
    backoff: 0.1,
    maxTime: 45 * 1000, // ms
    minTime: 0.01 * 1000, // ms
    runComet: function (cometUrl, key){
        $.ajax(cometUrl, {
            accepts: "text/plain",
            type: "POST",
            data: JSON.stringify([key, ajax.lastMsg]),
            timeout: ajax.maxTime,
            success: function(data, textStatus, jqXHR){
                var jsonMsgs = JSON.parse(data)

                for(i in jsonMsgs){
                    var msg = jsonMsgs[i]

                    ajax.cometListeners[msg.listener](msg.msg)
                    ajax.lastMsg = msg.id
                }

                ajax.backoff = Math.max(ajax.backoff / 2, ajax.minTime);
            },
            error: function(jqXHR, textStatus, errorThrown){
                if (textStatus != "timeout") ajax.backoff = Math.min(ajax.backoff * 2, ajax.maxTime);
            },
            complete: function(jqXHR, textStatus){
                setTimeout(function(){
                    ajax.runComet(cometUrl, key)
                }, ajax.backoff)
            }
        })
    },
    post: {
        data: function(data, url, pageId, cbId, callback){
            if(url == undefined) throw ("cannot handle undefined url")
            if(callback == undefined) callback = function(){}
            data = typeof data !== 'undefined' ? data : {};

            $.post(url, JSON.stringify([pageId, cbId, data]),
                function(data, textStatus, jqXHR){
                    callback(jqXHR.responseText)
                }
            );
        },

        field: function(element, url, pageId, cbId, callback){
            ajax.post.data($(element).val(), url, keys, callback)
        },
        form: function(element, url, pageId, cbId, callback){
            ajax.post.data($(element).serialize(), url, keys, callback)
        },
        post: function(url, pageId, cbId, callback){
            ajax.post.data({}, url, keys, callback)
        }
    }
}
