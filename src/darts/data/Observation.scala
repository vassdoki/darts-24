package darts.data

import org.bytedeco.javacpp.opencv_core.Mat

/**
  * Created by vassdoki on 2016.12.03..
  * This class holds one camera observation and every details of the processing
  * related to this. This is where the state of the processing is stored, while
  * the processors are stateless.
  */
class Observation(val orig: Mat,
                  val filename:String,
                  val camNum:Int,
                  val mogMask:Mat,
                  val mogMaskNonZero: Int) {
  var hit: Hit = null

  /**
    * (kernelSize, x, y)
    */
  var blurs: List[(Int, Int, Int)] = Nil

  def setMogMask(m: Mat) = {
    //mogMask = m.clone
  }

  def release = {
    if (orig != null) {
      orig.release
    }
    if (mogMask != null) {
      mogMask.release
    }
  }
}
