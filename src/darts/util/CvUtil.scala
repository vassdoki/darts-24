package darts.util

import java.awt.image.BufferedImage

import org.bytedeco.javacpp.indexer.FloatIndexer
import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.opencv_imgproc._
import org.bytedeco.javacv.Java2DFrameConverter
import org.bytedeco.javacv.OpenCVFrameConverter.ToMat

/**
 * Created by vassdoki on 2016.08.11..
 */
object CvUtil {
  val openCVConverter = new ToMat()
  val java2DConverter = new Java2DFrameConverter()


  def transform(image: Mat, cam: Int): Mat = synchronized {
    val config = Config.getProperties(cam)
    //val src: Array[Float] = (0 to 3).map { i => List(config.trSrc(i).x.toFloat, config.trSrc(i).y.toFloat) }.flatten.toArray
    val src = config.trSrc.map(p => new Point2f(p.getX.toFloat, p.getY.toFloat)).toSeq
    val dst = (0 to 3).map(i => new Point2f(Config.transformationDst(i * 2), Config.transformationDst(i*2 + 1))).toSeq

    val srcMat = toMatPoint2f(src)
    val dstMat = toMatPoint2f(dst)

    var mat = new Mat(3, 3, CV_64F)
    mat = getPerspectiveTransform(srcMat, dstMat)
    val i2 = new Mat(image.size, image.`type`())
    warpPerspective(image, i2, mat, i2.size())
    mat.release()
    i2
  }

  def toMatPoint2f(points: Seq[Point2f]): Mat = synchronized  {
    // Create Mat representing a vector of Points3f
    val dest = new Mat(1, points.size, CV_32FC2)
    val indx = dest.createIndexer().asInstanceOf[FloatIndexer]
    for (i <- points.indices) {
      val p = points(i)
      indx.put(0, i, 0, p.x)
      indx.put(0, i, 1, p.y)
    }
    require(dest.checkVector(2) >= 0)
    dest
  }

  def toMatArrayFloat(f: Array[Float]): Mat =  synchronized {
    // Create Mat representing a vector of Points3f
    val dest = new Mat(1, f.size, CV_32F)
    val indx = dest.createIndexer().asInstanceOf[FloatIndexer]
    for (i <- f.indices) {
      val p = f(i)
      indx.put(0, i, 0, p)
    }
    dest
  }


  def drawTable(src: Mat, color: Scalar, lineWidth: Int = 2) =  synchronized {
    val bull: Point = new Point(Config.bull.x, Config.bull.y)

    Config.distancesFromBull map { dist => circle(src, bull, dist, color, lineWidth, 8, 0) }
    for (d <- 9 to 351 by 18) {
      line(src, rotatePoint(bull, d, Config.distancesFromBull(1)), rotatePoint(bull, d, Config.distancesFromBull(5)), color,lineWidth, 8, 0)
    }
  }

  def drawNumbers(src: Mat, color: Scalar) =  synchronized {
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
  }

  def drawCross(src: Mat, x: Int, y: Int, colorNum: Int = 0, size: Int = 10) =  synchronized {
    val color = List(
      new Scalar(51,255,255,0),new Scalar(255,51,255,0),new Scalar(255,255,51,0)
    )
    line(src, new Point(x - size,y), new Point(x+size,y), color(colorNum), 2, 8, 0)
    line(src, new Point(x,y - size), new Point(x,y+size), color(colorNum), 2, 8, 0)
  }

  def rotatePoint(c: Point, degree: Float, radius: Float): Point = synchronized  {
    val cos = Math.cos(Math.PI * degree / 180)
    val sin = Math.sin(Math.PI * degree / 180)
    new Point((c.x + cos * radius).toInt, (c.y - sin * radius).toInt)
  }

  def toBufferedImage(mat: Mat): BufferedImage =  synchronized {
    try {
      if (openCVConverter == null || java2DConverter == null) {
        println("CONVERTER NULL")
        null
      } else {
        if (mat == null) {
          println("MAT NULL??")
          null
        } else {
          java2DConverter.convert(openCVConverter.convert(mat))
        }
      }
    }catch {
      case e: Exception => {
        println("ToBufferedImage EXCEPTION")
        e.printStackTrace()
        null
      }
    }
  }

  def getDistanceFromBull(p: Point): Double =  synchronized {
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
