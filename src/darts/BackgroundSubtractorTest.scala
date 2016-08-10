package darts

import java.io.File
import java.nio.ByteBuffer

import org.bytedeco.javacpp.{BytePointer, Pointer}
import org.bytedeco.javacpp.indexer.UByteBufferIndexer
import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.opencv_imgproc._
import org.bytedeco.javacpp.opencv_imgcodecs._
import org.bytedeco.javacpp.opencv_video._

/**
 * Created by vassdoki on 2016.08.08..
 */
object BackgroundSubtractorTest extends App{
//  val image1: Mat = imread("/home/vassdoki/Dropbox/darts/v2/cam/orig-0457-0028.jpg")
//  val image2: Mat = imread("/home/vassdoki/Dropbox/darts/v2/cam/orig-0458-0028.jpg")

//  var mask: Mat = new Mat(image1.rows(), image1.cols(), IPL_DEPTH_8U)
  var mask: Mat = new Mat()

  val bull = new Point(400, 400)
  val distancesFromBull = List(14, 28, 174, 192, 284, 300)


  var mog = createBackgroundSubtractorMOG2()
  mog.setDetectShadows(true)
  println("detect shadows: " + mog.getDetectShadows())
  mog.setShadowValue(500)
  mog.setComplexityReductionThreshold(0.05) // default: 0.05
  println("ComplexityReductionThreshold: " + mog.getComplexityReductionThreshold())
  mog.setBackgroundRatio(0.9999) // default: 0.9
  println("BackgroundRatio: " + mog.getBackgroundRatio)
  mog.setVarMin(4) // default: 4
  println("varMin: " + mog.getVarMin)
  mog.setVarMax(75) // default: 75
  println("varMax: " + mog.getVarMax)
  mog.setVarThreshold(128)  // default: 16
  println("varThreshold: " + mog.getVarThreshold)
  mog.setVarThresholdGen(9) // default: 9
  println("varThresholdGen: " + mog.getVarThresholdGen)

  val d = new File("/home/vassdoki/Dropbox/darts/v2/cam")
  val x: Seq[File] = Seq(d.listFiles.filter(_.isFile).filter(_.getName.startsWith("orig-")).sorted: _*)

  var i = 0
  var skip = false
  var first = false
  var image:Mat = null
  var maxOutputNumber = 99999

  x.foreach(f => {
    if (image != null) {
      image.release()
    }
    if (maxOutputNumber > 0) {
      image = imread(f.getAbsolutePath)
      mog.apply(image, mask, 0.1)
      //println("file: " + f.getName)
    } else {
      skip = true
    }

    if (countNonZero(mask) > 50) {
      if (skip == false) {
        skip = true
        imwrite(f"/tmp/d/$i%05d-a-orig-${f.getName}.jpg", image)
        imwrite(f"/tmp/d/$i%05d-c-mask.jpg", mask)
        maxOutputNumber -= 1

        // Remove noise with a median filter
        val dest = new Mat()
        val dest2 = new Mat()
        val kernelSize = 3
        medianBlur(mask, dest, kernelSize)
        medianBlur(dest, dest2, kernelSize + 2)
        imwrite(f"/tmp/d/$i%05d-d-median.jpg", dest2)
        val dest3 = TransformTest.transform(dest2)
        // TODO: nem értem miért nem jó a második, ami a MAT-tal dolgozik
        val (x,y) = findTopWhite(new IplImage(dest3))
        //val (x,y) = findTopWhite(dest3)
         val color: Scalar = new Scalar(250, 250, 5, 0)
        TransformTest.drawTable(dest3, color)

        val (mod, num) = identifyNumber(new Point(x,y))

        imwrite(f"/tmp/d/$i%05d-b-result-$mod-$num-2.jpg", dest3)


        dest.release()
        dest2.release()

        /* ERODE
      val src = new CvMat(mask)
      val dst = new CvMat(mask)
      cvErode(src,dst)
      //cvDilate(dst,dst)
      cvSaveImage(f"/tmp/d/mog$i%05d" + f.getName + "-dilate.jpg", dst)
      */
      }
    } else {
      skip = false
    }


    i += 1
  })

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
  def identifyNumber(p: Point): Tuple2[Int, Int] = {
    val degree = getDegreeFromBull(p)
    val distance = getDistanceFromBull(p)
    // 6-os közepe a 0 fok és óra járásával ellentétes irányba megy
    val nums = List(6, 13, 4, 18, 1, 20, 5, 12, 9, 14, 11, 8, 16, 7, 19, 3, 17, 2, 15, 10)

    val int: Int = Math.floor((degree + 9) / 18).toInt
    val number = if (int > 19) nums(0) else nums(int)

    val circleNumber: Int = distancesFromBull filter { dfb => dfb < distance } length

    circleNumber match {
      case 0 => (2, 25)
      case 1 => (1, 25)
      case 3 => (3, number)
      case 5 => (2, number)
      case _ => (1, number)
    }
  }

  def getDistanceFromBull(p: Point): Double = {
    Math.sqrt(sq(bull.x - p.x) + sq(bull.y - p.y))
  }


  def getDegreeFromBull(p: Point) = getDegree(bull, p)

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
    Math.sqrt(sq(bull.x - p.x) + sq(bull.y - p.y))
  }

  def sq(a: Float): Float = a * a

}
