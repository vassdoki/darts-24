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
  val myCamNum = camNum
  val imgName = pImgName
  var imageCount = 0

  var maskColoredTransformed: Mat = null
  var matColoredResult: Mat = null

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

      val matBlack = new Mat(maskOrig.size(), CV_8UC3, Config.COLOR_BLACK)
      maskBlue.copyTo(matBlack, maskOrig)
      maskBlue.release()

      maskColoredTransformed = CvUtil.transform(matBlack, camNum)
      val kernelSize = 5
      medianBlur(maskColoredTransformed, imageBlured, kernelSize)
      val maskBlured = new Mat

      medianBlur(maskOrig, maskBlured, kernelSize)
      val maskTransformed = CvUtil.transform(maskBlured, camNum)
      val (x, y) = findTopWhite(maskTransformed)
      val (mod, num) = identifyNumber(new Point(x, y))
      maskBlured.release()
      maskTransformed.release()

      if (imageCount <= 4) {
        if (matColoredResult != null) matColoredResult.release
        matColoredResult = new Mat
        imageBlured.copyTo(matColoredResult)
        println(s"x: $x y: $y")
        circle(matColoredResult, new Point(x, y), 20, color, 1, 8, 0)
        CvUtil.drawTable(matColoredResult, Config.COLOR_BLUE, 1)
        CvUtil.drawNumbers(matColoredResult, Config.COLOR_BLUE)
        putText(matColoredResult, f"Result: $num (X $mod)     [cam: $camNum]", new Point(30, 20 * Math.abs(camNum)),
          FONT_HERSHEY_PLAIN, // font type
          1, // font scale
          Config.COLOR_YELLOW, // text color (here white)
          3, // text thickness
          8, // Line type.
          false)
      }
      if (Config.SAVE_DR_COLORED) {
        imwrite(f"${Config.OUTPUT_DIR}/${imgName.replace(s"d${camNum}-", "")}-${camNum}.jpg", imageBlured)
      }

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
        //cvtColor(matBlack, matBlack, CV_GRAY2RGB)
        //CvUtil.drawTable(mask, Config.COLOR_YELLOW, 1)
        circle(matBlack, new Point(x, y), 20, Config.COLOR_RED, 3, 8, 0)
        resize(matBlack, resized, resized.size(), 0.5, 0.5, INTER_CUBIC)
        resized.copyTo(outMat(new Rect(w / 2, 0, w / 2, h / 2)))
        // IMG 3
        //cvtColor(imageBlured, imageBlured, CV_GRAY2RGB)
        CvUtil.drawTable(imageBlured, Config.COLOR_YELLOW, 1)
        CvUtil.drawNumbers(imageBlured, Config.COLOR_YELLOW)
        circle(imageBlured, new Point(x, y), 20, Config.COLOR_RED, 3, 8, 0)
        resize(imageBlured, resized, resized.size(), 0.5, 0.5, INTER_CUBIC)
        resized.copyTo(outMat(new Rect(0, h / 2, w / 2, h / 2)))
        // IMG 4
        if (x > 50 && y > 50 && x + 51 < w && y + 51 < h) {
          val hit = matBlack(new Rect(x - 50, y - 50, 101, 101))
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

//      if (Config.GUI_UPDATE) {
//        val w = imageMat.size().width()
//        val h = imageMat.size().height()
//        val outMat = new Mat(imageMat.size(), imageMat.`type`())
//        // IMG 1
//        CvUtil.drawTable(transformedOrig, Config.COLOR_YELLOW, 1)
//        CvUtil.drawNumbers(transformedOrig, Config.COLOR_YELLOW)
//        circle(transformedOrig, new Point(x, y), 20, Config.COLOR_RED, 3, 8, 0)
//
//        putText(transformedOrig, f"File${imgName} cam: $camNum", new Point(20, 20),
//          FONT_HERSHEY_PLAIN, // font type
//          2, // font scale
//          Config.COLOR_RED, // text color (here white)
//          3, // text thickness
//          8, // Line type.
//          false)
//        putText(transformedOrig, f"Number: $num (modifier: $mod)", new Point(20, 40),
//          FONT_HERSHEY_PLAIN, // font type
//          2, // font scale
//          Config.COLOR_RED, // text color (here white)
//          3, // text thickness
//          8, // Line type.
//          false)
//
//        resize(transformedOrig, outMat, outMat.size(), 0.5, 0.5, INTER_CUBIC)
//
//        GameUi.updateImage(Math.abs(camNum) - 1, new ImageIcon(CvUtil.toBufferedImage(outMat)))
//        outMat.release()
//      }
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

  def getResultImage : Mat = {
    matColoredResult
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
    val w = m.cols
    val h = m.rows
    if (w == 0 || h == 0) {
      println("valami nincs rendben")
      (0,0)
    } else {
      val d: BytePointer = i.imageData()

      var j: Int = 0
      while (j < (h * w) - 1 && d.get(j) < 50) {
//        if (d.get(j) > 100) {
//          circle(m, new Point(j % w, j / w), d.get(j) -99, Config.COLOR_WHITE, 1, 8, 0)
//        }
        j = j + 1

      }
      //println(s"$j: w:${j%w} h:{$j/w} value:${d.get(j)}")
      i.release()
      (j % w, j / w)
    }
  }

}

object DartRecognizer {

}
