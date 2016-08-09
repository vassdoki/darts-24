package darts

import java.io.File

import org.bytedeco.javacpp.Pointer
import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.opencv_imgproc._
import org.bytedeco.javacpp.opencv_imgcodecs._
import org.bytedeco.javacpp.opencv_video._

/**
 * Created by vassdoki on 2016.08.08..
 */
object BackgroundSubtractorTest extends App{
//  val image1: Mat = imread("/home/vassdoki/Dropbox/darts/v2/cam/orig-0457-0028.jpg")
//  val image2: Mat = imread("/home/vassdoki/Dropbox/darts/v2/cam/orig-0458-0028.jpg")

//  var mask: Mat = new Mat(image1.rows(), image1.cols(), IPL_DEPTH_8U)
  var mask: Mat = new Mat()

  var mog = createBackgroundSubtractorMOG2()
  mog.setDetectShadows(true)
  println("detect shadows: " + mog.getDetectShadows())
  mog.setShadowValue(225)
  mog.setComplexityReductionThreshold(0.05) // default: 0.05
  println("ComplexityReductionThreshold: " + mog.getComplexityReductionThreshold())
  mog.setBackgroundRatio(0.9999) // default: 0.9
  println("BackgroundRatio: " + mog.getBackgroundRatio)
  mog.setVarMin(4) // default: 4
  println("varMin: " + mog.getVarMin)
  mog.setVarMax(75) // default: 75
  println("varMax: " + mog.getVarMax)
  mog.setVarThreshold(128)  // default: 16
  println("varThreshold: " + mog.getVarThreshold)
  mog.setVarThresholdGen(9) // default: 9
  println("varThresholdGen: " + mog.getVarThresholdGen)

  val d = new File("/home/vassdoki/Dropbox/darts/v2/test")
  val x: Seq[File] = Seq(d.listFiles.filter(_.isFile).filter(_.getName.startsWith("orig-")).sorted: _*)

  var i = 0
  x.take(200).foreach(f => {
    val image: Mat = imread(f.getAbsolutePath)
    mog.apply(image, mask, 0.1)
    imwrite(f"/tmp/d/mog$i%05d" + f.getName + ".jpg", mask)
    val src = new CvMat(mask)
    val dst = new CvMat(mask)
    cvErode(src,dst)
    //cvDilate(src,dst)
    //cvDilate(dst,dst)
    cvSaveImage(f"/tmp/d/mog$i%05d" + f.getName + "-dilate.jpg", dst)
    i += 1
  })

}
