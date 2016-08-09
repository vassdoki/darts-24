package darts

import java.awt.Checkbox
import java.awt.Cursor._
import java.awt.image.{DataBufferByte, BufferedImage}
import java.io.File
import javax.swing.{SwingUtilities, ImageIcon}

import org.bytedeco.javacpp.opencv_core.{IplImage, Mat}
import org.bytedeco.javacv.Java2DFrameConverter
import org.bytedeco.javacv.OpenCVFrameConverter.ToMat
import org.bytedeco.javacpp.opencv_imgcodecs._


import scala.swing.SimpleSwingApplication
import scala.swing._
import scala.concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global
import scala.swing.FileChooser.Result.Approve
import scala.swing.Dialog.Message.Error


import scala.swing.event.{ButtonClicked, WindowClosing, MouseReleased}


/**
 * Created by vassdoki on 2016.08.09..
 */
object ConfigGui extends SimpleSwingApplication{

  var cameraAllowed = false
  val cameraCheckbox = new CheckBox("Use Camera")
  val imageViews: List[Label] = List.fill(2) {new Label}
  val fpsLabel = new Label
  var imgCount = 0

  val defaultDirectory = "/home/vassdoki/Dropbox/darts/v2/cam"
  private lazy val fileChooser = new FileChooser(new File(defaultDirectory))


  def top: Frame = new MainFrame {

    val openImageAction = Action("Load image") {
      cursor = getPredefinedCursor(WAIT_CURSOR)
      try {
        openImage() match {
          case Some(x) =>
            imageViews(0).icon = new ImageIcon(toBufferedImage(x))
            imageViews(1).icon = new ImageIcon(toBufferedImage(TransformTest.transform(x)))
          case None => {}
        }
      } finally {
        cursor = getPredefinedCursor(DEFAULT_CURSOR)
      }
    }

    val buttonsPanel = new GridPanel(rows0 = 0, cols0 = 1) {
      contents += cameraCheckbox
      contents += new Button(openImageAction)
      contents += fpsLabel
      vGap = 1
    }

    contents = new BorderPanel() {
      add(new FlowPanel(buttonsPanel), BorderPanel.Position.West)
      add(
        new GridPanel(rows0 = 2, cols0 = 1) {
          for (i <- 0 to 1) {contents += new ScrollPane(imageViews(i))}
          preferredSize = new Dimension(1024, 768)
        }, BorderPanel.Position.Center)
    }

    listenTo(imageViews(0).mouse.clicks)
    listenTo(cameraCheckbox)
    //listenTo(top)

    reactions += {
      case e: MouseReleased => {
        //setSrcPoint(selected, e.point.x, e.point.y)
        println("mouse click: " + e.point.x + "," + e.point.y)
      }
      case e: ButtonClicked => {
        if (e.source == cameraCheckbox) {
          setCameraState
        }
      }
      case e: WindowClosing => {

      }
      case e => {
        println("Unhandeled event: " + e)
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
  def toBufferedImage(mat: Mat): BufferedImage = {
    val openCVConverter = new ToMat()
    val java2DConverter = new Java2DFrameConverter()
    java2DConverter.convert(openCVConverter.convert(mat))
  }

  def setCameraState = {
    if (cameraCheckbox.selected) {
      // start the camera
      println("start the camera")
      cameraAllowed = true
      val fut = Future{
        continousCameraUpdate
      }
    } else {
      cameraAllowed = false
    }
  }

  def continousCameraUpdate = {
    val capture = CaptureUtil.get(0)
    var mat: Mat = null
    while (cameraAllowed) {
      println("cam allowed: " + cameraAllowed)
      mat = capture.captureFrame
      imgCount += 1
      println("frame megvan")
      updateImage(0, new ImageIcon(toBufferedImage(mat)))
      println("imageview kesz")
      Thread.sleep(20)
      println("sleep kesz")
    }
    CaptureUtil.releaseCamera()
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

  /** Ask user for location and open new image. */
  private def openImage(): Option[Mat] = {
    // Ask user for the location of the image file
    if (fileChooser.showOpenDialog(null) != Approve) {
      return None
    }

    // Load the image
    val path = fileChooser.selectedFile.getAbsolutePath
    val newImage = imread(path)
    if (newImage != null) {
      Some(newImage)
    } else {
      Dialog.showMessage(null, "Cannot open image file: " + path, top.title, Error)
      None
    }
  }

}
