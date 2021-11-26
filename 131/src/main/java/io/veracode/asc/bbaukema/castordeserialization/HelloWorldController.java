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
import java.util.Base64;

@RestController
public class HelloWorldController {

    @GetMapping("/")
    public String index() throws MarshalException, ValidationException, IOException, MappingException {

        String mappingString = "<?xml version=\"1.0\"?>";
        mappingString += "<!DOCTYPE mapping PUBLIC \"-//EXOLAB/Castor Mapping DTD Version 1.0//EN\"\n";
        mappingString += "                          \"http://castor.org/mapping.dtd\">";
        mappingString += "<mapping><class ";
        mappingString += "name=\"io.veracode.asc.bbaukema.castordeserialization.Dangerous\">";
        mappingString += "<map-to xml=\"io.veracode.asc.bbaukema.castordeserialization.Dangerous\"/>";
        mappingString += "</class>";
        mappingString += "<class ";
        mappingString += "name=\"io.veracode.asc.bbaukema.castordeserialization.Allowed\">";
        mappingString += "<map-to xml=\"io.veracode.asc.bbaukema.castordeserialization.Allowed\"/>";
        mappingString += "</class></mapping>";

        Mapping mapping = new Mapping();
        InputSource inputSource = new InputSource( new StringReader( mappingString ) );
        mapping.loadMapping(inputSource);

        StringWriter stringWriter = new StringWriter();
        //Marshaller marshaller = new Marshaller(stringWriter);
        XMLContext context = new XMLContext();
        context.addMapping(mapping);

        Marshaller marshaller = context.createMarshaller();
        marshaller.setWriter(stringWriter);
//        marshaller.setMapping(mapping);

        Allowed allowed = new Allowed();
        marshaller.marshal(allowed);
        String allowedXml = stringWriter.toString();
        System.out.println("AllowedXml: " + allowedXml);
        byte[] encodedAllowedXml = Base64.getEncoder().encode(allowedXml.getBytes());

        stringWriter = new StringWriter();
//        marshaller = new Marshaller(stringWriter);
        marshaller.setWriter(stringWriter);
//        marshaller.setMapping(mapping);
        Dangerous dangerous = new Dangerous();
        marshaller.marshal(dangerous);
        String dangerousXml = stringWriter.toString();
        System.out.println("DangerousXml: " + dangerousXml);
        stringWriter.flush();
        byte[] encodedDangerousXml = Base64.getEncoder().encode(dangerousXml.getBytes());

        return "<a href='/unmarshal?input=" +
                URLEncoder.encode(new String(encodedAllowedXml)) +
                "'>Unmarshal Allowed XML</a><br>" +
                "<a href='/unmarshal?input=" +
                URLEncoder.encode(new String(encodedDangerousXml)) +
                "'>Unmarshal Dangerous XML</a>";
    }

    @GetMapping("/unmarshal")
    public String unmarshal(@RequestParam(name = "input") String input) throws MarshalException, ValidationException {
        String xml = new String(Base64.getDecoder().decode(input));

        Unmarshaller unmarshaller = new Unmarshaller();
        Object unmarshalled = unmarshaller.unmarshal(new StringReader(xml));

        return "Unmarshalled: " + unmarshalled.getClass().toGenericString();
    }
}
