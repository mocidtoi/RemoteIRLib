var exec = require('cordova/exec');
var argscheck = require('cordova/argscheck');

function RemoteIRLib(){
	console.log("RemoteIRLib.js is created");
}

/*RemoteIRLib.prototype.configure = function(onSuccess, onFail, data){
	var getValue = argscheck.getValue;
	
	var ssid = getValue(data.ssid, "null");
	var password = getValue(data.password, "null");

	var args = [ssid, password];

	exec(onSuccess, onFail, "RemoteIRLib", "configure", args);
}*/
RemoteIRLib.prototype.configure = function(onSuccess, onFail, ssid, password){
	var args = [ssid, password];
	exec(onSuccess, onFail, "RemoteIRLib", "configure", args);

}
RemoteIRLib.prototype.cancelConfig = function(onSuccess, onFail) {
	exec(onSuccess, onFail, "RemoteIRLib", "cancelConfig", []);
}

RemoteIRLib.prototype.probe = function(onSuccess, onFail, deviceId, key) {
	var args = [deviceId, key];
	exec(onSuccess, onFail, "RemoteIRLib", "probe", args);
}

RemoteIRLib.prototype.sendCommand = function(onSuccess, onFail, deviceId, deviceIp, deviceServerIp, irData) {
	var args = [deviceId, deviceIp, deviceServerIp, irData];
	exec(onSuccess, onFail, "RemoteIRLib", "sendCommand", args);
}


var remoteIRLib = new RemoteIRLib();
module.exports = remoteIRLib;
