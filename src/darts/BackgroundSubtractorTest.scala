package darts

import java.io.{PrintWriter, File}
import java.nio.ByteBuffer
import javax.swing.ImageIcon

import darts.util._

import scala.util.control.Breaks._

import org.bytedeco.javacpp.{BytePointer, Pointer}
import org.bytedeco.javacpp.indexer.UByteBufferIndexer
import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.opencv_imgproc._
import org.bytedeco.javacpp.opencv_imgcodecs._
import org.bytedeco.javacpp.opencv_video._

/**
 * Created by vassdoki on 2016.08.08..
 */
class BackgroundSubtractorTest {

  val CAMERA_DEV_NUM = -1
  val COMMAND_LINE = true
  val INPUT_DIR = "/home/vassdoki/darts/v2/test"
  val OUTPUT_DIR = "/home/vassdoki/darts/v2/d"


  var cameraAllowed = false
  //var maxOutputNumber = 9999999

  def commandLine = {
    val d = new File(INPUT_DIR)
    val x: Seq[File] = Seq(d.listFiles.sorted: _*)
    val camera = CaptureTrait.get(x)
    runRecognizer(camera)
    CaptureTrait.releaseCamera()
  }

  def continousCameraUpdate = {
    //val capture = CaptureTrait.get(CAMERA_DEV_NUM)
    val d = new File(INPUT_DIR)
    var capture: CaptureTrait = null
    if (CAMERA_DEV_NUM >= 0) {
      capture = CaptureTrait.get(CAMERA_DEV_NUM)
    } else {
      val x: Seq[File] = Seq(d.listFiles.filter(_.isFile).filter(_.getName.startsWith("orig-")).sorted: _*)
      capture = CaptureTrait.get(x)
    }
    runRecognizer(capture)
    CaptureTrait.releaseCamera()
  }

  def runRecognizer(camera: CaptureTrait) = {
    val mog = createMog
    var imageMat: Mat = null
    var mask: Mat = new Mat()
    var countZero = 0

    // capture the first image, initialize mog
    imageMat = camera.captureFrame
    mog.apply(imageMat, mask, 1)

    try {
      while (cameraAllowed) {
        imageMat = camera.captureFrame
        mog.apply(imageMat, mask, 0.5)
        countZero = countNonZero(mask)
        println(f"${camera.lastFilename}%30s zero: $countZero")
        if (countZero > 0) {
          imwrite(f"$OUTPUT_DIR/${camera.imageNumber}%05d-${camera.lastFilename}%30s zero: $countZero.jpg", mask)
        }
      }
    }catch {
      case e: Exception => {
        println(e)
      }
    }
  }


  def runRecognizerOld(camera: CaptureTrait) = {
    var mask: Mat = new Mat()
    var maskBelso: Mat = new Mat()

//    val out = new PrintWriter(new File("backgroundSubstractorResult.csv"))
//    out.println(f"filename;count;talalat;none zero;mod;num")

    var i = 0
    var first = false
    var imageMat: Mat = null
    var prevImageMat: Mat = null
    var talalat = 0

    val mog = createMog
    val mog2 = createMog

    // Kezdő kép beolvasása
    if (imageMat != null) {
      imageMat.release()
    }
    imageMat = camera.captureFrame
    if (imageMat == null) {
      //maxOutputNumber = -1
    } else {
      mog.apply(imageMat, mask, 1)
    }


    while (cameraAllowed) {
      if (imageMat != null) {
        imageMat.release()
      }
      imageMat = camera.captureFrame
      if (!COMMAND_LINE) GameUi.updateImage(0,new ImageIcon(CvUtil.toBufferedImage(imageMat)))
      if (imageMat == null) {
        break
      } else {
        mog.apply(imageMat, mask, 0.4)
        mog2.apply(imageMat, maskBelso, 0.4)
        if (!COMMAND_LINE) GameUi.updateImage(3,new ImageIcon(CvUtil.toBufferedImage(mask)))
      }

      if (countNonZero(maskBelso) > 1000 && prevImageMat != null) {
        talalat = 1
        //maxOutputNumber -= 1

        //mog2.clear()
        mog2.apply(prevImageMat, maskBelso, 1)
        mog.apply(imageMat, mask, 0.4)
        if (!COMMAND_LINE) GameUi.updateImage(3,new ImageIcon(CvUtil.toBufferedImage(maskBelso)))
        var prevNonZero = -1
        var result_mod = 0
        var result_num = 0

        while (countNonZero(maskBelso) > 1000) {
          imwrite(f"$OUTPUT_DIR/$i%05d-mask-belso-$talalat%05d.jpg", maskBelso) //${f.getName}
          mog.apply(imageMat, mask, 0)
          mog2.apply(imageMat, maskBelso, 0)
          val nonZero = countNonZero(maskBelso)
          if (talalat == 1) {
            prevNonZero = nonZero
          }

          // Remove noise with a median filter
          val dest = new Mat()
          val dest2 = new Mat()
          val kernelSize = 3
          medianBlur(maskBelso, dest, kernelSize)
          medianBlur(dest, dest2, kernelSize + 2)
          val dest3 = CvUtil.transform(dest2)
          // TODO: nem értem miért nem jó a második, ami a MAT-tal dolgozik
          val tempIplImage = new IplImage(dest3)
          val (x, y) = findTopWhite(tempIplImage)
          tempIplImage.release()
          //val (x,y) = findTopWhite(dest3)
          val color: Scalar = new Scalar(250, 250, 5, 0)
          CvUtil.drawTable(dest3, color)

          val (mod, num) = identifyNumber(new Point(x, y))
          if (prevNonZero > nonZero) {
            result_mod = mod
            result_num = num
          }

          if (!COMMAND_LINE) GameUi.updateImage(2,new ImageIcon(CvUtil.toBufferedImage(dest2)))
          //imwrite(f"$OUTPUT_DIR/$i%05d-b-mask-${talalat}%02d-nonz:${nonZero}-res:$mod-$num.jpg", maskBelso)
          //imwrite(f"$OUTPUT_DIR/$i%05d-c-medi-${talalat}%02d-nonz:${nonZero}-res:$mod-$num.jpg", dest2)
          //imwrite(f"$OUTPUT_DIR/$i%05d-d-resu-${talalat}%02d-nonz:${nonZero}-res:$mod-$num.jpg", dest3)
          //imwrite(f"$OUTPUT_DIR/$i%05d-e-maskKuldso-${talalat}%02d-nonz:${nonZero}-res:$mod-$num.jpg", mask)

          putText(dest3, f"Number: $result_num (modifier: $result_mod)", new Point(50,50),
            FONT_HERSHEY_PLAIN, // font type
            3, // font scale
            color, // text color (here white)
            3, // text thickness
            8, // Line type.
            false)

          if (!COMMAND_LINE) GameUi.updateImage(1,new ImageIcon(CvUtil.toBufferedImage(dest3)))
          //println(f"${cameraFile.lastFilename};$i;$talalat;$nonZero;$mod;$num")
          //out.println(f"${cameraFile.lastFilename};$i;$talalat;$nonZero;$mod;$num")
          //out.flush()

          dest.release()
          dest2.release()
          dest3.release()

          if (imageMat != null) {
            imageMat.release()
          }
          imageMat = camera.captureFrame
          if (!COMMAND_LINE) GameUi.updateImage(0,new ImageIcon(CvUtil.toBufferedImage(imageMat)))

          prevNonZero = nonZero
          talalat += 1
        }

        val dest4 = CvUtil.transform(imageMat)
        imwrite(f"$OUTPUT_DIR/$i%05d-a-orig-${camera.lastFilename}-resu-${talalat}%02d-res:$result_mod-$result_num.jpg", dest4)
        dest4.release()
      } else {
        talalat = 0
      }
      if (prevImageMat != null) {
        prevImageMat.release()
      }
      prevImageMat = imageMat.clone()

      GameUi.imgCount += 1
      i += 1
    }
//    out.close()
  }

  def findTopWhite(i: IplImage) : (Int, Int) = {
    val d: BytePointer = i.imageData()

    var j: Int = 0
    while((d.get(j) == 0 || d.get(j+1) == 0) && j < 960 * 720) {
      j = j + 2
    }
    (j % i.width(), j / i.width())
  }
  def findTopWhite(i: Mat) : (Int, Int) = {
    val d: ByteBuffer = i.asByteBuffer()

    var j: Int = 0
    while((d.get(j) == 0 || d.get(j+1) == 0) && j < 960 * 720) {
      j = j + 2
    }
    (j % i.cols(), j / i.rows())
  }
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
      case 3 => (3, number)
      case 5 => (2, number)
      case _ => (1, number)
    }
  }

  def createMog: BackgroundSubtractorMOG2 = {
    var mog = createBackgroundSubtractorMOG2()
    mog.setDetectShadows(true)
    println("detect shadows: " + mog.getDetectShadows())
    mog.setShadowValue(500)
    mog.setComplexityReductionThreshold(0.05) // default: 0.05
    println("ComplexityReductionThreshold: " + mog.getComplexityReductionThreshold())
    mog.setBackgroundRatio(0.9999) // default: 0.9
    println("BackgroundRatio: " + mog.getBackgroundRatio)
    mog.setVarMin(4) // default: 4
    println("varMin: " + mog.getVarMin)
    mog.setVarMax(75) // default: 75
    println("varMax: " + mog.getVarMax)
    mog.setVarThreshold(128) // default: 16
    println("varThreshold: " + mog.getVarThreshold)
    mog.setVarThresholdGen(9) // default: 9
    println("varThresholdGen: " + mog.getVarThresholdGen)
    mog
  }

}

object BackgroundSubtractorTest extends App {
  val bs = new BackgroundSubtractorTest
  bs.cameraAllowed = true
  bs.commandLine
}