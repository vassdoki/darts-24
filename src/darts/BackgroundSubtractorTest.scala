package darts

import java.awt.image.BufferedImage
import java.io.{File, PrintWriter}
import java.nio.ByteBuffer
import java.time.LocalTime
import javax.swing.ImageIcon

import darts.cmd.Blue
import darts.util._
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

import scala.concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global
import scala.util.control.Breaks._
import org.bytedeco.javacpp.{BytePointer, Pointer}
import org.bytedeco.javacpp.indexer.{IntRawIndexer, UByteBufferIndexer}
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
    mog.apply(imageMat, mask, MOG_LEARNING_RATE)
    CvUtil.releaseMat(imageMat)
    try {
      while (BackgroundSubtractorTest.cameraAllowed) {
        imageMat = camera.captureFrame
//        var toBufferedImage: BufferedImage = CvUtil.toBufferedImage(imageMat)
//        if (toBufferedImage != null) {
//          if (Config.GUI_UPDATE && Math.abs(camNum) == 1) GameUi.updateImage(3, new ImageIcon(toBufferedImage))
//        }

        while (imageMat == null || imageMat.rows != 720 || imageMat.cols != 1280) {
          println(s"Error reading the camera ($camNum)")
          imageMat = camera.captureFrame
        }
        // save captured image
        if (Config.SAVE_CAPTURED) {
          imwrite(f"${Config.OUTPUT_DIR}/d${Math.abs(camNum)}-${Config.timeFormatter.print(DateTime.now)}.jpg", imageMat)
        }

        mog.apply(imageMat, mask, MOG_LEARNING_RATE)
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
                  dartRecognizer.newImage(imageMat, mask, camNum)
                  imgCount = 0
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
            case (1, z) if z >= MIN_NONE_ZERO && z < MAX_NONE_ZERO && imgCount > MAX_IMAGE_COUNT => {
              // the change is small, but takes too much time, it is hands state
              if (Config.PROC_CALL_DART_RECOGNIZE) {
                dartRecognizer.release
              }
              state = 2
            }

            case (1, z) if z >= MIN_NONE_ZERO && z < MAX_NONE_ZERO => {
              // recognizer active, and mog still reports change
              imgCount += 1
              if (Config.PROC_CALL_DART_RECOGNIZE) {
                dartRecognizer.newImage(imageMat, mask, camNum)
              }
            }
            case (1, z) if z <= MIN_NONE_ZERO => {
              // recognizer active, but mog reports no change
              if (Config.PROC_CALL_DART_RECOGNIZE) {
                handleRecognizerResultAndCLose(dartRecognizer)
              }
              imgCount = 0
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
          if (Config.SAVE_MOG && state == 1) {
            //if (countZero >= MIN_NONE_ZERO && countZero <= MAX_NONE_ZERO)
            if (countZero >= 500)
            imwrite(f"${Config.OUTPUT_DIR}/${camera.lastFilename}-cam:$camNum-zero:$countZero%06d-state:$state-count:$imgCount.jpg", mask);
            //Blue.t(mask, s"${camera.lastFilename}-cam:$camNum-zero:$countZero%06d-state:$state")
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

        prevState = state
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
    BackgroundSubtractorTest.compareResult(image, dartRecognizer.imgName, dartRecognizer.myCamNum, dartRecognizer.getResultXY)
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
}

object BackgroundSubtractorTest extends App {
  var cameraAllowed = false

  var running = true
  val fut1:Future[Int] = Future {
    val bs = new BackgroundSubtractorTest
    cameraAllowed = true
    val camera = CaptureTrait.get(1)
    val res = bs.runRecognizer(camera, 1)
    camera.release
    res
  }
  val fut2: Future[Int] = Future {
    val bs = new BackgroundSubtractorTest
    cameraAllowed = true
    val camera = CaptureTrait.get(2)
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
  var xy:Array[(Int, Int)] = Array((0,0),(0,0))

  /**
   * If hands are visible, then flush previous result.
   * This handles the case, when only one camera did detect a dart
   */
  def flushResult = synchronized {
    if (i(0) != null || i(1) != null) {
      val (cn, otherCn) = if (i(0) != null) {
        (0, 1)
      } else {
        (1, 0)
      }
      val (x, y) = xy(cn)

      var asMat = i(cn)

      try {

        circle(asMat, new Point(x, y), 10, Config.COLOR_YELLOW, 1, 8, 0)
        val (mod, num) = DartsUtil.identifyNumber(new Point(x, y))
        putText(asMat, f"$num (X $mod) [one cam]", new Point(800, 150),
          FONT_HERSHEY_PLAIN, // font type
          5, // font scale
          Config.COLOR_YELLOW, // text color (here white)
          3, // text thickness
          8, // Line type.
          false)
        CvUtil.drawTable(asMat, Config.COLOR_BLUE, 1)
        CvUtil.drawNumbers(asMat, Config.COLOR_BLUE)
        if (Config.SAVE_MERGE_COLORED) imwrite(f"${Config.OUTPUT_DIR}/${s(cn)}-XXXXXXX.jpg", asMat)
      } catch {
        case e: Exception => {
          println("AS MAT EXCEPTION, WHY?")
        }
      }
      if (Config.GUI_UPDATE) {
        val toBufferedImage: BufferedImage = CvUtil.toBufferedImage(asMat)
        if (toBufferedImage != null) {
          GameUi.updateImage(2, new ImageIcon(toBufferedImage))
        } else {
          println(s"TO BUFFERED IMAGE NULL???? miert? ${s(cn)}-XXXXXXX.jpg -be kiirva")
        }
      }
      i(cn).release()
      i(cn) = null
    }
  }

  def compareResult(img: Mat, imgName: String, camNum: Int, pxy: (Int, Int)) = synchronized {
    val (x, y) = pxy
    if (i == null) {
      i = Array(null, null)
      s = Array(-1,-1)
      xy = Array((0,0), (0,0))
    }
    val cn = Math.abs(camNum) - 1
    val otherCn = (cn + 1)%2
    val name = imgName.substring(13).replace("-", "").replace("_", "").toInt
    if (i(otherCn) == null) {
      if (i(cn) != null) i(cn).release()
      i(cn) = img
      s(cn) = name
      xy(cn) = pxy
    } else {
      val res: MatExpr = or(img, i(otherCn))
      var asMat: Mat = null

      try {
        asMat = res.asMat()
        line(asMat, new Point(x, y), new Point(xy(otherCn)._1, xy(otherCn)._2), Config.COLOR_YELLOW)
        val averagePoint: Point = new Point((x + xy(otherCn)._1) / 2, (y + xy(otherCn)._2) / 2)


//        var lines = new Mat
//        var gray = new Mat
//        cvtColor(asMat, gray, CV_RGB2GRAY)
//        val deltaRho: Double = 2
//        val deltaTheta: Double = CV_PI / 180
//        val minVotes: Int = 10
//        val minLength: Double = 10
//        val minGap: Double = 5d
//        HoughLinesP(gray, lines, deltaRho, deltaTheta, minVotes, minLength, minGap)
//        val indexer = lines.createIndexer().asInstanceOf[IntRawIndexer]
//        for (i <- 0 until lines.rows()) {
//          val pt1 = new Point(indexer.get(i, 0, 0), indexer.get(i, 0, 1))
//          val pt2 = new Point(indexer.get(i, 0, 2), indexer.get(i, 0, 3))
//
//          // draw the segment on the image
//          line(asMat, pt1, pt2, Config.COLOR_WHITE, 1, LINE_AA, 0)
//        }


        circle(asMat, averagePoint, 10, Config.COLOR_YELLOW, 1, 8, 0)
        val (mod, num) = DartsUtil.identifyNumber(averagePoint)
        CvUtil.drawTable(asMat, Config.COLOR_BLUE, 1)
        CvUtil.drawNumbers(asMat, Config.COLOR_BLUE)
        putText(asMat, f"$num (X $mod)", new Point(800, 150),
          FONT_HERSHEY_PLAIN, // font type
          5, // font scale
          Config.COLOR_YELLOW, // text color (here white)
          3, // text thickness
          8, // Line type.
          false)
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
    if (i != null) {
      if (i(0) != null) i(0).release()
      if (i(1) != null) i(1).release()
      i(0) = null
      i(1) = null
    }
  }

}