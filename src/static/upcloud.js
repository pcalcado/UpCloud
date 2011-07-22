var App = {};

App.uploader = function (refreshProgress, submitForm){
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
      refreshProgress (message); 
      return completed;      
    },
    start: function (uploadId, localPath) {
      uploadIdPrefix = uploadId;
      pathToLocalFile = localPath;
      var action = "/upload?" + remoteFileName ();
      submitForm (action);
    }  
  }
};


var Ui = {};

Ui.performUploadInBackground = function () {
  var uploadIdPrefix = $ ("#uploadIdPrefixField").val ();
  var pathToLocalFile = $ ("#uploadedFileBox").val ();

  Ui.uploader.start (uploadIdPrefix, pathToLocalFile);
  var completed = false;

  var ajaxRefresh = function () {
    $.ajax ({url: Ui.uploader.uploadProgressUrl (),
             dataType: 'json',             
             success: function (stats) { Ui.uploader.refresh (stats); },
             error: function (ignore, message) { console.log ("Error:" + message); }
            });    
  }; 
  
  var poll = function () {
    if (!completed)  {    
      setTimeout (function (){ 
        ajaxRefresh (); 
        poll ();
      }, 1000);
    }
  };

  poll ();
}

Ui.loadApp = function () {
  var refreshProgress = function (statusMessage) { $ ("#uploadPane").html (statusMessage); };
  var submitForm = function (action) { 
    $ ("#uploadForm").attr ("action", action);
    $ ("#uploadForm").submit ();
  };

  Ui.uploader = App.uploader (refreshProgress, submitForm);
  
  $ ("#uploadedFileBox").change (Ui.performUploadInBackground );
}

$ (document).load
