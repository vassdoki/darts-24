package darts.util

import java.io.File

import org.bytedeco.javacpp.opencv_imgcodecs._
import org.bytedeco.javacpp.opencv_core.Mat

/**
 * Created by vassdoki on 2016.08.11..
 */
class CaptureFile (inputFiles: Seq[File]) extends CaptureTrait{
  val inputFileCounter = 0
  var rest: Seq[File] = inputFiles
  var lastFilename: String = null
  var imageNumber  = 0

  val frame = new Mat()

  override def captureFrame: Mat = {
    if (rest.size == 0) {
      null
    } else {
      val file = rest.head
      lastFilename = file.getName
      rest = rest.tail
      imageNumber += 1
      imread(file.getAbsolutePath)
    }
  }

  override def release = {

  }

  def getSelf: CaptureFile = this
}
