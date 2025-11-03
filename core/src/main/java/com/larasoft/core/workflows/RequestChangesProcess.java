package com.larasoft.core.workflows;

import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Workflow process that handles change requests for Spanish content.
 * Sets workflow state to CHANGES_REQUESTED and stores reviewer feedback.
 */
@Component(
    service = WorkflowProcess.class,
    property = {
        "process.label=Request Changes Process"
    }
)
public class RequestChangesProcess implements WorkflowProcess {

    private static final Logger LOG = LoggerFactory.getLogger(RequestChangesProcess.class);
    
    private static final String WORKFLOW_STATE_PROPERTY = "workflowState";
    private static final String WORKFLOW_FEEDBACK_PROPERTY = "workflowFeedback";
    private static final String STATE_CHANGES_REQUESTED = "CHANGES_REQUESTED";

    @Override
    public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaDataMap) 
            throws WorkflowException {
        
        try {
            String payloadPath = workItem.getWorkflowData().getPayload().toString();
            LOG.info("Processing change request for payload: {}", payloadPath);
            
            ResourceResolver resourceResolver = workflowSession.adaptTo(ResourceResolver.class);
            if (resourceResolver == null) {
                throw new WorkflowException("Unable to obtain ResourceResolver");
            }
            
            Resource pageResource = resourceResolver.getResource(payloadPath);
            if (pageResource == null) {
                throw new WorkflowException("Resource not found: " + payloadPath);
            }
            
            Resource contentResource = pageResource.getChild("jcr:content");
            if (contentResource == null) {
                throw new WorkflowException("Page content not found: " + payloadPath);
            }
            
            // Get modifiable properties
            org.apache.sling.api.resource.ModifiableValueMap properties = contentResource.adaptTo(org.apache.sling.api.resource.ModifiableValueMap.class);
            if (properties == null) {
                throw new WorkflowException("Unable to modify properties for: " + payloadPath);
            }
            
            // Update workflow state to changes requested
            properties.put(WORKFLOW_STATE_PROPERTY, STATE_CHANGES_REQUESTED);
            
            // Store reviewer feedback if provided
            MetaDataMap wfMetadata = workItem.getWorkflow().getWorkflowData().getMetaDataMap();
            if (wfMetadata.containsKey("comment")) {
                String feedback = wfMetadata.get("comment", String.class);
                properties.put(WORKFLOW_FEEDBACK_PROPERTY, feedback);
                LOG.info("Stored reviewer feedback: {}", feedback);
            }
            
            resourceResolver.commit();
            
            LOG.info("Changes requested and feedback stored for: {}", payloadPath);
            
            // TODO: In production, send email notification to content editor
            // using Day CQ Mail Service with the requested changes
            
        } catch (org.apache.sling.api.resource.PersistenceException e) {
            LOG.error("Persistence exception during change request handling", e);
            throw new WorkflowException("Failed to process change request", e);
        }
    }
}

