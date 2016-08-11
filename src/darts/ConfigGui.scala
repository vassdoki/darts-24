package darts

import java.awt.{Color, Checkbox}
import java.awt.Cursor._
import java.awt.image.{DataBufferByte, BufferedImage}
import java.io.File
import javax.swing.{SwingUtilities, ImageIcon}

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
 * Created by vassdoki on 2016.08.09..
 */
object ConfigGui extends SimpleSwingApplication{

  val CAMERA_DEV_NUM = 1
  var cameraAllowed = false
  val cameraCheckbox = new CheckBox("Use Camera")
  val imageViews: List[Label] = List.fill(2) {
    new Label
  }
  val transCheckbox: List[CheckBox] = List.fill(4) {new CheckBox}
  var transCheckboxSelected: Int = 0
  //val transLabel = List("bull", "4", "14", "17")
  val transLabel = List("9a", "4a", "15t", "16t")
  val fpsLabel = new Label
  var imgCount = 0
  var openedImage: Mat = null
  var openedImageClone: Mat = null
  val conf = Utils.getProperties

  val defaultDirectory = "/home/vassdoki/darts/v2/cam"
  private lazy val fileChooser = new FileChooser(new File(defaultDirectory))


  def top: Frame = new MainFrame {

    val openImageAction = Action("Load image") {
      cursor = getPredefinedCursor(WAIT_CURSOR)
      try {
        openImage() match {
          case Some(x) =>
            // TODO:
            openedImage = x.clone()
            openedImageClone = x.clone()
            updateTransformCheck(openedImageClone)
          case None => {}
        }
      } finally {
        cursor = getPredefinedCursor(DEFAULT_CURSOR)
      }
    }

    for (i <- 0 to 3) {
      transCheckbox(i).text = f"${transLabel(i)} ${conf.trSrc(i).x};${conf.trSrc(i).y}"
    }
    transCheckbox(0).selected = true


    val buttonsPanel = new GridPanel(rows0 = 0, cols0 = 1) {
      contents += cameraCheckbox
      contents += new Button(openImageAction)
      contents += fpsLabel
      for (i <- 0 to 3) { contents += transCheckbox(i) }
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
    for (i <- (0 to 3)) {
      listenTo(transCheckbox(i))
    }
    listenTo(cameraCheckbox)
    listenTo(imageViews(0).keys)

    reactions += {
      case e: MouseReleased => {
        //setSrcPoint(selected, e.point.x, e.point.y)
        println("mouse click: " + e.point.x + "," + e.point.y)
        println(e)
        println("imageView width: " + imageViews(0).size.getWidth + " icon width: " + imageViews(0).icon.getIconWidth)

        conf.trSrc(transCheckboxSelected).x  = e.point.x - ((imageViews(0).size.getWidth - imageViews(0).icon.getIconWidth) / 2).toInt
        conf.trSrc(transCheckboxSelected).y  = e.point.y - ((imageViews(0).size.getHeight - imageViews(0).icon.getIconHeight) / 2).toInt
        transCheckbox(transCheckboxSelected).text = f"${transLabel(transCheckboxSelected)} ${conf.trSrc(transCheckboxSelected).x};${conf.trSrc(transCheckboxSelected).y}"
        openedImageClone.release()
        openedImageClone = openedImage.clone()
        updateTransformCheck(openedImageClone)
        imageViews(0).requestFocus()
      }
      case ButtonClicked(c) => {
        if (c == cameraCheckbox) {
          setCameraState
        } else {
          transCheckbox map { ci =>
            for (i <- (0 to 3)) {
              if (transCheckbox(i) == c) {
                transCheckbox(i).selected = true
                transCheckboxSelected = i
                imageViews(0).requestFocus()
              } else {
                transCheckbox(i).selected = false
              }
            }
            openedImageClone.release()
            openedImageClone = openedImage.clone()
            updateTransformCheck(openedImageClone)
          }

        }
      }
      case KeyPressed(_, key, _, _) => pressed(key)
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
    for (i <- 0 to 3) {
      if (transCheckbox(i).selected) {
        TransformTest.drawCross(x, conf.trSrc(i).x, conf.trSrc(i).y, 1)
      } else {
        TransformTest.drawCross(x, conf.trSrc(i).x, conf.trSrc(i).y, 0)
      }
      //transCheckbox(i).text = f"${transLabel(i)} ${conf.trSrc(i).x};${conf.trSrc(i).y}"
    }

      imageViews(0).icon = new ImageIcon(toBufferedImage(x))
      val y = TransformTest.transform(x)
      val color: Scalar = new Scalar(250, 250, 5, 0)
      TransformTest.drawTable(y, color)
      imageViews(1).icon = new ImageIcon(toBufferedImage(y))
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
    val capture = CaptureTrait.get(CAMERA_DEV_NUM)
    var mat: Mat = null
    while (cameraAllowed) {
      mat = capture.captureFrame
      updateTransformCheck(mat)
      imgCount += 1
      updateImage(0, new ImageIcon(toBufferedImage(mat)))
      Thread.sleep(20)
    }
    CaptureTrait.releaseCamera()
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

  def pressed(e: scala.swing.event.Key.Value) = {
    e match {
      case Key.Up => conf.trSrc(transCheckboxSelected).y  -= 1
      case Key.Down => conf.trSrc(transCheckboxSelected).y  += 1
      case Key.Left => conf.trSrc(transCheckboxSelected).x  -= 1
      case Key.Right => conf.trSrc(transCheckboxSelected).x  += 1
      case _ => {}
    }
    transCheckbox(transCheckboxSelected).text = f"${transLabel(transCheckboxSelected)} ${conf.trSrc(transCheckboxSelected).x};${conf.trSrc(transCheckboxSelected).y}"
    openedImageClone.release()
    openedImageClone = openedImage.clone()
    updateTransformCheck(openedImageClone)
    imageViews(0).requestFocus()
  }

}
