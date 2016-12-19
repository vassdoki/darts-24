package darts.processor

import darts.data.{LineDetected, Observation}
import darts.util.{Config, CvUtil}
import org.bytedeco.javacpp.indexer.IntRawIndexer
import org.bytedeco.javacpp.opencv_core.{CV_PI, LINE_AA, Mat, Point}
import org.bytedeco.javacpp.opencv_imgproc.{HoughLinesP, line}

/**
  * Created by vassdoki on 2016.12.20..
  */
object LineDetector {
  def detect(o: Observation) = {
    try {
      var lines = new Mat

      if (o.blurs.size == 0) {
        Blurer.blurUntilClear(o)
      }

      if (o.blurs.size == 0) throw new Exception("Blurer did not return result.")
      var gray = o.blurs.head.bluredImage

      val deltaRho: Double = 1
      val deltaTheta: Double = CV_PI / 90
      val minVotes: Int = 20
      val minLength: Double = 30
      val maxGap: Double = 15d
      var degreeList: List[Float] = Nil
      HoughLinesP(gray, lines, deltaRho, deltaTheta, minVotes, minLength, maxGap)

      if (lines == null && lines.rows == 0) throw  new Exception("Did not find any line.")

      val destPoint = new Point(o.blurs.head.x, o.blurs.head.y)

      val indexer = lines.createIndexer().asInstanceOf[IntRawIndexer]

      for (i <- 0 until lines.rows()) {
        val pt1 = new Point(indexer.get(i, 0, 0), indexer.get(i, 0, 1))
        val pt2 = new Point(indexer.get(i, 0, 2), indexer.get(i, 0, 3))
        val distLine = CvUtil.getPointDistanceFromLine(pt1, pt2, destPoint)
        if (distLine < 20) {
          degreeList = Math.round(CvUtil.getDegree(pt1, pt2).toFloat) :: degreeList
        }
      }
      var maxCountDegree: Double = 0
      if (degreeList.size == 0) throw new Exception("Degree List is empty")

      println(degreeList.groupBy(identity).mapValues((x: Seq[Float]) => x.size))
      val count: Map[Float, Int] = degreeList.groupBy(identity).mapValues((x: Seq[Float]) => x.size)
      maxCountDegree = count.filter((f: (Float, Int)) => f._2 == count.map(x => x._2).max).head._1

      val s = Math.sin(CvUtil.dec2rad(-1*maxCountDegree))
      val m = (destPoint.y - destPoint.x * s).toInt
      val length = 150
      val l1 = new Point(destPoint.x - length, m + (s * (destPoint.x - length)).toInt)
      val l2 = new Point(destPoint.x + length, m + (s * (destPoint.x + length)).toInt)

      o.lineDetected = LineDetected(s, m, l1, l2)

    }catch{
      case e: Exception => {
        print(s"LineDetector exception: ${e.getMessage}")
      }
    }

  }
}
