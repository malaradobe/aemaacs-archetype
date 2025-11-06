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
 * JUnit tests for RequestChangesProcess
 */
@ExtendWith({AemContextExtension.class, MockitoExtension.class})
class RequestChangesProcessTest {

    private final AemContext context = new AemContext();

    @Mock
    private WorkItem workItem;

    @Mock
    private WorkflowSession workflowSession;

    @Mock
    private Workflow workflow;

    @Mock
    private WorkflowData workflowData;

    private RequestChangesProcess process;
    private MetaDataMap processMetaDataMap;
    private MetaDataMap workflowMetaDataMap;

    @BeforeEach
    void setUp() {
        process = new RequestChangesProcess();
        context.registerInjectActivateService(process);
        processMetaDataMap = new SimpleMetaDataMap();
        workflowMetaDataMap = new SimpleMetaDataMap();
    }

    @Test
    void testExecute_WithFeedback_Success() throws Exception {
        // Create test page
        String pagePath = "/content/larasoft/us/es/test-page";
        Resource page = context.create().resource(pagePath,
                "jcr:primaryType", "cq:Page");
        context.create().resource(pagePath + "/jcr:content",
                "jcr:primaryType", "cq:PageContent",
                "jcr:title", "Test Page");

        // Mock workflow components with feedback
        when(workItem.getWorkflowData()).thenReturn(workflowData);
        when(workflowData.getPayload()).thenReturn(pagePath);
        when(workItem.getWorkflow()).thenReturn(workflow);
        when(workflow.getWorkflowData()).thenReturn(workflowData);
        when(workflowData.getMetaDataMap()).thenReturn(workflowMetaDataMap);
        workflowMetaDataMap.put("comment", "Please update the images and add more detail to section 3");
        when(workflowSession.adaptTo(ResourceResolver.class)).thenReturn(context.resourceResolver());

        // Execute process
        process.execute(workItem, workflowSession, processMetaDataMap);

        // Verify changes requested state was set
        Resource pageResource = context.resourceResolver().getResource(pagePath);
        assertNotNull(pageResource);
        Resource contentResource = pageResource.getChild("jcr:content");
        assertNotNull(contentResource);
        assertEquals("CHANGES_REQUESTED", contentResource.getValueMap().get("workflowState", String.class));
        assertEquals("Please update the images and add more detail to section 3", 
                    contentResource.getValueMap().get("workflowFeedback", String.class));
    }

    @Test
    void testExecute_WithoutFeedback_Success() throws Exception {
        // Create test page
        String pagePath = "/content/larasoft/us/es/test-page";
        Resource page = context.create().resource(pagePath,
                "jcr:primaryType", "cq:Page");
        context.create().resource(pagePath + "/jcr:content",
                "jcr:primaryType", "cq:PageContent",
                "jcr:title", "Test Page");

        // Mock workflow components without feedback
        when(workItem.getWorkflowData()).thenReturn(workflowData);
        when(workflowData.getPayload()).thenReturn(pagePath);
        when(workItem.getWorkflow()).thenReturn(workflow);
        when(workflow.getWorkflowData()).thenReturn(workflowData);
        when(workflowData.getMetaDataMap()).thenReturn(workflowMetaDataMap);
        when(workflowSession.adaptTo(ResourceResolver.class)).thenReturn(context.resourceResolver());

        // Execute process
        process.execute(workItem, workflowSession, processMetaDataMap);

        // Verify changes requested state was set but no feedback
        Resource pageResource = context.resourceResolver().getResource(pagePath);
        assertNotNull(pageResource);
        Resource contentResource = pageResource.getChild("jcr:content");
        assertNotNull(contentResource);
        assertEquals("CHANGES_REQUESTED", contentResource.getValueMap().get("workflowState", String.class));
        assertNull(contentResource.getValueMap().get("workflowFeedback", String.class));
    }

    @Test
    void testExecute_InvalidResource_ThrowsException() {
        // Mock workflow with invalid path
        when(workItem.getWorkflowData()).thenReturn(workflowData);
        when(workflowData.getPayload()).thenReturn("/content/larasoft/us/es/non-existent");
        when(workflowSession.adaptTo(ResourceResolver.class)).thenReturn(context.resourceResolver());

        // Execute process - should throw exception
        assertThrows(WorkflowException.class, () -> {
            process.execute(workItem, workflowSession, processMetaDataMap);
        });
    }

    @Test
    void testExecute_PageWithoutContent_ThrowsException() {
        // Create test page without jcr:content
        String pagePath = "/content/larasoft/us/es/test-page";
        context.create().resource(pagePath, "jcr:primaryType", "cq:Page");

        // Mock workflow components
        when(workItem.getWorkflowData()).thenReturn(workflowData);
        when(workflowData.getPayload()).thenReturn(pagePath);
        when(workflowSession.adaptTo(ResourceResolver.class)).thenReturn(context.resourceResolver());

        // Execute process - should throw exception
        assertThrows(WorkflowException.class, () -> {
            process.execute(workItem, workflowSession, processMetaDataMap);
        });
    }
}


