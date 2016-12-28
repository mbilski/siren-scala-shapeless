# siren-scala-shapeless

[![Build Status](https://travis-ci.org/mbilski/siren-scala-shapeless.svg)](https://travis-ci.org/mbilski/siren-scala-shapeless)
[![Download](https://api.bintray.com/packages/mbilski/maven/siren-scala-shapeless/images/download.svg)](https://bintray.com/mbilski/maven/siren-scala-shapeless/_latestVersion)
[![codecov](https://codecov.io/gh/mbilski/siren-scala-shapeless/branch/master/graph/badge.svg)](https://codecov.io/gh/mbilski/siren-scala-shapeless)

Automatic [siren](https://github.com/yetu/siren-scala) encoder and decoder derivation with [shapeless](https://github.com/milessabin/shapeless)

## Usage

Add to your `build.sbt`
```scala
resolvers += "siren-scala-shapeless" at "http://dl.bintray.com/mbilski/maven/"

libraryDependencies += "pl.immutables" %% "siren-scala-shapeless" % "1.0.1"
```

### Automatic encoder and decoder derivation for products and coproducts

```scala
import pl.immutables.siren.encoder._
import pl.immutables.siren.decoder._

sealed trait Sample
case class Foo(name: String, ints: List[Int]) extends Sample
case class Bar(id: Option[String], foo: Foo) extends Sample

val foo = Foo("foo", List(1, 2, 3))
val bar = Bar(Some("bar"), foo)

val encoded = ValueEncoder[Sample].encode(bar).asProps
> Properties = List(Property(__$$$__,StringValue(Bar)), Property(id,StringValue(bar)), Property(foo,JsObjectValue(List((name,StringValue(foo)), (ints,JsArrayValue(List(NumberValue(1), NumberValue(2), NumberValue(3))))))))

val decoded = ValueDecoder[Sample].decode(encoded.asJsObject)
> Either[DecoderFailure,Bar] = Right(Bar(Some(bar),Foo(foo,List(1, 2, 3))))
```

## Acknowledgement

[The Type Astronaut's Guide to Shapeless](https://github.com/underscoreio/shapeless-guide)

## Contributors

+ Mateusz Bilski ([@mbilski](https://github.com/mbilski/))

## License

Released under the MIT license. See LICENSE file for more details.
