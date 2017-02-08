package toolkit

import java.io.File
import java.nio.file.{Files, Paths}

import clifton.graph.{CliftonCollector, CliftonInjector}
import distributer.JarSpaceUpdater
import exonode.clifton.node.SpaceCache
import exonode.clifton.node.entries.DataEntry
import exonode.distributer.{FlyClassEntry, FlyJarEntry}
import org.scalatest.FlatSpec

/**
  * Created by #ScalaTeam on 19/01/2017.
  */
class ToolKitUnitTest extends FlatSpec {

  val jarSpace = SpaceCache.getJarSpace
  val signalSpace = SpaceCache.getSignalSpace

  def readFromSpace(tmpl: AnyRef): Option[AnyRef] = {
    jarSpace.take(tmpl, 0L)
  }

  "UpdateSpaceWithJar" should "Place all the classes into the space" in {
    val file = "examples" + File.separatorChar + "testJar.jar"
    val bytes: Array[Byte] = Files.readAllBytes(Paths.get("examples" + File.separatorChar + "testJar.jar"))
    val jarUpd = new JarSpaceUpdater
    jarUpd.update(new File(file), ???)
    val tmplJar = FlyJarEntry("testJar.jar", null)
    val tmplClass = FlyClassEntry("toolkit.Jar.DoubleString", "testJar.jar")
    val jarEntry = jarSpace.take(tmplJar, 0L)
    val classEntry = jarSpace.take(tmplClass, 0L)
    if (jarEntry.isDefined)
      assert(jarEntry.get.bytes.deep == bytes.deep && classEntry.isDefined)
  }

  "CliftonInjector" should "Insert Into the Space entries to be processed" in {
    val inj = new CliftonInjector(">", "A")
    inj.inject("test")
    val dataTmpl = DataEntry("A", ">", null, null)
    assert(signalSpace.take(dataTmpl, 0L).isDefined)
  }

  "CliftonCollector" should "Collect entries from the space" in {
    val col = new CliftonCollector("<")
    val inj = new CliftonInjector("<", "<")
    inj.inject("test")
    assert(col.collect().isDefined)

  }

}
