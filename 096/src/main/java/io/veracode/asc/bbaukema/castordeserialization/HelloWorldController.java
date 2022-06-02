package io.veracode.asc.bbaukema.castordeserialization;

import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.mapping.MappingException;
import org.exolab.castor.xml.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.xml.sax.InputSource;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Base64;

@RestController
public class HelloWorldController {

    @GetMapping("/")
    public String index() throws MarshalException, ValidationException, IOException, MappingException {

        String mappingString = "<?xml version=\"1.0\"?>";
        mappingString += "<!DOCTYPE mapping PUBLIC \"-//EXOLAB/Castor Mapping DTD Version 1.0//EN\"\n";
        mappingString += "                          \"http://castor.org/mapping.dtd\">";
        // Mapping for Dangerous included here for easy marshalled data generation.
        mappingString += "<mapping><class ";
        mappingString += "name=\"io.veracode.asc.bbaukema.castordeserialization.Dangerous\">";
        mappingString += "<map-to xml=\"io.veracode.asc.bbaukema.castordeserialization.Dangerous\"/>";
        mappingString += "</class>";

        // Mapping of Allowed
        mappingString += "<class ";
        mappingString += "name=\"io.veracode.asc.bbaukema.castordeserialization.Allowed\">";
        mappingString += "<map-to xml=\"io.veracode.asc.bbaukema.castordeserialization.Allowed\"/>";
        mappingString += "</class></mapping>";
        Mapping mapping = new Mapping();
        InputSource inputSource = new InputSource( new StringReader( mappingString ) );
        mapping.loadMapping(inputSource);

        // Set up objects to be marshalled
        Allowed allowed = new Allowed();
        allowed.dangerous = new Dangerous();

        // Marshall to XML
        StringWriter stringWriter = new StringWriter();
        Marshaller marshaller = new Marshaller(stringWriter);
        marshaller.setMapping(mapping);
        marshaller.marshal(allowed);
        String allowedXml = stringWriter.toString();
        System.out.println("AllowedXml: " + allowedXml);
        byte[] encodedAllowedXml = Base64.getEncoder().encode(allowedXml.getBytes());

        // Set up object to be marshalled
        Dangerous dangerous = new Dangerous();

        // Marshall to XML
        stringWriter = new StringWriter();
        marshaller = new Marshaller(stringWriter);
        marshaller.setMapping(mapping);
        marshaller.marshal(dangerous);
        String dangerousXml = stringWriter.toString();
        System.out.println("DangerousXml: " + dangerousXml);
        byte[] encodedDangerousXml = Base64.getEncoder().encode(dangerousXml.getBytes());

        return "<br><a href='/unmarshal?input=" +
                URLEncoder.encode(new String(encodedAllowedXml)) +
                "'>Unmarshal Allowed XML</a>" +
                "<br><a href='/unmarshal?input=" +
                URLEncoder.encode(new String(encodedDangerousXml)) +
                "'>Unmarshal Dangerous XML</a>" +
                "<br><a href='/unmarshal-safe?input=" +
                URLEncoder.encode(new String(encodedAllowedXml)) +
                "'>Safely Unmarshal Allowed XML</a>"+
                "<br><a href='/unmarshal-safe?input=" +
                URLEncoder.encode(new String(encodedDangerousXml)) +
                "'>Safely Unmarshal Dangerous XML</a>";
    }

    @GetMapping("/unmarshal")
    public String unmarshal(@RequestParam(name = "input") String input) throws MarshalException, ValidationException, IOException, MappingException {
        String xml = new String(Base64.getDecoder().decode(input));

        // Mapping of Allowed, but *not* of Dangerous.
        String mappingString = "<?xml version=\"1.0\"?>";
        mappingString += "<!DOCTYPE mapping PUBLIC \"-//EXOLAB/Castor Mapping DTD Version 1.0//EN\"\n";
        mappingString += "                          \"http://castor.org/mapping.dtd\">";
        mappingString += "<mapping>";
        mappingString += "<class ";
        mappingString += "name=\"io.veracode.asc.bbaukema.castordeserialization.Allowed\">";
        mappingString += "<map-to xml=\"io.veracode.asc.bbaukema.castordeserialization.Allowed\"/>";
        mappingString += "</class></mapping>";
        Mapping mapping = new Mapping(Dangerous.class.getClassLoader());
        InputSource inputSource = new InputSource( new StringReader( mappingString ) );
        mapping.loadMapping(inputSource);

        Unmarshaller unmarshaller = new Unmarshaller();
        unmarshaller.setMapping(mapping);
        Object unmarshalled = unmarshaller.unmarshal(new StringReader(xml));

        return "Unmarshalled: " + unmarshalled.getClass().toGenericString();
    }

    @GetMapping("/unmarshal-safe")
    public String unmarshalSafe(@RequestParam(name = "input") String input) throws MarshalException, ValidationException, IOException, MappingException {
        String xml = new String(Base64.getDecoder().decode(input));

        // Mapping of Allowed, but *not* of Dangerous.
        String mappingString = "<?xml version=\"1.0\"?>";
        mappingString += "<!DOCTYPE mapping PUBLIC \"-//EXOLAB/Castor Mapping DTD Version 1.0//EN\"\n";
        mappingString += "                          \"http://castor.org/mapping.dtd\">";
        mappingString += "<mapping>";
        mappingString += "<class ";
        mappingString += "name=\"io.veracode.asc.bbaukema.castordeserialization.Allowed\">";
        mappingString += "<map-to xml=\"io.veracode.asc.bbaukema.castordeserialization.Allowed\"/>";
        mappingString += "</class></mapping>";
        Class clazz = getClass();
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
        InputSource inputSource = new InputSource( new StringReader( mappingString ) );
        mapping.loadMapping(inputSource);

        Unmarshaller unmarshaller = new Unmarshaller();
        unmarshaller.setMapping(mapping);
        Object unmarshalled = unmarshaller.unmarshal(new StringReader(xml));

        return "Unmarshalled: " + unmarshalled.getClass().toGenericString();
    }
}
