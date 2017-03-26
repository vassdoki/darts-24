# darts-24

Short description of the system:

The entry point is GameUi.scala

If the camera checkbox is checked, then to Threads are started that is running in an
infinite loop in StateHandler.runStateHandler

Processing steps:

 * read an image from the webcam
 * apply the backgroundSubtractorMOG
 * see the resulting mask and decide if there is a new dart on the image.
   * If there is only few changed pixels, then nothing happened.
   * If there are a lot of changed pixels, then the camera sees the players hands removing the darts.
   * If in between, then continue processing until the state changes.
   
When the state changes, then Merger object is responsible for merging the result of the two cameras.
The mergeObservationListsOrdered is where the recognition is happening.

Blurer.blurUntilClear blures the image, until there are no other white pixels, only
the dart. Unforunatelly it pretty offen blures so much, that the tip of the dart is not
visible any more.

DartsUtil.identifyNumber calculates the score by finding the top white pixel on
the image. This was first the only step. This works with one camera either.

Because the tip is often not visible, I try to place a line on the dart, so
the lines from the two camera's image will intersect, and that is where the
dart hit the table. But this line is often not very good.
LineDetector.detect places the line over the dart.

Now I have two scores from the two identifyNumber calls.
And one more score from the line intersection.
I just believe that the intersection is better, but it's not always the case.
