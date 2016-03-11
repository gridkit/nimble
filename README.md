Nimble - a set of tools for performance testing in Java
====

Nimble is a set of tools for building fully automated
distributed performance tests.

- [NanoCloud][nanocloud] for orchestrate test over multiple machines
- Complex multi steps scenarios can be developed
- All metrics produced by test run are transfered to master node at end of test
- Number of monitoring tools are available to capture system/JVM metrics along with test specific KPIs
- BTrace integration is allowing to add automatic profiling to test setup

Please take a look at [Zookeeper Benchmark example][zktest] which is self contained performance test
for Zookeeper build on Nimble

 [nanocloud]: https://github.com/gridkit/nanocloud/
 [zktest]: https://github.com/gridkit/zk-benchmark-example