package com.agri.backend.config;

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.transport.http.MessageDispatcherServlet;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.xml.xsd.SimpleXsdSchema;
import org.springframework.xml.xsd.XsdSchema;

/**
 * Registers the SOAP endpoint at /ws/* and exposes the auto-generated
 * WSDL at /ws/farmerRequirement.wsdl
 */
@EnableWs
@Configuration
public class WebServiceConfig {

    @Bean
    public ServletRegistrationBean<MessageDispatcherServlet> messageDispatcherServlet(
            org.springframework.web.context.WebApplicationContext applicationContext) {
        MessageDispatcherServlet servlet = new MessageDispatcherServlet();
        servlet.setApplicationContext(applicationContext);
        servlet.setTransformWsdlLocations(true);
        return new ServletRegistrationBean<>(servlet, "/ws/*");
    }

    @Bean(name = "farmerRequirement")
    public DefaultWsdl11Definition defaultWsdl11Definition(XsdSchema farmerRequirementSchema) {
        DefaultWsdl11Definition wsdl11Definition = new DefaultWsdl11Definition();
        wsdl11Definition.setPortTypeName("FarmerRequirementPort");
        wsdl11Definition.setLocationUri("/ws");
        wsdl11Definition.setTargetNamespace("http://agri.com/soap/farmer");
        wsdl11Definition.setSchema(farmerRequirementSchema);
        return wsdl11Definition;
    }

    @Bean
    public XsdSchema farmerRequirementSchema() {
        return new SimpleXsdSchema(new ClassPathResource("farmer-requirement.xsd"));
    }
}
