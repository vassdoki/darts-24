package darts

import java.awt.{Checkbox, Color}
import java.awt.Cursor._
import java.awt.image.{BufferedImage, DataBufferByte}
import java.io.File
import javax.swing.{ImageIcon, SwingUtilities}

import darts.BackgroundSubtractorTest
import darts.util._
import org.bytedeco.javacpp.opencv_core.{IplImage, Mat, Scalar}
import org.bytedeco.javacv.Java2DFrameConverter
import org.bytedeco.javacv.OpenCVFrameConverter.ToMat
import org.bytedeco.javacpp.opencv_imgcodecs._

import scala.swing.SimpleSwingApplication
import scala.swing._
import scala.swing.event._
import java.awt.event._

import darts.processor.StateHandler

import scala.concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global
import scala.swing.FileChooser.Result.Approve
import scala.swing.Dialog.Message.Error
import scala.swing.event.{ButtonClicked, MouseReleased, WindowClosing}

/**
 * Created by vassdoki on 2016.08.11..
 */
object GameUi extends  SimpleSwingApplication{

  var guiCreated = false
  val stateHandler1 = new StateHandler
  val stateHandler2 = new StateHandler

  val cameraCheckbox = new CheckBox("Use Camera")
  val imageViews: List[Label] = List.fill(4) {
    new Label
  }
  val fpsLabel = new Label
  var imgCount = 0
  var openedImage: Mat = null
  var openedImageClone: Mat = null

  val defaultDirectory = "/home/vassdoki/darts/v2/cam-aug11"
  private lazy val fileChooser = new FileChooser(new File(defaultDirectory))


  def top: Frame = new MainFrame {
    guiCreated = true
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
        new GridPanel(rows0 = Math.sqrt(imageViews.size).toInt, cols0 = Math.sqrt(imageViews.size).toInt) {
          for (i <- 0 to imageViews.size-1) {contents += new ScrollPane(imageViews(i))}
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
      BackgroundSubtractorTest.cameraAllowed = false
      Thread.sleep(50)
      println("Closing applicatoin")
      top.close()
      exit(0)
    }
  }

  def setCameraState = {
    if (cameraCheckbox.selected) {
      // start the camera
      println("start the camera")
      StateHandler.cameraAllowed = true
      val fut1 = Future{
        println("1 varunk")
        Thread.sleep(1000)
        println("1 mehet")
        stateHandler1.continousCameraUpdate(-1)
      }
      val fut2 = Future{
        stateHandler2.continousCameraUpdate(-2)
      }
    } else {
      BackgroundSubtractorTest.cameraAllowed = false
    }
  }
//  def setCameraState = {
//    if (cameraCheckbox.selected) {
//      // start the camera
//      println("start the camera")
//      BackgroundSubtractorTest.cameraAllowed = true
//      val fut1 = Future{
//        println("1 varunk")
//        Thread.sleep(1000)
//        println("1 mehet")
//        backgroundSubtractorTest1.continousCameraUpdate(-1)
//      }
//      val fut2 = Future{
//        backgroundSubtractorTest2.continousCameraUpdate(-2)
//      }
//    } else {
//      BackgroundSubtractorTest.cameraAllowed = false
//    }
//  }


  def updateImage(imgNum: Int, imageIcon: ImageIcon) = synchronized {
    if (guiCreated) {
      val fut = Future {
        Swing.onEDT {
          //println(s"future updateImage start imgNum: $imgNum")
          imageViews(Math.abs(imgNum)).icon = imageIcon
          //println("future updateImage end")
          fpsLabel.text = f"C: $imgCount"
        }
      }
    }
  }


}
