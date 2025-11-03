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

import javax.jcr.Node;
import javax.jcr.Session;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * JUnit tests for WorkflowEnforcementProcess
 */
@ExtendWith({AemContextExtension.class, MockitoExtension.class})
class WorkflowEnforcementProcessTest {

    private final AemContext context = new AemContext();

    @Mock
    private WorkItem workItem;

    @Mock
    private WorkflowSession workflowSession;

    @Mock
    private Workflow workflow;

    @Mock
    private WorkflowData workflowData;

    @Mock
    private WorkflowEnforcementService enforcementService;

    private WorkflowEnforcementProcess process;
    private MetaDataMap metaDataMap;

    @BeforeEach
    void setUp() {
        process = new WorkflowEnforcementProcess();
        context.registerService(WorkflowEnforcementService.class, enforcementService);
        context.registerInjectActivateService(process);
        metaDataMap = new SimpleMetaDataMap();
    }

    @Test
    void testExecute_SpanishContent_Success() throws Exception {
        // Create test page under Spanish content path
        String pagePath = "/content/larasoft/es/test-page";
        Resource page = context.create().resource(pagePath,
                "jcr:primaryType", "cq:Page");
        context.create().resource(pagePath + "/jcr:content",
                "jcr:primaryType", "cq:PageContent",
                "jcr:title", "Test Page");

        // Mock workflow components
        when(workItem.getWorkflowData()).thenReturn(workflowData);
        when(workflowData.getPayload()).thenReturn(pagePath);
        when(workItem.getWorkflow()).thenReturn(workflow);
        when(workflow.getId()).thenReturn("test-workflow-123");
        when(workflowSession.adaptTo(ResourceResolver.class)).thenReturn(context.resourceResolver());

        // Execute process
        process.execute(workItem, workflowSession, metaDataMap);

        // Verify workflow state was set
        Resource pageResource = context.resourceResolver().getResource(pagePath);
        assertNotNull(pageResource);
        Resource contentResource = pageResource.getChild("jcr:content");
        assertNotNull(contentResource);
        assertEquals("IN_PROGRESS", contentResource.getValueMap().get("workflowState", String.class));
        assertEquals("test-workflow-123", contentResource.getValueMap().get("workflowId", String.class));
    }

    @Test
    void testExecute_NonSpanishContent_Skipped() throws Exception {
        // Create test page under non-Spanish content path
        String pagePath = "/content/larasoft/en/test-page";
        Resource page = context.create().resource(pagePath,
                "jcr:primaryType", "cq:Page");
        context.create().resource(pagePath + "/jcr:content",
                "jcr:primaryType", "cq:PageContent",
                "jcr:title", "Test Page");

        // Mock workflow components
        when(workItem.getWorkflowData()).thenReturn(workflowData);
        when(workflowData.getPayload()).thenReturn(pagePath);
        when(workflowSession.adaptTo(ResourceResolver.class)).thenReturn(context.resourceResolver());

        // Execute process - should skip
        process.execute(workItem, workflowSession, metaDataMap);

        // Verify workflow state was NOT set
        Resource pageResource = context.resourceResolver().getResource(pagePath);
        assertNotNull(pageResource);
        Resource contentResource = pageResource.getChild("jcr:content");
        assertNotNull(contentResource);
        assertNull(contentResource.getValueMap().get("workflowState", String.class));
    }

    @Test
    void testExecute_InvalidResource_ThrowsException() {
        // Mock workflow with invalid path
        when(workItem.getWorkflowData()).thenReturn(workflowData);
        when(workflowData.getPayload()).thenReturn("/content/larasoft/es/non-existent");
        when(workflowSession.adaptTo(ResourceResolver.class)).thenReturn(context.resourceResolver());

        // Execute process - should throw exception
        assertThrows(WorkflowException.class, () -> {
            process.execute(workItem, workflowSession, metaDataMap);
        });
    }

    @Test
    void testExecute_NullResourceResolver_ThrowsException() {
        // Mock workflow with null resource resolver
        when(workItem.getWorkflowData()).thenReturn(workflowData);
        when(workflowData.getPayload()).thenReturn("/content/larasoft/es/test");
        when(workflowSession.adaptTo(ResourceResolver.class)).thenReturn(null);

        // Execute process - should throw exception
        assertThrows(WorkflowException.class, () -> {
            process.execute(workItem, workflowSession, metaDataMap);
        });
    }
}


