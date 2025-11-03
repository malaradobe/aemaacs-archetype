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
 * Workflow process that marks Spanish content as APPROVED after reviewer approval.
 * Once marked as APPROVED, the editor-spanish user can publish the content.
 */
@Component(
    service = WorkflowProcess.class,
    property = {
        "process.label=Approval Marking Process"
    }
)
public class ApprovalMarkingProcess implements WorkflowProcess {

    private static final Logger LOG = LoggerFactory.getLogger(ApprovalMarkingProcess.class);
    
    private static final String WORKFLOW_STATE_PROPERTY = "workflowState";
    private static final String WORKFLOW_APPROVED_BY_PROPERTY = "workflowApprovedBy";
    private static final String WORKFLOW_APPROVED_DATE_PROPERTY = "workflowApprovedDate";
    private static final String STATE_APPROVED = "APPROVED";

    @Override
    public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaDataMap) 
            throws WorkflowException {
        
        try {
            String payloadPath = workItem.getWorkflowData().getPayload().toString();
            LOG.info("Marking content as approved for payload: {}", payloadPath);
            
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
            
            // Mark content as APPROVED
            properties.put(WORKFLOW_STATE_PROPERTY, STATE_APPROVED);
            
            // Store approval metadata
            properties.put(WORKFLOW_APPROVED_BY_PROPERTY, workItem.getWorkflow().getInitiator());
            properties.put(WORKFLOW_APPROVED_DATE_PROPERTY, java.util.Calendar.getInstance());
            
            // Store approval comment if provided
            MetaDataMap wfMetadata = workItem.getWorkflow().getWorkflowData().getMetaDataMap();
            if (wfMetadata.containsKey("comment")) {
                String comment = wfMetadata.get("comment", String.class);
                properties.put("workflowApprovalComment", comment);
                LOG.info("Stored approval comment: {}", comment);
            }
            
            // Commit changes
            resourceResolver.commit();
            
            LOG.info("Content marked as APPROVED: {}. Editor can now publish.", payloadPath);
            
            // TODO: In production, send notification to editor that content is approved and ready to publish
            // using Day CQ Mail Service
            
        } catch (org.apache.sling.api.resource.PersistenceException e) {
            LOG.error("Persistence exception during approval marking", e);
            throw new WorkflowException("Failed to mark content as approved", e);
        }
    }
}

