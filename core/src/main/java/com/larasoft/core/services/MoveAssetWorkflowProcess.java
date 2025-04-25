package com.larasoft.core.services;

import org.apache.commons.lang3.StringUtils;
import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import com.adobe.granite.workflow.exec.WorkflowData;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
    service = WorkflowProcess.class, 
    property = {
        Constants.SERVICE_DESCRIPTION + "=Move Asset Workflow Process",
        Constants.SERVICE_VENDOR + "=Adobe Systems",
        "process.label=Move Asset Workflow Process"
})
@ServiceDescription("Move Asset Workflow Process")
public class MoveAssetWorkflowProcess implements WorkflowProcess {

    private static final Logger LOG = LoggerFactory.getLogger(MoveAssetWorkflowProcess.class);
    private static final String TARGET_PATH = "/content/dam/larasoft/approved";

    @Override
    public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaDataMap)
            throws WorkflowException {
        ResourceResolver resourceResolver = workflowSession.adaptTo(ResourceResolver.class);
        final WorkflowData workflowData = workItem.getWorkflowData();
        final String workflowType = workflowData.getPayloadType();
        if (!StringUtils.equals(workflowType, "JCR_PATH")) {
            return;
        }

        // Get the path to the JCR resource from the payload
        final String payloadPath = workflowData.getPayload().toString();

        Resource assetResource = resourceResolver.getResource(payloadPath);
        if (assetResource != null) {
            LOG.info("Asset resource found at path: {}", payloadPath);
            
            try {
                // Get the asset name from the path
                String assetName = assetResource.getName();
                
                // Create the target path
                String targetPath = TARGET_PATH;
                
                // Ensure the target folder exists
                Resource targetFolder = resourceResolver.getResource(TARGET_PATH);
                if (targetFolder == null) {
                    LOG.info("Creating target folder at: {}", TARGET_PATH);
                    // Create the target folder if it doesn't exist
                    Resource parentFolder = resourceResolver.getResource("/content/dam/larasoft");
                    if (parentFolder == null) {
                        throw new WorkflowException("Parent folder /content/dam/larasoft does not exist");
                    }
                    resourceResolver.create(parentFolder, "approved", null);
                    resourceResolver.commit();
                    LOG.info("Successfully created target folder at: {}", TARGET_PATH);
                }

                LOG.info("Moving asset from {} to {}", payloadPath, targetPath);
                
                // Move the asset using ResourceResolver
                resourceResolver.move(payloadPath, targetPath);
                resourceResolver.commit();
                
                LOG.info("Successfully moved asset from {} to {}", payloadPath, targetPath);
            } catch (Exception e) {
                LOG.error("Error moving asset: {}", e.getMessage());
                throw new WorkflowException("Failed to move asset", e);
            }
        } else {
            LOG.error("No resource found at path: {}", payloadPath);
        }
    }
}