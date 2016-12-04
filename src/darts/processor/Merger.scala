package darts.processor

import darts.util.{Config, CvUtil}
import org.bytedeco.javacpp.opencv_core.{Mat, MatExpr, and, or}
import org.bytedeco.javacpp.opencv_imgcodecs.imwrite
import org.bytedeco.javacpp.opencv_imgproc.{COLOR_GRAY2BGR, cvtColor, medianBlur}

/**
  * Created by vassdoki on 2016.12.04..
  */
object Merger {
  def merge(m1: Mat, m2: Mat, filename: String) = {
    val kernelSize = 5

    val m1tr = CvUtil.transform(m1, 1)
    //cvtColor(m1tr, m1tr, COLOR_GRAY2BGR)
    medianBlur(m1tr, m1tr, kernelSize)
    val m1trColor = and(m1tr, Config.COLOR_GREEN).asMat

    val m2tr = CvUtil.transform(m2, 2)
    //cvtColor(m2tr, m2tr, COLOR_GRAY2BGR)
    medianBlur(m2tr, m2tr, kernelSize)
    val m2trColor = and(m2tr, Config.COLOR_RED).asMat

    val res: MatExpr = or(m1trColor, m2trColor)
    try {
      val asMat = res.asMat()
      imwrite(f"${Config.OUTPUT_DIR}/${filename}-merged.jpg", asMat)
    } catch {
      case e: Exception => e.printStackTrace()
    }

    m2tr.release
    m1tr.release
    m1.release
    m2.release
  }
}
