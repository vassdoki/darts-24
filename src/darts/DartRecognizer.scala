package darts

import java.nio.ByteBuffer
import javax.swing.ImageIcon

import darts.util.{Config, CvUtil, DartsUtil}
import darts.util.CvUtil._
import org.bytedeco.javacpp.indexer.{UByteBufferIndexer, UByteRawIndexer}
import org.bytedeco.javacpp.{BytePointer, opencv_features2d, opencv_imgproc}
import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.opencv_imgcodecs._
import org.bytedeco.javacpp.opencv_imgproc._
import org.bytedeco.javacpp.opencv_video._
import org.bytedeco.javacpp.opencv_features2d._
import org.joda.time.DateTime

import scala.collection.parallel.mutable
import scala.swing.Dimension

/**
 * Created by vassdoki on 2016.08.12..
 */
class DartRecognizer(pImgName: String, camNum: Int) {
  val myCamNum = camNum
  val imgName = pImgName
  var imageCount = 0

  var matColoredResult: Mat = null
  var storedOrig = new Mat

  var savedStates: List[(String, Mat)] = Nil

  val results = scala.collection.mutable.ArrayBuffer.empty[(Int, Int, Int, Int, Int)]
  var result:(Int, Int) = (0,0)
  var x = 0
  var y = 0
  var countNoise = 0


  def findXY(maskOrig: Mat, kernelSize: Int, rect: Rect): (Int, Int) = {
    val part = new Mat
    medianBlur(maskOrig, part, kernelSize)
    val partTransformed = CvUtil.transform(part, camNum)
    if (rect.x + rect.width > partTransformed.cols) {
      rect.width(partTransformed.cols - rect.x)
    }
    if (rect.y + rect.height > partTransformed.rows) {
      rect.height(partTransformed.rows - rect.y)
    }
    if (rect.x < 0) rect.x(0)
    if (rect.y < 0) rect.y(0)

    val (x, y) = findTopWhite(partTransformed(rect), rect.x, rect.y)

//    cvtColor(partTransformed, partTransformed, CV_GRAY2RGB)
//    circle(partTransformed, new Point(x, y), 20, Config.COLOR_GREEN, 3, 8, 0)
//    rectangle(partTransformed,rect, Config.COLOR_YELLOW)
//    imwrite(s"${Config.OUTPUT_DIR}/${pImgName}-$imageCount-kernel:$kernelSize-x:$x-y:$y.jpg", partTransformed)
    part.release()
    partTransformed.release()
    (x,y)
  }

  /**
   * new image to process
   * @param imageMat
   */
  def newImage(imageMat: Mat, maskOrig: Mat, camNum2: Int) = {
    imageCount += 1
    //println(s"new image $imageCount (cam: $camNum)")

    try {
//      val maskBlured = new Mat
//      val kernelSize = 5
//      medianBlur(maskOrig, maskBlured, kernelSize)
//      val maskTransformed = CvUtil.transform(maskBlured, camNum)
//      val (cx, cy) = findTopWhite(maskTransformed, 0, 0)
//      imwrite(s"${Config.OUTPUT_DIR}/${pImgName}-kernel:$kernelSize-x:$cx-y:$cy-orig.jpg", maskTransformed)

      val (x1, y1) = findXY(maskOrig, 17, new Rect(0,0,maskOrig.cols, maskOrig.rows))
      val rectOfInterest = new Rect(x1 - 260, y1 - 130, 520, 140)
      //val rectOfInterest = new Rect(x1 - 5, y1 - 5, 10, 10)
      val (x2, y2) = findXY(maskOrig, 5, rectOfInterest)
      val (mod, num) = DartsUtil.identifyNumber(new Point(x2, y2))
      results += Tuple5(0, mod, num, x, y)

      // if cy < y then the dart was moving on the prev image
      if (imageCount == 1 || y2 < y) {
        // save the last result
        if (matColoredResult != null) matColoredResult.release
        val color = if (Math.abs(camNum) == 1) Config.COLOR_GREEN else Config.COLOR_RED
        matColoredResult = CvUtil.transform(maskOrig, camNum)
        cvtColor(matColoredResult, matColoredResult, COLOR_GRAY2BGR)
        matColoredResult = and(matColoredResult, color).asMat
        //println(s"x: $x y: $y")
        x = x2
        y = y2
        countNoise = countNonZero(maskOrig)
        result = (num, mod)
      }
      // TODO: a kamera oldalától függően jobb felső vagy bal felső pontot kell keresni
      // TODO: ha messze van a két eredmény, akkor a kevésbé zajos képet használjuk
      // TODO: ha túl zajos az 5-ös blur, akkor esetleg legyen nagyobb?
      // TODO: egyenes vonal meghatározás? és azon keresni a végét a dart-nak?

      if (Config.SAVE_DR_COLORED) {
        imwrite(f"${Config.OUTPUT_DIR}/${imgName.replace(s"d${camNum}-", "")}-${camNum}.jpg", matColoredResult)
      }

      if (Config.SAVE_DR_STATE) {
        val transformedOrig = CvUtil.transform(imageMat, camNum)

        val color = if (Math.abs(camNum) == 1) Config.COLOR_GREEN else Config.COLOR_RED
        //val maskBlue = new Mat(maskOrig.size(), CV_8UC3, color)

        var origColored = maskOrig.clone
        cvtColor(origColored, origColored, CV_GRAY2RGB)
        origColored = and(origColored, color).asMat

        val w = imageMat.size().width()
        val h = imageMat.size().height()
        val outMat = new Mat(imageMat.size(), imageMat.`type`())
        var resized = new Mat(h / 2, w / 2, imageMat.`type`())
        // IMG 1
        circle(transformedOrig, new Point(x, y), 20, Config.COLOR_RED, 3, 8, 0)
        resize(transformedOrig, resized, resized.size(), 0.5, 0.5, INTER_CUBIC)
        resized.copyTo(outMat(new Rect(0, 0, w / 2, h / 2)))
        // IMG 2
        resize(origColored, resized, resized.size(), 0.5, 0.5, INTER_CUBIC)
        resized.copyTo(outMat(new Rect(w / 2, 0, w / 2, h / 2)))
        // IMG 3
        //cvtColor(imageBlured, imageBlured, CV_GRAY2RGB)
        val imageBlured = CvUtil.transform(maskOrig, camNum)
        cvtColor(imageBlured, imageBlured, CV_GRAY2RGB)
        CvUtil.drawTable(imageBlured, Config.COLOR_YELLOW, 1)
        CvUtil.drawNumbers(imageBlured, Config.COLOR_YELLOW)
        circle(imageBlured, new Point(x1, y1), 20, Config.COLOR_RED, 3, 8, 0)
        circle(imageBlured, new Point(x2, y2), 20, Config.COLOR_GREEN, 3, 8, 0)
        resize(imageBlured, resized, resized.size(), 0.5, 0.5, INTER_CUBIC)
        resized.copyTo(outMat(new Rect(0, h / 2, w / 2, h / 2)))
        // IMG 4
        if (x > 50 && y > 50 && x + 51 < w && y + 51 < h) {
          val hit = imageBlured(new Rect(x - 50, y - 50, 101, 101))
          resize(hit, resized, resized.size(), 2, 2, INTER_CUBIC)
          resized.copyTo(outMat(new Rect(w / 2, h / 2, w / 2, h / 2)))
        }

        putText(outMat, f"Number: $num (modifier: $mod) c:$imageCount", new Point(w / 2 + 50, 30),
          FONT_HERSHEY_PLAIN, // font type
          2, // font scale
          Config.COLOR_YELLOW, // text color (here white)
          3, // text thickness
          8, // Line type.
          false)
        putText(outMat, f"Number: ${result._1} (modifier: ${result._2})", new Point(w / 2 + 50, 50),
          FONT_HERSHEY_PLAIN, // font type
          2, // font scale
          Config.COLOR_WHITE, // text color (here white)
          3, // text thickness
          8, // Line type.
          false)

        //imwrite(f"${Config.OUTPUT_DIR}/${imgName.replace(s"d${camNum}-", "")}-${camNum}-$imageCount%04d-num:$num% 2d-mod:$mod% 2d.jpg", outMat)
        savedStates = savedStates :+ (s"${imgName.replace(s"d${camNum}-", "")}-${camNum}-$imageCount%04d-num:$num% 2d-mod:$mod% 2d.jpg", outMat)

        resized.release()
        imageBlured.release()
        transformedOrig.release()
        origColored.release()
      }

      //maskTransformed.release()
      Thread.`yield`()
    }catch {
      case e: Exception => {
        println("EXCEPTION in new image")
        e.printStackTrace()
        null
      }
    }

  }

  def getResultImage : Mat = {
    if (Config.SAVE_DR_STATE) {
      savedStates.foreach((x: (String, Mat)) => imwrite(s"${Config.OUTPUT_DIR}/${x._1}", x._2) )
    }
    matColoredResult
  }
  def getResultXY: (Int, Int, Int) = {
    (x, y, countNoise)
  }

  /**
   * Return the result and free every allocated memory
   * @return (mod, num): mod: 1, 2(double), 3(triple), num: the number hit
   */
  def getResult: Tuple2[Int, Int] = {
    (result._1, result._2)
//    if (results.isEmpty) {
//      (0,0)
//    } else {
//      val res = results.reduceLeft((a, b) => if (a._1 < b._1) a else b)
//      //println("result: results.size: " + results.size + " mod: " + res._2 + " num: " + res._3)
//      (res._2, res._3)
//    }
  }

  def release = {
    //imageBlured.release()
    //images.foreach(i => i.release())
    storedOrig.release()
    savedStates.foreach((x: (String, Mat)) => x._2.release() )
  }

  def findTopWhite(m: Mat, xOffset: Int, yOffset: Int): (Int, Int) = {
    val sI: UByteRawIndexer = m.createIndexer()
    var j = 0
    val w = m.cols
    val h = m.rows
    var x = 0
    var y = 0
    var color = 0
    var resJ = 0

    //val debug = new Mat(m.rows, m.cols,CV_8UC3)

    while(y < h  && x < w && resJ == 0) { //  && color < 50
      color = sI.get(y, x, 0) & 0xFF
      if (resJ == 0 && color > 100) {
        resJ = j
      }
//      if (color == 0) {
//        circle(debug, new Point(x, y), 1, Config.COLOR_BLUE, 1, 8, 0)
//      } else {
//        if (color < 100) {
//          circle(debug, new Point(x, y), 1, Config.COLOR_RED, 1, 8, 0)
//        } else {
//          circle(debug, new Point(x, y), 1, Config.COLOR_YELLOW, 1, 8, 0)
//        }
//      }
      j += 1
      x = j % w
      y = j / w
    }

    //imwrite(s"${Config.OUTPUT_DIR}/${pImgName}-$imageCount-box-a-source-$xOffset-$w-$h.jpg", m)
    //imwrite(s"${Config.OUTPUT_DIR}/${pImgName}-$imageCount-box-b.jpg-$xOffset-$w-$h.jpg", debug)

    (resJ % w + xOffset, resJ / w + yOffset)

  }
  def findTopWhiteOld(m: Mat, xOffset: Int, yOffset: Int) : (Int, Int) = {
    val i = new IplImage(m)
    val w = m.cols
    val h = m.rows
    val debug = new Mat(m.rows, m.cols,CV_8UC3)
    if (w == 0 || h == 0) {
      println("valami nincs rendben")
      (0,0)
    } else {
      val d: BytePointer = i.imageData()

      var j: Int = 0
      var resJ = 0
      while (j < (h * w) - 1 && (resJ == 0 || xOffset > 0)) {
        val byte: Int = d.get(j) & 0xFF
        if (byte > 200 && resJ == 0) {
          resJ = j
        }
        if (byte > 50) {
                  circle(m, new Point(j % w, j / w), byte/10, Config.COLOR_WHITE, 1, 8, 0)
        }
        if (xOffset > 0) {
          if (byte == 0) {
            circle(debug, new Point(j % w, j / w), 1, Config.COLOR_BLUE, 1, 8, 0)
          } else {
            if (byte < 200) {
              circle(debug, new Point(j % w, j / w), 1, Config.COLOR_RED, 1, 8, 0)
            } else {
              circle(debug, new Point(j % w, j / w), 1, Config.COLOR_YELLOW, 1, 8, 0)
            }
          }
        }

        j = j + 1
      }
      if (xOffset > 0) {
        imwrite(s"${Config.OUTPUT_DIR}/${pImgName}-$imageCount-box-a-source.jpg", m)
        imwrite(s"${Config.OUTPUT_DIR}/${pImgName}-$imageCount-box-b.jpg", debug)
      }
      debug.release()
      println(s"$j: w:${j%w} h:{$j/w} value:${d.get(resJ)}")
      i.release()
      (resJ % w + xOffset, resJ / w + yOffset)
    }
//    // TODO: implement it using Mat
//    val w = m.cols
//    val h = m.rows
//    if (w == 0 || h == 0) {
//      println("valami nincs rendben")
//      (0,0)
//    } else {
//      val d: ByteBuffer = m.asByteBuffer()
//
//
//      var j: Int = 0
//      while (j < (h * w) - 1 && d.get(j) <= 200) {
//        if (d.get(j) > 10)
//        circle(m, new Point(j % w, j / w), d.get(j) / 10, Config.COLOR_WHITE, 1, 8, 0)
//        j = j + 1
//
//      }
//      println(s"$j: w:${j%w} h:{$j/w} value:${d.get(j)}")
//      (j % w, j / w)
//    }
  }

}

object DartRecognizer {

}
