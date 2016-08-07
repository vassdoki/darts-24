package darts

import javax.swing.JFrame

import org.bytedeco.javacpp.opencv_core.Mat
import org.bytedeco.javacpp.opencv_videoio._
import org.bytedeco.javacpp.opencv_imgcodecs._
import org.bytedeco.javacpp.opencv_imgproc._
import org.bytedeco.javacv.{OpenCVFrameConverter, CanvasFrame}

import scala.math._

/**
 * Created by vassdoki on 2016.08.07..
 */
object CaptureTest extends App {
  // Open video file
  val capture = new VideoCapture(0)

  // check if video successfully opened
  require(capture.isOpened, "Failed to open input video")

  // Get the frame rate
  val rate = capture.get(CAP_PROP_FPS)
  println("Frame rate: " + rate + "fps")

  var stop        = false
  // current video frame
  val frame       = new Mat()
//  val canvasFrame = new CanvasFrame("Extracted Frame", 1)
  val width       = capture.get(CAP_PROP_FRAME_WIDTH).toInt
  val height      = capture.get(CAP_PROP_FRAME_HEIGHT).toInt
//  canvasFrame.setCanvasSize(width, height)
//  // Exit the example when the canvas frame is closed
//  canvasFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)

  //
  val canvasConverter = new OpenCVFrameConverter.ToMat()


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

  // Close the video file
  capture.release()

}
