package darts.data

import darts.util.Config
import org.bytedeco.javacpp.opencv_core.Mat
import org.bytedeco.javacpp.opencv_imgcodecs.imwrite

/**
  * Created by vassdoki on 2016.12.03..
  * List of observations that are sequentian and are in the same state
  */
class ObservationList(camNum: Int) {
  /**
    * States:
    * 0: empty table
    * 1: table with dart
    * 2: table with hands taking out the dart
    */
  var state = 0
  var list = List[Observation]()
  var emptyCount = 0

  def add(orig: Mat, filename:String, camNum:Int, mogMask:Mat, mogMaskNonZero: Int) = {
    if (needsMore) {
      list = list :+ new Observation(orig, filename, camNum, mogMask, mogMaskNonZero)
    }else {
      addEmpty
    }
  }

  /** if we don't need more image to decide the state, then it would be a waste of resource to clone and store the
    * images. Just count them in this case.
    * @return
    */
  def needsMore: Boolean = list.size < 5
  def addEmpty = {
    emptyCount += 1
  }

  def removeFirstObservation = {
    if (list.size > 0) {
      list.head.release
      list = list.tail
    }
  }

  /**
    * Decide the observation list state by the length of the movement
    * @return
    */
  def getState: Int = {
    list.size match{
      case i if i <= 2 => state = 0
      case i if i == 3 => state = 1
      case 4 => {
        // this first observation is probably a moving dart
        removeFirstObservation
        state = 1
      }
      case i if i > 4 => state = 2
    }
    state
  }


  def reset = {
    list.foreach((o: Observation) => o.release)
    list = List()
    state = 0
  }

  def debugPrintMask = {
    println(s"handle list. size: ${list.size + emptyCount} state: $state ---------------------")
    if (list.size > 1) {
      imwrite(f"${Config.OUTPUT_DIR}/${list.head.filename}-cam:$camNum-state:$state-count:${list.size}.jpg", list.head.mogMask);
    }
  }
}
