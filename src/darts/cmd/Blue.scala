package darts.cmd

import darts.util.Config
import org.bytedeco.javacpp.indexer.IntRawIndexer
import org.bytedeco.javacpp.opencv_core.Scalar
import org.bytedeco.javacpp.opencv_imgcodecs.{imread, imwrite}
import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.opencv_imgproc._
import org.joda.time.DateTime


/**
  * Created by vassdoki on 2016.11.28..
  */
object Blue extends App {
  val m = imread(s"${Config.OUTPUT_DIR}/2016-Nov-25_11-25_48-87-cam:-1-zero:008770-state:1.jpg")
  t(m, "2016-Nov-25_11-25_48-87-cam:-1-zero:008770-state:1")

  def t(m: Mat, filename: String) = {
    try {
      val imageBlured = new Mat
      val h = m.rows
      val w = m.cols
      for (i <- 7 until 15 by 2) {
        medianBlur(m, imageBlured, i)
        var resized = new Mat(h / 8, w / 8, imageBlured.`type`())
        // IMG 1
        resize(imageBlured, resized, resized.size(), 0.5, 0.5, INTER_CUBIC)

        var lines = new Mat
        var gray = resized
        //cvtColor(resized, gray, CV_RGB2GRAY)
        val deltaRho: Double = 2
        val deltaTheta: Double = CV_PI / 180
        val minVotes: Int = 50
        val minLength: Double = 10
        val minGap: Double = 5d
        HoughLinesP(gray, lines, deltaRho, deltaTheta, minVotes, minLength, minGap)
        if (lines != null) {
          val indexer = lines.createIndexer().asInstanceOf[IntRawIndexer]
          //val resized2 = new Mat
          cvtColor(imageBlured, imageBlured, CV_GRAY2BGR)
          for (i <- 0 until lines.rows()) {
            val pt1 = new Point(indexer.get(i, 0, 0) * 8, indexer.get(i, 0, 1) * 8)
            val pt2 = new Point(indexer.get(i, 0, 2) * 8, indexer.get(i, 0, 3) * 8)
            line(imageBlured, pt1, pt2, Config.COLOR_BLUE, 1, LINE_AA, 0)
          }
          imwrite(f"${Config.OUTPUT_DIR}/$filename-test$i%2d.jpg", imageBlured)
        }
      }
    } catch {
      case e: Exception => println("exception: " + e.getMessage)
    }
  }

  def blue(m: Mat) = {
    val re = and(m, new Scalar(255, 0, 0, 0))
    imwrite(f"${Config.OUTPUT_DIR}/a.jpg", re.asMat())

  }

}
