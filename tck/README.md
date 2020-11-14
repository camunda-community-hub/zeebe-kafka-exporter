Zeebe Exporter Technology Compatibility Kit
===========================================

The goal of this module is to provide a more flexible TCK than the one provided in the
`io.zeebe.zeebe-test`, e.g. `ExporterIntegrationTestRule`. That is, a set of utilities to make it
as simple as possible for exporter authors to test their own exporters.

It's very basic at the moment, and focused on integration tests, but can be used with any of them.

> NOTE: This is definitely not the right place for such a TCK (i.e. in the Kafka Exporter repo), but
> right now I'm using this to incubate the feature before proposing it to the Zeebe team. So feel
> free to use it, but keep in mind that it's very experimental and I don't guarantee any kind of
> backwards compatibility. However, ideas, contributions, etc., are welcome!
