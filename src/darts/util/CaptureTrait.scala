package darts.util

import java.io.File

import org.bytedeco.javacpp.opencv_core.Mat

/**
 * Created by vassdoki on 2016.08.11..
 */
trait CaptureTrait {
  def captureFrame: Mat
  def release
  def getSelf: Any
  def lastFilename: String
  var imageNumber: Int
  var skipNext: Int
}

object CaptureTrait {
  implicit def captureTrait2File(t: CaptureTrait): CaptureFile = t.getSelf.asInstanceOf[CaptureFile]
  implicit def captureTrait2Camera(t: CaptureTrait): CaptureCamera = t.getSelf.asInstanceOf[CaptureCamera]

  def get: CaptureTrait = {
    get(Config.CAMERA_DEV_NUM)
  }
  def get(num: Int): CaptureTrait = {
    if (num < 0) {
      val d = new File(Config.INPUT_DIR)
      val x: Seq[File] = Seq(d.listFiles.filter(_.isFile).filter(_.getName.startsWith(s"d${Math.abs(num)}")).sorted: _*)
      get(x, Math.abs(num))
    } else {
      getVideo(num)
    }
  }
  def getVideo(videoDeviceNumber: Int): CaptureTrait = {
      new CaptureCamera(videoDeviceNumber = videoDeviceNumber)
  }
  def get(inputFiles: Seq[File], camNum: Int): CaptureTrait = {
      new CaptureFile(inputFiles = inputFiles, camNum = camNum)
  }
}