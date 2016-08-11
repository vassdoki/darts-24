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

  val CAMERA_DEV_NUM = 1
  var imageCount = 0

  private val bull = new Point(400, 400)
  val distancesFromBull = List(14, 28, 174, 192, 284, 300)
  var cameraAllowed = false
  //var maxOutputNumber = 9999999

  def commandLine = {
    val d = new File("/home/vassdoki/Dropbox/darts/v2/cam")
    val x: Seq[File] = Seq(d.listFiles.filter(_.isFile).filter(_.getName.startsWith("orig-")).sorted: _*)
    val camera = CaptureTrait.get(x)
  }

  def continousCameraUpdate = {
    //val capture = CaptureTrait.get(CAMERA_DEV_NUM)
    val d = new File("/home/vassdoki/Dropbox/darts/v2/cam")
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
    // TODO: Itt miért nem működik az implicit?
    //val cameraFile: CaptureFile = camera.getSelf.asInstanceOf[CaptureFile]

    var mask: Mat = new Mat()

//    val out = new PrintWriter(new File("backgroundSubstractorResult.csv"))
//    out.println(f"filename;count;talalat;none zero;mod;num")

    var i = 0
    var first = false
    var image: Mat = null
    var prevImage: Mat = null
    var talalat = 0

    val mog = createMog
    val mog2 = createMog

    // Kezdő kép beolvasása
    image = camera.captureFrame
    imwrite(f"/tmp/d/$imageCount%05d.jpg", image)
    imageCount += 1
    if (image == null) {
      //maxOutputNumber = -1
    } else {
      mog.apply(image, mask, 1)
    }


    while (cameraAllowed) {
      if (image != null) {
        image.release()
      }
      image = camera.captureFrame
      GameUi.updateImage(0,new ImageIcon(Utils.toBufferedImage(image)))
      imwrite(f"/tmp/d/$imageCount%05d.jpg", image)
      imageCount += 1
      if (image == null) {
        break
      } else {
        mog.apply(image, mask, 0.4)
        GameUi.updateImage(3,new ImageIcon(Utils.toBufferedImage(mask)))
      }

      if (countNonZero(mask) > 500 && prevImage != null) {
        talalat = 1
        //maxOutputNumber -= 1

        image = camera.captureFrame
        imwrite(f"/tmp/d/$imageCount%05d-eldobva1.jpg", image)
        image = camera.captureFrame
        imwrite(f"/tmp/d/$imageCount%05d-eldobva2.jpg", image)
        image = camera.captureFrame
        imwrite(f"/tmp/d/$imageCount%05d-eldobva3.jpg", image)
        imageCount += 1

        mog2.clear()
        mog2.apply(prevImage, mask, 1)
        mog.apply(image, mask, 0.4)
        GameUi.updateImage(3,new ImageIcon(Utils.toBufferedImage(mask)))
        imwrite(f"/tmp/d/$imageCount%05d.jpg", image)
        var prevNonZero = -1
        var result_mod = 0
        var result_num = 0

        while (talalat < 15) {
          mog2.apply(image, mask, 0)
          val nonZero = countNonZero(mask)
          if (talalat == 1) {
            prevNonZero = nonZero
            //imwrite(f"/tmp/d/$i%05d-a-orig-${cameraFile.lastFilename}.jpg", image) //${f.getName}
          }

          // Remove noise with a median filter
          val dest = new Mat()
          val dest2 = new Mat()
          val kernelSize = 3
          medianBlur(mask, dest, kernelSize)
          medianBlur(dest, dest2, kernelSize + 2)
          val dest3 = TransformTest.transform(dest2)
          // TODO: nem értem miért nem jó a második, ami a MAT-tal dolgozik
          val (x, y) = findTopWhite(new IplImage(dest3))
          //val (x,y) = findTopWhite(dest3)
          val color: Scalar = new Scalar(250, 250, 5, 0)
          TransformTest.drawTable(dest3, color)

          val (mod, num) = identifyNumber(new Point(x, y))
          if (prevNonZero > nonZero) {
            result_mod = mod
            result_num = num
          }

          GameUi.updateImage(2,new ImageIcon(Utils.toBufferedImage(dest2)))
          //imwrite(f"/tmp/d/$i%05d-b-mask-${talalat}%02d-nonz:${nonZero}-res:$mod-$num.jpg", mask)
          //imwrite(f"/tmp/d/$i%05d-c-medi-${talalat}%02d-nonz:${nonZero}-res:$mod-$num.jpg", dest2)
          //imwrite(f"/tmp/d/$i%05d-d-resu-${talalat}%02d-nonz:${nonZero}-res:$mod-$num.jpg", dest3)
          putText(dest3, f"Number: $result_num (modifier: $result_mod)", new Point(50,50),
            FONT_HERSHEY_PLAIN, // font type
            3, // font scale
            color, // text color (here white)
            3, // text thickness
            8, // Line type.
            false)

          GameUi.updateImage(1,new ImageIcon(Utils.toBufferedImage(dest3)))
          //println(f"${cameraFile.lastFilename};$i;$talalat;$nonZero;$mod;$num")
          //out.println(f"${cameraFile.lastFilename};$i;$talalat;$nonZero;$mod;$num")
          //out.flush()

          dest.release()
          dest2.release()

          image = camera.captureFrame
          imwrite(f"/tmp/d/$imageCount%05d.jpg", image)
          imageCount += 1
          GameUi.updateImage(0,new ImageIcon(Utils.toBufferedImage(image)))

          prevNonZero = nonZero
          talalat += 1
        }
        while (talalat < 20) {
          image = camera.captureFrame
          mog.apply(image, mask, 0.4)
          talalat += 1
        }
      } else {
        talalat = 0
      }
      if (prevImage != null) {
        prevImage.release()
      }
      prevImage = image.clone()

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
    val degree = getDegreeFromBull(p)
    val distance = getDistanceFromBull(p)
    // 6-os közepe a 0 fok és óra járásával ellentétes irányba megy
    val nums = List(6, 13, 4, 18, 1, 20, 5, 12, 9, 14, 11, 8, 16, 7, 19, 3, 17, 2, 15, 10)

    val int: Int = Math.floor((degree + 9) / 18).toInt
    val number = if (int > 19) nums(0) else nums(int)

    val circleNumber: Int = distancesFromBull filter { dfb => dfb < distance } length

    circleNumber match {
      case 0 => (2, 25)
      case 1 => (1, 25)
      case 3 => (3, number)
      case 5 => (2, number)
      case _ => (1, number)
    }
  }

  def getDistanceFromBull(p: Point): Double = {
    Math.sqrt(sq(bull.x - p.x) + sq(bull.y - p.y))
  }


  def getDegreeFromBull(p: Point) = getDegree(bull, p)

  def getDegree(bull: Point, p: Point) = {
    val x = p.x - bull.x
    val y = bull.y - p.y
    var v = 180 * Math.atan2(y, x) / Math.PI
    if (v > 180) {
      v = 180
    }
    if (v < -180) {
      v = -180
    }
    if (v < 0) {
      v += 360
    }
    if (v == 0) {
      //Log.i(TAG, "arch: y: " + y + " x: " + x + " archtan: " + v);
    }
    v
  }

  def getDistanceFromBull(p: CvPoint): Double = {
    Math.sqrt(sq(bull.x - p.x) + sq(bull.y - p.y))
  }

  def sq(a: Float): Float = a * a

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
