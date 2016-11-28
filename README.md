# siren-scala-shapeless

Automatic [siren](https://github.com/yetu/siren-scala) encoder derivation with [shapeless](https://github.com/milessabin/shapeless)

## Usage

Add to your `build.sbt`
```scala
resolvers += "siren-scala-shapeless" at "http://dl.bintray.com/mbilski/maven/"

libraryDependencies += "pl.immutables" %% "siren-scala-shapeless" % "0.0.1"
```

### Automatic encoder derivation

```scala
import pl.immutables.siren.generic._

case class Foo(name: String, ints: List[Int])
case class Bar(id: Option[String], foo: Foo)

val foo = Foo("foo", List(1, 2, 3))
val bar = Bar(Some("bar"), foo)

ValueEncoder[Bar].encode(bar).asProps

> List(Property(id,StringValue(bar)), Property(foo,JsObjectValue(List((name,StringValue(foo)), (ints,JsArrayValue(List(NumberValue(1), NumberValue(2), NumberValue(3))))))))
```

## Acknowledgement

[The Type Astronaut's Guide to Shapeless](https://github.com/underscoreio/shapeless-guide)

## Contributors

+ Mateusz Bilski ([@mbilski](https://github.com/mbilski/))

## License

Released under the MIT license. See LICENSE file for more details.
