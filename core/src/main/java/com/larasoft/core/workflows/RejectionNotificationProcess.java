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
 * Workflow process that handles rejection of Spanish content.
 * Sets workflow state to REJECTED and can trigger notifications.
 */
@Component(
    service = WorkflowProcess.class,
    property = {
        "process.label=Rejection Notification Process"
    }
)
public class RejectionNotificationProcess implements WorkflowProcess {

    private static final Logger LOG = LoggerFactory.getLogger(RejectionNotificationProcess.class);
    
    private static final String WORKFLOW_STATE_PROPERTY = "workflowState";
    private static final String WORKFLOW_COMMENT_PROPERTY = "workflowComment";
    private static final String STATE_REJECTED = "REJECTED";

    @Override
    public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaDataMap) 
            throws WorkflowException {
        
        try {
            String payloadPath = workItem.getWorkflowData().getPayload().toString();
            LOG.info("Processing rejection for payload: {}", payloadPath);
            
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
            
            // Update workflow state to rejected
            properties.put(WORKFLOW_STATE_PROPERTY, STATE_REJECTED);
            
            // Store rejection comment if provided
            MetaDataMap wfMetadata = workItem.getWorkflow().getWorkflowData().getMetaDataMap();
            if (wfMetadata.containsKey("comment")) {
                String comment = wfMetadata.get("comment", String.class);
                properties.put(WORKFLOW_COMMENT_PROPERTY, comment);
                LOG.info("Stored rejection comment: {}", comment);
            }
            
            resourceResolver.commit();
            
            LOG.info("Content rejected and marked appropriately: {}", payloadPath);
            
            // TODO: In production, send email notification to content editor
            // using Day CQ Mail Service
            
        } catch (org.apache.sling.api.resource.PersistenceException e) {
            LOG.error("Persistence exception during rejection handling", e);
            throw new WorkflowException("Failed to process rejection", e);
        }
    }
}

