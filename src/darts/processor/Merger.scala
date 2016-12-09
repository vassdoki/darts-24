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
    var cc = o.camNum - 1 // current camera number
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
      case 2 => o.release // nothing to do, hands on the image, nothing is unprocessed
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

  def mergeObservationLists(o1: ObservationList, o2: ObservationList) = {
    // change the order if necessary
    if (o1.camNum == 0) {
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
    val (kernel, x, y) = o.list.head.blurs.head
    val (mod, num) = DartsUtil.identifyNumber(new Point(x, y))
    println(s"$num x $mod (kernel: $kernel x: $x y: $y)")
    if (Config.GUI_UPDATE) {
      val image = CvUtil.transform(o.list.head.mogMask, o.camNum)
      cvtColor(image, image, COLOR_GRAY2BGR)
      CvUtil.drawTable(image, Config.COLOR_YELLOW, 1)
      CvUtil.drawNumbers(image, Config.COLOR_YELLOW)
      circle(image, new Point(x, y), 20, Config.COLOR_RED, 3, 8, 0)
      putText(image, f"Number: $num (modifier: $mod) n:${o.list.head.filename}", new Point(30, 30),
        FONT_HERSHEY_PLAIN, // font type
        2, // font scale
        Config.COLOR_YELLOW, // text color (here white)
        3, // text thickness
        8, // Line type.
        false)

      GameUi.updateImage(3, new ImageIcon(CvUtil.toBufferedImage(image)))
    }

    o.release
  }
  def mergeObservationListsOrdered(o1: ObservationList, o2: ObservationList) =  synchronized {
    Blurer.blurUntilClear(o1.list.head)
    var (kernel1, x1, y1) = o1.list.head.blurs.head
    val (mod1, num1) = DartsUtil.identifyNumber(new Point(x1, y1))
    println(s"$num1 x1 $mod1 (kernel1: $kernel1 x1: $x1 y1: $y1)")

    Blurer.blurUntilClear(o2.list.head)
    var (kernel2, x2, y2) = o2.list.head.blurs.head
    val (mod2, num2) = DartsUtil.identifyNumber(new Point(x2, y2))
    println(s"$num2 x2 $mod2 (kernel2: $kernel2 x2: $x2 y2: $y2)")

    if (x1 * y1 == 0) {x1 = x2; y1 = y2}
    if (x2 * y2 == 0) {x2 = x1; y2 = y1}

    if (CvUtil.getDistance(x1, y1, x2, y2) > 60) {
      // too big distance, take the better image
      if (kernel1 < kernel2) {
        x2 = x1; y2 = y1
      } else {
        x1 = x2; y1 = y2
      }
    }

    val (x, y) = (((x1+x2)/2).toInt, ((y1+y2)/2).toInt)
    val (mod, num) = DartsUtil.identifyNumber(new Point(x, y))

    if (Config.GUI_UPDATE) {

      val i1 = CvUtil.transform(o1.list.head.mogMask, o1.camNum)
      cvtColor(i1, i1, COLOR_GRAY2BGR)
      val or1 = and(i1, Config.COLOR_GREEN).asMat
      //val orig1 = CvUtil.transform(o1.list.head.mogMask, 1)
      //imwrite(f"${Config.OUTPUT_DIR}/${o1.list.head.filename}-$num-$mod-orig1.jpg", orig1)
      //GameUi.updateImage(1, new ImageIcon(CvUtil.toBufferedImage(orig1)))


      val i2 = CvUtil.transform(o2.list.head.mogMask, o2.camNum)
      cvtColor(i2, i2, COLOR_GRAY2BGR)
      val or2 = and(i2, Config.COLOR_RED).asMat
      //val orig2 = CvUtil.transform(o2.list.head.orig, 2)
      //imwrite(f"${Config.OUTPUT_DIR}/${o1.list.head.filename}-$num-$mod-orig2.jpg", orig2)
      //GameUi.updateImage(0, new ImageIcon(CvUtil.toBufferedImage(orig2)))

      val image = or(or1, or2).asMat

      CvUtil.drawTable(image, Config.COLOR_YELLOW, 1)
      CvUtil.drawNumbers(image, Config.COLOR_YELLOW)
      circle(image, new Point(x1, y1), 20, Config.COLOR_GREEN, 3, 8, 0)
      circle(image, new Point(x2, y2), 20, Config.COLOR_RED, 3, 8, 0)
      circle(image, new Point(x, y), 10, Config.COLOR_WHITE, 3, 8, 0)
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

      GameUi.updateImage(3, new ImageIcon(CvUtil.toBufferedImage(image)))
      imwrite(f"${Config.OUTPUT_DIR}/${o1.list.head.filename}-$num-$mod.jpg", image)


      image.release
      i1.release()
      i2.release()
      //orig1.release()
      //orig2.release()
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
