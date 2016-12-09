package darts.util

import java.io.File

import org.bytedeco.javacpp.opencv_core.Mat
import org.bytedeco.javacpp.opencv_imgcodecs._
import org.bytedeco.javacpp.opencv_videoio._
import org.joda.time.DateTime

import scala.math._

/**
 * Created by vassdoki on 2016.08.07..
 */
class CaptureCamera(videoDeviceNumber: Int) extends CaptureTrait{
  var skipNext: Int = -1 // no use of it here
  val capture: VideoCapture = new VideoCapture(videoDeviceNumber)
  var imageNumber = 0
  var lastFilename: String = null
  var lastOrigFilename: String = null

  val rate = capture.get(CAP_PROP_FPS)
  println(s"Default (${videoDeviceNumber}) Frame rate: " + rate + "fps width: " + capture.get(CAP_PROP_FRAME_WIDTH).toInt + " height: " + capture.get(CAP_PROP_FRAME_HEIGHT).toInt)
  val frame       = new Mat()
  //  val canvasFrame = new CanvasFrame("Extracted Frame", 1)
  capture.set(CAP_PROP_FRAME_WIDTH, 1920)
  capture.set(CAP_PROP_FRAME_HEIGHT, 720)

  val width       = capture.get(CAP_PROP_FRAME_WIDTH).toInt
  val height      = capture.get(CAP_PROP_FRAME_HEIGHT).toInt

  def captureFrame: Mat = {
    lastOrigFilename = s"d${Math.abs(videoDeviceNumber)}-${Config.timeFormatter.print(DateTime.now)}"
    lastFilename = s"${Config.timeFormatter.print(DateTime.now)}"
    capture.read(frame)
    imageNumber += 1
    frame
  }

  def captureFrame(f: Mat): Mat = {
    lastOrigFilename = s"d${Math.abs(videoDeviceNumber)}-${Config.timeFormatter.print(DateTime.now)}"
    lastFilename = s"${Config.timeFormatter.print(DateTime.now)}"
    capture.read(f)
    imageNumber += 1
    f
  }

  def release = {
    capture.release()
  }

  //val canvasConverter = new OpenCVFrameConverter.ToMat()

  var stop        = false
  def run = {
    // Delay between each frame
    // corresponds to video frame rate
    val delay = round(1000 / rate)
    // for all frames in video
    var i = 0;
    while (!stop) {
      // read next frame if any
      if (capture.read(frame)) {
        //      canvasFrame.showImage(canvasConverter.convert(frame))
        // Introduce a delay
        Thread.sleep(delay)
        imwrite(f"/tmp/x-$i%03d.jpg", frame)
        //cvGetPerspectiveTransform(null, null, null)
        //cvWarpPerspective()
        i += 1
      } else {
        stop = true
      }
    }
  }
  def getSelf: CaptureCamera = this
}
