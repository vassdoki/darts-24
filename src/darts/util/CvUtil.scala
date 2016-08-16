package darts.util

import java.awt.image.BufferedImage

import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.opencv_imgproc._
import org.bytedeco.javacv.Java2DFrameConverter
import org.bytedeco.javacv.OpenCVFrameConverter.ToMat

/**
 * Created by vassdoki on 2016.08.11..
 */
object CvUtil {
  def transform(image: Mat): Mat = {
    val config = Config.getProperties
    val src: Array[Float] = (0 to 3).map { i => List(config.trSrc(i).x.toFloat, config.trSrc(i).y.toFloat) }.flatten.toArray
    val dst: Array[Float] = Config.transformationDst
    var mat = new CvMat(new Mat(3, 3, CV_64F))
    mat = cvGetPerspectiveTransform(src, dst, mat)
    val i2 = new CvMat(image)
    val src2 = cvCloneMat(i2)
    cvWarpPerspective(i2, src2, mat)
    val mat2 = new Mat(src2)

    //    val color: Scalar = new Scalar(250, 250, 5, 0)
    //    drawTable(mat2, color)
    mat2
  }


  def drawTable(src: Mat, color: Scalar) = {
    val bull: Point = new Point(Config.bull.x, Config.bull.y)

    Config.distancesFromBull map { dist => circle(src, bull, dist, color, 4, 8, 0) }
    for (d <- 9 to 351 by 18) {
      line(src, rotatePoint(bull, d, Config.distancesFromBull(1)), rotatePoint(bull, d, Config.distancesFromBull(5)), color,4, 8, 0)
    }
    drawCross(src, Config.transformationDst(0).toInt, Config.transformationDst(1).toInt)
    drawCross(src, Config.transformationDst(2).toInt, Config.transformationDst(3).toInt)
    drawCross(src, Config.transformationDst(4).toInt, Config.transformationDst(5).toInt)
    drawCross(src, Config.transformationDst(6).toInt, Config.transformationDst(7).toInt)
    src
  }

  def drawNumbers(src: Mat, color: Scalar) = {
    val bull: Point = new Point(Config.bull.x, Config.bull.y)

    var i = 0
    for (d <- 9 to 351 by 18) {
      putText(src, f"${Config.nums(i)}", rotatePoint(bull, d-9, (Config.distancesFromBull(5)*1.1).toInt),
        FONT_HERSHEY_PLAIN, // font type
        3, // font scale
        color, // text color (here white)
        3, // text thickness
        8, // Line type.
        false)
      i += 1
    }
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

  def toBufferedImage(mat: Mat): BufferedImage = {
    val openCVConverter = new ToMat()
    val java2DConverter = new Java2DFrameConverter()
    java2DConverter.convert(openCVConverter.convert(mat))
  }

  def getDistanceFromBull(p: Point): Double = {
    Math.sqrt(sq(Config.bull.x - p.x) + sq(Config.bull.y - p.y))
  }


  def getDegreeFromBull(p: Point) = getDegree(new Point(Config.bull.x, Config.bull.y), p)

  def getDegree(bull: Point, p: Point) = {
    val x = p.x - bull.x
    val y = bull.y - p.y
    var v = 180 * Math.atan2(y, x) / Math.PI
    if (v > 180) {
      v = 180
    }
    if (v < -180) {
      v = -180
    }
    if (v < 0) {
      v += 360
    }
    if (v == 0) {
      //Log.i(TAG, "arch: y: " + y + " x: " + x + " archtan: " + v);
    }
    v
  }

  def getDistanceFromBull(p: CvPoint): Double = {
    Math.sqrt(sq(Config.bull.x - p.x) + sq(Config.bull.y - p.y))
  }

  def sq(a: Float): Float = a * a

  def releaseMat(m: Mat): Unit = {
    if (m != null) {
      m.release()
    }
  }

}
