package darts.processor

import java.awt.image.BufferedImage
import javax.swing.ImageIcon

import darts.data.{Observation, ObservationList}
import darts.GameUi
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
  val MIN_NONE_ZERO = 500 // if below, then the table is empty
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
    var imageMat: Mat = new Mat()
    var mask: Mat = new Mat()

    // warm up the mog
    for(i <- 1 to 5) {
      println(s"i: ${i}")
      imageMat = camera.captureFrame(imageMat)
      println("i utan 1")
      mog.apply(imageMat, mask, MOG_LEARNING_RATE)
      println("i utan 2")
      imageMat.release
      println("i utan 3")
    }
    try {
      var obsList = new ObservationList(camNum)
      while (StateHandler.cameraAllowed) {
        imageMat = camera.captureFrame(imageMat)

        while (imageMat == null || imageMat.rows != Config.CAM_HEIGHT || imageMat.cols != Config.CAM_WIDTH) {
          println(s"Error reading the camera ($camNum)")
          imageMat = camera.captureFrame(imageMat)
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
              //handleObservations(obsList)
              obsList.setState
              //println(s"State change: state: ${obsList.state} imgCount: ${obsList.list.size} camera: $camNum --------------------------------")
              if (Config.SAVE_MOG_FEATURE && obsList.state == 1) {
                imwrite(f"${Config.OUTPUT_DIR}/${camera.lastFilename}-cam:$camNum-state:$state-transformed.jpg", obsList.list.head.mogMask);
              }
              if (Config.RUN_MERGER) {
                Merger.merge(obsList)
              } else {
                obsList.release
              }
              obsList = new ObservationList(camNum)
              state = 0
            }
          }
        }
        println(s"cam: ${camNum} file: ${camera.lastFilename} z: ${maskNonZero}    state: ${state} o.state: ${obsList.state}")
        //print(s"${camNum}")
        if (Config.SAVE_MOG && state == 1) {
          imwrite(f"${Config.OUTPUT_DIR}/${camera.lastFilename}-cam:$camNum-state:$state-ostate-${obsList.state}-zero-${maskNonZero}.jpg", mask)
        }
        if (Config.GUI_UPDATE) { //  && state > 0
          val transformed = CvUtil.transform(imageMat, camNum)
          //imwrite(f"${Config.OUTPUT_DIR}/${camera.lastFilename}-cam:$camNum-state:$state-transformed.jpg", transformed);
          val toBufferedImage: BufferedImage = CvUtil.toBufferedImage(mask)
          transformed.release

          if (toBufferedImage != null && camNum == 1) { //  && camNum == 1
            GameUi.updateImage(2, new ImageIcon(toBufferedImage))
          } else {
            //println(s"TO BUFFERED IMAGE NULL???? miert? ${camera.lastFilename}-XXXXXXX.jpg -be kiirva")
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
    val state = obsList.setState
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
    mog.setVarThreshold(90) // default: 16
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
    println("conti 1")
    val capture = CaptureTrait.get(camNum)
    println("conti 2")
    runStateHandler(capture, Math.abs(camNum))
    println("conti 3")
    println(s"camera release: $camNum")
    println("conti 4")
    capture.release
  }
}

object StateHandler {
  var cameraAllowed = false
}