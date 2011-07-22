# upcloud

Simple file uploading app running on Heroku.

## Usage

FIXME: write

## Comments
This is very simple implementation of a file uploader in Clojure.

I decided to keep depedencies to a minimum, so the only external
library used by the main Clojure app is Ring. I use Midje for testing
--it's my favourite testing framework for Clojure at the moment. The
JavaScript piece uses JQuery (I am not doing JS without JQuery!).

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
- The map which tracks progress for uploads must be cleaned at some
stage, and stale uploads must be removed.
- The file system needs to be cleaned. The original idea was to use
the local hard drive just as a swap area and send the binaries to S3.
- In a real application I would probably solve these two problems
having a service (or even a local agent in the same JVM, for a simpler
scenario) handling the life cycle of uploaded files after the initial
upload. It would perform housekeeping in the in-memory map and send
the files to S3. This service should be fairly easy to scale horizontally.
- For scalability, the in-memory map can probably be replaced by
memcached or some simple key-value storage system.
- The only security check at the moment is to make sure the file name
doesn't have any funny chars, which could be used to temper with the
filesystem. I assumed this was enough for this exercise, in a
production-ready setup I would probably think about checking the file
structure to make sure it complies with the MP3 format before doing
anything with it.

## License

Distributed under the Eclipse Public License, the same as Clojure.