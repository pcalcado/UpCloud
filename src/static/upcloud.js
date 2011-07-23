var App = {};

App.uploader = function (refreshProgressFn, sendFileFn){
  var pathToLocalFile = null;
  var uploadIdPrefix = null;

  var fileExtension = function () {
    var findEverythingAfterLastDot = /\.?(\w+)$/;
    return pathToLocalFile.match (findEverythingAfterLastDot) [1];
  };

  var remoteFileName = function () {
    return uploadIdPrefix + "." + fileExtension ();
  }

  return {
    uploadProgressUrl: function (){
      return "/status?" + remoteFileName ();
    },
    refresh: function (stats) { 
      var message = "Status: " + stats.progress + "%."
      var completed = stats.progress > 99;
      if (completed) {
        message = message + " <a href=\"/temp/" + remoteFileName ()  + "\">Uploaded to here.</a>";
      }
      refreshProgressFn (message); 
      return completed;      
    },
    start: function (uploadId, localPath) {
      uploadIdPrefix = uploadId;
      pathToLocalFile = localPath;
      sendFileFn (remoteFileName ());
    }  
  }
};


var Ui = {};

Ui.enableDescriptionForm = function (enabled) {
  var value = null;
  if (!enabled) {
    value = true;
  }
  $ ("#submitDescriptionButton").attr ("disabled", value); 
}

Ui.performUploadInBackground = function () {
  var uploadIdPrefix = $ ("#uploadIdPrefixField").val ();
  var pathToLocalFile = $ ("#uploadedFileBox").val ();

  Ui.uploader.start (uploadIdPrefix, pathToLocalFile);
  var completed = false;

  var ajaxRefresh = function () {
    $.ajax ({url: Ui.uploader.uploadProgressUrl (),
             dataType: 'json',             
             success: function (stats) { completed = Ui.uploader.refresh (stats); },
             error: function (ignore, message) { console.log ("Error:" + message); }
            });    
  }; 
  
  var poll = function () {
    if (completed){
      Ui.enableDescriptionForm (true);
    } 
    else {    
      setTimeout (function (){ 
        ajaxRefresh (); 
        poll ();
      }, 500);
    }
  };

  poll ();
}

Ui.loadApp = function () {
  var refreshProgressFn = function (statusMessage) { $ ("#uploadPane").html (statusMessage); };
  var sendFileFn = function (remoteFileName) { 
    $ ("#uploadForm").attr ("action", "/upload?" + remoteFileName);    
    $ ("#uploadForm").submit ();
    $ ("#remoteFileNameField").val (remoteFileName);    
  };

  Ui.uploader = App.uploader (refreshProgressFn, sendFileFn);
  
  $ ("#uploadedFileBox").change (Ui.performUploadInBackground );
  Ui.enableDescriptionForm (false);
}

$ (document).load
