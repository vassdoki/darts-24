package darts.cmd

import java.io.File

import darts.data.Observation
import darts.processor.Blurer
import darts.util.{Config, CvUtil}
import org.bytedeco.javacpp.indexer.{FloatRawIndexer, IntRawIndexer}
import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.opencv_imgcodecs.{imread, imwrite}
import org.bytedeco.javacpp.opencv_imgproc._

import scala.math.{Pi, cos, round, sin}

/**
  * Created by vassdoki on 2016.12.13..
  */
object LineTest extends App{
  val d = new File(Config.OUTPUT_DIR + "/a-work/")
  val x: Seq[File] = Seq(d.listFiles.filter(_.isFile).sorted: _*)

  val files = Array[File](null, null)
  //  val nums = Array[Int](0, 0)
  //  val names = Array[String]("","")

  // 2016-Nov-28_13-07_25-35-cam:2-state:2-count:5.jpg
  x.foreach((f:File) => {
    val state = f.getName.substring(36,37).toInt
    val cam = f.getName.substring(28,29).toInt
    val num = f.getName.substring(12,24).replaceAll("[_-]", "").toInt
    val filename = f.getName.substring(0,23)

    if (state == 1) {
      val o = new Observation(null, filename, cam, imread(f.getAbsolutePath), -1)
      Blurer.blurUntilClear(o)

      var gray = o.blurs.head.bluredImage //.convertTo(gray, 0)
      cvtColor(gray, gray, CV_BGR2GRAY)
      var lines = new Mat

//      val distanceResolutionInPixels = 1
//      val angleResolutionInRadians = CV_PI / 90
//      val minimumVotes = 200
//      HoughLines(gray, lines, distanceResolutionInPixels, angleResolutionInRadians, minimumVotes)
//
//      cvtColor(gray, gray, CV_GRAY2BGR)
//      var out = gray
//      if (lines != null) {
//        val indexer = lines.createIndexer().asInstanceOf[FloatRawIndexer]
//        var rhoList: List[Float] = Nil
//        var thetaList: List[Float] = Nil
//        for (i <- 0 until lines.rows()) {
//          val rho: Float = indexer.get(i, 0, 0)
//          val theta = indexer.get(i, 0, 1)
//
//          rhoList = rho :: rhoList
//          thetaList = theta :: thetaList
//
//          val (pt1, pt2) = if (theta < Pi / 4.0 || theta > 3.0 * Pi / 4.0) {
//            // ~vertical line
//            // point of intersection of the line with first row
//            val p1 = new Point(round(rho / cos(theta)).toInt, 0)
//            // point of intersection of the line with last row
//            val p2 = new Point(round((rho - out.rows * sin(theta)) / cos(theta)).toInt, out.rows)
//            (p1, p2)
//          } else {
//            // ~horizontal line
//            // point of intersection of the line with first column
//            val p1 = new Point(0, round(rho / sin(theta)).toInt)
//            // point of intersection of the line with last column
//            val p2 = new Point(out.cols, round((rho - out.cols * cos(theta)) / sin(theta)).toInt)
//            (p1, p2)
//          }
//
//          // draw a white line
//          line(out, pt1, pt2, Config.COLOR_GREEN, 1, LINE_8, 0)
//        }
//        //println(rhoList.groupBy(identity).mapValues((x: Seq[Float]) => x.size).filter(b => b._2 > 3))
//        println(thetaList.groupBy(identity).mapValues((x: Seq[Float]) => x.size).filter(b => b._2 > 3))
//        imwrite(f"${Config.OUTPUT_DIR}/$filename-test.jpg", out)
//      }


      val deltaRho: Double = 1
      val deltaTheta: Double = CV_PI / 90
      val minVotes: Int = 20
      val minLength: Double = 30
      val maxGap: Double = 15d
      var degreeList: List[Float] = Nil
      HoughLinesP(gray, lines, deltaRho, deltaTheta, minVotes, minLength, maxGap)
      println(s"gray size: ${gray.size.width()}x${gray.size.height()}")
      if (lines != null && lines.rows > 0) {
        println(s"lines: ${lines.size().height()} x ${lines.size().width()} file: $filename ------")
        val indexer = lines.createIndexer().asInstanceOf[IntRawIndexer]
        //val resized2 = new Mat
        cvtColor(gray, gray, CV_GRAY2BGR)

        val destPoint = new Point(o.blurs.head.x, o.blurs.head.y)
        circle(gray, destPoint, 20, Config.COLOR_YELLOW, 3, 8, 0)

        var maxCountDegree: Double = 0

        for (i <- 0 until lines.rows()) {
          val pt1 = new Point(indexer.get(i, 0, 0), indexer.get(i, 0, 1))
          val pt2 = new Point(indexer.get(i, 0, 2), indexer.get(i, 0, 3))
          val distLine = CvUtil.getPointDistanceFromLine(pt1, pt2, destPoint)
          if (distLine < 20) {
            degreeList = Math.round(CvUtil.getDegree(pt1, pt2).toFloat) :: degreeList
          }
        }
        if (degreeList.size > 0) {
          println(degreeList.groupBy(identity).mapValues((x: Seq[Float]) => x.size))
          val count: Map[Float, Int] = degreeList.groupBy(identity).mapValues((x: Seq[Float]) => x.size)
          maxCountDegree = count.filter((f: (Float, Int)) => f._2 == count.map(x => x._2).max).head._1
          for (i <- 0 until lines.rows()) {
            val pt1 = new Point(indexer.get(i, 0, 0), indexer.get(i, 0, 1))
            val pt2 = new Point(indexer.get(i, 0, 2), indexer.get(i, 0, 3))
            val distLine = CvUtil.getPointDistanceFromLine(pt1, pt2, destPoint)
            if (distLine < 20) {
              if (Math.round(CvUtil.getDegree(pt1, pt2).toFloat) == maxCountDegree) {
                line(gray, pt1, pt2, Config.COLOR_RED, 2, LINE_AA, 0)
              } else {
                line(gray, pt1, pt2, Config.COLOR_YELLOW, 1, LINE_AA, 0)
              }
            } else {
              line(gray, pt1, pt2, Config.COLOR_BLUE, 1, LINE_AA, 0)
            }
          }
        }

        val s = Math.sin(CvUtil.dec2rad(-1*maxCountDegree))
        val m = (destPoint.y - destPoint.x * s).toInt
        val length = 150
        val l1 = new Point(destPoint.x - length, m + (s * (destPoint.x - length)).toInt)
        val l2 = new Point(destPoint.x + length, m + (s * (destPoint.x + length)).toInt)
        line(gray, l1, l2, Config.COLOR_WHITE, 2, LINE_AA, 0)


        imwrite(f"${Config.OUTPUT_DIR}/$filename-test.jpg", gray)
      }



      o.release
    }

  })
}
