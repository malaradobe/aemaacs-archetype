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
 * Workflow process that initiates or re-initiates approval request for Spanish content.
 * Changes workflow state from CHANGES_REQUESTED to IN_PROGRESS (approval in progress).
 * This is used when:
 * 1. Editor first requests approval for new content
 * 2. Editor resubmits content after addressing reviewer's requested changes
 */
@Component(
    service = WorkflowProcess.class,
    property = {
        "process.label=Request Approval Process"
    }
)
public class RequestApprovalProcess implements WorkflowProcess {

    private static final Logger LOG = LoggerFactory.getLogger(RequestApprovalProcess.class);
    
    private static final String WORKFLOW_STATE_PROPERTY = "workflowState";
    private static final String WORKFLOW_SUBMITTED_BY_PROPERTY = "workflowSubmittedBy";
    private static final String WORKFLOW_SUBMITTED_DATE_PROPERTY = "workflowSubmittedDate";
    private static final String STATE_IN_PROGRESS = "IN_PROGRESS";

    @Override
    public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaDataMap) 
            throws WorkflowException {
        
        try {
            String payloadPath = workItem.getWorkflowData().getPayload().toString();
            LOG.info("Initiating approval request for payload: {}", payloadPath);
            
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
                throw new WorkflowException("jcr:content not found for: " + payloadPath);
            }
            
            // Get modifiable value map to update properties
            org.apache.sling.api.resource.ModifiableValueMap properties = contentResource.adaptTo(org.apache.sling.api.resource.ModifiableValueMap.class);
            if (properties == null) {
                throw new WorkflowException("Unable to modify properties for: " + payloadPath);
            }
            
            // Get current state for logging
            String currentState = properties.get(WORKFLOW_STATE_PROPERTY, String.class);
            LOG.info("Current workflow state: {} for path: {}", currentState, payloadPath);
            
            // Change state to IN_PROGRESS (approval in progress)
            properties.put(WORKFLOW_STATE_PROPERTY, STATE_IN_PROGRESS);
            
            // Store submission metadata
            properties.put(WORKFLOW_SUBMITTED_BY_PROPERTY, workItem.getWorkflow().getInitiator());
            properties.put(WORKFLOW_SUBMITTED_DATE_PROPERTY, java.util.Calendar.getInstance());
            
            // Store submission comment if provided
            MetaDataMap wfMetadata = workItem.getWorkflow().getWorkflowData().getMetaDataMap();
            if (wfMetadata.containsKey("comment")) {
                String comment = wfMetadata.get("comment", String.class);
                properties.put("workflowSubmissionComment", comment);
                LOG.info("Stored submission comment: {}", comment);
            }
            
            // Clear previous feedback if content was resubmitted after changes
            if ("CHANGES_REQUESTED".equals(currentState)) {
                LOG.info("Content resubmitted after changes - clearing previous feedback");
                properties.remove("workflowFeedback");
            }
            
            // Commit changes
            resourceResolver.commit();
            
            LOG.info("Content approval request submitted successfully: {}. State changed to IN_PROGRESS.", payloadPath);
            
            // TODO: In production, send notification to reviewer-spanish group members
            // using Day CQ Mail Service to inform them of the approval request
            
        } catch (org.apache.sling.api.resource.PersistenceException e) {
            LOG.error("Persistence exception during approval request submission", e);
            throw new WorkflowException("Failed to submit approval request", e);
        }
    }
}

