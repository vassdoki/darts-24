package darts

import java.awt.image.BufferedImage
import java.io.{PrintWriter, File}
import java.nio.ByteBuffer
import java.time.LocalTime
import javax.swing.ImageIcon

import darts.util._
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

import scala.concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global

import scala.util.control.Breaks._

import org.bytedeco.javacpp.{BytePointer, Pointer}
import org.bytedeco.javacpp.indexer.UByteBufferIndexer
import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.opencv_imgproc._
import org.bytedeco.javacpp.opencv_imgcodecs._
import org.bytedeco.javacpp.opencv_video._

import org.joda.time


/**
 * Created by vassdoki on 2016.08.08..
 */
class BackgroundSubtractorTest {

  val COMMAND_LINE = true
  val SKIP_STORED_FILE = 0

  // this triggers the dart recognision
  val MIN_NONE_ZERO = 2000 // if below, then the table is empty
  val MAX_NONE_ZERO = 40000 // if above, then hands are visible

  /*
  0: default state
  1: there was a change that needs to be recognized on the following images
  2: the recognizer still runs, but the mog reports no change
  n: hands on the image, darts are being taken out (? usable state?)
   */
  var state: Int = 0
  var imgCount = 0;

  //var maxOutputNumber = 9999999


  def runRecognizer(camera: CaptureTrait, camNum: Int) = {
    var dartRecognizer: DartRecognizer = null

    val mog = createMog
    var imageMat: Mat = null
    var prevImageMat: Mat = null
    var mask: Mat = new Mat()
    var countZero = 0

    // capture the first two image, initialize mog
    prevImageMat = camera.captureFrame
    mog.apply(prevImageMat, mask, 1)
    imageMat = camera.captureFrame
    mog.apply(imageMat, mask, 0.1)
    CvUtil.releaseMat(imageMat)
    try {
      while (BackgroundSubtractorTest.cameraAllowed) {
        imageMat = camera.captureFrame
        var toBufferedImage: BufferedImage = CvUtil.toBufferedImage(imageMat)
        if (toBufferedImage != null) {
          if (Config.GUI_UPDATE && Math.abs(camNum) == 1) GameUi.updateImage(3, new ImageIcon(toBufferedImage))
        }

        while (imageMat == null || imageMat.rows != 720 || imageMat.cols != 1280) {
          println(s"Error reading the camera ($camNum)")
          imageMat = camera.captureFrame
        }
        imgCount += 1
        // save captured image
        if (Config.SAVE_CAPTURED) {
          imwrite(f"${Config.OUTPUT_DIR}/d${Math.abs(camNum)}-${Config.timeFormatter.print(DateTime.now)}.jpg", imageMat)
        }

        mog.apply(imageMat, mask, 0.1)
        if (mask == null) {
          println("mask is null")
        } else {
          //GameUi.updateImage(0,new ImageIcon(CvUtil.toBufferedImage(mask)))
          countZero = countNonZero(mask)
          // state: 0: waiting for the next, 1: darts 2: hands
          (state, countZero) match {
            case (0, z) if (z <= MIN_NONE_ZERO) => {
              // nothing interesting
            }
            case (0, z) if z > MIN_NONE_ZERO => {
              if (dartRecognizer != null) {
                dartRecognizer.release
              }
              if (countZero < MAX_NONE_ZERO) {
                if (Config.PROC_CALL_DART_RECOGNIZE) {
                  dartRecognizer = startNewRecognizer(camera.lastFilename, camNum)
                  val resultMask = dartRecognizer.newImage(imageMat, mask, camNum)
                  //BackgroundSubtractorTest.compareResult(resultMask, camera.lastFilename, camNum)
                }
                state = 1
              } else {
                state = 2
              }
              //imwrite(f"$OUTPUT_DIR/${camera.imageNumber}%05d-${camera.lastFilename}%30s zero: $countZero.jpg", mask)
            }
            case (1, z) if z >= MAX_NONE_ZERO => {
              // it is not darts, it's hands
              if (Config.PROC_CALL_DART_RECOGNIZE) {
                dartRecognizer.release
              }
              state = 2
            }
            case (1, z) if z >= MIN_NONE_ZERO && z < MAX_NONE_ZERO => {
              // recognizer active, and mog still reports change
              if (Config.PROC_CALL_DART_RECOGNIZE) {
                val resultMask = dartRecognizer.newImage(imageMat, mask, camNum)
                //BackgroundSubtractorTest.compareResult(resultMask, camera.lastFilename, camNum)
              }
            }
            case (1, z) if z <= MIN_NONE_ZERO => {
              // recognizer active, but mog reports no change
              if (Config.PROC_CALL_DART_RECOGNIZE) {
                handleRecognizerResultAndCLose(dartRecognizer)
              }
              state = 0
            }
            case (2, z) if z <= MIN_NONE_ZERO => {
              BackgroundSubtractorTest.resetResult
              state = 0
            }
            case (2, z) if z > MIN_NONE_ZERO => {
              state = 2
            }
          }
          //println(f"file: ${camera.lastFilename} zero: $countZero state: $state)")
          if (Config.SAVE_MOG) {
            //if (countZero >= MIN_NONE_ZERO && countZero <= MAX_NONE_ZERO)
            if (countZero >= 500)
            imwrite(f"${Config.OUTPUT_DIR}/${camera.lastFilename}-cam:$camNum-zero:$countZero%06d-state:$state.jpg", mask);
          }
          if (Config.GUI_UPDATE) {
            // && Math.abs(camNum) == 2
            putText(mask, f"State: ${state} zero: ${countZero}%6d count: ${imgCount} file: ${camera.lastOrigFilename}", new Point(20, 20),
              FONT_HERSHEY_PLAIN, // font type
              1, // font scale
              Config.COLOR_WHITE, // text color (here white)
              3, // text thickness
              8, // Line type.
              false)
            val bufferedImage: BufferedImage = CvUtil.toBufferedImage(mask)
            if (bufferedImage != null) {
              if (Config.GUI_UPDATE) GameUi.updateImage(Math.abs(camNum) - 1, new ImageIcon(bufferedImage))
            } else {
              println("BUFFERED IMAGE NULL ?????")
            }
          }
        }

        CvUtil.releaseMat(prevImageMat)
        prevImageMat = imageMat
      }
    }catch {
      case e: Exception => {
        println("BackgroundSubstractorTest exception: " + e)
        e.printStackTrace()
      }
    }
    countZero
  }

  def startNewRecognizer(imgName: String, camNum: Int) = {
    new DartRecognizer(imgName, camNum)
  }

  def handleRecognizerResultAndCLose(dartRecognizer: DartRecognizer) = {
    val (result_mod, result_num) = dartRecognizer.getResult
    val image = dartRecognizer.getResultImage
    BackgroundSubtractorTest.compareResult(image, dartRecognizer.imgName, dartRecognizer.myCamNum)
    //val color: Scalar = new Scalar(250, 250, 5, 0)
    //println(s"result: $result_num x $result_mod (${dartRecognizer.imgName})")

    dartRecognizer.release
  }

  def continousCameraUpdate(camNum: Int) = {
    val capture = CaptureTrait.get(camNum)
    runRecognizer(capture, camNum)
    println(s"camera release: $camNum")
    capture.release
  }



  def findTopWhite(i: IplImage) : (Int, Int) = {
    val d: BytePointer = i.imageData()

    var j: Int = 0
    while((d.get(j) == 0 || d.get(j+1) == 0) && j < 960 * 720) {
      j = j + 2
    }
    (j % i.width(), j / i.width())
  }
  def findTopWhite(i: Mat) : (Int, Int) = {
    val d: ByteBuffer = i.asByteBuffer()

    var j: Int = 0
    while((d.get(j) == 0 || d.get(j+1) == 0) && j < 960 * 720) {
      j = j + 2
    }
    (j % i.cols(), j / i.rows())
  }
  def identifyNumber(p: Point): Pair[Int, Int] = {
    val degree = CvUtil.getDegreeFromBull(p)
    val distance = CvUtil.getDistanceFromBull(p)
    // 6-os közepe a 0 fok és óra járásával ellentétes irányba megy

    val int: Int = Math.floor((degree + 9) / 18).toInt
    val number = if (int > 19) Config.nums(0) else Config.nums(int)

    val circleNumber: Int = Config.distancesFromBull filter { dfb => dfb < distance } length

    circleNumber match {
      case 0 => (2, 25)
      case 1 => (1, 25)
      case 3 => (3, number)
      case 5 => (2, number)
      case _ => (1, number)
    }
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
    //mog.setHistory(100)
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


}

object BackgroundSubtractorTest extends App {
  var cameraAllowed = false



  var running = true
  val fut1:Future[Int] = Future {
    val bs = new BackgroundSubtractorTest
    cameraAllowed = true
    val camera = CaptureTrait.get(-1)
    val res = bs.runRecognizer(camera, 1)
    camera.release
    res
  }
  val fut2: Future[Int] = Future {
    val bs = new BackgroundSubtractorTest
    cameraAllowed = true
    val camera = CaptureTrait.get(-2)
    val res = bs.runRecognizer(camera, 2)
    camera.release
    res
  }
  val result = for {
    r1 <- fut1
    r2 <- fut2
  } yield (r1 + r2)

  result onSuccess {
    case result => {
      println("result")
      running = false
    }
  }
  while(running) {
    Thread.sleep(1000)
  }

  var i:Array[Mat] = Array[Mat](null, null)
  var s:Array[Int] = Array[Int](-1, -1)
  def compareResult(img: Mat, imgName: String, camNum: Int) = synchronized {
    if (i == null) {
      i = Array(null, null)
      s = Array(-1,-1)
    }
    val cn = Math.abs(camNum) - 1
    val otherCn = (cn + 1)%2
    val name = imgName.substring(13).replace("-", "").replace("_", "").toInt
    if (i(otherCn) == null) {
      if (i(cn) != null) i(cn).release()
      i(cn) = img
      s(cn) = name
    } else {
      val res: MatExpr = or(img, i(otherCn))
      var asMat: Mat = null
      try {
        asMat = res.asMat()
        if (Config.SAVE_MERGE_COLORED) imwrite(f"${Config.OUTPUT_DIR}/${imgName}-XXXXXXX.jpg", asMat)
      }catch{
        case e: Exception => {
          println("AS MAT EXCEPTION, WHY?")
        }
      }
      if (Config.GUI_UPDATE) {
        val toBufferedImage: BufferedImage = CvUtil.toBufferedImage(asMat)
        if (toBufferedImage != null) {
          GameUi.updateImage(2, new ImageIcon(toBufferedImage))
        } else {
          println(s"TO BUFFERED IMAGE NULL???? miert? ${imgName}-XXXXXXX.jpg -be kiirva")
        }
      }
      i(otherCn).release()
      i(otherCn) = null
      img.release()
    }
  }
  def resetResult = {
    if (i(0) != null) i(0).release()
    if (i(1) != null) i(1).release()
    i(0) = null
    i(1) = null
  }

}