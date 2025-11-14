package com.larasoft.core.workflows;

import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationActionType;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
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
 * 
 * This service ensures that:
 * 1. Only editor-spanish group members can publish Spanish content
 * 2. Content must have an APPROVED workflow state to be published
 * 3. Content in IN_PROGRESS, REJECTED, or CHANGES_REQUESTED state cannot be published
 */
@Component(
    service = EventHandler.class,
    immediate = true,
    property = {
        EventConstants.EVENT_TOPIC + "=" + ReplicationAction.EVENT_TOPIC,
        "service.description=Workflow Enforcement Service for Spanish Content"
    }
)
@Designate(ocd = SpanishWorkflowConfig.class)
public class WorkflowEnforcementService implements EventHandler {

    private static final Logger LOG = LoggerFactory.getLogger(WorkflowEnforcementService.class);
    
    private static final String WORKFLOW_STATE_PROPERTY = "workflowState";
    private static final String STATE_APPROVED = "APPROVED";
    private static final String STATE_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATE_REJECTED = "REJECTED";
    private static final String STATE_CHANGES_REQUESTED = "CHANGES_REQUESTED";
    private static final String EDITOR_SPANISH_GROUP = "editor-spanish";
    
    private String spanishContentPath;

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Activate
    @Modified
    protected void activate(SpanishWorkflowConfig config) {
        this.spanishContentPath = config.spanish_content_path();
        LOG.info("Workflow Enforcement Service activated - monitoring path: {}", spanishContentPath);
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
            // Match exact path OR descendants (with trailing slash)
            if (path == null || !isUnderSpanishContentPath(path)) {
                return;
            }

            if (actionType != ReplicationActionType.ACTIVATE) {
                return;
            }

            LOG.info("Checking workflow enforcement for publish action: {} by user: {}", path, userId);

            // Get service session to check workflow state and user permissions
            Map<String, Object> authInfo = new HashMap<>();
            authInfo.put(ResourceResolverFactory.SUBSERVICE, "internal-content");
            
            try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(authInfo)) {
                Session session = resolver.adaptTo(Session.class);
                if (session == null) {
                    LOG.error("Unable to obtain session for workflow enforcement check");
                    throw new SecurityException("Unable to validate workflow state");
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
                boolean isEditor = isUserInEditorSpanishGroup(resolver, userId);

                // Only editor-spanish users can publish, and only when content is APPROVED
                if (!isEditor) {
                    LOG.warn("SECURITY VIOLATION: Blocking publish attempt - user '{}' is not in {} group for: {}", 
                             userId, EDITOR_SPANISH_GROUP, path);
                    throw new SecurityException("Only Spanish content editors can publish approved content. User '" + 
                                              userId + "' is not authorized.");
                }

                // Check workflow state - must be APPROVED
                if (!contentNode.hasProperty(WORKFLOW_STATE_PROPERTY)) {
                    LOG.warn("WORKFLOW VIOLATION: Blocking publish attempt - no workflow state found for: {} by user: {}", 
                             path, userId);
                    throw new SecurityException("Content must go through approval workflow before publishing. " +
                                              "Please click 'Request Approval' button first.");
                }

                String workflowState = contentNode.getProperty(WORKFLOW_STATE_PROPERTY).getString();
                
                // Block specific states
                if (STATE_IN_PROGRESS.equals(workflowState)) {
                    LOG.warn("WORKFLOW VIOLATION: Blocking publish - workflow in progress for: {} by user: {}", 
                             path, userId);
                    throw new SecurityException("Cannot publish while workflow is in progress. " +
                                              "Please wait for reviewer approval.");
                }
                
                if (STATE_REJECTED.equals(workflowState)) {
                    LOG.warn("WORKFLOW VIOLATION: Blocking publish - content rejected for: {} by user: {}", 
                             path, userId);
                    throw new SecurityException("Cannot publish rejected content. " +
                                              "Please review rejection comments and make necessary changes.");
                }
                
                if (STATE_CHANGES_REQUESTED.equals(workflowState)) {
                    LOG.warn("WORKFLOW VIOLATION: Blocking publish - changes requested for: {} by user: {}", 
                             path, userId);
                    throw new SecurityException("Cannot publish content with requested changes. " +
                                              "Please address reviewer feedback and request approval again.");
                }

                if (!STATE_APPROVED.equals(workflowState)) {
                    LOG.warn("WORKFLOW VIOLATION: Blocking publish - invalid workflow state '{}' for: {} by user: {}", 
                             workflowState, path, userId);
                    throw new SecurityException("Content must be approved by reviewer before publishing. " +
                                              "Current state: " + workflowState);
                }

                // All checks passed - allow publish
                LOG.info("✓ Publish action ALLOWED for: {} by editor: {} (workflow state: APPROVED)", path, userId);

            } catch (LoginException e) {
                LOG.error("Failed to obtain service resource resolver", e);
                throw new SecurityException("Unable to validate workflow state: " + e.getMessage());
            }

        } catch (SecurityException e) {
            // Re-throw security exceptions to block the publish
            LOG.error("Security exception blocking publish: {}", e.getMessage());
            throw new RuntimeException(e.getMessage(), e);
        } catch (RepositoryException e) {
            LOG.error("Repository exception during workflow enforcement", e);
            throw new RuntimeException("Workflow enforcement check failed: " + e.getMessage(), e);
        } catch (Exception e) {
            LOG.error("Unexpected exception during workflow enforcement", e);
            throw new RuntimeException("Workflow enforcement check failed: " + e.getMessage(), e);
        }
    }

    /**
     * Check if the path is under Spanish content path (exact match or descendant)
     * This ensures /content/larasoft/us/es and /content/larasoft/us/es/* match,
     * but NOT /content/larasoft/us/es-mx (sibling paths)
     */
    private boolean isUnderSpanishContentPath(String path) {
        if (path == null) {
            return false;
        }
        // Exact match
        if (path.equals(spanishContentPath)) {
            return true;
        }
        // Descendant match (must have trailing slash)
        return path.startsWith(spanishContentPath + "/");
    }

    /**
     * Check if user is member of editor-spanish group using UserManager API
     */
    private boolean isUserInEditorSpanishGroup(ResourceResolver resolver, String userId) {
        try {
            UserManager userManager = resolver.adaptTo(UserManager.class);
            if (userManager == null) {
                LOG.error("Unable to get UserManager for group membership check");
                return false;
            }

            Authorizable user = userManager.getAuthorizable(userId);
            if (user == null) {
                LOG.warn("User not found: {}", userId);
                return false;
            }

            // Get the editor-spanish group
            Authorizable editorGroup = userManager.getAuthorizable(EDITOR_SPANISH_GROUP);
            if (editorGroup == null || !editorGroup.isGroup()) {
                LOG.error("Editor-spanish group not found or is not a group");
                return false;
            }

            // Check if user is member of the group
            Group group = (Group) editorGroup;
            boolean isMember = group.isMember(user);
            
            if (isMember) {
                LOG.debug("User '{}' is member of {} group", userId, EDITOR_SPANISH_GROUP);
            } else {
                LOG.debug("User '{}' is NOT member of {} group", userId, EDITOR_SPANISH_GROUP);
            }
            
            return isMember;
            
        } catch (RepositoryException e) {
            LOG.error("Error checking user group membership for user: {}", userId, e);
            return false;
        }
    }
}

