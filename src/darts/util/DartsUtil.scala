package darts.util

import org.bytedeco.javacpp.opencv_core.Point

/**
 * Created by vassdoki on 2016.11.28..
 */
object DartsUtil {
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
      case 2 => (1, number)
      case 3 => (3, number)
      case 4 => (1, number)
      case 5 => (2, number)
      case 6 => (0, number)
    }
  }

}
