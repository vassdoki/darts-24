package darts.processor

import java.awt.image.BufferedImage
import javax.swing.ImageIcon

import darts.data.{Observation, ObservationList}
import darts.{DartRecognizer, GameUi}
import darts.util.{CaptureTrait, Config, CvUtil}
import org.bytedeco.javacpp.opencv_core.{FONT_HERSHEY_PLAIN, Mat, Point, countNonZero}
import org.bytedeco.javacpp.opencv_imgcodecs.imwrite
import org.bytedeco.javacpp.opencv_imgproc.putText
import org.bytedeco.javacpp.opencv_video.{BackgroundSubtractor, createBackgroundSubtractorMOG2}
import org.joda.time.DateTime

/**
  * Created by vassdoki on 2016.12.03..
  */
class StateHandler {
  // this triggers the dart recognision
  val MIN_NONE_ZERO = 2000 // if below, then the table is empty
  val MAX_NONE_ZERO = 40000 // if above, then hands are visible

  val MOG_LEARNING_RATE = 0.2
  val MAX_IMAGE_COUNT = 2 // depends on the mog settings

  /*
  0: default state
  1: there was a change that needs to be recognized on the following images
  2: the recognizer still runs, but the mog reports no change
  n: hands on the image, darts are being taken out (? usable state?)
   */
  var state: Int = 0
  var prevState = state
  var maskNonZero = 0

  def runStateHandler(camera: CaptureTrait, camNum: Int) = {

    val mog = createMog
    var imageMat: Mat = null
    var mask: Mat = new Mat()
    var obsList = new ObservationList(camNum)

    // warm up the mog
    for(i <- 1 to 5) {
      imageMat = camera.captureFrame
      mog.apply(imageMat, mask, MOG_LEARNING_RATE)
    }
    try {
      while (StateHandler.cameraAllowed) {
        imageMat = camera.captureFrame

        while (imageMat == null || imageMat.rows != Config.CAM_HEIGHT || imageMat.cols != Config.CAM_WIDTH) {
          println(s"Error reading the camera ($camNum)")
          imageMat = camera.captureFrame
        }
        // save captured image
        if (Config.SAVE_CAPTURED) {
          imwrite(f"${Config.OUTPUT_DIR}/d$camNum-${Config.timeFormatter.print(DateTime.now)}.jpg", imageMat)
        }

        mog.apply(imageMat, mask, MOG_LEARNING_RATE)
        if (mask == null) {
          println("mask is null")
        } else {
          maskNonZero = countNonZero(mask)
          // state: 0: waiting for the next, 1: mog change detected
          (state, maskNonZero) match {
            case (0, z) if (z <= MIN_NONE_ZERO) => {
              // nothing interesting
            }
            case (0, z) if z > MIN_NONE_ZERO => {
              // mog change detected
              obsList.add(imageMat.clone, camera.lastFilename, camNum, mask.clone, maskNonZero)
              state = 1
            }
            case (1, z) if z > MIN_NONE_ZERO => {
              // mog change detected
              obsList.add(imageMat.clone, camera.lastFilename, camNum, mask.clone, maskNonZero)
            }
            case (1, z) if z <= MIN_NONE_ZERO => {
              // no more change reported by the mog
              handleObservations(obsList)
              obsList.reset
              state = 0
            }
          }
        }
        CvUtil.releaseMat(imageMat)
      }
    }catch {
      case e: Exception => {
        println("BackgroundSubstractorTest exception: " + e)
        e.printStackTrace()
      }
    }
  }

  def handleObservations(obsList: ObservationList) = {
    val state = obsList.getState
    obsList.debugPrintMask
  }

  def createMog: BackgroundSubtractor = {
    //    val mog = createBackgroundSubtractorKNN()
    //    println("getHistory: " + mog.getHistory) // 500
    //    println("getNSamples: " + mog.getNSamples) // 7
    //    //mog.setNSamples(14)
    //    println("getDist2Threshold: " + mog.getDist2Threshold) // 400.0
    //    mog.setDist2Threshold(400.0)
    //    println("getkNNSamples: " + mog.getkNNSamples) // 2
    //    mog.setkNNSamples(2)
    //    println("getShadowValue: " + mog.getShadowValue) // 127
    //    println("getShadowThreshold: " + mog.getShadowThreshold) // 0.5

    var mog = createBackgroundSubtractorMOG2()
    println("detect shadows: " + mog.getDetectShadows())
    mog.setDetectShadows(true)
    mog.setShadowValue(255)
    println("History size: " + mog.getHistory)
    mog.setHistory(200)
    //mog.setVarThreshold(20)
    mog.setVarThreshold(128) // default: 16
    println("varThreshold: " + mog.getVarThreshold)
    mog.setComplexityReductionThreshold(0.05) // default: 0.05
    println("ComplexityReductionThreshold: " + mog.getComplexityReductionThreshold())
    mog.setBackgroundRatio(0.5) // default: 0.9
    println("BackgroundRatio: " + mog.getBackgroundRatio)
    mog.setVarMin(4) // default: 4
    println("varMin: " + mog.getVarMin)
    mog.setVarMax(75) // default: 75
    println("varMax: " + mog.getVarMax)
    mog.setVarThresholdGen(9) // default: 9
    println("varThresholdGen: " + mog.getVarThresholdGen)

    mog
  }

  def continousCameraUpdate(camNum: Int) = {
    val capture = CaptureTrait.get(camNum)
    runStateHandler(capture, Math.abs(camNum))
    println(s"camera release: $camNum")
    capture.release
  }
}

object StateHandler {
  var cameraAllowed = false
}