package darts

import javax.swing.ImageIcon

import darts.util.{Config, CvUtil}
import darts.util.CvUtil._
import org.bytedeco.javacpp.{opencv_imgproc, opencv_features2d, BytePointer}
import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.opencv_imgcodecs._
import org.bytedeco.javacpp.opencv_imgproc._
import org.bytedeco.javacpp.opencv_video._
import org.bytedeco.javacpp.opencv_features2d._
import org.joda.time.DateTime

import scala.collection.parallel.mutable

/**
 * Created by vassdoki on 2016.08.12..
 */
class DartRecognizer(pImgName: String, camNum: Int) {
  val imgName = pImgName
  var imageCount = 0

  var maskColoredTransformed: Mat = null

  var imageBlured = new Mat
  //val images = scala.collection.mutable.ListBuffer.empty[Mat]
  var storedOrig = new Mat

  val results = scala.collection.mutable.ArrayBuffer.empty[(Int, Int, Int, Int, Int)]

  /**
   * new image to process
   * @param imageMat
   */
  def newImage(imageMat: Mat, maskOrig: Mat, camNum2: Int): Mat = {
    imageCount += 1
    //println(s"new image $imageCount (cam: $camNum)")

    try {
      val transformedOrig = CvUtil.transform(imageMat, camNum)
      //imwrite(f"${Config.OUTPUT_DIR}/${imgName}-b-$imageCount%04d-cam$camNum.jpg", transformedOrig)


      val color = if (Math.abs(camNum) == 1) Config.COLOR_GREEN else Config.COLOR_RED
      val maskBlue = new Mat(maskOrig.size(), CV_8UC3, color)
      //maskBlue.setTo(Config.COLOR_WHITE, maskOrig)
      val matBlack = new Mat(maskOrig.size(), CV_8UC3, Config.COLOR_BLACK)
      maskBlue.copyTo(matBlack, maskOrig)
      maskBlue.release()
      //setTo(matBlack, maskOrig)
      //threshold()
      maskColoredTransformed = CvUtil.transform(matBlack, camNum)
      if (Config.SAVE_DR_COLORED) {
        imwrite(f"${Config.OUTPUT_DIR}/${imgName.replace(s"d${camNum}-", "")}-${camNum}.jpg", maskColoredTransformed)
      }

      val mask = CvUtil.transform(maskOrig, camNum)
      //val transformedOrig = imageMat

      //images += transformedOrig
      storedOrig = imageMat
      //CvUtil.drawTable(transformedOrig, Config.COLOR_BLUE, 6)
      val kernelSize = 5
      medianBlur(mask, imageBlured, kernelSize)

      val (x, y) = findTopWhite(imageBlured)
      val (mod, num) = identifyNumber(new Point(x, y))

      //      circle(imageBlured, new Point(x, y), 20, Config.COLOR_RED, 3, 8, 0)
      //      imwrite(f"${OUTPUT_DIR}/a-$imageCount%04d-${imgName}-zero:$nonZero%06d-num:$num% 2d-mod:$mod% 2d.jpg", imageBlured)
      //      GameUi.updateImage(0,new ImageIcon(CvUtil.toBufferedImage(imageBlured)))

      if (Config.SAVE_DR_STATE) {
        val w = imageMat.size().width()
        val h = imageMat.size().height()
        val outMat = new Mat(imageMat.size(), imageMat.`type`())
        var resized = new Mat(h / 2, w / 2, imageMat.`type`())
        // IMG 1
        circle(transformedOrig, new Point(x, y), 20, Config.COLOR_RED, 3, 8, 0)
        resize(transformedOrig, resized, resized.size(), 0.5, 0.5, INTER_CUBIC)
        resized.copyTo(outMat(new Rect(0, 0, w / 2, h / 2)))
        // IMG 2
        cvtColor(mask, mask, CV_GRAY2RGB)
        //CvUtil.drawTable(mask, Config.COLOR_YELLOW, 1)
        circle(mask, new Point(x, y), 20, Config.COLOR_RED, 3, 8, 0)
        resize(mask, resized, resized.size(), 0.5, 0.5, INTER_CUBIC)
        resized.copyTo(outMat(new Rect(w / 2, 0, w / 2, h / 2)))
        // IMG 3
        cvtColor(imageBlured, imageBlured, CV_GRAY2RGB)
        CvUtil.drawTable(imageBlured, Config.COLOR_YELLOW, 1)
        CvUtil.drawNumbers(imageBlured, Config.COLOR_YELLOW)
        circle(imageBlured, new Point(x, y), 20, Config.COLOR_RED, 3, 8, 0)
        resize(imageBlured, resized, resized.size(), 0.5, 0.5, INTER_CUBIC)
        resized.copyTo(outMat(new Rect(0, h / 2, w / 2, h / 2)))
        // IMG 4
        if (x > 50 && y > 50 && x + 51 < w && y + 51 < h) {
          val hit = mask(new Rect(x - 50, y - 50, 101, 101))
          resize(hit, resized, resized.size(), 2, 2, INTER_CUBIC)
          resized.copyTo(outMat(new Rect(w / 2, h / 2, w / 2, h / 2)))
        }

        putText(outMat, f"Number: $num (modifier: $mod)", new Point(w / 2 + 50, 30),
          FONT_HERSHEY_PLAIN, // font type
          2, // font scale
          Config.COLOR_YELLOW, // text color (here white)
          3, // text thickness
          8, // Line type.
          false)

        //imwrite(f"${Config.OUTPUT_DIR}/${imgName}-a-$imageCount%04d-zero:$nonZero%06d-num:$num% 2d-mod:$mod% 2d.jpg", mask)
        imwrite(f"${Config.OUTPUT_DIR}/${imgName.replace(s"d${camNum}-", "")}-${camNum}-$imageCount%04d-num:$num% 2d-mod:$mod% 2d.jpg", outMat)

        outMat.release()
        resized.release()
      }

      if (Config.GUI_UPDATE) {
        val w = imageMat.size().width()
        val h = imageMat.size().height()
        val outMat = new Mat(imageMat.size(), imageMat.`type`())
        // IMG 1
        CvUtil.drawTable(transformedOrig, Config.COLOR_YELLOW, 1)
        CvUtil.drawNumbers(transformedOrig, Config.COLOR_YELLOW)
        circle(transformedOrig, new Point(x, y), 20, Config.COLOR_RED, 3, 8, 0)

        putText(transformedOrig, f"File${imgName} cam: $camNum", new Point(20, 20),
          FONT_HERSHEY_PLAIN, // font type
          2, // font scale
          Config.COLOR_RED, // text color (here white)
          3, // text thickness
          8, // Line type.
          false)
        putText(transformedOrig, f"Number: $num (modifier: $mod)", new Point(20, 40),
          FONT_HERSHEY_PLAIN, // font type
          2, // font scale
          Config.COLOR_RED, // text color (here white)
          3, // text thickness
          8, // Line type.
          false)

        resize(transformedOrig, outMat, outMat.size(), 0.5, 0.5, INTER_CUBIC)

        GameUi.updateImage(Math.abs(camNum) - 1, new ImageIcon(CvUtil.toBufferedImage(outMat)))
        outMat.release()
      }
      Thread.`yield`()

      //transformedOrig.release()
      results += Tuple5(0, mod, num, x, y)
      //println(s"end image $imageCount (cam: $camNum)")
      maskColoredTransformed
    }catch {
      case e: Exception => {
        println("EXCEPTION in new image")
        e.printStackTrace()
        null
      }
    }

  }

  def getImage(num: Int) : Mat = {
//    if (num >= images.size) {
//      images(images.size - 1)
//    } else {
//      images(num)
//    }
    storedOrig
  }

  /**
   * Return the result and free every allocated memory
   * @return (mod, num): mod: 1, 2(double), 3(triple), num: the number hit
   */
  def getResult: Tuple2[Int, Int] = {
    if (results.isEmpty) {
      (0,0)
    } else {
      val res = results.reduceLeft((a, b) => if (a._1 < b._1) a else b)
      //println("result: results.size: " + results.size + " mod: " + res._2 + " num: " + res._3)
      (res._2, res._3)
    }
  }

  def release = {
    imageBlured.release()
    //images.foreach(i => i.release())
    storedOrig.release()
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
    val w = m.rows
    val h = m.cols
    if (w == 0 || h == 0) {
      println("valami nincs rendben")
      (0,0)
    } else {
      val d: BytePointer = i.imageData()

      var j: Int = 0
      while (j < (h * w) - 1 && (d.get(j) == 0 || d.get(j + 1) == 0)) {
        j = j + 2
      }
      i.release()
      (j % w, j / w)
    }
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
    imwrite(f"${Config.OUTPUT_DIR}/${imgName}-b-$imageCount%04d-mask.jpg", image)
    image.release()
  }

}

object DartRecognizer {
  //val mog = createMog

  def createMog: BackgroundSubtractorMOG2 = {
    var mog = createBackgroundSubtractorMOG2()
    mog.setShadowValue(255)
    mog.setVarThreshold(16) // default: 16
    /*
    mog.setDetectShadows(true)
    println("detect shadows: " + mog.getDetectShadows())
    mog.setShadowValue(100)
    mog.setShadowThreshold(0.2)
    mog.setComplexityReductionThreshold(0.05) // default: 0.05
    println("ComplexityReductionThreshold: " + mog.getComplexityReductionThreshold())
    mog.setBackgroundRatio(0.7) // default: 0.9
    println("BackgroundRatio: " + mog.getBackgroundRatio)
    mog.setVarMin(4) // default: 4
    println("varMin: " + mog.getVarMin)
    mog.setVarMax(75) // default: 75
    println("varMax: " + mog.getVarMax)
    mog.setVarThreshold(400) // default: 16
    println("varThreshold: " + mog.getVarThreshold)
    mog.setVarThresholdGen(9) // default: 9
    println("varThresholdGen: " + mog.getVarThresholdGen)
    */
    mog
  }

}
