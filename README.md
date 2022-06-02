Castor XML Unmarshalling CWE 502 examples
=====

This project has an example of using Castor to try to deserialize 
to arbitrary classes (CWE 502 flaw).

While this appears to be possible with version 1.3.1
as well as with 0.9.6
it does not appear to be possible with version 0.9.5.

Castor 0.9.5 documentation does say:
> By default, if Castor finds no information for a given class in the mapping file, it will introspect the class 
> and apply a set of default rules to guess the fields and marshal them. The default rules are as follows:
> 
> -	All primitive types, including the primitive type wrappers (Boolean, Short, etc...) are marshalled as attributes.
> -	All other objects are marshalled as elements with either text content or element content.

But it does not appear to work?

Demo of 1.3.1
-------------

```shell
cd 131 && mvn spring-boot:run
```

Access http://localhost:8080/

This will marshall the following XML:
```xml
AllowedXml: <?xml version="1.0" encoding="UTF-8"?>
<io.veracode.asc.bbaukema.castordeserialization.Allowed/>
DangerousXml: <?xml version="1.0" encoding="UTF-8"?>
<io.veracode.asc.bbaukema.castordeserialization.Dangerous/>
```
Embedded in the following links:
* [Allowed](http://localhost:8080/unmarshal?input=PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4KPGlvLnZlcmFjb2RlLmFzYy5iYmF1a2VtYS5jYXN0b3JkZXNlcmlhbGl6YXRpb24uQWxsb3dlZC8%2B)
* [Dangerous](http://localhost:8080/unmarshal?input=PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4KPGlvLnZlcmFjb2RlLmFzYy5iYmF1a2VtYS5jYXN0b3JkZXNlcmlhbGl6YXRpb24uRGFuZ2Vyb3VzLz4%3D)

(Note that Dangerous does not do anything actually dangerous)

Going to Allowed shows:
```shell
% curl "http://localhost:8080/unmarshal?input=PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4KPGlvLnZlcmFjb2RlLmFzYy5iYmF1a2VtYS5jYXN0b3JkZXNlcmlhbGl6YXRpb24uQWxsb3dlZC8%2B"
Unmarshalled: public class io.veracode.asc.bbaukema.castordeserialization.Allowed%   
```

Going to Dangerous shows:
```shell
 % curl "http://localhost:8080/unmarshal?input=PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4KPGlvLnZlcmFjb2RlLmFzYy5iYmF1a2VtYS5jYXN0b3JkZXNlcmlhbGl6YXRpb24uRGFuZ2Vyb3VzLz4%3D"
Unmarshalled: public class io.veracode.asc.bbaukema.castordeserialization.Dangerous%   
```

The Unmarshaller was not provided a Mapping for Dangerous and 
should not have unmarshalled this.

Fixed 1.3.1
===========

To fix this we can provide our Mapping with a custom ClassLoader that applies an allowlist like so:

```java
       Mapping mapping = new Mapping(new ClassLoader() {
            private int maxInstantiations = 10;

            private final String[] allowedClasses = new String[] {
                    XMLMappingLoader.class.getCanonicalName(),
                    Allowed.class.getCanonicalName(),
                    String.class.getCanonicalName()
            };

            @Override
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                if (maxInstantiations-- < 0) {
                    System.out.println("Castor XML ClassLoader: Maximum number of instantiations reached");
                    throw new ClassNotFoundException("Maximum number of instantiations reached");
                }

                if (!Arrays.asList(allowedClasses).contains(name)) {
                    System.out.println("Castor XML ClassLoader: Not allowed to create class of name: " + name);
                    throw new ClassNotFoundException("Not allowed to create class of name: " + name);
                }

                System.out.println("Castor XML ClassLoader: Loading " + name);
                return clazz.getClassLoader().loadClass(name);
            }
        });
```

Then this mapping must be set on the Unmarshaller:
```java
        Unmarshaller unmarshaller = new Unmarshaller();
        unmarshaller.setMapping(mapping);
```
Embedded in the following links:
* [Allowed](http://localhost:8080/unmarshal-safe?input=PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4KPGlvLnZlcmFjb2RlLmFzYy5iYmF1a2VtYS5jYXN0b3JkZXNlcmlhbGl6YXRpb24uQWxsb3dlZC8%2B)
* [Dangerous](http://localhost:8080/unmarshal-safe?input=PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4KPGlvLnZlcmFjb2RlLmFzYy5iYmF1a2VtYS5jYXN0b3JkZXNlcmlhbGl6YXRpb24uRGFuZ2Vyb3VzLz4%3D)

Going to Allowed shows:
```shell
% curl "http://localhost:8080/unmarshal-safe?input=PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4KPGlvLnZlcmFjb2RlLmFzYy5iYmF1a2VtYS5jYXN0b3JkZXNlcmlhbGl6YXRpb24uQWxsb3dlZC8%2B"
Unmarshalled: public class io.veracode.asc.bbaukema.castordeserialization.Allowed%
```

Going to Dangerous shows:
```shell
 %  curl "http://localhost:8080/unmarshal-safe?input=PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4KPGlvLnZlcmFjb2RlLmFzYy5iYmF1a2VtYS5jYXN0b3JkZXNlcmlhbGl6YXRpb24uRGFuZ2Vyb3VzLz4%3D"
{"timestamp":"2022-06-02T07:33:30.552+00:00","status":500,"error":"Internal Server Error","path":"/unmarshal-safe"}% 
```

And results in the following log:
```shell
Castor XML ClassLoader: Not allowed to create class of name: io.veracode.asc.bbaukema.castordeserialization.DangerousDescriptor
Castor XML ClassLoader: Not allowed to create class of name: io.veracode.asc.bbaukema.castordeserialization.descriptors.DangerousDescriptor
Castor XML ClassLoader: Not allowed to create class of name: io.veracode.asc.bbaukema.castordeserialization.Dangerous
Castor XML ClassLoader: Not allowed to create class of name: Io_veracode_asc_bbaukema_castordeserialization_Dangerous
2022-06-02 09:33:30.428 ERROR 57043 --- [io-8080-exec-10] o.a.c.c.C.[.[.[/].[dispatcherServlet]    : Servlet.service() for servlet [dispatcherServlet] in context with path [] threw exception [Request processing failed; nested exception is org.exolab.castor.xml.MarshalException: The class for the root element 'io.veracode.asc.bbaukema.castordeserialization.Dangerous' could not be found.{File: [not available]; line: 2; column: 60}] with root cause

org.xml.sax.SAXException: The class for the root element 'io.veracode.asc.bbaukema.castordeserialization.Dangerous' could not be found.
        at org.exolab.castor.xml.UnmarshalHandler.startElement(UnmarshalHandler.java:1607) ~[castor-xml-1.3.1.jar:$VERSION$$RELEASE$]
        at org.exolab.castor.xml.UnmarshalHandler.startElement(UnmarshalHandler.java:1436) ~[castor-xml-1.3.1.jar:$VERSION$$RELEASE$]
        at com.sun.org.apache.xerces.internal.parsers.AbstractSAXParser.startElement(AbstractSAXParser.java:509) ~[na:1.8.0_202]
        at com.sun.org.apache.xerces.internal.parsers.AbstractXMLDocumentParser.emptyElement(AbstractXMLDocumentParser.java:182) ~[na:1.8.0_202]
        at com.sun.org.apache.xerces.internal.impl.dtd.XMLDTDValidator.emptyElement(XMLDTDValidator.java:766) ~[na:1.8.0_202]
        at com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl.scanStartElement(XMLDocumentFragmentScannerImpl.java:1339) ~[na:1.8.0_202]
        at com.sun.org.apache.xerces.internal.impl.XMLDocumentScannerImpl$ContentDriver.scanRootElementHook(XMLDocumentScannerImpl.java:1289) ~[na:1.8.0_202]
        at com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl$FragmentContentDriver.next(XMLDocumentFragmentScannerImpl.java:3132) ~[na:1.8.0_202]
        at com.sun.org.apache.xerces.internal.impl.XMLDocumentScannerImpl$PrologDriver.next(XMLDocumentScannerImpl.java:852) ~[na:1.8.0_202]
        at com.sun.org.apache.xerces.internal.impl.XMLDocumentScannerImpl.next(XMLDocumentScannerImpl.java:602) ~[na:1.8.0_202]
        at com.sun.org.apache.xerces.internal.impl.XMLDocumentFragmentScannerImpl.scanDocument(XMLDocumentFragmentScannerImpl.java:505) ~[na:1.8.0_202]
        at com.sun.org.apache.xerces.internal.parsers.XML11Configuration.parse(XML11Configuration.java:842) ~[na:1.8.0_202]
        at com.sun.org.apache.xerces.internal.parsers.XML11Configuration.parse(XML11Configuration.java:771) ~[na:1.8.0_202]
        at com.sun.org.apache.xerces.internal.parsers.XMLParser.parse(XMLParser.java:141) ~[na:1.8.0_202]
        at com.sun.org.apache.xerces.internal.parsers.AbstractSAXParser.parse(AbstractSAXParser.java:1213) ~[na:1.8.0_202]
        at com.sun.org.apache.xerces.internal.jaxp.SAXParserImpl$JAXPSAXParser.parse(SAXParserImpl.java:643) ~[na:1.8.0_202]
        at org.exolab.castor.xml.Unmarshaller.unmarshal(Unmarshaller.java:748) ~[castor-xml-1.3.1.jar:$VERSION$$RELEASE$]
        at org.exolab.castor.xml.Unmarshaller.unmarshal(Unmarshaller.java:626) ~[castor-xml-1.3.1.jar:$VERSION$$RELEASE$]
        at io.veracode.asc.bbaukema.castordeserialization.HelloWorldController.unmarshalSafe(HelloWorldController.java:134) ~[classes/:na]
        at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method) ~[na:1.8.0_202]
        at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62) ~[na:1.8.0_202]
        at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43) ~[na:1.8.0_202]
        at java.lang.reflect.Method.invoke(Method.java:498) ~[na:1.8.0_202]
        at org.springframework.web.method.support.InvocableHandlerMethod.doInvoke(InvocableHandlerMethod.java:205) ~[spring-web-5.3.13.jar:5.3.13]
        at org.springframework.web.method.support.InvocableHandlerMethod.invokeForRequest(InvocableHandlerMethod.java:150) ~[spring-web-5.3.13.jar:5.3.13]
        at org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod.invokeAndHandle(ServletInvocableHandlerMethod.java:117) ~[spring-webmvc-5.3.13.jar:5.3.13]
        at org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter.invokeHandlerMethod(RequestMappingHandlerAdapter.java:895) ~[spring-webmvc-5.3.13.jar:5.3.13]
        at org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter.handleInternal(RequestMappingHandlerAdapter.java:808) ~[spring-webmvc-5.3.13.jar:5.3.13]
        at org.springframework.web.servlet.mvc.method.AbstractHandlerMethodAdapter.handle(AbstractHandlerMethodAdapter.java:87) ~[spring-webmvc-5.3.13.jar:5.3.13]
        at org.springframework.web.servlet.DispatcherServlet.doDispatch(DispatcherServlet.java:1067) ~[spring-webmvc-5.3.13.jar:5.3.13]
        at org.springframework.web.servlet.DispatcherServlet.doService(DispatcherServlet.java:963) ~[spring-webmvc-5.3.13.jar:5.3.13]
        at org.springframework.web.servlet.FrameworkServlet.processRequest(FrameworkServlet.java:1006) ~[spring-webmvc-5.3.13.jar:5.3.13]
        at org.springframework.web.servlet.FrameworkServlet.doGet(FrameworkServlet.java:898) ~[spring-webmvc-5.3.13.jar:5.3.13]
        at javax.servlet.http.HttpServlet.service(HttpServlet.java:655) ~[tomcat-embed-core-9.0.55.jar:4.0.FR]
        at org.springframework.web.servlet.FrameworkServlet.service(FrameworkServlet.java:883) ~[spring-webmvc-5.3.13.jar:5.3.13]
        at javax.servlet.http.HttpServlet.service(HttpServlet.java:764) ~[tomcat-embed-core-9.0.55.jar:4.0.FR]
        at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:227) ~[tomcat-embed-core-9.0.55.jar:9.0.55]
        at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:162) ~[tomcat-embed-core-9.0.55.jar:9.0.55]
        at org.apache.tomcat.websocket.server.WsFilter.doFilter(WsFilter.java:53) ~[tomcat-embed-websocket-9.0.55.jar:9.0.55]
        at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:189) ~[tomcat-embed-core-9.0.55.jar:9.0.55]
        at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:162) ~[tomcat-embed-core-9.0.55.jar:9.0.55]
        at org.springframework.web.filter.RequestContextFilter.doFilterInternal(RequestContextFilter.java:100) ~[spring-web-5.3.13.jar:5.3.13]
        at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:119) ~[spring-web-5.3.13.jar:5.3.13]
        at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:189) ~[tomcat-embed-core-9.0.55.jar:9.0.55]
        at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:162) ~[tomcat-embed-core-9.0.55.jar:9.0.55]
        at org.springframework.web.filter.FormContentFilter.doFilterInternal(FormContentFilter.java:93) ~[spring-web-5.3.13.jar:5.3.13]
        at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:119) ~[spring-web-5.3.13.jar:5.3.13]
        at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:189) ~[tomcat-embed-core-9.0.55.jar:9.0.55]
        at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:162) ~[tomcat-embed-core-9.0.55.jar:9.0.55]
        at org.springframework.web.filter.CharacterEncodingFilter.doFilterInternal(CharacterEncodingFilter.java:201) ~[spring-web-5.3.13.jar:5.3.13]
        at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:119) ~[spring-web-5.3.13.jar:5.3.13]
        at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:189) ~[tomcat-embed-core-9.0.55.jar:9.0.55]
        at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:162) ~[tomcat-embed-core-9.0.55.jar:9.0.55]
        at org.apache.catalina.core.StandardWrapperValve.invoke(StandardWrapperValve.java:197) ~[tomcat-embed-core-9.0.55.jar:9.0.55]
        at org.apache.catalina.core.StandardContextValve.invoke(StandardContextValve.java:97) [tomcat-embed-core-9.0.55.jar:9.0.55]
        at org.apache.catalina.authenticator.AuthenticatorBase.invoke(AuthenticatorBase.java:540) [tomcat-embed-core-9.0.55.jar:9.0.55]
        at org.apache.catalina.core.StandardHostValve.invoke(StandardHostValve.java:135) [tomcat-embed-core-9.0.55.jar:9.0.55]
        at org.apache.catalina.valves.ErrorReportValve.invoke(ErrorReportValve.java:92) [tomcat-embed-core-9.0.55.jar:9.0.55]
        at org.apache.catalina.core.StandardEngineValve.invoke(StandardEngineValve.java:78) [tomcat-embed-core-9.0.55.jar:9.0.55]
        at org.apache.catalina.connector.CoyoteAdapter.service(CoyoteAdapter.java:357) [tomcat-embed-core-9.0.55.jar:9.0.55]
        at org.apache.coyote.http11.Http11Processor.service(Http11Processor.java:382) [tomcat-embed-core-9.0.55.jar:9.0.55]
        at org.apache.coyote.AbstractProcessorLight.process(AbstractProcessorLight.java:65) [tomcat-embed-core-9.0.55.jar:9.0.55]
        at org.apache.coyote.AbstractProtocol$ConnectionHandler.process(AbstractProtocol.java:895) [tomcat-embed-core-9.0.55.jar:9.0.55]
        at org.apache.tomcat.util.net.NioEndpoint$SocketProcessor.doRun(NioEndpoint.java:1722) [tomcat-embed-core-9.0.55.jar:9.0.55]
        at org.apache.tomcat.util.net.SocketProcessorBase.run(SocketProcessorBase.java:49) [tomcat-embed-core-9.0.55.jar:9.0.55]
        at org.apache.tomcat.util.threads.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1191) [tomcat-embed-core-9.0.55.jar:9.0.55]
        at org.apache.tomcat.util.threads.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:659) [tomcat-embed-core-9.0.55.jar:9.0.55]
        at org.apache.tomcat.util.threads.TaskThread$WrappingRunnable.run(TaskThread.java:61) [tomcat-embed-core-9.0.55.jar:9.0.55]
        at java.lang.Thread.run(Thread.java:748) [na:1.8.0_202]
```

0.9.6
-----
A similar example has been provided for 0.9.6 but as this has known vulnerabilities we don't recommend use of this version.

Demo of 0.9.5
-------------

```shell
cd 095 && mvn spring-boot:run
```

Access http://localhost:8080/

This will marshall the following XML:
```xml
AllowedXml: <?xml version="1.0" encoding="UTF-8"?>
<io.veracode.asc.bbaukema.castordeserialization.Allowed><io.veracode.asc.bbaukema.castordeserialization.Dangerous/></io.veracode.asc.bbaukema.castordeserialization.Allowed>
DangerousXml: <?xml version="1.0" encoding="UTF-8"?>
<io.veracode.asc.bbaukema.castordeserialization.Dangerous/>
```
Embedded in the following links:
* [Allowed](http://localhost:8080/unmarshal?input=PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4KPGlvLnZlcmFjb2RlLmFzYy5iYmF1a2VtYS5jYXN0b3JkZXNlcmlhbGl6YXRpb24uQWxsb3dlZD48aW8udmVyYWNvZGUuYXNjLmJiYXVrZW1hLmNhc3RvcmRlc2VyaWFsaXphdGlvbi5EYW5nZXJvdXMvPjwvaW8udmVyYWNvZGUuYXNjLmJiYXVrZW1hLmNhc3RvcmRlc2VyaWFsaXphdGlvbi5BbGxvd2VkPg%3D%3D)
* [Dangerous](http://localhost:8080/unmarshal?input=PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4KPGlvLnZlcmFjb2RlLmFzYy5iYmF1a2VtYS5jYXN0b3JkZXNlcmlhbGl6YXRpb24uRGFuZ2Vyb3VzLz4%3D)

(Note that Dangerous does not do anything actually dangerous)

Going to Allowed shows:
```shell
% curl "http://localhost:8080/unmarshal?input=PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4KPGlvLnZlcmFjb2RlLmFzYy5iYmF1a2VtYS5jYXN0b3JkZXNlcmlhbGl6YXRpb24uQWxsb3dlZD48aW8udmVyYWNvZGUuYXNjLmJiYXVrZW1hLmNhc3RvcmRlc2VyaWFsaXphdGlvbi5EYW5nZXJvdXMvPjwvaW8udmVyYWNvZGUuYXNjLmJiYXVrZW1hLmNhc3RvcmRlc2VyaWFsaXphdGlvbi5BbGxvd2VkPg%3D%3D"
{"timestamp":"2021-11-26T13:38:31.903+00:00","status":500,"error":"Internal Server Error","path":"/unmarshal"}%      
```
With the following ERROR in the console output:
```
2021-11-26 14:38:31.896 ERROR 72517 --- [nio-8080-exec-4] o.a.c.c.C.[.[.[/].[dispatcherServlet]    : Servlet.service() for servlet [dispatcherServlet] in context with path [] threw exception [Request processing failed; nested exception is org.xml.sax.SAXException: unable to find FieldDescriptor for 'io.veracode.asc.bbaukema.castordeserialization.Dangerous' in ClassDescriptor of io.veracode.asc.bbaukema.castordeserialization.Allowed{file: [not available]; line: 2; column: 116}] with root cause

org.exolab.castor.xml.MarshalException: unable to find FieldDescriptor for 'io.veracode.asc.bbaukema.castordeserialization.Dangerous' in ClassDescriptor of io.veracode.asc.bbaukema.castordeserialization.Allowed
	at org.exolab.castor.xml.Unmarshaller.unmarshal(Unmarshaller.java:563) ~[castor-0.9.5.jar:0.9.5]
	at org.exolab.castor.xml.Unmarshaller.unmarshal(Unmarshaller.java:487) ~[castor-0.9.5.jar:0.9.5]
	at io.veracode.asc.bbaukema.castordeserialization.HelloWorldController.unmarshal(HelloWorldController.java:100) ~[classes/:na]
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method) ~[na:na]
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62) ~[na:na]
	at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43) ~[na:na]
	at java.base/java.lang.reflect.Method.invoke(Method.java:566) ~[na:na]
	at org.springframework.web.method.support.InvocableHandlerMethod.doInvoke(InvocableHandlerMethod.java:205) ~[spring-web-5.3.13.jar:5.3.13]
	at org.springframework.web.method.support.InvocableHandlerMethod.invokeForRequest(InvocableHandlerMethod.java:150) ~[spring-web-5.3.13.jar:5.3.13]
	at org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod.invokeAndHandle(ServletInvocableHandlerMethod.java:117) ~[spring-webmvc-5.3.13.jar:5.3.13]
	at org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter.invokeHandlerMethod(RequestMappingHandlerAdapter.java:895) ~[spring-webmvc-5.3.13.jar:5.3.13]
	at org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter.handleInternal(RequestMappingHandlerAdapter.java:808) ~[spring-webmvc-5.3.13.jar:5.3.13]
	at org.springframework.web.servlet.mvc.method.AbstractHandlerMethodAdapter.handle(AbstractHandlerMethodAdapter.java:87) ~[spring-webmvc-5.3.13.jar:5.3.13]
	at org.springframework.web.servlet.DispatcherServlet.doDispatch(DispatcherServlet.java:1067) ~[spring-webmvc-5.3.13.jar:5.3.13]
	at org.springframework.web.servlet.DispatcherServlet.doService(DispatcherServlet.java:963) ~[spring-webmvc-5.3.13.jar:5.3.13]
	at org.springframework.web.servlet.FrameworkServlet.processRequest(FrameworkServlet.java:1006) ~[spring-webmvc-5.3.13.jar:5.3.13]
	at org.springframework.web.servlet.FrameworkServlet.doGet(FrameworkServlet.java:898) ~[spring-webmvc-5.3.13.jar:5.3.13]
	at javax.servlet.http.HttpServlet.service(HttpServlet.java:655) ~[tomcat-embed-core-9.0.55.jar:4.0.FR]
	at org.springframework.web.servlet.FrameworkServlet.service(FrameworkServlet.java:883) ~[spring-webmvc-5.3.13.jar:5.3.13]
	at javax.servlet.http.HttpServlet.service(HttpServlet.java:764) ~[tomcat-embed-core-9.0.55.jar:4.0.FR]
	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:227) ~[tomcat-embed-core-9.0.55.jar:9.0.55]
	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:162) ~[tomcat-embed-core-9.0.55.jar:9.0.55]
	at org.apache.tomcat.websocket.server.WsFilter.doFilter(WsFilter.java:53) ~[tomcat-embed-websocket-9.0.55.jar:9.0.55]
	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:189) ~[tomcat-embed-core-9.0.55.jar:9.0.55]
	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:162) ~[tomcat-embed-core-9.0.55.jar:9.0.55]
	at org.springframework.web.filter.RequestContextFilter.doFilterInternal(RequestContextFilter.java:100) ~[spring-web-5.3.13.jar:5.3.13]
	at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:119) ~[spring-web-5.3.13.jar:5.3.13]
	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:189) ~[tomcat-embed-core-9.0.55.jar:9.0.55]
	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:162) ~[tomcat-embed-core-9.0.55.jar:9.0.55]
	at org.springframework.web.filter.FormContentFilter.doFilterInternal(FormContentFilter.java:93) ~[spring-web-5.3.13.jar:5.3.13]
	at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:119) ~[spring-web-5.3.13.jar:5.3.13]
	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:189) ~[tomcat-embed-core-9.0.55.jar:9.0.55]
	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:162) ~[tomcat-embed-core-9.0.55.jar:9.0.55]
	at org.springframework.web.filter.CharacterEncodingFilter.doFilterInternal(CharacterEncodingFilter.java:201) ~[spring-web-5.3.13.jar:5.3.13]
	at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:119) ~[spring-web-5.3.13.jar:5.3.13]
	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:189) ~[tomcat-embed-core-9.0.55.jar:9.0.55]
	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:162) ~[tomcat-embed-core-9.0.55.jar:9.0.55]
	at org.apache.catalina.core.StandardWrapperValve.invoke(StandardWrapperValve.java:197) ~[tomcat-embed-core-9.0.55.jar:9.0.55]
	at org.apache.catalina.core.StandardContextValve.invoke(StandardContextValve.java:97) ~[tomcat-embed-core-9.0.55.jar:9.0.55]
	at org.apache.catalina.authenticator.AuthenticatorBase.invoke(AuthenticatorBase.java:540) ~[tomcat-embed-core-9.0.55.jar:9.0.55]
	at org.apache.catalina.core.StandardHostValve.invoke(StandardHostValve.java:135) ~[tomcat-embed-core-9.0.55.jar:9.0.55]
	at org.apache.catalina.valves.ErrorReportValve.invoke(ErrorReportValve.java:92) ~[tomcat-embed-core-9.0.55.jar:9.0.55]
	at org.apache.catalina.core.StandardEngineValve.invoke(StandardEngineValve.java:78) ~[tomcat-embed-core-9.0.55.jar:9.0.55]
	at org.apache.catalina.connector.CoyoteAdapter.service(CoyoteAdapter.java:357) ~[tomcat-embed-core-9.0.55.jar:9.0.55]
	at org.apache.coyote.http11.Http11Processor.service(Http11Processor.java:382) ~[tomcat-embed-core-9.0.55.jar:9.0.55]
	at org.apache.coyote.AbstractProcessorLight.process(AbstractProcessorLight.java:65) ~[tomcat-embed-core-9.0.55.jar:9.0.55]
	at org.apache.coyote.AbstractProtocol$ConnectionHandler.process(AbstractProtocol.java:895) ~[tomcat-embed-core-9.0.55.jar:9.0.55]
	at org.apache.tomcat.util.net.NioEndpoint$SocketProcessor.doRun(NioEndpoint.java:1722) ~[tomcat-embed-core-9.0.55.jar:9.0.55]
	at org.apache.tomcat.util.net.SocketProcessorBase.run(SocketProcessorBase.java:49) ~[tomcat-embed-core-9.0.55.jar:9.0.55]
	at org.apache.tomcat.util.threads.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1191) ~[tomcat-embed-core-9.0.55.jar:9.0.55]
	at org.apache.tomcat.util.threads.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:659) ~[tomcat-embed-core-9.0.55.jar:9.0.55]
	at org.apache.tomcat.util.threads.TaskThread$WrappingRunnable.run(TaskThread.java:61) ~[tomcat-embed-core-9.0.55.jar:9.0.55]
	at java.base/java.lang.Thread.run(Thread.java:829) ~[na:na]
```

Going to Dangerous shows:
```shell
 % curl "http://localhost:8080/unmarshal?input=PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4KPGlvLnZlcmFjb2RlLmFzYy5iYmF1a2VtYS5jYXN0b3JkZXNlcmlhbGl6YXRpb24uRGFuZ2Vyb3VzLz4%3D"
{"timestamp":"2021-11-26T13:39:17.375+00:00","status":500,"error":"Internal Server Error","path":"/unmarshal"}%      
```

With the following output in the console:
```
2021-11-26 14:39:17.373 ERROR 72517 --- [nio-8080-exec-5] o.a.c.c.C.[.[.[/].[dispatcherServlet]    : Servlet.service() for servlet [dispatcherServlet] in context with path [] threw exception [Request processing failed; nested exception is org.xml.sax.SAXException: The class for the root element 'io.veracode.asc.bbaukema.castordeserialization.Dangerous' could not be found.{file: [not available]; line: 2; column: 60}] with root cause

org.exolab.castor.xml.MarshalException: The class for the root element 'io.veracode.asc.bbaukema.castordeserialization.Dangerous' could not be found.
	at org.exolab.castor.xml.Unmarshaller.unmarshal(Unmarshaller.java:563) ~[castor-0.9.5.jar:0.9.5]
	at org.exolab.castor.xml.Unmarshaller.unmarshal(Unmarshaller.java:487) ~[castor-0.9.5.jar:0.9.5]
	at io.veracode.asc.bbaukema.castordeserialization.HelloWorldController.unmarshal(HelloWorldController.java:100) ~[classes/:na]
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method) ~[na:na]
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62) ~[na:na]
	at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43) ~[na:na]
	at java.base/java.lang.reflect.Method.invoke(Method.java:566) ~[na:na]
	at org.springframework.web.method.support.InvocableHandlerMethod.doInvoke(InvocableHandlerMethod.java:205) ~[spring-web-5.3.13.jar:5.3.13]
	at org.springframework.web.method.support.InvocableHandlerMethod.invokeForRequest(InvocableHandlerMethod.java:150) ~[spring-web-5.3.13.jar:5.3.13]
	at org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod.invokeAndHandle(ServletInvocableHandlerMethod.java:117) ~[spring-webmvc-5.3.13.jar:5.3.13]
	at org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter.invokeHandlerMethod(RequestMappingHandlerAdapter.java:895) ~[spring-webmvc-5.3.13.jar:5.3.13]
	at org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter.handleInternal(RequestMappingHandlerAdapter.java:808) ~[spring-webmvc-5.3.13.jar:5.3.13]
	at org.springframework.web.servlet.mvc.method.AbstractHandlerMethodAdapter.handle(AbstractHandlerMethodAdapter.java:87) ~[spring-webmvc-5.3.13.jar:5.3.13]
	at org.springframework.web.servlet.DispatcherServlet.doDispatch(DispatcherServlet.java:1067) ~[spring-webmvc-5.3.13.jar:5.3.13]
	at org.springframework.web.servlet.DispatcherServlet.doService(DispatcherServlet.java:963) ~[spring-webmvc-5.3.13.jar:5.3.13]
	at org.springframework.web.servlet.FrameworkServlet.processRequest(FrameworkServlet.java:1006) ~[spring-webmvc-5.3.13.jar:5.3.13]
	at org.springframework.web.servlet.FrameworkServlet.doGet(FrameworkServlet.java:898) ~[spring-webmvc-5.3.13.jar:5.3.13]
	at javax.servlet.http.HttpServlet.service(HttpServlet.java:655) ~[tomcat-embed-core-9.0.55.jar:4.0.FR]
	at org.springframework.web.servlet.FrameworkServlet.service(FrameworkServlet.java:883) ~[spring-webmvc-5.3.13.jar:5.3.13]
	at javax.servlet.http.HttpServlet.service(HttpServlet.java:764) ~[tomcat-embed-core-9.0.55.jar:4.0.FR]
	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:227) ~[tomcat-embed-core-9.0.55.jar:9.0.55]
	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:162) ~[tomcat-embed-core-9.0.55.jar:9.0.55]
	at org.apache.tomcat.websocket.server.WsFilter.doFilter(WsFilter.java:53) ~[tomcat-embed-websocket-9.0.55.jar:9.0.55]
	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:189) ~[tomcat-embed-core-9.0.55.jar:9.0.55]
	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:162) ~[tomcat-embed-core-9.0.55.jar:9.0.55]
	at org.springframework.web.filter.RequestContextFilter.doFilterInternal(RequestContextFilter.java:100) ~[spring-web-5.3.13.jar:5.3.13]
	at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:119) ~[spring-web-5.3.13.jar:5.3.13]
	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:189) ~[tomcat-embed-core-9.0.55.jar:9.0.55]
	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:162) ~[tomcat-embed-core-9.0.55.jar:9.0.55]
	at org.springframework.web.filter.FormContentFilter.doFilterInternal(FormContentFilter.java:93) ~[spring-web-5.3.13.jar:5.3.13]
	at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:119) ~[spring-web-5.3.13.jar:5.3.13]
	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:189) ~[tomcat-embed-core-9.0.55.jar:9.0.55]
	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:162) ~[tomcat-embed-core-9.0.55.jar:9.0.55]
	at org.springframework.web.filter.CharacterEncodingFilter.doFilterInternal(CharacterEncodingFilter.java:201) ~[spring-web-5.3.13.jar:5.3.13]
	at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:119) ~[spring-web-5.3.13.jar:5.3.13]
	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:189) ~[tomcat-embed-core-9.0.55.jar:9.0.55]
	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:162) ~[tomcat-embed-core-9.0.55.jar:9.0.55]
	at org.apache.catalina.core.StandardWrapperValve.invoke(StandardWrapperValve.java:197) ~[tomcat-embed-core-9.0.55.jar:9.0.55]
	at org.apache.catalina.core.StandardContextValve.invoke(StandardContextValve.java:97) ~[tomcat-embed-core-9.0.55.jar:9.0.55]
	at org.apache.catalina.authenticator.AuthenticatorBase.invoke(AuthenticatorBase.java:540) ~[tomcat-embed-core-9.0.55.jar:9.0.55]
	at org.apache.catalina.core.StandardHostValve.invoke(StandardHostValve.java:135) ~[tomcat-embed-core-9.0.55.jar:9.0.55]
	at org.apache.catalina.valves.ErrorReportValve.invoke(ErrorReportValve.java:92) ~[tomcat-embed-core-9.0.55.jar:9.0.55]
	at org.apache.catalina.core.StandardEngineValve.invoke(StandardEngineValve.java:78) ~[tomcat-embed-core-9.0.55.jar:9.0.55]
	at org.apache.catalina.connector.CoyoteAdapter.service(CoyoteAdapter.java:357) ~[tomcat-embed-core-9.0.55.jar:9.0.55]
	at org.apache.coyote.http11.Http11Processor.service(Http11Processor.java:382) ~[tomcat-embed-core-9.0.55.jar:9.0.55]
	at org.apache.coyote.AbstractProcessorLight.process(AbstractProcessorLight.java:65) ~[tomcat-embed-core-9.0.55.jar:9.0.55]
	at org.apache.coyote.AbstractProtocol$ConnectionHandler.process(AbstractProtocol.java:895) ~[tomcat-embed-core-9.0.55.jar:9.0.55]
	at org.apache.tomcat.util.net.NioEndpoint$SocketProcessor.doRun(NioEndpoint.java:1722) ~[tomcat-embed-core-9.0.55.jar:9.0.55]
	at org.apache.tomcat.util.net.SocketProcessorBase.run(SocketProcessorBase.java:49) ~[tomcat-embed-core-9.0.55.jar:9.0.55]
	at org.apache.tomcat.util.threads.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1191) ~[tomcat-embed-core-9.0.55.jar:9.0.55]
	at org.apache.tomcat.util.threads.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:659) ~[tomcat-embed-core-9.0.55.jar:9.0.55]
	at org.apache.tomcat.util.threads.TaskThread$WrappingRunnable.run(TaskThread.java:61) ~[tomcat-embed-core-9.0.55.jar:9.0.55]
	at java.base/java.lang.Thread.run(Thread.java:829) ~[na:na]
```

Why this does not reproduce in 0.9.5
-------------------------------------

Debugging the application is appears that the ```Unmarshaller``` calls 
the SAX streaming XML parser with the ```UnmarshallerHandler``` which
which has a ```startElement``` method.
This eventually calls ```_cdResolver.resolveByXMLName``` (on line 948).
This appears to use 3 locations to check for a 'Descriptor', 2 of these
are caches (which are empty) the only other one is the mapping.

This appears to be a bug in 0.9.5?
Tested in 0.9.5.4 this also does not work

Testing in 0.9.6 does show that Dangerous is unmarshalled.
