package darts

import darts.util.{Config, CvUtil}
import org.bytedeco.javacpp.BytePointer
import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.opencv_imgcodecs._
import org.bytedeco.javacpp.opencv_imgproc._
import org.bytedeco.javacpp.opencv_video._

import scala.collection.parallel.mutable

/**
 * Created by vassdoki on 2016.08.12..
 */
class DartRecognizer(paramPrevTable: Mat, imgNum: Int) {
  val OUTPUT_DIR = "/home/vassdoki/darts/v2/d"
  val prevTable: Mat = CvUtil.transform(paramPrevTable)
  // the first images are usually useless
  val START_FROM_IMAGE = 2
  var imageCount = 0

  var mask = new Mat
  var imageBlured = new Mat
  var imageBlured2 = new Mat
  val images = scala.collection.mutable.ListBuffer.empty[Mat]

  val results = scala.collection.mutable.ArrayBuffer.empty[(Int, Int, Int, Int, Int)]

  /**
   * new image to process
   * @param imageMat
   */
  def newImage(imageMat: Mat) = {
    imageCount += 1

    if (imageCount == 1) {
      // initialize mog
      DartRecognizer.mog.apply(prevTable, mask, 1)
    }
    
    if (imageCount > START_FROM_IMAGE) {
      val transformed = CvUtil.transform(imageMat)
      images += transformed
      DartRecognizer.mog.apply(transformed, mask, 0)
      val nonZero = countNonZero(mask)
      val kernelSize = 3
      medianBlur(mask, imageBlured, kernelSize)

      medianBlur(imageBlured, imageBlured2, kernelSize + 4)

      val (x, y) = findTopWhite(imageBlured2)
      val (mod, num) = identifyNumber(new Point(x, y))

//      CvUtil.drawTable(mask, Config.COLOR_RED)
//      imwrite(f"${OUTPUT_DIR}/${imgNum}%05d-b-$imageCount%04d-mask.jpg", mask)
//      CvUtil.drawTable(imageBlured, Config.COLOR_RED)
//      imwrite(f"${OUTPUT_DIR}/${imgNum}%05d-c-$imageCount%04d-blur1.jpg", imageBlured)
//      CvUtil.drawTable(imageBlured2, Config.COLOR_RED)
//      imwrite(f"${OUTPUT_DIR}/${imgNum}%05d-d-$imageCount%04d-blur2.jpg", imageBlured2)

      //transformed.release()
      results += Tuple5(nonZero, mod, num, x, y)
    }
  }

  def getImage(num: Int) : Mat = {
    images(num)
  }

  // TODO: ezt csak igy lehet?
  def getStartImgNum = imgNum

  /**
   * Return the result and free every allocated memory
   * @return (mod, num): mod: 1, 2(double), 3(triple), num: the number hit
   */
  def getResult: Tuple2[Int, Int] = {
    val res = results.reduceLeft((a, b) => if (a._1 < b._1) a else b)
    println("result: results.size: " + results.size + " mod: " + res._2 + " num: " + res._3)
    (res._2, res._3)
  }

  def release = {
    prevTable.release()
    mask.release()
    imageBlured.release()
    images.foreach(i => i.release())
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


  def findTopWhite(m: Mat) : (Int, Int) = {
    // TODO: implement it using Mat
    val i = new IplImage(m)
    val w = i.width()
    val d: BytePointer = i.imageData()

    var j: Int = 0
    while((d.get(j) == 0 || d.get(j+1) == 0) && j < 960 * 720) {
      j = j + 2
    }
    i.release()
    (j % w, j / w)
  }
}

object DartRecognizer {
  val mog = createMog

  def createMog: BackgroundSubtractorMOG2 = {
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
    mog.setVarThreshold(128) // default: 16
    println("varThreshold: " + mog.getVarThreshold)
    mog.setVarThresholdGen(9) // default: 9
    println("varThresholdGen: " + mog.getVarThresholdGen)
    mog
  }

}
