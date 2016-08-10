package darts

import java.nio.FloatBuffer
import java.util

import org.bytedeco.javacpp.helper.opencv_core.CvArr
import org.bytedeco.javacpp.presets.opencv_core
import org.bytedeco.javacpp.{FloatPointer, IntPointer}
import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.opencv_imgcodecs._
import org.bytedeco.javacpp.opencv_imgproc._


/**
 * Created by vassdoki on 2016.08.08..
 */
object TransformTest{
  val image: Mat = imread("/home/vassdoki/Dropbox/darts/v2/cam/orig-0001-0000.jpg")

  def transform(image: Mat): Mat = {
    val config = Utils.getProperties
    val src: Array[Float] = (0 to 3).map { i => List(config.trSrc(i).x.toFloat, config.trSrc(i).y.toFloat) }.flatten.toArray
    val dst: Array[Float] = Array(200f, 200f, 333f, 131f, 66f, 131f, 223f, 348f).map { f => f * 2 }
    var mat = new CvMat(new Mat(3, 3, CV_64F))
    mat = cvGetPerspectiveTransform(src, dst, mat)
    val i2 = new CvMat(image)
    val src2 = cvCloneMat(i2)
    cvWarpPerspective(i2, src2, mat)
    val mat2 = new Mat(src2)

    val color: Scalar = new Scalar(250, 250, 5, 0)
    drawTable(mat2, color)
    mat2
  }


  def drawTable(src: Mat, color: Scalar) = {
    val distancesFromBull = List(7, 14, 87, 96, 142, 150).map(x => x * 2)
    val bull: Point = new Point(400, 400)

    distancesFromBull map { dist => circle(src, bull, dist, color, 2, 8, 0) }
    for (d <- 9 to 351 by 18) {
      line(src, rotatePoint(bull, d, 17), rotatePoint(bull, d, 300), color,2, 8, 0)
    }
    drawCross(src, 400, 400)
    drawCross(src, 666, 262)
    drawCross(src, 132, 262)
    drawCross(src, 446, 696)
    src
  }

  def drawCross(src: Mat, x: Int, y: Int, colorNum: Int = 0, size: Int = 10) = {
    val color = List(
      new Scalar(51,255,255,0),new Scalar(255,51,255,0),new Scalar(255,255,51,0)
    )
    line(src, new Point(x - size,y), new Point(x+size,y), color(colorNum), 2, 8, 0)
    line(src, new Point(x,y - size), new Point(x,y+size), color(colorNum), 2, 8, 0)
  }

  def rotatePoint(c: Point, degree: Float, radius: Float): Point = {
    val cos = Math.cos(Math.PI * degree / 180)
    val sin = Math.sin(Math.PI * degree / 180)
    new Point((c.x + cos * radius).toInt, (c.y - sin * radius).toInt)
  }

}
