package com.larasoft.core.workflows;

import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.Workflow;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowData;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import com.adobe.granite.workflow.metadata.SimpleMetaDataMap;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * JUnit tests for ApprovalMarkingProcess
 */
@ExtendWith({AemContextExtension.class, MockitoExtension.class})
class ApprovalMarkingProcessTest {

    private final AemContext context = new AemContext();

    @Mock
    private WorkItem workItem;

    @Mock
    private WorkflowSession workflowSession;

    @Mock
    private Workflow workflow;

    @Mock
    private WorkflowData workflowData;

    private ApprovalMarkingProcess process;
    private MetaDataMap processMetaDataMap;
    private MetaDataMap workflowMetaDataMap;

    @BeforeEach
    void setUp() {
        process = new ApprovalMarkingProcess();
        context.registerInjectActivateService(process);
        processMetaDataMap = new SimpleMetaDataMap();
        workflowMetaDataMap = new SimpleMetaDataMap();
    }

    @Test
    void testExecute_MarksContentAsApproved() throws Exception {
        // Create test page
        String pagePath = "/content/larasoft/es/test-page";
        Resource page = context.create().resource(pagePath,
                "jcr:primaryType", "cq:Page");
        context.create().resource(pagePath + "/jcr:content",
                "jcr:primaryType", "cq:PageContent",
                "jcr:title", "Test Page",
                "workflowState", "IN_PROGRESS");

        // Mock workflow components
        when(workItem.getWorkflowData()).thenReturn(workflowData);
        when(workflowData.getPayload()).thenReturn(pagePath);
        when(workItem.getWorkflow()).thenReturn(workflow);
        when(workflow.getWorkflowData()).thenReturn(workflowData);
        when(workflow.getInitiator()).thenReturn("reviewer-user");
        when(workflowData.getMetaDataMap()).thenReturn(workflowMetaDataMap);
        when(workflowSession.adaptTo(ResourceResolver.class)).thenReturn(context.resourceResolver());

        // Execute process
        process.execute(workItem, workflowSession, processMetaDataMap);

        // Verify content is marked as APPROVED
        Resource pageResource = context.resourceResolver().getResource(pagePath);
        assertNotNull(pageResource);
        Resource contentResource = pageResource.getChild("jcr:content");
        assertNotNull(contentResource);
        assertEquals("APPROVED", contentResource.getValueMap().get("workflowState", String.class));
        assertEquals("reviewer-user", contentResource.getValueMap().get("workflowApprovedBy", String.class));
        assertNotNull(contentResource.getValueMap().get("workflowApprovedDate"));
    }

    @Test
    void testExecute_WithApprovalComment() throws Exception {
        // Create test page
        String pagePath = "/content/larasoft/es/test-page";
        Resource page = context.create().resource(pagePath,
                "jcr:primaryType", "cq:Page");
        context.create().resource(pagePath + "/jcr:content",
                "jcr:primaryType", "cq:PageContent",
                "jcr:title", "Test Page");

        // Mock workflow with approval comment
        when(workItem.getWorkflowData()).thenReturn(workflowData);
        when(workflowData.getPayload()).thenReturn(pagePath);
        when(workItem.getWorkflow()).thenReturn(workflow);
        when(workflow.getWorkflowData()).thenReturn(workflowData);
        when(workflow.getInitiator()).thenReturn("reviewer-user");
        when(workflowData.getMetaDataMap()).thenReturn(workflowMetaDataMap);
        workflowMetaDataMap.put("comment", "Content looks great, approved for publication");
        when(workflowSession.adaptTo(ResourceResolver.class)).thenReturn(context.resourceResolver());

        // Execute process
        process.execute(workItem, workflowSession, processMetaDataMap);

        // Verify approval comment was stored
        Resource contentResource = context.resourceResolver().getResource(pagePath + "/jcr:content");
        assertNotNull(contentResource);
        assertEquals("APPROVED", contentResource.getValueMap().get("workflowState", String.class));
        assertEquals("Content looks great, approved for publication",
                contentResource.getValueMap().get("workflowApprovalComment", String.class));
    }

    @Test
    void testExecute_InvalidResource_ThrowsException() {
        // Mock workflow with invalid path
        when(workItem.getWorkflowData()).thenReturn(workflowData);
        when(workflowData.getPayload()).thenReturn("/content/larasoft/es/non-existent");
        when(workflowSession.adaptTo(ResourceResolver.class)).thenReturn(context.resourceResolver());

        // Execute process - should throw exception
        assertThrows(WorkflowException.class, () -> {
            process.execute(workItem, workflowSession, processMetaDataMap);
        });
    }

    @Test
    void testExecute_StateTransitionFromInProgress() throws Exception {
        // Create test page with IN_PROGRESS state
        String pagePath = "/content/larasoft/es/test-page";
        context.create().resource(pagePath, "jcr:primaryType", "cq:Page");
        context.create().resource(pagePath + "/jcr:content",
                "jcr:primaryType", "cq:PageContent",
                "workflowState", "IN_PROGRESS",
                "workflowId", "workflow-123");

        // Mock workflow components
        when(workItem.getWorkflowData()).thenReturn(workflowData);
        when(workflowData.getPayload()).thenReturn(pagePath);
        when(workItem.getWorkflow()).thenReturn(workflow);
        when(workflow.getWorkflowData()).thenReturn(workflowData);
        when(workflow.getInitiator()).thenReturn("reviewer-user");
        when(workflowData.getMetaDataMap()).thenReturn(workflowMetaDataMap);
        when(workflowSession.adaptTo(ResourceResolver.class)).thenReturn(context.resourceResolver());

        // Execute process
        process.execute(workItem, workflowSession, processMetaDataMap);

        // Verify state changed from IN_PROGRESS to APPROVED
        Resource contentResource = context.resourceResolver().getResource(pagePath + "/jcr:content");
        assertNotNull(contentResource);
        assertEquals("APPROVED", contentResource.getValueMap().get("workflowState", String.class));
    }
}


