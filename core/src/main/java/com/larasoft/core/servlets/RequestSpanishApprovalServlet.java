package com.larasoft.core.servlets;

import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkflowData;
import com.adobe.granite.workflow.model.WorkflowModel;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.larasoft.core.workflows.SpanishWorkflowConfig;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Servlet to manually request approval for Spanish content pages.
 * Only accessible to users in the editor-spanish group and only for pages under configured path.
 */
@Component(
    service = Servlet.class,
    property = {
        "sling.servlet.methods=POST",
        "sling.servlet.paths=/bin/larasoft/workflow/request-spanish-approval",
        "sling.servlet.extensions=json"
    }
)
@Designate(ocd = SpanishWorkflowConfig.class)
public class RequestSpanishApprovalServlet extends SlingAllMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(RequestSpanishApprovalServlet.class);
    private static final String EDITOR_SPANISH_GROUP = "editor-spanish";
    private static final String PARAM_PAGE_PATH = "pagePath";

    private String spanishContentPath;
    private String workflowModelPath;

    @Activate
    @Modified
    protected void activate(SpanishWorkflowConfig config) {
        this.spanishContentPath = config.spanish_content_path();
        this.workflowModelPath = config.workflow_model_path();
        LOG.info("Spanish Workflow Config - Content Path: {}, Workflow Model: {}", 
                 spanishContentPath, workflowModelPath);
    }

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String pagePath = request.getParameter(PARAM_PAGE_PATH);
        
        if (pagePath == null || pagePath.isEmpty()) {
            sendError(response, 400, "Missing required parameter: pagePath");
            return;
        }

        ResourceResolver resourceResolver = request.getResourceResolver();
        
        try {
            // 1. Check if user is in editor-spanish group
            if (!isUserInEditorSpanishGroup(resourceResolver)) {
                sendError(response, 403, "User is not authorized to request approval. Must be in editor-spanish group.");
                return;
            }

            // 2. Check if page is under configured Spanish content path
            if (!pagePath.startsWith(spanishContentPath)) {
                sendError(response, 403, 
                    String.format("Page is not under Spanish content path. Expected path to start with: %s", spanishContentPath));
                return;
            }

            // 3. Verify page exists
            PageManager pageManager = resourceResolver.adaptTo(PageManager.class);
            if (pageManager == null) {
                sendError(response, 500, "Unable to get PageManager");
                return;
            }

            Page page = pageManager.getPage(pagePath);
            if (page == null) {
                sendError(response, 404, "Page not found: " + pagePath);
                return;
            }

            // 4. Start the workflow
            WorkflowSession workflowSession = resourceResolver.adaptTo(WorkflowSession.class);
            if (workflowSession == null) {
                sendError(response, 500, "Unable to get WorkflowSession");
                return;
            }

            WorkflowModel workflowModel = workflowSession.getModel(workflowModelPath);
            if (workflowModel == null) {
                sendError(response, 500, "Workflow model not found: " + workflowModelPath);
                return;
            }

            // Create workflow data
            WorkflowData workflowData = workflowSession.newWorkflowData("JCR_PATH", pagePath);
            
            // Set workflow metadata
            Map<String, Object> metaData = new HashMap<>();
            metaData.put("initiatedBy", resourceResolver.getUserID());
            metaData.put("pagePath", pagePath);
            metaData.put("pageTitle", page.getTitle() != null ? page.getTitle() : page.getName());

            // Start the workflow
            workflowSession.startWorkflow(workflowModel, workflowData, metaData);

            LOG.info("Spanish approval workflow initiated for page: {} by user: {}", 
                     pagePath, resourceResolver.getUserID());

            // Send success response
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "Approval workflow initiated successfully");
            responseData.put("pagePath", pagePath);
            responseData.put("pageTitle", page.getTitle() != null ? page.getTitle() : page.getName());

            response.setStatus(200);
            response.getWriter().write(toJson(responseData));

        } catch (WorkflowException e) {
            LOG.error("Error starting workflow for page: {}", pagePath, e);
            sendError(response, 500, "Error starting workflow: " + e.getMessage());
        } catch (RepositoryException e) {
            LOG.error("Repository error while checking permissions", e);
            sendError(response, 500, "Error checking user permissions: " + e.getMessage());
        } catch (Exception e) {
            LOG.error("Unexpected error in RequestSpanishApprovalServlet", e);
            sendError(response, 500, "Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Check if the current user is member of editor-spanish group
     */
    private boolean isUserInEditorSpanishGroup(ResourceResolver resourceResolver) 
            throws RepositoryException {
        
        Session session = resourceResolver.adaptTo(Session.class);
        if (session == null) {
            return false;
        }

        UserManager userManager = resourceResolver.adaptTo(UserManager.class);
        if (userManager == null) {
            return false;
        }

        String userId = session.getUserID();
        Authorizable authorizable = userManager.getAuthorizable(userId);
        
        if (authorizable == null) {
            return false;
        }

        // Check if user is directly in editor-spanish group
        Authorizable editorGroup = userManager.getAuthorizable(EDITOR_SPANISH_GROUP);
        if (editorGroup != null && editorGroup.isGroup()) {
            Group group = (Group) editorGroup;
            return group.isMember(authorizable);
        }

        return false;
    }

    /**
     * Send error response
     */
    private void sendError(SlingHttpServletResponse response, int statusCode, String message) 
            throws IOException {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("error", message);
        
        response.setStatus(statusCode);
        response.getWriter().write(toJson(error));
    }

    /**
     * Simple JSON serialization (for basic Map)
     */
    private String toJson(Map<String, Object> map) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) {
                json.append(",");
            }
            first = false;
            
            json.append("\"").append(entry.getKey()).append("\":");
            
            Object value = entry.getValue();
            if (value instanceof String) {
                json.append("\"").append(escape((String) value)).append("\"");
            } else if (value instanceof Boolean) {
                json.append(value);
            } else {
                json.append("\"").append(value).append("\"");
            }
        }
        
        json.append("}");
        return json.toString();
    }

    /**
     * Escape JSON string
     */
    private String escape(String str) {
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}

