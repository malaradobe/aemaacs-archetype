package com.larasoft.core.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;
import javax.jcr.PathNotFoundException;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.AssetManager;
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.WorkflowData;
import com.day.cq.workflow.metadata.MetaDataMap;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import uk.org.lidalia.slf4jext.Level;
import uk.org.lidalia.slf4jtest.LoggingEvent;
import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT)
class MoveAssetWorkflowProcessTest {

    private final AemContext context = new AemContext();
    private MoveAssetWorkflowProcess fixture;
    private TestLogger logger;

    @Mock
    private ResourceResolverFactory resolverFactory;

    @Mock
    private WorkItem workItem;

    @Mock
    private WorkflowSession workflowSession;

    @Mock
    private MetaDataMap metaDataMap;

    @Mock
    private WorkflowData workflowData;

    @Mock
    private ResourceResolver resourceResolver;

    @Mock
    private Resource assetResource;

    @Mock
    private Resource targetFolderResource;

    @Mock
    private Asset asset;

    @Mock
    private AssetManager assetManager;

    @Mock
    private Session jcrSession;

    private static final String ASSET_PATH = "/content/dam/test/asset.jpg";
    private static final String TARGET_FOLDER = "/content/dam/test/target";

    @BeforeEach
    void setup() throws LoginException {
        TestLoggerFactory.clear();
        fixture = new MoveAssetWorkflowProcess();
        fixture.setResolverFactory(resolverFactory);
        fixture.setAssetManager(assetManager);

        logger = TestLoggerFactory.getTestLogger(fixture.getClass());

        // Setup common mocks
        lenient().when(workItem.getWorkflowData()).thenReturn(workflowData);
        lenient().when(workflowData.getPayload()).thenReturn(ASSET_PATH);
        lenient().when(workItem.getMetaDataMap()).thenReturn(metaDataMap);
        lenient().when(metaDataMap.get("targetFolder", String.class)).thenReturn(TARGET_FOLDER);

        // Setup resolver factory mock
        Map<String, Object> authInfo = new HashMap<>();
        authInfo.put(ResourceResolverFactory.SUBSERVICE, "dam-service");
        lenient().when(resolverFactory.getServiceResourceResolver(authInfo)).thenReturn(resourceResolver);

        // Register service
        context.registerService(ResourceResolverFactory.class, resolverFactory);
    }

    @Test
    void testExecute_SuccessfulMove() throws WorkflowException, RepositoryException, 
            ItemExistsException, PathNotFoundException, VersionException, 
            ConstraintViolationException, LockException, AccessDeniedException, 
            ReferentialIntegrityException, InvalidItemStateException {
        // Setup
        when(resourceResolver.getResource(ASSET_PATH)).thenReturn(assetResource);
        when(assetResource.adaptTo(Asset.class)).thenReturn(asset);
        when(resourceResolver.getResource(TARGET_FOLDER)).thenReturn(targetFolderResource);
        when(resourceResolver.adaptTo(Session.class)).thenReturn(jcrSession);

        // Execute
        fixture.execute(workItem, workflowSession, metaDataMap);

        // Verify
        verify(jcrSession).move(ASSET_PATH, TARGET_FOLDER + "/asset.jpg");
        verify(jcrSession).save();

        List<LoggingEvent> events = logger.getLoggingEvents();
        assertEquals(1, events.size());
        LoggingEvent event = events.get(0);
        assertEquals(Level.INFO, event.getLevel());
        assertTrue(event.getMessage().contains("Successfully moved asset"));
    }

    @Test
    void testExecute_LoginException() throws LoginException {
        // Setup
        Map<String, Object> authInfo = new HashMap<>();
        authInfo.put(ResourceResolverFactory.SUBSERVICE, "dam-service");
        when(resolverFactory.getServiceResourceResolver(authInfo)).thenThrow(new LoginException("Test login exception"));

        // Execute and verify exception
        WorkflowException exception = assertThrows(WorkflowException.class, () -> {
            fixture.execute(workItem, workflowSession, metaDataMap);
        });
        assertTrue(exception.getMessage().contains("Error moving asset: Test login exception"));

        List<LoggingEvent> events = logger.getLoggingEvents();
        assertEquals(1, events.size());
        LoggingEvent event = events.get(0);
        assertEquals(Level.ERROR, event.getLevel());
        assertTrue(event.getMessage().contains("Error moving asset"));
    }

    @Test
    void testExecute_AssetNotFound() throws WorkflowException {
        // Setup
        when(resourceResolver.getResource(ASSET_PATH)).thenReturn(null);

        // Execute and verify exception
        WorkflowException exception = assertThrows(WorkflowException.class, () -> {
            fixture.execute(workItem, workflowSession, metaDataMap);
        });
        assertEquals("Error moving asset: Asset not found at path: " + ASSET_PATH, exception.getMessage());

        List<LoggingEvent> events = logger.getLoggingEvents();
        assertEquals(1, events.size());
        LoggingEvent event = events.get(0);
        assertEquals(Level.ERROR, event.getLevel());
        assertTrue(event.getMessage().contains("Error moving asset"));
    }

    @Test
    void testExecute_TargetFolderNotFound() throws WorkflowException {
        // Setup
        when(resourceResolver.getResource(ASSET_PATH)).thenReturn(assetResource);
        when(assetResource.adaptTo(Asset.class)).thenReturn(asset);
        when(resourceResolver.getResource(TARGET_FOLDER)).thenReturn(null);

        // Execute and verify exception
        WorkflowException exception = assertThrows(WorkflowException.class, () -> {
            fixture.execute(workItem, workflowSession, metaDataMap);
        });
        assertEquals("Error moving asset: Target folder not found: " + TARGET_FOLDER, exception.getMessage());

        List<LoggingEvent> events = logger.getLoggingEvents();
        assertEquals(1, events.size());
        LoggingEvent event = events.get(0);
        assertEquals(Level.ERROR, event.getLevel());
        assertTrue(event.getMessage().contains("Error moving asset:"));
    }

    @Test
    void testExecute_MissingTargetFolder() throws WorkflowException {
        // Setup
        when(metaDataMap.get("targetFolder", String.class)).thenReturn(null);

        // Execute and verify exception
        WorkflowException exception = assertThrows(WorkflowException.class, () -> {
            fixture.execute(workItem, workflowSession, metaDataMap);
        });
        assertEquals("Target folder path is required", exception.getMessage());
    }

    @Test
    void testExecute_SessionNull() throws WorkflowException {
        // Setup
        when(resourceResolver.getResource(ASSET_PATH)).thenReturn(assetResource);
        when(assetResource.adaptTo(Asset.class)).thenReturn(asset);
        when(resourceResolver.getResource(TARGET_FOLDER)).thenReturn(targetFolderResource);
        when(resourceResolver.adaptTo(Session.class)).thenReturn(null);

        // Execute and verify exception
        WorkflowException exception = assertThrows(WorkflowException.class, () -> {
            fixture.execute(workItem, workflowSession, metaDataMap);
        });
        assertEquals("Error moving asset: Could not get JCR Session", exception.getMessage());
    }

    @Test
    void testExecute_RepositoryException() throws WorkflowException, RepositoryException {
        // Setup
        when(resourceResolver.getResource(ASSET_PATH)).thenReturn(assetResource);
        when(assetResource.adaptTo(Asset.class)).thenReturn(asset);
        when(resourceResolver.getResource(TARGET_FOLDER)).thenReturn(targetFolderResource);
        when(resourceResolver.adaptTo(Session.class)).thenReturn(jcrSession);
        doThrow(new RepositoryException("Test exception")).when(jcrSession).move(anyString(), anyString());

        // Execute and verify exception
        WorkflowException exception = assertThrows(WorkflowException.class, () -> {
            fixture.execute(workItem, workflowSession, metaDataMap);
        });
        assertTrue(exception.getMessage().contains("Error moving asset: Test exception"));
    }

    @Test
    void testExecute_NullAssetAdaptation() throws LoginException {
        // Setup
        when(resourceResolver.getResource(ASSET_PATH)).thenReturn(assetResource);
        when(assetResource.adaptTo(Asset.class)).thenReturn(null);

        // Execute and verify exception
        WorkflowException exception = assertThrows(WorkflowException.class, () -> {
            fixture.execute(workItem, workflowSession, metaDataMap);
        });
        assertEquals("Error moving asset: Resource is not an asset: " + ASSET_PATH, exception.getMessage());
    }

    @Test
    void testExecute_NullAssetManager() throws LoginException {
        // Setup
        when(resourceResolver.getResource(ASSET_PATH)).thenReturn(assetResource);
        when(assetResource.adaptTo(Asset.class)).thenReturn(asset);
        when(resourceResolver.getResource(TARGET_FOLDER)).thenReturn(targetFolderResource);
        when(resourceResolver.adaptTo(AssetManager.class)).thenReturn(null);

        // Execute and verify exception
        WorkflowException exception = assertThrows(WorkflowException.class, () -> {
            fixture.execute(workItem, workflowSession, metaDataMap);
        });
        assertEquals("Error moving asset: Could not get JCR Session", exception.getMessage());
    }

    @Test
    void testExecute_SaveFailure() throws Exception {
        // Setup
        when(resourceResolver.getResource(ASSET_PATH)).thenReturn(assetResource);
        when(assetResource.adaptTo(Asset.class)).thenReturn(asset);
        when(resourceResolver.getResource(TARGET_FOLDER)).thenReturn(targetFolderResource);
        when(resourceResolver.adaptTo(Session.class)).thenReturn(jcrSession);
        doNothing().when(jcrSession).move(anyString(), anyString());
        doThrow(new RepositoryException("Failed to save session")).when(jcrSession).save();

        // Execute and verify exception
        WorkflowException exception = assertThrows(WorkflowException.class, () -> {
            fixture.execute(workItem, workflowSession, metaDataMap);
        });
        assertTrue(exception.getMessage().contains("Error moving asset: Failed to save session"));

        List<LoggingEvent> events = logger.getLoggingEvents();
        assertEquals(1, events.size());
        LoggingEvent event = events.get(0);
        assertEquals(Level.ERROR, event.getLevel());
        assertTrue(event.getMessage().contains("Error moving asset"));
    }
} 