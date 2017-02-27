function DocumentScanner() {
}


DocumentScanner.prototype.process = function (options, successCallback, errorCallback) {
	cordova.exec(successCallback, errorCallback, "DocumentScanner", "process", [options]);
};

DocumentScanner.install = function () {
  if (!window.plugins) {
    window.plugins = {};
  }

  window.plugins.documentscanner = new DocumentScanner();
  return window.plugins.documentscanner;
};

cordova.addConstructor(DocumentScanner.install);