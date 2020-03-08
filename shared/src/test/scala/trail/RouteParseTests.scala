package trail

import org.scalatest._

import scala.util.Try

class RouteParseTests extends FunSpec with Matchers {
  it("Parse root") {
    val root = Root
    assert(root.parse("/").contains(()))
    assert(root.parse("/test").contains(())) // Use EndPath if you don't want this
    assert(root.parse("/?user=bob").contains(()))
  }

  it("Parse one path element") {
    val root = Root / "test"
    assert(root.parse("/").isEmpty)
    assert(root.parse("/test").contains(()))
    assert(root.parse("/test/2").contains(())) // Use EndPath if you don't want this
    assert(root.parse("/test2").isEmpty)
  }

  it("Route with one argument can be parsed") {
    val route = Root / "user" / Arg[String]
    assert(route.parse(trail.Path("/user/bob")) === Some("bob"))
    assert(route.parse(trail.Path("/user/bob/test")) === Some("bob"))
  }

  it("Parse root with end") {
    val root = Root / EndPath
    assert(root.parse("/").contains(()))
    assert(root.parse("/test").isEmpty)
    assert(root.parse("/test/2").isEmpty)
    assert(root.parse("/?user=bob").contains(()))
  }

  it("Parse one path element with end") {
    val route = Root / "test" / EndPath
    assert(route.parse("/").isEmpty)
    assert(route.parse("/test").contains(()))
    assert(route.parse("/test/").contains(())) // @TODO Not sure if I want this
    assert(route.parse("/test?user=bob").contains(()))
    assert(route.parse("/test/?user=bob").contains(()))
    assert(route.parse("/test2").isEmpty)
    assert(route.parse("/test/2").isEmpty)
  }

  it("Route can be parsed from Path") {
    val userInfo = Root / "user" / Arg[String] / Arg[Boolean]

    val parsed = userInfo.parse(trail.Path("/user/bob/true"))
    assert(parsed === Some(("bob", true)))
    userInfo.parse(trail.Path("/user/bob")) shouldBe empty
  }

  it("Route with query parameters can be parsed from Path") {
    val userInfo = Root / "user" / Arg[String] / Arg[Boolean] & Param[Int]("n")

    val parsed = userInfo.parse(trail.Path("/user/bob/true", List("n" -> "42")))
    assert(parsed === Some((("bob", true), 42)))
  }

  it("Route with optional path element can be parsed") {
    val disk = Root / Arg[Option[Long]]
    assert(disk.parse("/").contains(None))
    assert(disk.parse("/42").contains(Some(42L)))
  }

  it("Route with optional path element can be parsed (2)") {
    val disk = Root / "disk" / Arg[Option[Long]]
    assert(disk.parse("/disk").contains(None))
    assert(disk.parse("/disk/").contains(None))
    assert(disk.parse("/disk/42").contains(Some(42L)))
  }

  it("Route can be parsed from string") {
    val userInfo = Root / "user" / Arg[String] / Arg[Boolean]

    val parsed = userInfo.parse("/user/bob/true")
    parsed shouldBe defined

    assert(parsed === Some(("bob", true)))

    // It is possible to provide more path elements than specified in the route
    userInfo.parse("/user/bob/true/true") shouldBe defined

    userInfo.parse("/user/bob") shouldBe empty
    userInfo.parse("/user/bob/1") shouldBe empty
    userInfo.parse("/usr/bob/1") shouldBe empty
  }

  it("Boolean argument can be parsed") {
    val userInfo = Root / "user" / Arg[String] / Arg[Boolean]

    assert(userInfo.parse("/user/hello/false")
      .contains(("hello", false)))
  }

  it("Set codec") {
    implicit case object LongSetCodec extends Codec[Set[Long]] {
      override def encode(s: Set[Long]): Option[String] = Some(s.mkString(","))
      override def decode(s: Option[String]): Option[Set[Long]] =
        s.flatMap(value =>
          if (value.isEmpty) Some(Set())
          else Try(value.split(',').map(_.toLong).toSet).toOption)
    }

    val route = Root / "size" & Param[Set[Long]]("folders")
    assert(route.parse(route(Set[Long]())).contains(Set[Long]()))
    assert(route.parse(route(Set[Long](1))).contains(Set[Long](1)))
    assert(route.parse(route(Set[Long](1, 2, 3))).contains(Set[Long](1, 2, 3)))
  }
}
