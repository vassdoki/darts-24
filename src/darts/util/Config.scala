package darts.util

import java.awt.Point

import java.awt.image.BufferedImage
import java.io.{FileInputStream, FileOutputStream, InputStream}
import java.util.Properties

import org.bytedeco.javacpp.opencv_core
import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacv.Java2DFrameConverter
import org.bytedeco.javacv.OpenCVFrameConverter.ToMat
import org.joda.time.format.DateTimeFormat

import scala.collection.mutable

/**
 * Created by vassdoki on 2016.08.08..
 */
class Config {
  var trSrc: mutable.MutableList[Point] = mutable.MutableList.fill(4) {new Point}

}
object Config {
  val CAMERA_DEV_NUM = 1
  val SAVE_CAPTURED = false
  val INPUT_DIR = "/home/vassdoki/darts/v2/test"
  val OUTPUT_DIR = "/home/vassdoki/darts/v2/d"

  // alter processing
  val PROC_CALL_DART_RECOGNIZE = false // call DartRecognizer
  val SAVE_MOG = false // save mog mask from BackgroundSubtractor (state....)
  val SAVE_DR_STATE = false // save DartRecognizer process visualization
  val GUI_UPDATE = true // call the GameUi update method
  val SAVE_DR_COLORED = true  // save the result of the backgroundSubtractor colored
  val SAVE_MERGE_COLORED = true // save the result of the two backgroundSubtractor colored merged

  val COLOR_RED: Scalar = new Scalar(0, 0, 254, 0) // BGR
  val COLOR_GREEN: Scalar = new Scalar(0, 254, 0, 0) // BGR
  val COLOR_YELLOW: Scalar = new Scalar(91, 240, 245, 0)
  val COLOR_BLUE: Scalar = new Scalar(250, 150, 5, 0)
  val COLOR_WHITE: Scalar = new Scalar(255, 255, 255, 0)
  val COLOR_BLACK: Scalar = new Scalar(0, 0, 0, 0)

  val timeFormatter = DateTimeFormat.forPattern("Y-MMM-d_H-m_ss-SS");

  val MAT_black = new Mat(1,1,CV_16SC1, 0)

  var confFile: Array[Config] = new Array[Config](2)

  val conversion = 1
  val transformationDst = Array(134f, 262f,   666f, 262f,   666f, 538f,   134f, 538f, 400f, 400f).map(_/conversion)
  val distancesFromBull = Array(14, 28, 174, 192, 284, 300).map(_/conversion)
  val nums = List(6, 13, 4, 18, 1, 20, 5, 12, 9, 14, 11, 8, 16, 7, 19, 3, 17, 2, 15, 10).map(_/conversion)
  val bull = new Point(400/conversion, 400/conversion)
  var obull = new opencv_core.Point(bull.x, bull.y)


  def getProperties: Config = {
    getProperties(Config.CAMERA_DEV_NUM)
  }
  def getProperties(cam: Int): Config = {
    val confNum = Math.abs(cam) - 1;
    if (confFile(confNum) == null) {
      val prop = new Properties()
      var input: InputStream = null
      input = new FileInputStream(s"config${Math.abs(cam)}.properties")
      prop.load(input)

      confFile(confNum) = new Config
      for (i <- 0 to confFile(confNum).trSrc.size - 1) {
        confFile(confNum).trSrc(i).x = prop.getProperty(s"src${i}x").toInt
        confFile(confNum).trSrc(i).y = prop.getProperty(s"src${i}y").toInt
      }
      if (input != null) {
        input.close();
      }
    }
    confFile(confNum)
  }
  def saveProperties: Any = {
    saveProperties(Config.CAMERA_DEV_NUM)
  }
  def saveProperties(cam: Int): Any = {
    val confNum = Math.abs(cam) - 1
    val prop = new Properties()
    var output = new FileOutputStream(s"config${cam}.properties")
    for(i <- 0 to 3) {
      prop.setProperty(s"src${i}x", confFile(confNum).trSrc(i).x.toString)
      prop.setProperty(s"src${i}y", confFile(confNum).trSrc(i).y.toString)
    }

    prop.store(output, null)
    output.close
  }
}
