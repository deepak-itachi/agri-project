package com.agri.backend.soap;

import com.agri.backend.soap.gen.FarmerRequirementRequest;
import com.agri.backend.soap.gen.FarmerRequirementResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.core.WebServiceTemplate;

/**
 * SOAP CLIENT - this is what the REST facade (FarmerController) calls
 * internally so the React frontend only ever talks REST/JSON, while the
 * actual classification logic runs through the real SOAP endpoint at /ws.
 */
@Component
public class FarmerRequirementClient {

    @Value("${soap.farmer-requirement.uri:http://localhost:8080/ws}")
    private String defaultUri;

    private final WebServiceTemplate webServiceTemplate;

    public FarmerRequirementClient(WebServiceTemplate webServiceTemplate) {
        this.webServiceTemplate = webServiceTemplate;
    }

    public FarmerRequirementResponse analyze(FarmerRequirementRequest request) {
        return (FarmerRequirementResponse) webServiceTemplate
                .marshalSendAndReceive(defaultUri, request);
    }
}
