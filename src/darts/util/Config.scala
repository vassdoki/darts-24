package darts.util

import java.awt.Point

import java.awt.image.BufferedImage
import java.io.{FileInputStream, FileOutputStream, InputStream}
import java.util.Properties

import org.bytedeco.javacv.Java2DFrameConverter
import org.bytedeco.javacv.OpenCVFrameConverter.ToMat

import scala.collection.mutable

/**
 * Created by vassdoki on 2016.08.08..
 */
class Config {
  var trSrc: mutable.MutableList[Point] = mutable.MutableList.fill(4) {new Point}

}
object Config {
  var prop:Properties = null
  var u: Config = null

  val transformationDst = Array(134f, 262f,   666f, 262f,   666f, 538f,   134f, 538f)
  val distancesFromBull = List(14, 28, 174, 192, 284, 300)
  val nums = List(6, 13, 4, 18, 1, 20, 5, 12, 9, 14, 11, 8, 16, 7, 19, 3, 17, 2, 15, 10)
  val bull = new Point(400, 400)


  def getProperties: Config = {
    if (prop == null) {
      prop = new Properties()
      var input: InputStream = null
      input = new FileInputStream("config.properties")
      prop.load(input)

      u = new Config()
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
    var output = new FileOutputStream("config.properties")
    for(i <- 0 to 3) {
      prop.setProperty(s"src${i}x", u.trSrc(i).x.toString)
      prop.setProperty(s"src${i}y", u.trSrc(i).y.toString)
    }

    prop.store(output, null)
    output.close
  }
}
