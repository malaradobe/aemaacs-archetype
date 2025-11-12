# Manual Workflow Approval Setup

## Overview

The Spanish Content Approval Workflow has been configured to use **manual initiation** instead of automatic triggering. This means editors must explicitly request approval by clicking a button in the Page Editor.

## Key Changes

### 1. Automatic Launcher Disabled
- The workflow launcher at `/conf/global/settings/workflow/launcher/config/spanish-content-approval-launcher/.content.xml` has been **disabled**
- Workflow is no longer triggered automatically when content is modified

### 2. Manual "Request Approval" Action
- A new button appears in the Page Editor toolbar: **"Request Approval"**
- Only visible to users in the `editor-spanish` group
- Only shown for pages under the configured Spanish content path

### 3. Configurable Spanish Content Path
- Path is configurable via OSGi configuration
- Default: `/content/larasoft/us/es`
- Can be changed without code modifications

## How It Works

### For Editors (editor-spanish group)

1. **Edit Content**
   - Navigate to a Spanish page under `/content/larasoft/us/es`
   - Edit the page content
   - Save your changes

2. **Request Approval**
   - Click the **"Request Approval"** button in the Page Editor toolbar
   - A success notification will appear
   - The workflow is now initiated

3. **Wait for Approval**
   - Check your workflow inbox (`/aem/inbox`)
   - Once approved, you can publish the page

### For Reviewers (reviewer-spanish group)

1. **Review Workflow Task**
   - Check workflow inbox: `/aem/inbox`
   - Find the Spanish content approval task
   - Click to open the task

2. **Make Decision**
   - **Approve**: Content can be published by editor
   - **Reject**: Content is rejected with comments
   - **Request Changes**: Send back to editor with feedback

## Configuration

### Change Spanish Content Path

To change the path from `/content/larasoft/us/es` to a different path:

1. Navigate to OSGi Configuration Console:
   ```
   http://localhost:4502/system/console/configMgr
   ```

2. Search for: **"Spanish Content Workflow Configuration"**

3. Update the **Spanish Content Path** field:
   ```
   /content/larasoft/us/es  →  /content/your-site/your-path
   ```

4. Click **Save**

5. The "Request Approval" button will now only appear for pages under the new path

### Alternative: Edit Configuration File

Edit the file directly:
```
ui.config/src/main/content/jcr_root/apps/larasoft/osgiconfig/config/com.larasoft.core.workflows.SpanishWorkflowConfig.cfg.json
```

Change:
```json
{
  "spanish.content.path": "/content/larasoft/us/es",
  "workflow.model.path": "/var/workflow/models/spanish-content-approval"
}
```

Then rebuild and deploy:
```bash
mvn clean install -PautoInstallPackage
```

## Technical Components

### 1. Servlet: RequestSpanishApprovalServlet

**Location**: `core/src/main/java/com/larasoft/core/servlets/RequestSpanishApprovalServlet.java`

**Endpoint**: `/bin/larasoft/workflow/request-spanish-approval`

**Features**:
- Validates user is in `editor-spanish` group
- Checks page is under configured Spanish content path
- Starts the workflow with proper metadata
- Returns JSON response with success/error status

**Example Request**:
```bash
curl -X POST \
  -u editor-spanish-user:password \
  -d "pagePath=/content/larasoft/us/es/about" \
  http://localhost:4502/bin/larasoft/workflow/request-spanish-approval
```

**Example Response**:
```json
{
  "success": true,
  "message": "Approval workflow initiated successfully",
  "pagePath": "/content/larasoft/us/es/about",
  "pageTitle": "About Us"
}
```

### 2. Page Editor Action

**Location**: `ui.apps/src/main/content/jcr_root/apps/larasoft/clientlibs/clientlib-spanish-workflow/`

**Files**:
- `.content.xml` - ClientLib definition
- `js/request-spanish-approval.js` - JavaScript logic

**Features**:
- Loads in Page Editor (category: `cq.authoring.editor`)
- Checks if page is under Spanish content path
- Verifies user is in `editor-spanish` group
- Adds "Request Approval" button to toolbar
- Handles button click and calls servlet
- Shows success/error notifications

### 3. OSGi Configuration

**File**: `ui.config/.../com.larasoft.core.workflows.SpanishWorkflowConfig.cfg.json`

**Properties**:
- `spanish.content.path` - Root path for Spanish content
- `workflow.model.path` - Path to workflow model

**Configuration Interface**: `SpanishWorkflowConfig.java`

## Testing

### Test 1: Button Visibility

**As editor-spanish user**:
1. Open a page under `/content/larasoft/us/es` in Page Editor
2. Verify "Request Approval" button is visible in toolbar
3. Open a page under a different path (e.g., `/content/larasoft/us/en`)
4. Verify "Request Approval" button is NOT visible

**As non-editor user**:
1. Open a Spanish page
2. Verify "Request Approval" button is NOT visible

### Test 2: Manual Workflow Initiation

1. As `editor-spanish` user, open a Spanish page
2. Click "Request Approval" button
3. Verify success notification appears
4. Check workflow inbox at `/aem/inbox`
5. Verify workflow task was created

### Test 3: Workflow Approval

1. As `editor-spanish`, request approval for a page
2. As `reviewer-spanish`, approve the workflow task
3. As `editor-spanish`, verify you can now publish the page

### Test 4: Permission Validation

**Test unauthorized access**:
```bash
# As non-editor user, try to call servlet directly
curl -X POST \
  -u non-editor-user:password \
  -d "pagePath=/content/larasoft/us/es/test" \
  http://localhost:4502/bin/larasoft/workflow/request-spanish-approval
```

**Expected Response**:
```json
{
  "success": false,
  "error": "User is not authorized to request approval. Must be in editor-spanish group."
}
```

### Test 5: Path Validation

```bash
# Try to request approval for non-Spanish page
curl -X POST \
  -u editor-spanish-user:password \
  -d "pagePath=/content/larasoft/us/en/test" \
  http://localhost:4502/bin/larasoft/workflow/request-spanish-approval
```

**Expected Response**:
```json
{
  "success": false,
  "error": "Page is not under Spanish content path. Expected path to start with: /content/larasoft/us/es"
}
```

## Troubleshooting

### Button Not Appearing

**Problem**: "Request Approval" button doesn't appear in Page Editor

**Solutions**:
1. Verify user is member of `editor-spanish` group
2. Verify page is under configured Spanish content path
3. Check browser console for JavaScript errors
4. Clear browser cache
5. Verify ClientLib is loaded:
   - Open browser dev tools
   - Check if `clientlib-spanish-workflow` is loaded in Network tab
   - Check console for script errors

### Workflow Not Starting

**Problem**: Clicking button doesn't start workflow

**Solutions**:
1. Check servlet is registered:
   ```
   http://localhost:4502/system/console/components
   Search: RequestSpanishApprovalServlet
   ```
2. Verify OSGi configuration:
   ```
   http://localhost:4502/system/console/configMgr
   Search: Spanish Content Workflow Configuration
   ```
3. Check error logs:
   ```
   http://localhost:4502/system/console/slinglog
   ```
4. Verify workflow model exists:
   ```
   http://localhost:4502/var/workflow/models/spanish-content-approval
   ```

### Permission Denied

**Problem**: "User is not authorized" error

**Solutions**:
1. Verify user is member of `editor-spanish` group:
   ```
   http://localhost:4502/useradmin
   ```
2. Check group membership in CRXDE
3. Log out and log back in (group membership may be cached)

## Migration from Automatic to Manual

If you previously had automatic workflow triggering enabled:

1. **No Action Required**: The launcher is already disabled
2. **Inform Users**: Notify editors they must now click "Request Approval"
3. **Monitor**: Check that workflows are being initiated manually
4. **Optional**: Remove launcher configuration entirely (currently just disabled)

## Benefits of Manual Approach

1. **Editor Control**: Editors decide when content is ready for review
2. **Reduced Noise**: No workflows for work-in-progress content
3. **Clear Intent**: Explicit approval request makes workflow intention clear
4. **Better UX**: Button appears in context (Page Editor) where it's needed
5. **Configurable**: Path is configurable without code changes

## Support

For issues or questions:
- Check error logs at `/system/console/slinglog`
- Verify OSGi configuration at `/system/console/configMgr`
- Review servlet status at `/system/console/components`
- Test with browser console open to see JavaScript errors

## Related Documentation

- [SPANISH_CONTENT_APPROVAL_WORKFLOW.md](./SPANISH_CONTENT_APPROVAL_WORKFLOW.md) - Full workflow documentation
- [IMPLEMENTATION_SUMMARY.md](./IMPLEMENTATION_SUMMARY.md) - Implementation details
- [README.md](./README.md) - Project overview



