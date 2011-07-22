describe("Uploader", function() {
  var pathToLocalFile =  "/home/pcalcado/this.is-da original.mp3";
  var uploadIdPrefix = "123";

  var statusMessage = "";
  var refreshProgress = function (s){ statusMessage = s;};




  var action = "";
  var submitForm = function (a) { action = a;};

  beforeEach(function() {
    uploader = App.uploader(refreshProgress, submitForm);
  });


  it ("should inform the path to retrieve status for file upload", function() {
    uploader.start (uploadIdPrefix, pathToLocalFile);
    expect (uploader.uploadProgressUrl ()).toBe ("/status?123.mp3");
  });

  it ("should modify action and submit form when upload starts", function() {
    uploader.start (uploadIdPrefix, pathToLocalFile);
    expect (action).toBe ("/upload?123.mp3");
  })
  
  it ("should return progess message when not 100% uploaded",function () {
    var stats = {progress: 97};

    uploader.start (uploadIdPrefix, pathToLocalFile);
    var finished = uploader.refresh (stats)

    expect (finished).toBeFalsy ();
    expect(statusMessage).toBe("Status: 97%.");    
  });

  it ("should change message to 'completed' and return true when reaches 100%", function () {    
    var stats = {progress: 100};

    uploader.start (uploadIdPrefix, pathToLocalFile);
    var finished = uploader.refresh (stats)

    expect (finished).toBeTruthy ();
    expect(statusMessage).toBe("Status: 100%. <a href=\"/temp/123.mp3\">Uploaded to here.</a>");  
  });
});
