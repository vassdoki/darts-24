package darts.util

import java.awt.Point
import java.awt.image.BufferedImage
import java.io.{FileInputStream, FileOutputStream, InputStream}
import java.util.Properties

import org.bytedeco.javacpp.opencv_core.Mat
import org.bytedeco.javacv.Java2DFrameConverter
import org.bytedeco.javacv.OpenCVFrameConverter.ToMat

import scala.collection.mutable

/**
 * Created by vassdoki on 2016.08.08..
 */
class Utils {
  var trSrc: mutable.MutableList[Point] = mutable.MutableList.fill(4) {new Point}
}
object Utils {
  var prop:Properties = null
  var u: Utils = null

  def getProperties: Utils = {
    if (prop == null) {
      prop = new Properties()
      var input: InputStream = null
      input = new FileInputStream("config.properties")
      prop.load(input)

      u = new Utils()
      for (i <- 0 to 3) {
        u.trSrc(i).x = prop.getProperty(s"src${i}x").toInt
        u.trSrc(i).y = prop.getProperty(s"src${i}y").toInt
      }
      if (input != null) {
        input.close();
      }
    }
    u
  }
  def saveProperties = {
    var output = new FileOutputStream("/home/vassdoki/git/darts-fedex/darts-opencv/config.properties")
    for(i <- 0 to 3) {
      prop.setProperty(s"src${i}x", u.trSrc(i).x.toString)
      prop.setProperty(s"src${i}y", u.trSrc(i).y.toString)
    }

    prop.store(output, null)
    output.close
  }

  def toBufferedImage(mat: Mat): BufferedImage = {
    val openCVConverter = new ToMat()
    val java2DConverter = new Java2DFrameConverter()
    java2DConverter.convert(openCVConverter.convert(mat))
  }

}
