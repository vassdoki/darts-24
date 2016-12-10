package darts.processor

import darts.data.{Blured, Observation}
import darts.util.{Config, CvUtil}
import org.bytedeco.javacpp.opencv_core.{Mat, Point, Rect}
import org.bytedeco.javacpp.opencv_imgcodecs.imwrite
import org.bytedeco.javacpp.opencv_imgproc.{circle, medianBlur}

/**
  * Created by vassdoki on 2016.12.07..
  */
object Blurer {
  val MIN_KERNEL_SIZE = 3;
  val MAX_KERNEL_SIZE = 23;
  def blurUntilClear(o: Observation) = {
    val bl = new Mat
    var kernelSize = MIN_KERNEL_SIZE
    while (kernelSize < MAX_KERNEL_SIZE && o.blurs.size < 3) {
      var releaseBlured = true
      medianBlur(o.mogMask, bl, kernelSize)
      // TODO: nem biztos, hogy kell transformálni...
      val blTransformed = CvUtil.transform(bl, o.camNum)
      val (x, y) = CvUtil.findTopWhite(blTransformed, 0, 0)
      if (o.blurs.size == 0 || CvUtil.getDistance(x, y, o.blurs.head.x, o.blurs.head.y) < 10) {
        o.blurs = o.blurs :+ Blured(x, y, kernelSize, blTransformed)
        releaseBlured = false
      } else {
        o.blurs = List(Blured(x, y, kernelSize, blTransformed))
        releaseBlured = false
      }
      if (Config.DEBUG_BLURER) {
        circle(blTransformed, new Point(x, y), 20, Config.COLOR_WHITE, 3, 8, 0)
        imwrite(f"${Config.OUTPUT_DIR}/${o.filename}-cam:${o.camNum}-blured-${kernelSize}%02d.jpg", blTransformed);
      }
      kernelSize += 2
      if (releaseBlured) blTransformed.release
      bl.release
    }

    if (Config.DEBUG_BLURER) {
      val blTransformed = CvUtil.transform(o.mogMask, o.camNum)
      o.blurs.foreach((i: Blured) =>
        circle(blTransformed, new Point(i.x, i.y), 20, Config.COLOR_WHITE, 3, 8, 0)
      )

      imwrite(f"${Config.OUTPUT_DIR}/${o.filename}-cam:${o.camNum}-blured-x.jpg", blTransformed);
      blTransformed.release
    }
  }

}
