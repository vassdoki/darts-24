package darts.processor

import darts.data.{LineDetected, Observation}
import darts.util.{Config, CvUtil}
import org.bytedeco.javacpp.indexer.{IntRawIndexer, UByteRawIndexer}
import org.bytedeco.javacpp.opencv_core.{CV_PI, LINE_AA, Mat, Point}
import org.bytedeco.javacpp.opencv_imgcodecs.imwrite
import org.bytedeco.javacpp.opencv_imgproc.{HoughLinesP, circle, line}

/**
  * Created by vassdoki on 2016.12.20..
  */
object LineDetector {

  def detect2(o: Observation) = {
    if (o.blurs.size == 0) {
      Blurer.blurUntilClear(o)
    }
    if (o.blurs.size == 0) throw new Exception("Blurer did not return result.")
    var blured = o.blurs.head.bluredImage
    val destPoint = new Point(o.blurs.head.x, o.blurs.head.y)
    val indexer = blured.createIndexer().asInstanceOf[UByteRawIndexer]
    val minRows = Math.max(destPoint.x - 500, 0)
    val maxRows = Math.min(destPoint.x + 500, blured.rows)
    val minCols = destPoint.y
    val maxCols = blured.cols

    /** (degree, count) */
    val degCount = scala.collection.mutable.Map[Int,Int]()

    for(i <- minRows to maxRows; j <- minCols to maxCols) {
      if (indexer.get(j, i, 0) > 200) {
        if (destPoint.x != j) {
          val degree: Int = Math.round(CvUtil.rad2dec(Math.atan((destPoint.y - j).toDouble / (destPoint.x - i)))).toInt
          degCount(degree) = degCount.getOrElse(degree, 0) + 1
          indexer.put(j, i, 1, 15)
        }
      } else {
        indexer.put(j, i, 2, 5)
      }
    }
    val degMaxCount = degCount.map(x => x._2).max

    for(i <- minRows to maxRows; j <- minCols to maxCols) {
      if (indexer.get(j, i, 0) > 200) {
        if (destPoint.x != j) {
          val degree: Int = Math.round(CvUtil.rad2dec(Math.atan((destPoint.y - j).toDouble / (destPoint.x - i)))).toInt
          if (degCount.getOrElse(degree, 0) == degMaxCount) {
            circle(blured, new Point(i, j), 3, Config.COLOR_GREEN, 2, 8, 0)
            val rad = Math.sin(CvUtil.dec2rad(-1*degree))
            val m = (i - j * rad).toInt

//            indexer.put(j, i, 0, 50)
//            indexer.put(j, i, 1, 55)
//            indexer.put(j, i, 3, 255)
          }
        }
      }
    }


    println(degCount)
    circle(blured, destPoint, 10, Config.COLOR_RED, 3, 8, 0)
    imwrite(f"${Config.OUTPUT_DIR}/${o.filename}-line2.jpg", blured)

    //TODO itt az átlag s és átlag m-et kéne visszaadni, illetve abból generálni egy l1 és l2 pontot
    o.lineDetected = LineDetected(0.5, 10, new Point(10,10), new Point(50,100))

  }

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
      var mList: List[Int] = Nil
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

      val map = scala.collection.mutable.Map[Int,Int]()

      for (i <- 0 until lines.rows()) {
        val pt1 = new Point(indexer.get(i, 0, 0), indexer.get(i, 0, 1))
        val pt2 = new Point(indexer.get(i, 0, 2), indexer.get(i, 0, 3))
        val distLine = CvUtil.getPointDistanceFromLine(pt1, pt2, destPoint)
        if (distLine < 20) {
          val degree = Math.round(CvUtil.getDegree(pt1, pt2).toFloat)
          if (degree == maxCountDegree) {
            map((pt1.y - pt1.x * s).toInt) = map.getOrElse((pt1.y - pt1.x * s).toInt, 0) + 1
          }
        }
      }


      val longests = map.filter(x => x._2 == map.map(_._2).max).map(_._1)
      val m = (longests.sum / longests.size).toInt
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
