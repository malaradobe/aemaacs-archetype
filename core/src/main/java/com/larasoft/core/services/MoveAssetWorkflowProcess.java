package com.larasoft.core.services;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Session;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.AssetManager;
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.WorkflowProcess;
import com.day.cq.workflow.metadata.MetaDataMap;

@Component(service = WorkflowProcess.class, property = {
    "process.label=Move Asset Workflow Process"
})
public class MoveAssetWorkflowProcess implements WorkflowProcess {

    private static final Logger LOGGER = LoggerFactory.getLogger(MoveAssetWorkflowProcess.class);
    private static final String DAM_SERVICE = "dam-service";
    public static final String TAGRET_FOLDER = "targetFolder";
    private static final String PROCESS_ARGS = "PROCESS_ARGS";

    @Reference
    private ResourceResolverFactory resolverFactory;

    private AssetManager assetManager;

    @Override
    public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaDataMap) throws WorkflowException {
        String assetPath = workItem.getWorkflowData().getPayload().toString();

        if (!metaDataMap.containsKey(PROCESS_ARGS)) {
            throw new WorkflowException("Arguments are not provided to this workflow process");
        }

        Map<String, String> processArgsMap = new HashMap<>();
        String[] processArgs = metaDataMap.get(PROCESS_ARGS, "string").toString().split(",");

        for (String wfArgs : processArgs) {
            String[] args = wfArgs.split(":");
            if (args.length == 2) {
                processArgsMap.put(args[0], args[1]);
            } else if (!wfArgs.trim().isEmpty()) {
                throw new WorkflowException("Invalid process argument format: " + wfArgs);
            }
        }

        if (!processArgsMap.containsKey(TAGRET_FOLDER)) {
            throw new WorkflowException("Target folder path is required");
        }

        String targetFolder = processArgsMap.get(TAGRET_FOLDER);

        Map<String, Object> authInfo = new HashMap<>();
        authInfo.put(ResourceResolverFactory.SUBSERVICE, DAM_SERVICE);

        try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(authInfo)) {
            Resource assetResource = resolver.getResource(assetPath);
            if (assetResource == null) {
                throw new WorkflowException("Asset not found at path: " + assetPath);
            }

            Asset asset = assetResource.adaptTo(Asset.class);
            if (asset == null) {
                throw new WorkflowException("Resource is not an asset: " + assetPath);
            }

            Resource targetFolderResource = resolver.getResource(targetFolder);
            if (targetFolderResource == null) {
                throw new WorkflowException("Target folder not found: " + targetFolder);
            }

            if (assetManager == null) {
                // Get the asset manager and move the asset
                assetManager = resolver.adaptTo(AssetManager.class);
                if (assetManager == null) {
                    throw new WorkflowException("Could not get AssetManager");
                }
            }

            // Get the asset name from the path
            String assetName = assetPath.substring(assetPath.lastIndexOf('/') + 1);
            String targetPath = targetFolder + "/" + assetName;

            // Move the asset using the JCR session
            Session session = resolver.adaptTo(Session.class);
            if (session == null) {
                throw new WorkflowException("Could not get JCR Session");
            }

            session.move(assetPath, targetPath);
            session.save();
            LOGGER.info("Successfully moved asset from {} to {}", assetPath, targetPath);
        } catch (Exception e) {
            LOGGER.error("Error moving asset: {}", e.getMessage(), e);
            throw new WorkflowException("Error moving asset: " + e.getMessage(), e);
        }
    }

    public void setResolverFactory(ResourceResolverFactory resolverFactory) {
        this.resolverFactory = resolverFactory;
    }

    public void setAssetManager(AssetManager assetManager) {
        this.assetManager = assetManager;
    }
}