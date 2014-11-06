/**
 * Created by CM on 11/6/2014.
 */
String.prototype.startsWith = function (str){
    return this.indexOf(str) == 0;
};

String.prototype.isNumber = function isNumber() {
    return !isNaN(parseFloat(this)) && isFinite(this);
}