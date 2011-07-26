# upcloud

Simple file uploading app running on Heroku.

## Usage

FIXME: write

## Comments
This is very simple implementation of a file uploader in Clojure.

I decided to keep depedencies to a minimum, so the only external
library used by the main Clojure app is Ring. I use Midje for testing
--it's my favourite testing framework for Clojure at the moment. The
JavaScript piece uses JQuery (I am not doing JS without JQuery!) and I
am testing it with Jasmine.

I started using a [simpler and syncrhonous read/write
pipeline](https://github.com/pcalcado/UpCloud/blob/26d33a84d17f8241778829071067a4da64cfcbb5/src/upcloud/upload.clj);
but in [a8b8156 decided that it would be better to delegate writing to
an agent](https://github.com/pcalcado/UpCloud/commit/a8b8156a51a520e2e36e24218319636cbcddbd7e)
so that the HTTP connection with the browser could be released faster.

Heroku released support to Clojure a couple of weeks ago, as I really
like what they do for Ruby I decided to use this little project to
test their platform.

### Notes
- To support multiple uploads and not have any kind of session on the
server, I have the browser sending an "upload-id". I don't really like
this solution but some basic research tells me that this is what
jquery plugins and the like use --at least until we can use the File
Events API in the browser. 
- This upload id, required to handle multiple uploads, is passed in the
query string because Ring doesn't let me access the :params object
from inside a loader function. I am submitting a patch to them to make
this possible -really dislike the idea of using the query string for
this.
- In a production-ready application, the map which tracks progress for
uploads would  be cleaned at some stage, and stale uploads must be
removed. I am currently only cleaning the map if an exception happened.
- We currently only save the application to the local file system, but
the idea is to use the local hard drive just as a swap area and send
the binaries to S3.
- I played around buffer sizes for a bit but changing values didn't
improve performance on my machine. I think 512 bytes provides good
feedback for the user considering most files would be MP3, whioch I
believe are often around 3MB.
- For scalability, the in-memory map can probably be replaced by
memcached or some other simple key-value storage system.
- The only security check at the moment is to make sure the file name
doesn't have any funny chars, which could be used to temper with the
filesystem. I assumed this was enough for this exercise, in a
production-ready setup I would probably think about checking the file
structure to make sure it complies with the MP3 format before doing
anything with it.
- Leiningen 1.6.1 [has a
bug](https://github.com/technomancy/leiningen/issues/227) when trying
to run agents using the REPL. This code worked fine for me using the
master branch (2.x.x-SNAPSHOT). If that's too hard to setup the code
still works using _lein run_ or _lein midje_.

## License

Distributed under the Eclipse Public License, the same as Clojure.
