package darts.cmd

import java.io.File

import darts.processor.Merger
import darts.util.Config
import org.bytedeco.javacpp.opencv_imgcodecs.imread

/**
  * Created by vassdoki on 2016.12.04..
  */
object MergeTest extends App {
  val d = new File(Config.OUTPUT_DIR)
  val x: Seq[File] = Seq(d.listFiles.filter(_.isFile).filter(_.getName.contains("-cam:")).sorted: _*)

  val files = Array[File](null, null)
//  val nums = Array[Int](0, 0)
//  val names = Array[String]("","")

  // 2016-Nov-28_13-07_25-35-cam:2-state:2-count:5.jpg
  x.foreach((f:File) => {
    val state = f.getName.substring(36,37).toInt
    val cam = f.getName.substring(28,29).toInt
    val num = f.getName.substring(12,24).replaceAll("[_-]", "").toInt
    val filename = f.getName.substring(0,23)

    if (state == 1) {
      println(s"state: $state cam: $cam num: $num")
      val cn = cam - 1
      val otherCn = (cn + 1) % 2

      if (files(otherCn) != null) {
        files(cn) = f
        Merger.merge(imread(files(0).getAbsolutePath), imread(files(1).getAbsolutePath), filename)
        files(0) = null
        files(1) = null
      } else {
        files(cn) = f
      }
    }
  })
}
