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
}

object CaptureTrait {
  implicit def captureTrait2File(t: CaptureTrait): CaptureFile = t.getSelf.asInstanceOf[CaptureFile]
  implicit def captureTrait2Camera(t: CaptureTrait): CaptureCamera = t.getSelf.asInstanceOf[CaptureCamera]

  var captureUtil: CaptureTrait = null
  def get(videoDeviceNumber: Int): CaptureTrait = {
    if (captureUtil == null) {
      captureUtil = new CaptureCamera(videoDeviceNumber = videoDeviceNumber)
    }
    captureUtil
  }
  def get(inputFiles: Seq[File]): CaptureTrait = {
    if (captureUtil == null) {
      captureUtil = new CaptureFile(inputFiles = inputFiles)
    }
    captureUtil
  }

  def releaseCamera() = {
    if (captureUtil != null) {
      captureUtil.release
      captureUtil = null
    }
  }

}