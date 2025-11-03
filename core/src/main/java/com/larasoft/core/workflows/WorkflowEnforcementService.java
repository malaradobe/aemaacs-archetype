package com.larasoft.core.workflows;

import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationActionType;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.HashMap;
import java.util.Map;

/**
 * Service that enforces workflow completion before allowing publish actions on Spanish content.
 * Listens to replication events and prevents direct publishing without workflow approval.
 */
@Component(
    service = EventHandler.class,
    immediate = true,
    property = {
        EventConstants.EVENT_TOPIC + "=" + ReplicationAction.EVENT_TOPIC,
        "service.description=Workflow Enforcement Service for Spanish Content"
    }
)
public class WorkflowEnforcementService implements EventHandler {

    private static final Logger LOG = LoggerFactory.getLogger(WorkflowEnforcementService.class);
    
    private static final String WORKFLOW_STATE_PROPERTY = "workflowState";
    private static final String STATE_APPROVED = "APPROVED";
    private static final String STATE_IN_PROGRESS = "IN_PROGRESS";
    private static final String SPANISH_CONTENT_PATH = "/content/larasoft/es";

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Reference
    private SlingRepository repository;

    @Activate
    protected void activate() {
        LOG.info("Workflow Enforcement Service activated");
    }

    @Override
    public void handleEvent(Event event) {
        try {
            // Get replication action details
            String path = (String) event.getProperty("path");
            ReplicationActionType actionType = ReplicationActionType.fromName(
                (String) event.getProperty(ReplicationAction.PROPERTY_TYPE)
            );
            String userId = (String) event.getProperty("userId");

            // Only enforce for activate/publish actions on Spanish content
            if (path == null || !path.startsWith(SPANISH_CONTENT_PATH)) {
                return;
            }

            if (actionType != ReplicationActionType.ACTIVATE) {
                return;
            }

            LOG.info("Checking workflow enforcement for publish action: {} by user: {}", path, userId);

            // Get admin session to check workflow state
            Map<String, Object> authInfo = new HashMap<>();
            authInfo.put(ResourceResolverFactory.SUBSERVICE, "internal-content");
            
            try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(authInfo)) {
                Session session = resolver.adaptTo(Session.class);
                if (session == null) {
                    LOG.error("Unable to obtain session for workflow enforcement check");
                    return;
                }

                Resource pageResource = resolver.getResource(path);
                if (pageResource == null) {
                    LOG.warn("Resource not found for path: {}", path);
                    return;
                }

                Node pageNode = pageResource.adaptTo(Node.class);
                if (pageNode == null || !pageNode.hasNode("jcr:content")) {
                    LOG.warn("Page node or jcr:content not found for: {}", path);
                    return;
                }

                Node contentNode = pageNode.getNode("jcr:content");

                // Check if user is in editor-spanish group (only they can publish after approval)
                Session userSession = repository.login();
                boolean isEditor = isUserInGroup(userSession, userId, "editor-spanish");
                userSession.logout();

                // Only editor-spanish users can publish, and only when content is APPROVED
                if (!isEditor) {
                    LOG.warn("Blocking publish attempt - user {} is not in editor-spanish group for: {}", userId, path);
                    throw new SecurityException("Only Spanish content editors can publish approved content");
                }

                // Check workflow state - must be APPROVED
                if (!contentNode.hasProperty(WORKFLOW_STATE_PROPERTY)) {
                    LOG.warn("Blocking publish attempt - no workflow state found for: {}", path);
                    throw new SecurityException("Content must go through approval workflow before publishing");
                }

                String workflowState = contentNode.getProperty(WORKFLOW_STATE_PROPERTY).getString();
                
                if (STATE_IN_PROGRESS.equals(workflowState)) {
                    LOG.warn("Blocking publish attempt - workflow still in progress for: {}", path);
                    throw new SecurityException("Cannot publish while workflow is in progress. Please wait for reviewer approval.");
                }

                if (!STATE_APPROVED.equals(workflowState)) {
                    LOG.warn("Blocking publish attempt - content not approved for: {} (current state: {})", path, workflowState);
                    throw new SecurityException("Content must be approved by reviewer before publishing. Current state: " + workflowState);
                }

                LOG.info("Publish action allowed for: {} by editor: {}", path, userId);

            } catch (LoginException e) {
                LOG.error("Failed to obtain service resource resolver", e);
            }

        } catch (RepositoryException e) {
            LOG.error("Repository exception during workflow enforcement", e);
        } catch (Exception e) {
            LOG.error("Unexpected exception during workflow enforcement", e);
        }
    }

    /**
     * Check if user is member of specified group
     */
    private boolean isUserInGroup(Session session, String userId, String groupName) throws RepositoryException {
        try {
            // Simplified check - in production use UserManager API
            String userPath = "/home/users/" + userId;
            if (session.nodeExists(userPath)) {
                Node user = session.getNode(userPath);
                if (user.hasProperty("rep:groups")) {
                    String[] groups = user.getProperty("rep:groups").toString().split(",");
                    for (String group : groups) {
                        if (group.contains(groupName)) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.debug("Error checking user group membership", e);
        }
        return false;
    }
}

