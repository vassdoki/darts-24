package darts.processor

import javax.swing.ImageIcon

import darts.GameUi
import darts.data.ObservationList
import darts.util.{Config, CvUtil, DartsUtil}
import org.bytedeco.javacpp.opencv_core.{or, _}
import org.bytedeco.javacpp.opencv_imgcodecs.imwrite
import org.bytedeco.javacpp.opencv_imgproc._

/**
  * Created by vassdoki on 2016.12.04..
  */
object Merger {
  val unprocessed: Array[ObservationList] = Array(null, null)

  def merge(o: ObservationList) = synchronized {
    var cc = o.camNum -1 // current camera number
    var oc = (cc + 1) % 2 // the other camera number
    println(s"cam: ${o.camNum} state: ${o.state} imgCount: ${o.list.size} null 1: ${unprocessed(0) == null} null 2: ${unprocessed(1) == null}")
    o.state match {
      case 2 if unprocessed(cc) != null => {
        // do nothing, we might get an observation from the other cam
        o.release
      }
      case 2 if unprocessed(oc) != null => {
        // process the other cam's last observation
        mergeObservationListAlone(unprocessed(oc))
        unprocessed(oc) = null
        o.release
      }
      case 2 => {
        handleHands
        o.release
      } // nothing to do, hands on the image, nothing is unprocessed
      case 1 if unprocessed(oc) != null => {
        mergeObservationLists(unprocessed(oc), o)
        unprocessed(oc) = null
      }
      case 1 if unprocessed(cc) != null => {
        // we got two from the same cam, but none from the other
        // process the older one alone
        mergeObservationListAlone(unprocessed(cc))
        unprocessed(cc) = o
      }
      case 1 if unprocessed(cc) == null => {
        // store it and wait for the other cam
        unprocessed(cc) = o
      }
      case 0 => o.release // do nothing with the emtpy table
    }
  }

  def httpGet(url: String) = {
    println(s"get: ${url}")
    scala.io.Source.fromURL(url).mkString
  }

  def handleHands = {
    httpGet(s"http://10.27.7.26:8080/cam?handsVisible=1")
  }

  def mergeObservationLists(o1: ObservationList, o2: ObservationList) = {
    // change the order if necessary
    if (o1.camNum == 1) {
      mergeObservationListsOrdered(o1, o2)
    } else {
      mergeObservationListsOrdered(o2, o1)
    }
  }

  def mergeObservationListAlone(o: ObservationList) = {
    // take the best x,y
    if (o.list.size == 0) {
      println("mi van?")
    }
    Blurer.blurUntilClear(o.list.head)
    val b = o.list.head.blurs.head
    val (mod, num) = DartsUtil.identifyNumber(new Point(b.x, b.y))
    println(s"$num x1 $mod (kernel1: ${b.kernelSize} x: ${b.x} y1: ${b.y})")
    if (Config.GUI_UPDATE) {
      val image = CvUtil.transform(o.list.head.mogMask, o.camNum)
      cvtColor(image, image, COLOR_GRAY2BGR)
      CvUtil.drawTable(image, Config.COLOR_YELLOW, 1)
      CvUtil.drawNumbers(image, Config.COLOR_YELLOW)
      circle(image, new Point(b.x, b.y), 20, Config.COLOR_RED, 3, 8, 0)
      putText(image, f"Number: $num (modifier: $mod) n:${o.list.head.filename}", new Point(30, 30),
        FONT_HERSHEY_PLAIN, // font type
        2, // font scale
        Config.COLOR_YELLOW, // text color (here white)
        3, // text thickness
        8, // Line type.
        false)

      GameUi.updateImage(3, new ImageIcon(CvUtil.toBufferedImage(image)))
      imwrite(f"${Config.OUTPUT_DIR}/${o.list.head.filename}-$num-$mod-alone.jpg", image)
      httpGet(s"http://10.27.7.26:8080/cam?num=${num}&modifier=${mod}")
    }

    o.release
  }
  def mergeObservationListsOrdered(o1: ObservationList, o2: ObservationList) =  synchronized {
    Blurer.blurUntilClear(o1.list.head)
    var b1 = o1.list.head.blurs.head
    val (mod1, num1) = DartsUtil.identifyNumber(new Point(b1.x, b1.y))
    println(s"$num1 x1 $mod1 (kernel1: ${b1.kernelSize} x1: ${b1.x} y1: ${b1.y})")
    LineDetector.detect(o1.list.head)

    Blurer.blurUntilClear(o2.list.head)
    var b2 = o2.list.head.blurs.head
    val (mod2, num2) = DartsUtil.identifyNumber(new Point(b2.x, b2.y))
    println(s"$num2 x2 $mod2 (kernel2: ${b2.kernelSize} x2: ${b2.x} y2: ${b2.y})")
    LineDetector.detect(o2.list.head)

    if (b1.x * b1.y == 0) {b1 = b2}
    if (b2.x * b2.y == 0) {b2 = b1}

    if (CvUtil.getDistance(b1.x, b1.y, b2.x, b2.y) > 100) {
      // too big distance, take the better image
      if (b1.kernelSize < b2.kernelSize) {
        b2 = b1
      } else {
        b1 = b2
      }
    }

    // get the intersection
    val l1 = o1.list.head.lineDetected
    val l2 = o2.list.head.lineDetected
    val intersectionPoint = if (l1 == null || l2 == null) {
      new Point(((b1.x+b2.x)/2).toInt, Math.min(b1.y,b2.y))
    } else {
      CvUtil.lineIntersection(l1.s, l1.m, l2.s, l2.m)
    }

    val (modI, numI) = DartsUtil.identifyNumber(intersectionPoint)

    val (x, y) = (((b1.x+b2.x)/2).toInt, Math.min(b1.y,b2.y))
    val (mod, num) = DartsUtil.identifyNumber(new Point(x, y))

    if (Config.GUI_UPDATE) {

      val i1 = CvUtil.transform(o1.list.head.mogMask, o1.camNum)
      cvtColor(i1, i1, COLOR_GRAY2BGR)
      val or1 = and(i1, Config.COLOR_GREEN).asMat
      val orig1 = CvUtil.transform(o1.list.head.orig, 1)
      //imwrite(f"${Config.OUTPUT_DIR}/${o1.list.head.filename}-$num-$mod-orig1.jpg", orig1)
      GameUi.updateImage(1, new ImageIcon(CvUtil.toBufferedImage(orig1)))


      val i2 = CvUtil.transform(o2.list.head.mogMask, o2.camNum)
      cvtColor(i2, i2, COLOR_GRAY2BGR)
      val or2 = and(i2, Config.COLOR_RED).asMat
      val orig2 = CvUtil.transform(o2.list.head.orig, 2)
      //imwrite(f"${Config.OUTPUT_DIR}/${o1.list.head.filename}-$num-$mod-orig2.jpg", orig2)
      GameUi.updateImage(0, new ImageIcon(CvUtil.toBufferedImage(orig2)))

      val image = or(or1, or2).asMat
      val h = image.size.height
      val w = image.size.width
      val output = new Mat(h * 2, w * 2, orig1.`type`())
      //GameUi.updateImage(2, new ImageIcon(CvUtil.toBufferedImage(image)))

      CvUtil.drawTable(image, Config.COLOR_YELLOW, 1)
      CvUtil.drawNumbers(image, Config.COLOR_YELLOW)
      circle(image, new Point(b1.x, b1.y), 20, Config.COLOR_GREEN, 1, 8, 0)
      circle(image, new Point(b2.x, b2.y), 20, Config.COLOR_RED, 1, 8, 0)
      circle(image, new Point(x, y), 10, Config.COLOR_WHITE, 1, 8, 0)
      putText(image, f"A: $num1 X $mod1 B: $num2 X $mod2 n:${o1.list.head.filename}", new Point(30, 30),
        FONT_HERSHEY_PLAIN, // font type
        2, // font scale
        Config.COLOR_YELLOW, // text color (here white)
        3, // text thickness
        8, // Line type.
        false)
      putText(image, f"$num X $mod", new Point(30, 60),
        FONT_HERSHEY_PLAIN, // font type
        2, // font scale
        Config.COLOR_WHITE, // text color (here white)
        3, // text thickness
        8, // Line type.
        false)
      circle(image, intersectionPoint, 6, Config.COLOR_BLUE, 3, 8, 0)
      putText(image, f"$numI X $modI", new Point(150, 60),
        FONT_HERSHEY_PLAIN, // font type
        2, // font scale
        Config.COLOR_BLUE, // text color (here white)
        3, // text thickness
        8, // Line type.
        false)

      GameUi.updateImage(3, new ImageIcon(CvUtil.toBufferedImage(image)))

      if (b1.bluredImage.`type`() < 2) {
        cvtColor(b1.bluredImage, b1.bluredImage, COLOR_GRAY2BGR)
      }
      val bl1 = and(b1.bluredImage, Config.COLOR_GREEN).asMat
      circle(bl1, new Point(b1.x, b1.y), 20, Config.COLOR_GREEN, 2, 8, 0)

      if (b2.bluredImage.`type`() < 2) {
        cvtColor(b2.bluredImage, b2.bluredImage, COLOR_GRAY2BGR)
      }
      val bl2 = and(b2.bluredImage, Config.COLOR_RED).asMat
      circle(bl2, new Point(b2.x, b2.y), 20, Config.COLOR_RED, 2, 8, 0)
      val image2 = or(bl1, bl2).asMat
      circle(image2, intersectionPoint, 8, Config.COLOR_BLUE, 2, 8, 0)
      if (l1 != null) line(image2, l1.p1, l1.p2, Config.COLOR_BLUE, 1, LINE_AA, 0)
      if (l2 != null) line(image2, l2.p1, l2.p2, Config.COLOR_BLUE, 1, LINE_AA, 0)



      orig2.copyTo(output(new Rect(0,  0,  w, h)))
      orig1.copyTo(output(new Rect(w , 0 , w, h)))
      image2.copyTo(output(new Rect(0, h , w, h)))
      image.copyTo(output(new Rect(w , h,  w, h)))
      println(s"image type: ${image.`type`()} orig type: ${orig1.`type`()} image2 type: ${image2.`type`()} b1 type: ${b1.bluredImage.`type`()} b2 type: ${b2.bluredImage.`type`()}")

      imwrite(f"${Config.OUTPUT_DIR}/${o1.list.head.filename}-$num-$mod.jpg", output)
      httpGet(s"http://10.27.7.26:8080/cam?num=${numI}&modifier=${modI}")

      bl1.release
      bl2.release
      image2.release
      image.release
      i1.release
      i2.release
      orig1.release
      orig2.release
      or1.release
      or2.release
    }
    o1.release
    o2.release
  }






  def merge(m1: Mat, m2: Mat, filename: String) = synchronized {
    val kernelSize = 5

    val m1tr = CvUtil.transform(m1, 1)
    //cvtColor(m1tr, m1tr, COLOR_GRAY2BGR)
    medianBlur(m1tr, m1tr, kernelSize)
    val m1trColor = and(m1tr, Config.COLOR_GREEN).asMat

    val m2tr = CvUtil.transform(m2, 2)
    //cvtColor(m2tr, m2tr, COLOR_GRAY2BGR)
    medianBlur(m2tr, m2tr, kernelSize)
    val m2trColor = and(m2tr, Config.COLOR_RED).asMat

    val res: MatExpr = or(m1trColor, m2trColor)
    try {
      val asMat = res.asMat()
      imwrite(f"${Config.OUTPUT_DIR}/${filename}-merged.jpg", asMat)
    } catch {
      case e: Exception => e.printStackTrace()
    }

    m2tr.release
    m1tr.release
    m1.release
    m2.release
  }
}
