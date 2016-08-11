package darts

import java.awt.{Color, Checkbox}
import java.awt.Cursor._
import java.awt.image.{DataBufferByte, BufferedImage}
import java.io.File
import javax.swing.{SwingUtilities, ImageIcon}

import darts.BackgroundSubtractorTest
import darts.util.{CaptureTrait, CaptureCamera, Utils}
import org.bytedeco.javacpp.opencv_core.{Scalar, IplImage, Mat}
import org.bytedeco.javacv.Java2DFrameConverter
import org.bytedeco.javacv.OpenCVFrameConverter.ToMat
import org.bytedeco.javacpp.opencv_imgcodecs._


import scala.swing.SimpleSwingApplication
import scala.swing._
import scala.swing.event._
import java.awt.event._
import scala.concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global
import scala.swing.FileChooser.Result.Approve
import scala.swing.Dialog.Message.Error


import scala.swing.event.{ButtonClicked, WindowClosing, MouseReleased}

/**
 * Created by vassdoki on 2016.08.11..
 */
object GameUi extends  SimpleSwingApplication{

  val CAMERA_DEV_NUM = 0

  val backgroundSubtractorTest = new BackgroundSubtractorTest

  val cameraCheckbox = new CheckBox("Use Camera")
  val imageViews: List[Label] = List.fill(4) {
    new Label
  }
  val fpsLabel = new Label
  var imgCount = 0
  var openedImage: Mat = null
  var openedImageClone: Mat = null
  val conf = Utils.getProperties

  val defaultDirectory = "/home/vassdoki/Dropbox/darts/v2/cam"
  private lazy val fileChooser = new FileChooser(new File(defaultDirectory))


  def top: Frame = new MainFrame {

    val buttonsPanel = new FlowPanel() {
      contents += cameraCheckbox
//      contents += new Button(openImageAction)
      contents += fpsLabel
//      for (i <- 0 to 3) { contents += transCheckbox(i) }
      vGap = 1
    }

    contents = new BorderPanel() {
      add(new FlowPanel(buttonsPanel), BorderPanel.Position.North)
      add(
        new GridPanel(rows0 = 2, cols0 = 2) {
          for (i <- 0 to 3) {contents += new ScrollPane(imageViews(i))}
          preferredSize = new Dimension(1024, 768)
        }, BorderPanel.Position.Center)
    }

    listenTo(cameraCheckbox)

    reactions += {
      case ButtonClicked(c) => {
        if (c == cameraCheckbox) {
          setCameraState
        }
      }
      case e: WindowClosing => {
        println("WIndowClosing event")
      }
      case e => {
        //println("Unhandeled event: " + e)
      }
      //      case e: window
    }

    override def closeOperation(): Unit = {
      //darts.CaptureTest.releaseCamera()
      println("Closing applicatoin")
      top.close()
      exit(0)
    }
  }

  def updateTransformCheck(x: Mat): Unit = {
    imageViews(0).icon = new ImageIcon(Utils.toBufferedImage(x))
    val y = TransformTest.transform(x)
    val color: Scalar = new Scalar(250, 250, 5, 0)
    TransformTest.drawTable(y, color)
    imageViews(1).icon = new ImageIcon(Utils.toBufferedImage(y))
  }


  def setCameraState = {
    if (cameraCheckbox.selected) {
      // start the camera
      println("start the camera")
      backgroundSubtractorTest.cameraAllowed = true
      val fut = Future{
        backgroundSubtractorTest.continousCameraUpdate
      }
    } else {
      backgroundSubtractorTest.cameraAllowed = false
    }
  }


  def updateImage(imgNum: Int, imageIcon: ImageIcon) = {
    val fut = Future {
      Swing.onEDT {
        println("future updateImage start")
        imageViews(imgNum).icon = imageIcon
        println("future updateImage end")
        fpsLabel.text = f"C: $imgCount"
      }
    }
  }


}
