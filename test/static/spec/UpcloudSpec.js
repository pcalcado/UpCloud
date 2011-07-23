describe("Uploader", function() {
  var pathToLocalFile =  "/home/pcalcado/this.is-da original.mp3";
  var uploadIdPrefix = "123";

  var statusMessage = "";
  var refreshProgress = function (s){ statusMessage = s;};

  var remoteFileName = "";
  var sendFileFn = function (r) { remoteFileName = r};

  beforeEach(function() {
    uploader = App.uploader(refreshProgress, sendFileFn);
  });


  it ("should inform the path to retrieve status for file upload", function() {
    uploader.start (uploadIdPrefix, pathToLocalFile);
    expect (uploader.uploadProgressUrl ()).toBe ("/status?123.mp3");
  });

  it ("should inform remote file name when upload starts", function() {
    uploader.start (uploadIdPrefix, pathToLocalFile);
    expect (remoteFileName).toBe ("123.mp3");
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
