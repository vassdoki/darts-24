package darts

import java.nio.FloatBuffer
import java.util

import org.bytedeco.javacpp.helper.opencv_core.CvArr
import org.bytedeco.javacpp.{FloatPointer, IntPointer}
import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.opencv_imgcodecs._
import org.bytedeco.javacpp.opencv_imgproc._


/**
 * Created by vassdoki on 2016.08.08..
 */
object TransformTest extends App {
  val image: Mat = imread("/home/vassdoki/Dropbox/darts/v2/cam/orig-0001-0000.jpg")
  val i2 = new CvMat(image)
  val config = Utils.getProperties
  val src: Array[Float] = (0 to 3).map { i => List(config.trSrc(i).x.toFloat, config.trSrc(i).y.toFloat) }.flatten.toArray
  val dst: Array[Float] = Array(200f, 200f, 333f, 131f, 66f, 131f, 223f, 348f).map{f => f * 2}
  var mat = new CvMat(new Mat(3,3, CV_64F))
  mat = cvGetPerspectiveTransform(src, dst, mat)
  val src2 = cvCloneMat(i2)
  cvWarpPerspective(i2, src2, mat)

  val color: CvScalar = new CvScalar(250, 250, 5, 0)
  drawTable(src2, color)
  val src22 = new Mat(src2)

  imwrite("/tmp/d/x.jpg", src22)
  imwrite("/tmp/d/orig.jpg", image)
  imwrite("/tmp/d/orig-i2.jpg", new Mat(i2))


  def drawTable(src: CvArr, color: CvScalar) = {
    val distancesFromBull = List(7, 14, 87, 96, 142, 150).map(x => x * 2)
    val bull: CvPoint = new CvPoint(400, 400)

    distancesFromBull map { dist => cvCircle(src, bull, dist, color, 2, 8, 0) }
    for (d <- 9 to 351 by 18) {
      cvLine(src, rotatePoint(bull, d, 17), rotatePoint(bull, d, 300), color,2, 8, 0)
    }
    val color2 = new CvScalar(51,255,255,0)
    cvLine(src, Array(400,400), Array(400,400), color2,3, 8, 0)
    cvLine(src, Array(666, 262), Array(400,400), color2,3, 8, 0)
    cvLine(src, Array(132, 262), Array(400,400), color2,3, 8, 0)
    cvLine(src, Array(446, 696), Array(400,400), color2,3, 8, 0)
    src
  }

  def rotatePoint(c: CvPoint, degree: Float, radius: Float): CvPoint = {
    val cos = Math.cos(Math.PI * degree / 180)
    val sin = Math.sin(Math.PI * degree / 180)
    new CvPoint((c.x + cos * radius).toInt, (c.y - sin * radius).toInt)
  }

}
