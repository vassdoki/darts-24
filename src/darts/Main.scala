package darts

import java.awt.{Dimension, Point, Color}
import java.awt.Cursor._
import java.awt.image.BufferedImage
import java.io._
import java.util.Properties
import javax.swing.{ImageIcon, SwingUtilities}

import com.googlecode.javacv.cpp.opencv_core.CvMat
import com.googlecode.javacv.cpp.opencv_core.CvPoint
import com.googlecode.javacv.cpp.opencv_core.CvScalar
import com.googlecode.javacv.cpp.opencv_core.IplImage
import com.googlecode.javacv.cpp.opencv_core._
import com.googlecode.javacv.cpp.opencv_core._
import com.googlecode.javacv.cpp.opencv_highgui.CvCapture
import com.googlecode.javacv.cpp.opencv_highgui._
import com.googlecode.javacv.cpp.opencv_highgui._
import com.googlecode.javacv.cpp.opencv_imgproc._
import com.googlecode.javacv.cpp.opencv_imgproc._
import com.googlecode.javacv.cpp.opencv_video.BackgroundSubtractorMOG

import _root_.scala.collection.mutable
import _root_.scala.concurrent.Future
import _root_.scala.swing.Action
import _root_.scala.swing.BorderPanel
import _root_.scala.swing.Button
import _root_.scala.swing.CheckBox
import _root_.scala.swing.Dialog
import _root_.scala.swing.Dialog.Message._
import _root_.scala.swing.FileChooser
import _root_.scala.swing.FlowPanel
import _root_.scala.swing.Frame
import _root_.scala.swing.GridPanel
import _root_.scala.swing.Label
import _root_.scala.swing.MainFrame
import _root_.scala.swing.ScrollPane
import _root_.scala.swing.SimpleSwingApplication
import _root_.scala.swing.Slider
import _root_.scala.swing._
import _root_.scala.swing.event.ButtonClicked
import _root_.scala.swing.event.MouseReleased
import _root_.scala.swing.event.ValueChanged
import scala.collection.mutable
import scala.collection.parallel.ParSeq
import scala.collection.parallel.mutable.ParArray
import scala.concurrent.Future

//import opencv2_cookbook.chapter01.PanAndZoom.PanAndZoomCanvas

import scala.concurrent.ExecutionContext.Implicits.global
import scala.swing.Dialog.Message.Error
import scala.swing.FileChooser.Result.Approve
import scala.swing._
import scala.swing.event._


object Main extends SimpleSwingApplication {


  var backgroundImage: IplImage = null
  var foregroundImage: IplImage = null
  var currentImage: IplImage = null
  val mog = new BackgroundSubtractorMOG


  // CONFIG
  val allowCameraAutoRefresh = true
  val defaultDirectory = "/home/vassdoki/Dropbox/darts/v2/cam"

  val hslCalibX1 = 1
  val hslCalibX2 = 360
  val hslCalibY1 = 1
  val hslCalibY2 = 360

  // UI elments -----------------------------------
  private lazy val fileChooser = new FileChooser(new File(defaultDirectory))
  val checkboxes: List[CheckBox] = List.fill(4) {new CheckBox}
  checkboxes(0).selected = true
  //val labels: List[Label] = List.fill(4) {new Label("n.a.")}
  var selected = 0
  val imageViews: List[Label] = List.fill(4) {new Label}
  var throwResult = new Label("Throw result")

  // DATA ----------------------------------------------
  var prop:Properties = new Properties();

  val labelTexts = List("bull", "4", "14", "17")
  val trdst: Array[Float] = Array(200, 200, 333, 131, 66, 131, 223, 348)
  // the transformed image's attributes
  val distancesFromBull = List(7, 14, 87, 96, 142, 150)
  val bull: CvPoint = new CvPoint(200, 200)

  // The 4 points, that are transfomed to trdst points
  var trSrc: mutable.MutableList[Point] = mutable.MutableList.fill(4) {new Point}
  val savedSrcPoint = List(
    new CvPoint(222, 231),
    new CvPoint(349, 200),
    new CvPoint(141, 119),
    new CvPoint(227, 389)
  )


  var laHue = new Label("Hue")
  var tfHueMin = createSlider(0, 100, 0)
  var tfHueMax = createSlider(0, 100, 0)
  var laSat = new Label("Sat")
  var tfSatMin = createSlider(0, 400, 0)
  var tfSatMax = createSlider(0, 400, 0)
  var laLig = new Label("Lig")
  var tfLigMin = createSlider(0, 100, 0)
  var tfLigMax = createSlider(0, 100, 0)

  var waitingForImage = true

  var throwCount = 0
  var lastThrow: mutable.Set[String] = mutable.Set()

  var cvCapture: CvCapture = null

  // Variable for holding loaded image
  var originalImage: Option[IplImage] = None
  var originalImageColors: IplImage = null
  var transformedImageWithBoard: IplImage = null
  var transformedImageResult: IplImage = null

  var hslCalibration: mutable.Map[String, Float] = null
  var min = mutable.Map[String, Int]().withDefaultValue(300)
  var max = mutable.Map[String, Int]().withDefaultValue(-1)
  var pixelCalibration = List(0,0,0)
  var pixelCalibrationCorrection = List(0.0, 0.0, 0.0)


  def top: Frame = new MainFrame {
    title = "iMind darts camera"

    // Action performed when "Open Image" button is pressed
    val openImageAction = Action("Open Image") {
      cursor = getPredefinedCursor(WAIT_CURSOR)
      try {
        openImage() match {
          case Some(x) =>
            loadOriginalImage()
            processAction.enabled = true
          case None => {}
        }
      } finally {
        cursor = getPredefinedCursor(DEFAULT_CURSOR)
      }
    }

    val processAction = Action("Process") {
      cursor = getPredefinedCursor(WAIT_CURSOR)
      try {
        originalImage match {
          case Some(x) =>
            doTransform
          case None =>
            Dialog.showMessage(null, "Image not opened", title, Error)
        }
      } finally {
        cursor = getPredefinedCursor(DEFAULT_CURSOR)
      }
    }
    processAction.enabled = false

    val cameraAction = Action("Camera") {
      cursor = getPredefinedCursor(WAIT_CURSOR)
      var c: CvCapture = null
      try {
        c = cvCreateCameraCapture(1)
        cvSetCaptureProperty(c,CV_CAP_PROP_FRAME_WIDTH,960)
        cvSetCaptureProperty(c,CV_CAP_PROP_FRAME_HEIGHT,720)
        val img: IplImage = cvQueryFrame(c)
        originalImage = Some(cvCloneImage(img))
        if (originalImage.get != null && originalImage.get.width() > 0) {
          imageViews(0).icon = new ImageIcon(originalImage.get.getBufferedImage)
        }
        processAction.enabled = true
      } finally {
        cvReleaseCapture(c)
        cursor = getPredefinedCursor(DEFAULT_CURSOR)
      }
    }

    val calibrateAction = Action("Calibrate") {
      cursor = getPredefinedCursor(WAIT_CURSOR)
      var c: CvCapture = null
      try {
        println("create capture start")
        c = cvCreateCameraCapture(1)
        cvSetCaptureProperty(c,CV_CAP_PROP_FRAME_WIDTH,960)
        cvSetCaptureProperty(c,CV_CAP_PROP_FRAME_HEIGHT,720)
        var i = 0
        while(i < 100) {
          println("foto start")
          val img: IplImage = cvQueryFrame(c)

          println("clone start")
          originalImage = Some(cvCloneImage(img))
          if (originalImage.get != null && originalImage.get.width() > 0) {
            println("transform start")
            val perspectiveTransformed = transformImage(originalImage.get)

            println("mog start")
            val foreground = cvCreateImage(cvGetSize(img), IPL_DEPTH_8U, 1)
            mog.apply(perspectiveTransformed, foreground,.01);

            println("icon 0")
            imageViews(0).icon = new ImageIcon(originalImage.get.getBufferedImage)
            println("icon 1")
            imageViews(1).icon = new ImageIcon(perspectiveTransformed.getBufferedImage)
            println("icon 2")
            imageViews(2).icon = new ImageIcon(foreground.getBufferedImage)
            val x = cvCloneImage(foreground)
            val color: CvScalar = new CvScalar(250, 250, 5, 0)
            drawTable(x, color)
            imageViews(3).icon = new ImageIcon(x.getBufferedImage)
            println("vege")

          }
          println(i)
          Thread.sleep(400)
          i = i + 1
        }
        processAction.enabled = true
      } finally {
        cvReleaseCapture(c)
        cursor = getPredefinedCursor(DEFAULT_CURSOR)
      }


//      cursor = getPredefinedCursor(WAIT_CURSOR)
//      try {
//        minMax(hslCalibX1, hslCalibX2, hslCalibY1, hslCalibY2)
//      } finally {
//        cursor = getPredefinedCursor(DEFAULT_CURSOR)
//      }
    }

    //
    // Create UI
    //

    val buttonsPanel = new GridPanel(rows0 = 0, cols0 = 1) {
      contents += new Button(openImageAction)
      contents += new Button(processAction)
      contents += new Button(cameraAction)
      contents += new Button(calibrateAction)
      for (i <- 0 to 3) {
        contents += new FlowPanel {
          contents += checkboxes(i)
          contents += new Label(labelTexts(i))
        }
      }

      contents += laHue
      contents += tfHueMin
      contents += tfHueMax
      contents += laSat
      contents += tfSatMin
      contents += tfSatMax
      contents += laLig
      contents += tfLigMin
      contents += tfLigMax
      contents += throwResult
      vGap = 1
    }
    updateSliderLabels()

    // Layout frame contents
    contents = new BorderPanel() {
      // Action buttons on the left
      add(new FlowPanel(buttonsPanel), BorderPanel.Position.West)
      add(
        new GridPanel(rows0 = 2, cols0 = 2) {
          for (i <- 0 to 3) {contents += new ScrollPane(imageViews(i))}
          preferredSize = new Dimension(1024, 768)
        }, BorderPanel.Position.Center)
    }

    // load the properties before the event handlers
    loadProperties

    // UI events
    listenTo(imageViews(0).mouse.clicks)
    listenTo(tfHueMin)
    listenTo(tfHueMax)
    listenTo(tfSatMin)
    listenTo(tfSatMax)
    listenTo(tfLigMin)
    listenTo(tfLigMax)
    checkboxes map { c => listenTo(c) }
    reactions += {
      case e: MouseReleased => {
        setSrcPoint(selected, e.point.x, e.point.y)
      }
      case ButtonClicked(c) => {
        checkboxes map { ci =>
          for (i <- (0 to 3)) {
            if (checkboxes(i) == c) {
              checkboxes(i).selected = true
              selected = i
            } else {
              checkboxes(i).selected = false
            }
          }
        }
      }
      case ValueChanged(tf) => {
        doTransform
      }
      case e => {
        //println ("Unreacted event " + e)
      }
    }


    //centerOnScreen()

    if (allowCameraAutoRefresh) {
      val f = Future {
        cvCapture = cvCreateCameraCapture(0)
        cvSetCaptureProperty(cvCapture,CV_CAP_PROP_FRAME_WIDTH,960)
        cvSetCaptureProperty(cvCapture,CV_CAP_PROP_FRAME_HEIGHT,720)

        var count = 0
        var talalat = 0
        try {
          while (true) {
            while (!waitingForImage) {
              Thread.sleep(100)
            }
            waitingForImage = false
            val img: IplImage = cvQueryFrame(cvCapture)
            originalImage = Some(cvCloneImage(img))
            count += 1
            processAction.enabled = true
            SwingUtilities.invokeLater(new Runnable() {

              def findTopWhite(i: IplImage) : (Int, Int) = {
                val d = i.imageData()
                var j: Int = 0
                while(d.get(j) == 0 && j < 960 * 720) {
                  j = j + 1
                }
                (j % i.width(), j / i.width())
              }

              def run() = {
                if (originalImage.get != null && originalImage.get.width() > 0) {
                  val perspectiveTransformed = transformImage(originalImage.get)

                  val foreground = cvCreateImage(cvGetSize(img), IPL_DEPTH_8U, 1)
                  mog.apply(perspectiveTransformed, foreground,.01);

                  imageViews(0).icon = new ImageIcon(originalImage.get.getBufferedImage)
                  imageViews(1).icon = new ImageIcon(perspectiveTransformed.getBufferedImage)

                  val TotalNumberOfPixels = 960 * 720
                  val nonZeroCount = cvCountNonZero(foreground);
                  if (talalat > 0) {
                    if (nonZeroCount > 0) {
                      // meg nem halvanyodott el
                      talalat = talalat + 1
                      if (talalat <= 10) {
                        print(" talalat: " + talalat + " feher: " + nonZeroCount)
                        val (x,y) = findTopWhite(foreground)
                        val (mod, num) = identifyNumber(cvPoint(x,y))
                        println(" top: x: " + x + " y: " + y + " mod: " + mod + " num: " + num)
                      }
                      if (talalat == 7) {
                        // mar kicsit elhalvanyult, lassuk hova ment
//                        val (x,y) = findTopWhite(foreground)
//                        val (mod, num) = identifyNumber(cvPoint(x,y))
//                        println(">>>>>>>>>>>>>>>>>>>>>> top: x: " + x + " y: " + y + " mod: " + mod + " num: " + num)
                      }
                    } else {
                      // mar elhalvanyodott
                      talalat = 0
                    }
                  } else {
                    if (nonZeroCount > 0) {
                      // most lattuk meg a talalatot
                      talalat = 1
                    }
                  }

                  imageViews(2).icon = new ImageIcon(foreground.getBufferedImage)
                  val x = cvCloneImage(foreground)
                  val color: CvScalar = new CvScalar(250, 250, 5, 0)
                  drawTable(x, color)
                  imageViews(3).icon = new ImageIcon(x.getBufferedImage)

                }
                throwResult.text = "camera " + count
                waitingForImage = true
              }
            });
          }
        } finally {
          cvReleaseCapture(cvCapture)
          cursor = getPredefinedCursor(DEFAULT_CURSOR)
        }
      }
    }
  }

  def doTransform(): Unit = {
    updateSliderLabels
    saveProperties

    if (! originalImage.isDefined) {
      return
    }

    val start = invTransformPoint(new CvPoint(50, 50))
    val end = invTransformPoint(new CvPoint(350, 350))
    println("start")

    val perspectiveTransformed = transformImage(originalImage.get)
    println("original image transfered")

    //var (filteredOriginal, firstPointOriginal) = filterColors(originalImage.get, start.x, end.x, start.y, end.y)
    //var filteredOriginal = hslCalibrationDiff(originalImage.get, hslCalibX1, hslCalibX2, hslCalibY1, hslCalibY2)
    //var filteredOriginal = diffBlue(originalImage.get)
    var filteredOriginal = minMaxDiff(perspectiveTransformed, hslCalibX1, hslCalibX2, hslCalibY1, hslCalibY2)
    println("filter color original finished")
    imageViews(1).icon = new ImageIcon(filteredOriginal.getBufferedImage)
    println("imageViews(1) refreshed")

    val color: CvScalar = new CvScalar(250, 250, 5, 0)
    var z = cvCloneImage(perspectiveTransformed)
    drawTable(z, color)
    imageViews(2).icon = new ImageIcon(z.getBufferedImage)

    //var (filteredImage, firstPoint) = filterColors(perspectiveTransformed, 50, 350, 50, 350)
    //println("transfomed image filtered")
    //findValueOnFilteredImage(filteredImage, firstPoint)
    //val filteredTransformed = transformImage(filteredOriginal)
    var filteredWithTable = cvCloneImage(filteredOriginal)
    drawTable(filteredWithTable, color)
    imageViews(3).icon = new ImageIcon(filteredWithTable.getBufferedImage)
    //
    //        val src2 = cvCloneImage(originalImage.get)
    //        trSrc map { p =>
    //            cvLine(src2, new CvPoint(p.x - 1, p.y), new CvPoint(p.x + 1, p.y), new CvScalar(255, 255, 255, 0), 1, 8, 0)
    //            cvLine(src2, new CvPoint(p.x, p.y - 1), new CvPoint(p.x, p.y + 1), new CvScalar(255, 255, 255, 0), 1, 8, 0)
    //        }
    //        imageViews(0).icon = new ImageIcon(src2.getBufferedImage)
    //        println("end")

  }

  private def setSrcPoint(i: Int, x: Int, y: Int): Unit = {
    trSrc(i).x = x
    trSrc(i).y = y
    println("src point(" + i + ") " + x + ";" + y)
    //labels(i).text = x + " ; " + y
    // kirajzoljuk a pontokat az eredeti kepre
    if (originalImage.isDefined) {
      doTransform()
    }
  }


  /** Ask user for location and open new image. */
  private def openImage(): Option[IplImage] = {
    // Ask user for the location of the image file
    if (fileChooser.showOpenDialog(null) != Approve) {
      return None
    }

    // Load the image
    val path = fileChooser.selectedFile.getAbsolutePath
    val newImage = cvLoadImage(path)
    if (newImage != null) {
      originalImage = Some(newImage)
      originalImage
    } else {
      Dialog.showMessage(null, "Cannot open image file: " + path, top.title, Error)
      None
    }
  }

  def invTransformPoint(p: CvPoint) = {
    var mat: CvMat = CvMat.create(3, 3)
    val trsrc: Array[Float] = (0 to 3).map { i => List(trSrc(i).x.toFloat, trSrc(i).y.toFloat) }.flatten.toArray
    var invTrans = Array(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
    cvGetPerspectiveTransform(trdst, trsrc, mat).getDoubleBuffer.get(invTrans)

    val m1 = p.x * invTrans(0) + p.y * invTrans(1) + invTrans(2);
    val m2 = p.x * invTrans(3) + p.y * invTrans(4) + invTrans(5);
    val m3 = p.x * invTrans(6) + p.y * invTrans(7) + invTrans(8);

    new CvPoint((m1 / m3).toInt, (m2 / m3).toInt)
  }

  /** Process image in place.  */
  private def transformImage(src: IplImage): IplImage = {
    var mat: CvMat = CvMat.create(3, 3)
    val trsrc: Array[Float] = (0 to 3).map { i => List(trSrc(i).x.toFloat, trSrc(i).y.toFloat) }.flatten.toArray
    mat = cvGetPerspectiveTransform(trsrc, trdst, mat)
    val src2 = cvCloneImage(src)
    cvWarpPerspective(src, src2, mat)
    src2
  }

  def getHslPixel(src2: IplImage, x: Int, y: Int) = {
    val r: Int = (src2.imageData.get(src2.widthStep * y + x * 3 + 2) + 256) % 256
    val g: Int = (src2.imageData.get(src2.widthStep * y + x * 3 + 1) + 256) % 256
    val b: Int = (src2.imageData.get(src2.widthStep * y + x * 3 + 0) + 256) % 256
    var hsl: Array[Float] = Array(0, 0, 0)
    hsl = Color.RGBtoHSB(r, g, b, hsl)
    hsl
  }

  def getRgbPixel(src2: IplImage, x: Int, y: Int) = {
    if (x < 1 || x > src2.width() || y < 1 || y > src2.height()) {
      (0,0,0)
    } else {
      val r: Int = (src2.imageData.get(src2.widthStep * y + x * 3 + 2) + 256) % 256
      val g: Int = (src2.imageData.get(src2.widthStep * y + x * 3 + 1) + 256) % 256
      val b: Int = (src2.imageData.get(src2.widthStep * y + x * 3 + 0) + 256) % 256
      (r, g, b)
    }
  }

  def getRgbPixel(src2: BufferedImage, x: Int, y: Int) = {
    if (x < 1 || x > src2.getWidth || y < 1 || y > src2.getHeight) {
      (0,0,0)
    } else {
      val rgb: Color = new Color(src2.getRGB(x,y))
      (rgb.getRed, rgb.getGreen, rgb.getBlue)
    }
  }

  def getRgbPixelWithCorrection(src2: IplImage, x: Int, y: Int) = {
    val r: Int = (pixelCalibrationCorrection(0) * (src2.imageData.get(src2.widthStep * y + x * 3 + 2) + 256) % 256).toInt
    val g: Int = (pixelCalibrationCorrection(1) * (src2.imageData.get(src2.widthStep * y + x * 3 + 1) + 256) % 256).toInt
    val b: Int = (pixelCalibrationCorrection(2) * (src2.imageData.get(src2.widthStep * y + x * 3 + 0) + 256) % 256).toInt
    (r, g, b)
  }

  def identifyNumber(p: CvPoint): Tuple2[Int, Int] = {
    val degree = getDegreeFromBull(p)
    val distance = getDistanceFromBull(p)
    // 6-os közepe a 0 fok és óra járásával ellentétes irányba megy
    val nums = List(6, 13, 4, 18, 1, 20, 5, 12, 9, 14, 11, 8, 16, 7, 19, 3, 17, 2, 15, 10)

    val number = nums(Math.floor((degree + 9) / 18).toInt)

    val circleNumber: Int = distancesFromBull filter { dfb => dfb < distance } length

    circleNumber match {
      case 0 => (2, 25)
      case 1 => (1, 25)
      case 3 => (3, number)
      case 5 => (2, number)
      case _ => (1, number)
    }
  }

  def getDegreeFromBull(p: CvPoint) = getDegree(bull, p)

  def getDegree(bull: CvPoint, p: CvPoint) = {
    val x = p.x - bull.x
    val y = bull.y - p.y
    var v = 180 * Math.atan2(y, x) / Math.PI;
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

  def drawTable(src: IplImage, color: CvScalar) = {
    distancesFromBull map { dist => cvCircle(src, bull, dist, color, 1, 8, 0) }
    for (d <- 9 to 351 by 18) {
      cvLine(src, rotatePoint(bull, d, 17), rotatePoint(bull, d, 150), color, 1, 8, 0)
    }
    src
  }

  def rotatePoint(c: CvPoint, degree: Float, radius: Float): CvPoint = {
    val cos = Math.cos(Math.PI * degree / 180)
    val sin = Math.sin(Math.PI * degree / 180)
    new CvPoint((c.x + cos * radius).toInt, (c.y - sin * radius).toInt)
  }

  def loadOriginalImage() = {
    originalImage map {
      i => imageViews(0).icon = new ImageIcon(i.getBufferedImage)
    }
  }

  def createSlider(pmin: Int, pmax: Int, pvalue: Int) = {
    new Slider {
      min = pmin
      max = pmax
      //value = pvalue
      labels = (pmin to pmax by (pmax / 5)).map { x: Int => x -> new Label(f"${x / 100.0}%.1f") }.toMap
      paintLabels = true
    }
  }

  def updateSliderLabels() = {
    if (tfHueMax.value - tfHueMin.value < 50) {
      laHue.text = f"Hue ${tfHueMin.value / 100.0}%.2f - ${tfHueMax.value / 100.0}%.2f"
    } else {
      laHue.text = f"Hue ${tfHueMax.value / 100.0}%.2f - ${tfHueMin.value / 100.0}%.2f **"
    }
    laSat.text = f"Sat ${tfSatMin.value / 100.0}%.2f - ${tfSatMax.value / 100.0}%.2f"
    laLig.text = f"Li ${tfLigMin.value / 100.0}%.2f - ${tfLigMax.value / 100.0}%.2f"
  }

  def minMax(x1: Int, x2: Int, y1: Int, y2: Int) = {
    val d = new File("/home/vassdoki/Dropbox/darts/v2/cam-ures")
    val files = d.listFiles.filter(_.isFile).toList

    files map {
      file: File =>
        println("File(" + file.getAbsolutePath + " START")
        val i = cvLoadImage(file.getAbsolutePath)

        for (x <- x1 to x2; y <- y1 to y2) {
          var (r, g, b) = getRgbPixel(i, x, y)
          if (x == x1 && y == y1) {
            pixelCalibration = List(r,g,b)
          }
          min(f"R$x%d-$y%d") = Math.min(min(f"R$x%d-$y%d"), r)
          max(f"R$x%d-$y%d") = Math.max(max(f"R$x%d-$y%d"), r)
          min(f"G$x%d-$y%d") = Math.min(min(f"G$x%d-$y%d"), g)
          max(f"G$x%d-$y%d") = Math.max(max(f"G$x%d-$y%d"), g)
          min(f"B$x%d-$y%d") = Math.min(min(f"B$x%d-$y%d"), b)
          max(f"B$x%d-$y%d") = Math.max(max(f"B$x%d-$y%d"), b)
        }
        println("File(" + file.getAbsolutePath + " END")
    }
    (min.toMap, max.toMap)
  }
  def minMaxDiff(i: IplImage, x1: Int, x2: Int, y1: Int, y2: Int) = {
    var dst = cvCloneImage(i)
    val maxDiff = 0.9

    {
      var (rc, gc, bc) = getRgbPixel(i, x1, y1)
      // calculate calibration correction
      pixelCalibrationCorrection = List(
        pixelCalibration(0) / rc.toFloat,
        pixelCalibration(1) / gc.toFloat,
        pixelCalibration(2) /bc.toFloat
      )
    }

    for (x <- x1 to x2; y <- y1 to y2) {
      //var (r, g, b) = getRgbPixelWithCorrection(i, x, y)
      var (r, g, b) = getRgbPixel(i, x, y)
      val p = new CvPoint(x, y)

      println("R("+x+","+y+"): " + min(f"B$x%d-$y%d") + " " + max(f"B$x%d-$y%d") + " curr: " + b)
      if (
        (min(f"R$x%d-$y%d") * maxDiff > r || max(f"R$x%d-$y%d") / maxDiff < r)
          && (min(f"G$x%d-$y%d") * maxDiff > g || max(f"G$x%d-$y%d") / maxDiff < g)
          && (min(f"B$x%d-$y%d") * maxDiff > b || max(f"B$x%d-$y%d") / maxDiff < b)
      ) {
        cvLine(dst, p, p, new CvScalar(100, g*1.5, r*1.5, 255), 1, 8, 0)
      } else {
        cvLine(dst, p, p, new CvScalar(0,0,0, 255), 1, 8, 0)
      }
      //            val p2 = new CvPoint(x+11, y)
      //            cvLine(dst, p2, p2, new CvScalar(b, g, r, 255), 1, 8, 0)
      //            val p3 = new CvPoint(x+22, y)
      //            cvLine(dst, p3, p3, new CvScalar(min(f"B$x%d-$y%d"), min(f"G$x%d-$y%d"), min(f"R$x%d-$y%d"), 255), 1, 8, 0)
      //            val p4 = new CvPoint(x+33, y)
      //            cvLine(dst, p4, p4, new CvScalar(max(f"B$x%d-$y%d"), max(f"G$x%d-$y%d"), max(f"R$x%d-$y%d"), 255), 1, 8, 0)

    }
    dst
  }

  def loadProperties = {

    var input: InputStream = null
    input = new FileInputStream("config.properties")
    prop.load(input)

    for(i <- 0 to 3) {
      setSrcPoint(i, prop.getProperty(s"src${i}x").toInt, prop.getProperty(s"src${i}y").toInt)
    }
    tfHueMin.value = prop.getProperty("tfHueMin").toInt
    tfHueMax.value = prop.getProperty("tfHueMax").toInt
    tfSatMin.value = prop.getProperty("tfSatMin").toInt
    tfSatMax.value = prop.getProperty("tfSatMax").toInt
    tfLigMin.value = prop.getProperty("tfLigMin").toInt
    tfLigMax.value = prop.getProperty("tfLigMax").toInt

    if (input != null) {
      input.close();
    }
  }
  def saveProperties = {
    var output = new FileOutputStream("config.properties")
    for(i <- 0 to 3) {
      prop.setProperty(s"src${i}x", trSrc(i).x.toString)
      prop.setProperty(s"src${i}y", trSrc(i).y.toString)
    }
    prop.setProperty("tfHueMin", tfHueMin.value.toString)
    prop.setProperty("tfHueMax", tfHueMax.value.toString)
    prop.setProperty("tfSatMin", tfSatMin.value.toString)
    prop.setProperty("tfSatMax", tfSatMax.value.toString)
    prop.setProperty("tfLigMin", tfLigMin.value.toString)
    prop.setProperty("tfLigMax", tfLigMax.value.toString)

    prop.store(output, null)
    output.close
  }

}