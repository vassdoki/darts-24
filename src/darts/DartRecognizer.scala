package darts

import darts.util.{Config, CvUtil}
import darts.util.CvUtil._
import org.bytedeco.javacpp.{opencv_imgproc, opencv_features2d, BytePointer}
import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.opencv_imgcodecs._
import org.bytedeco.javacpp.opencv_imgproc._
import org.bytedeco.javacpp.opencv_video._
import org.bytedeco.javacpp.opencv_features2d._

import scala.collection.parallel.mutable

/**
 * Created by vassdoki on 2016.08.12..
 */
class DartRecognizer(paramPrevTable: Mat, imgName: String) {
  val OUTPUT_DIR = "/home/vassdoki/darts/v2/d"
  val prevTable: Mat = CvUtil.transform(paramPrevTable)
  // the first images are usually useless
  val START_FROM_IMAGE = 2
  var imageCount = 0

  var mask = new Mat
  var imageBlured = new Mat
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
      //CvUtil.drawTable(prevTable, Config.COLOR_BLUE, 6)
      DartRecognizer.mog.clear()
      DartRecognizer.mog.apply(prevTable, mask, 1)
    }
    
    if (imageCount > START_FROM_IMAGE) {
      val transformed = CvUtil.transform(imageMat)
      images += transformed
      //CvUtil.drawTable(transformed, Config.COLOR_BLUE, 6)
      DartRecognizer.mog.apply(transformed, mask, 0)
      val nonZero = countNonZero(mask)
      val kernelSize = 19
      medianBlur(mask, imageBlured, kernelSize)

      val (x, y) = findTopWhite(imageBlured)
      val (mod, num) = identifyNumber(new Point(x, y))

      cvtColor(imageBlured, imageBlured, CV_GRAY2RGB)
      cvtColor(mask, mask, CV_GRAY2RGB)

      CvUtil.drawTable(mask, Config.COLOR_YELLOW, 1)
      CvUtil.drawTable(imageBlured, Config.COLOR_YELLOW, 1)
      CvUtil.drawNumbers(imageBlured, Config.COLOR_YELLOW)
      circle(imageBlured, new Point(x, y), 20, Config.COLOR_RED, 3, 8, 0)

      val w = imageMat.size().width()
      val h = imageMat.size().height()
      val outMat = new Mat(imageMat.size(), imageMat.`type`())
      var resized = new Mat(h/2, w/2, imageMat.`type`())
      // IMG 1
      resize(transformed, resized, resized.size(), 0.5, 0.5, INTER_CUBIC)
      resized.copyTo(outMat(new Rect(0,0,w/2,h/2)))
      // IMG 2
      resize(mask, resized, resized.size(), 0.5, 0.5, INTER_CUBIC)
      resized.copyTo(outMat(new Rect(w/2,0,w/2,h/2)))
      // IMG 3
      resize(imageBlured, resized, resized.size(), 0.5, 0.5, INTER_CUBIC)
      resized.copyTo(outMat(new Rect(0,h/2,w/2,h/2)))
      // IMG 4
//      resize(imageBlured, resized, resized.size(), 0.5, 0.5, INTER_CUBIC)
//      resized.copyTo(outMat(new Rect(w/2,h/2,w/2,h/2)))

      putText(outMat, f"Number: $num (modifier: $mod)", new Point(50,h/2+50),
        FONT_HERSHEY_PLAIN, // font type
        2, // font scale
        Config.COLOR_YELLOW, // text color (here white)
        3, // text thickness
        8, // Line type.
        false)

      imwrite(f"${OUTPUT_DIR}/${imgName}-b-$imageCount%04d-zero:$nonZero%06d-num:$num% 2d-mod:$mod% 2d.jpg", outMat)
      outMat.release()
      resized.release()


      //transformed.release()
      results += Tuple5(nonZero, mod, num, x, y)
    }
  }

  def getImage(num: Int) : Mat = {
    if (num >= images.size) {
      images(images.size - 1) 
    } else {
      images(num)
    }
  }

  // TODO: ezt csak igy lehet?
  def getStartImgName = imgName

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

  def detectBlobs(pimage: Mat) = {
    val image: Mat = pimage.clone()
    val params = new SimpleBlobDetector.Params
    params.minDistBetweenBlobs(50.0f)
    params.filterByInertia(false)
    params.filterByConvexity(false)
    params.filterByColor(true)
    params.filterByCircularity(false)
    params.filterByArea(true)
    params.minArea(10.0f)
    params.maxArea(5000.0f)

    val bd = SimpleBlobDetector.create(params)
    //FastFeatureDetector.create()
    //var kv = new opencv_features2d.KeyPoint() //KeyPoint()
    val kv = new KeyPointVector()
    bd.detect(image, kv)

    for (i <- 0 to kv.size().toInt){
      drawCross(image, kv.get(i).pt.x.toInt, kv.get(i).pt.y.toInt)
    }
    imwrite(f"${OUTPUT_DIR}/${imgName}-b-$imageCount%04d-mask.jpg", image)
    image.release()
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
