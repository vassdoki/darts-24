package darts.util

import java.io.File

import org.bytedeco.javacpp.opencv_imgcodecs._
import org.bytedeco.javacpp.opencv_core.Mat

/**
 * Created by vassdoki on 2016.08.11..
 */
class CaptureFile (inputFiles: Seq[File], camNum: Int) extends CaptureTrait{
  val otherCamNum = camNum % 2 + 1
  val inputFileCounter = 0
  CaptureFile.rest(camNum-1) = inputFiles
  var lastFilename: String = null
  var lastOrigFilename: String = null
  var imageNumber  = 0
  var skipNext: Int = 0

  val frame = new Mat()

  override def captureFrame: Mat = synchronized {
    if (CaptureFile.rest(camNum-1).size == 0) {
      throw new Exception("No more file from CaptureFile")
    } else {
      if (skipNext > 0) {
        CaptureFile.rest(camNum-1) = CaptureFile.rest(camNum-1).slice(skipNext, CaptureFile.rest(camNum-1).size)
        skipNext = 0
      }
      val file = CaptureFile.rest(camNum-1).head
      lastOrigFilename = file.getName
      lastFilename = file.getName.substring(3, 26)
      CaptureFile.lastFile(camNum-1) = lastFilename
      if (Config.CAM_FILE_SYNC_INPUT) {
        while (lastFilename > CaptureFile.lastFile(otherCamNum - 1) && CaptureFile.lastFile(otherCamNum - 1) != "") {
          Thread.sleep(10)
        }
      }
      CaptureFile.rest(camNum-1) = CaptureFile.rest(camNum-1).tail
      imageNumber += 1
      imread(file.getAbsolutePath)
    }
  }

  override def release = {

  }

  def getSelf: CaptureFile = this

}

object CaptureFile{
  val lastFile = scala.collection.mutable.ArrayBuffer[String]("", "")
  val rest: Array[Seq[File]] = new Array[Seq[File]](2)
}
