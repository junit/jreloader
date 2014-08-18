====jreloader

JReloader is a very small tool (~11KB) that enables class hot-swapping without the need of a debugger.

Class reloading is a must-have tool for developers that need to change small pieces of code on-the-fly without the burden of restarting the application. See [QuickStart](https://github.com/junit/jreloader/wiki/QuickStart) for details on how to use JReloader in a couple of minutes. If you have questions, see the [FAQs](https://github.com/junit/jreloader/wiki/FAQs).

With JReloader it's possible to reload classes with instances actively invoked by one or more threads. Current invocations will see the old code until they return. New invocations will always see the new code. Most of the time everything works fine. This all is made possible by JVMTI, an API available since Java 1.5.

Drop me an email if you find this tool useful. By the way, you may also want to take a look at another project I created, [Profiler4j](http://profiler4j.sourceforge.net/), a very easy-to-use CPU Profiler for Java.

If you need more flexibility, such as the ability to add and remove methods, [JRebel](http://www.zeroturnaround.com/jrebel/) is a good choice. Although non-free, it may be worth the money.

====Limitations
Only changes in method bodies can be reloaded. Changes in class signature are not allowed.
