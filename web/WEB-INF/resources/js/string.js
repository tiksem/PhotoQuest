/**
 * Created by CM on 11/6/2014.
 */
String.prototype.startsWith = function (str){
    return this.indexOf(str) == 0;
};

String.prototype.isNumber = function () {
    return !isNaN(parseFloat(this)) && isFinite(this);
};

String.prototype.getPreview = function isNumber(max, endString) {
    if(this.length > max){
        var end = max - endString.length - 1;
        if(end < 0){
            end = 0;
        }

        var result = this.substring(0, end);
        result += endString;
        return result;
    } else {
        return this.toString();
    }
};