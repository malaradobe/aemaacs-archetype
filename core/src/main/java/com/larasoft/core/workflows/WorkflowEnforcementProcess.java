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
 * Workflow process step that enforces workflow completion before allowing publish actions.
 * This process marks content with workflow state metadata to prevent bypass attempts.
 */
@Component(
    service = WorkflowProcess.class,
    property = {
        "process.label=Workflow Enforcement Process"
    }
)
public class WorkflowEnforcementProcess implements WorkflowProcess {

    private static final Logger LOG = LoggerFactory.getLogger(WorkflowEnforcementProcess.class);
    
    private static final String WORKFLOW_STATE_PROPERTY = "workflowState";
    private static final String WORKFLOW_ID_PROPERTY = "workflowId";
    private static final String STATE_IN_PROGRESS = "IN_PROGRESS";

    @Override
    public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaDataMap) 
            throws WorkflowException {
        
        try {
            String payloadPath = workItem.getWorkflowData().getPayload().toString();
            LOG.info("Executing workflow enforcement for payload: {}", payloadPath);
            
            // Get the resource resolver
            ResourceResolver resourceResolver = workflowSession.adaptTo(ResourceResolver.class);
            if (resourceResolver == null) {
                throw new WorkflowException("Unable to obtain ResourceResolver");
            }
            
            // Verify the path is under Spanish content
            if (!payloadPath.startsWith("/content/larasoft/es")) {
                LOG.warn("Workflow enforcement called for non-Spanish content: {}", payloadPath);
                return;
            }
            
            // Get the page resource
            Resource pageResource = resourceResolver.getResource(payloadPath);
            if (pageResource == null) {
                throw new WorkflowException("Resource not found: " + payloadPath);
            }
            
            // Get jcr:content resource
            Resource contentResource = pageResource.getChild("jcr:content");
            if (contentResource == null) {
                LOG.warn("No jcr:content node found for: {}", payloadPath);
                return;
            }
            
            // Get modifiable properties
            org.apache.sling.api.resource.ModifiableValueMap properties = contentResource.adaptTo(org.apache.sling.api.resource.ModifiableValueMap.class);
            if (properties == null) {
                throw new WorkflowException("Unable to modify properties for: " + payloadPath);
            }
            
            // Mark the content as being in workflow
            properties.put(WORKFLOW_STATE_PROPERTY, STATE_IN_PROGRESS);
            properties.put(WORKFLOW_ID_PROPERTY, workItem.getWorkflow().getId());
            
            resourceResolver.commit();
            
            LOG.info("Successfully marked content as in workflow: {}", payloadPath);
            
        } catch (org.apache.sling.api.resource.PersistenceException e) {
            LOG.error("Persistence exception during workflow enforcement", e);
            throw new WorkflowException("Failed to enforce workflow state", e);
        }
    }
}

