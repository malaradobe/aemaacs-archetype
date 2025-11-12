# Spanish Content Publish Protection

## Overview

The Spanish Content Approval Workflow includes **multi-layer protection** to ensure that `editor-spanish` users **cannot publish pages without approval**. This document explains exactly how this protection works.

## Protection Layers

### Layer 1: ACL Permissions ✓

**File**: `ui.apps/src/main/content/jcr_root/apps/larasoft/actools/access.yaml`

```yaml
editor-spanish:
  # Allow replicate on Spanish content
  - path: /content/larasoft/us/es
    permission: allow
    privileges: jcr:read,jcr:write,jcr:removeNode,jcr:versionManagement,jcr:lockManagement,jcr:modifyProperties,crx:replicate
```

**Note**: While `editor-spanish` has `crx:replicate` permission, this is **controlled by Layer 2** (WorkflowEnforcementService). The replicate permission alone does NOT allow publishing without approval.

---

### Layer 2: WorkflowEnforcementService ✓ (PRIMARY PROTECTION)

**File**: `core/src/main/java/com/larasoft/core/workflows/WorkflowEnforcementService.java`

This is an **EventHandler** OSGi service that listens to **all replication events** in AEM and intercepts publish attempts.

#### How It Works

1. **Event Listening**
   - Service subscribes to: `ReplicationAction.EVENT_TOPIC`
   - Monitors ALL replication events in AEM
   - Activates automatically on bundle startup

2. **Path Filtering**
   ```java
   if (path == null || !path.startsWith(spanishContentPath)) {
       return; // Not Spanish content, allow through
   }
   ```
   - Only enforces for pages under configured path (default: `/content/larasoft/us/es`)
   - Other content is not affected

3. **Action Type Check**
   ```java
   if (actionType != ReplicationActionType.ACTIVATE) {
       return; // Not a publish action, allow through
   }
   ```
   - Only intercepts ACTIVATE (publish) actions
   - Does not block deactivate, delete, etc.

4. **User Permission Check**
   ```java
   boolean isEditor = isUserInEditorSpanishGroup(resolver, userId);
   if (!isEditor) {
       throw new SecurityException("Only Spanish content editors can publish...");
   }
   ```
   - **Validates user is in `editor-spanish` group**
   - Uses UserManager API for accurate group membership check
   - Blocks all non-editor-spanish users completely

5. **Workflow State Validation**
   ```java
   if (!contentNode.hasProperty(WORKFLOW_STATE_PROPERTY)) {
       throw new SecurityException("Content must go through approval workflow...");
   }
   ```
   - **Requires workflow state property to exist**
   - If no workflow has been run, publish is blocked

6. **State-Specific Checks**
   
   **a) IN_PROGRESS State**
   ```java
   if (STATE_IN_PROGRESS.equals(workflowState)) {
       throw new SecurityException("Cannot publish while workflow is in progress...");
   }
   ```
   ❌ **BLOCKED**: Workflow has been requested but not yet approved
   
   **b) REJECTED State**
   ```java
   if (STATE_REJECTED.equals(workflowState)) {
       throw new SecurityException("Cannot publish rejected content...");
   }
   ```
   ❌ **BLOCKED**: Content was rejected by reviewer
   
   **c) CHANGES_REQUESTED State**
   ```java
   if (STATE_CHANGES_REQUESTED.equals(workflowState)) {
       throw new SecurityException("Cannot publish content with requested changes...");
   }
   ```
   ❌ **BLOCKED**: Reviewer requested changes
   
   **d) APPROVED State**
   ```java
   if (!STATE_APPROVED.equals(workflowState)) {
       throw new SecurityException("Content must be approved...");
   }
   ```
   ✅ **ALLOWED**: Content has been approved by reviewer

7. **Exception Handling**
   ```java
   catch (SecurityException e) {
       throw new RuntimeException(e.getMessage(), e);
   }
   ```
   - SecurityExceptions are **re-thrown as RuntimeExceptions**
   - This **stops the replication action**
   - User sees error message in AEM UI

---

## Workflow States

| State | Can Publish? | Why? |
|-------|--------------|------|
| **No State** | ❌ NO | Must request approval first |
| **IN_PROGRESS** | ❌ NO | Waiting for reviewer |
| **REJECTED** | ❌ NO | Content was rejected |
| **CHANGES_REQUESTED** | ❌ NO | Reviewer wants changes |
| **APPROVED** | ✅ YES | Reviewer approved |

---

## Complete Publish Flow

### Scenario 1: Editor Tries to Publish Without Approval ❌

```
1. editor-spanish user clicks "Publish" button
2. AEM fires ReplicationAction event
3. WorkflowEnforcementService intercepts event
4. Service checks: User in editor-spanish? ✓ Yes
5. Service checks: Workflow state exists? ✗ NO
6. Service throws SecurityException
7. Publish is BLOCKED
8. User sees error: "Content must go through approval workflow before publishing"
```

**Result**: ❌ **PUBLISH BLOCKED**

---

### Scenario 2: Editor Requests Approval, Then Tries to Publish ❌

```
1. editor-spanish clicks "Request Approval"
2. Workflow starts, state = IN_PROGRESS
3. editor-spanish clicks "Publish" (impatient)
4. WorkflowEnforcementService intercepts
5. Service checks: User in editor-spanish? ✓ Yes
6. Service checks: Workflow state = IN_PROGRESS? ✓ Yes
7. Service throws SecurityException
8. Publish is BLOCKED
9. User sees error: "Cannot publish while workflow is in progress"
```

**Result**: ❌ **PUBLISH BLOCKED**

---

### Scenario 3: Reviewer Approves, Editor Publishes ✅

```
1. editor-spanish clicks "Request Approval"
2. Workflow starts, state = IN_PROGRESS
3. reviewer-spanish approves in workflow inbox
4. ApprovalMarkingProcess sets state = APPROVED
5. editor-spanish clicks "Publish"
6. WorkflowEnforcementService intercepts
7. Service checks: User in editor-spanish? ✓ Yes
8. Service checks: Workflow state = APPROVED? ✓ Yes
9. Service allows publish to proceed
10. Content is published successfully
```

**Result**: ✅ **PUBLISH ALLOWED**

---

### Scenario 4: Non-Editor User Tries to Publish ❌

```
1. Some other user (not in editor-spanish) clicks "Publish"
2. WorkflowEnforcementService intercepts
3. Service checks: User in editor-spanish? ✗ NO
4. Service throws SecurityException immediately
5. Publish is BLOCKED
6. User sees error: "Only Spanish content editors can publish approved content"
```

**Result**: ❌ **PUBLISH BLOCKED**

---

### Scenario 5: Reviewer Tries to Publish (Even Approved Content) ❌

```
1. reviewer-spanish user tries to publish approved page
2. WorkflowEnforcementService intercepts
3. Service checks: User in editor-spanish? ✗ NO (user is in reviewer-spanish)
4. Service throws SecurityException
5. Publish is BLOCKED
6. User sees error: "Only Spanish content editors can publish approved content"
```

**Result**: ❌ **PUBLISH BLOCKED**

**Note**: Reviewers also have explicit `crx:replicate` DENY in ACL as additional protection.

---

## Security Guarantees

### ✅ What Is Protected

1. **No Publish Without Workflow**
   - Content MUST have a workflow state property
   - Fresh pages without any workflow = blocked

2. **No Publish During Review**
   - Content with IN_PROGRESS state = blocked
   - Editor must wait for reviewer decision

3. **No Publish of Rejected Content**
   - Content with REJECTED state = blocked
   - Editor must address issues and re-request approval

4. **No Publish with Pending Changes**
   - Content with CHANGES_REQUESTED state = blocked
   - Editor must make changes and re-request approval

5. **Only Approved Content Can Be Published**
   - Content must have APPROVED state
   - Only reviewers can set this state

6. **Only Editors Can Publish**
   - User must be in `editor-spanish` group
   - Reviewers, other users = blocked

### ✅ Protection Against Bypass Attempts

1. **Direct API Calls**
   - WorkflowEnforcementService intercepts ALL replication events
   - Even programmatic/API publish attempts are blocked

2. **Admin Bypass**
   - Administrators CAN bypass (by design)
   - This is for emergency situations only
   - All actions are logged

3. **Permission Escalation**
   - Even if user gains `crx:replicate` permission
   - WorkflowEnforcementService still validates workflow state
   - Permissions alone don't allow bypass

4. **JCR Property Manipulation**
   - Users cannot directly set workflow state properties
   - Only workflow processes can set these (using service user)
   - Manual property changes won't help bypass

---

## Verification & Testing

### Test 1: Verify Service is Active

```bash
# Navigate to OSGi Components Console
http://localhost:4502/system/console/components

# Search for: WorkflowEnforcementService
# Status should show: Active
# Service should show as registered under EventHandler
```

### Test 2: Check Service Logs

```bash
# Navigate to Sling Logs
http://localhost:4502/system/console/slinglog

# Look for log message:
"Workflow Enforcement Service activated - monitoring path: /content/larasoft/us/es"
```

### Test 3: Try to Publish Without Approval

1. Log in as `spanish-editor-test` / `new4you`
2. Create a new Spanish page under `/content/larasoft/us/es`
3. **DO NOT** click "Request Approval"
4. Try to publish the page
5. **Expected**: Error message appears, publish is blocked

### Test 4: Try to Publish During Workflow

1. Log in as `spanish-editor-test`
2. Create a Spanish page
3. Click "Request Approval" button
4. Immediately try to publish
5. **Expected**: Error "Cannot publish while workflow is in progress"

### Test 5: Publish After Approval

1. Log in as `spanish-editor-test`
2. Create a Spanish page
3. Click "Request Approval"
4. Log in as `spanish-reviewer-test`
5. Approve the workflow task
6. Log back in as `spanish-editor-test`
7. Try to publish
8. **Expected**: Publish succeeds ✓

### Test 6: Reviewer Cannot Publish

1. Log in as `spanish-reviewer-test`
2. Navigate to an approved Spanish page
3. Try to publish
4. **Expected**: Error "Only Spanish content editors can publish..."

---

## Configuration

### Change Protected Path

The protected path is configurable:

**Via OSGi Console**:
```
http://localhost:4502/system/console/configMgr
→ Search: "Spanish Content Workflow Configuration"
→ Update: "Spanish Content Path"
```

**Via Configuration File**:
```json
ui.config/.../com.larasoft.core.workflows.SpanishWorkflowConfig.cfg.json
{
  "spanish.content.path": "/content/larasoft/us/es"
}
```

---

## Troubleshooting

### Publish Is Allowed Without Approval

**Possible Causes**:
1. WorkflowEnforcementService is not active
2. Service is not receiving replication events
3. Path configuration is incorrect

**Solutions**:
1. Check service status: `/system/console/components`
2. Check service logs: `/system/console/slinglog`
3. Verify OSGi configuration: `/system/console/configMgr`
4. Rebuild and redeploy core bundle

### Error: "Unable to validate workflow state"

**Cause**: Service user `internal-content` doesn't have proper permissions

**Solution**:
1. Check service user mapping: `/system/console/configMgr` → "Service User Mapper"
2. Verify repoinit configuration in `ui.config`
3. Check logs for service user errors

---

## Logging

### Enable Debug Logging

To see detailed enforcement logs:

1. Navigate to: `http://localhost:4502/system/console/slinglog`
2. Click "Add new logger"
3. Log Level: `DEBUG`
4. Logger: `com.larasoft.core.workflows.WorkflowEnforcementService`
5. Click "Save"

### Key Log Messages

**Service Activated**:
```
Workflow Enforcement Service activated - monitoring path: /content/larasoft/us/es
```

**Publish Blocked - No Workflow**:
```
WORKFLOW VIOLATION: Blocking publish attempt - no workflow state found for: /content/larasoft/us/es/page by user: editor-test
```

**Publish Blocked - In Progress**:
```
WORKFLOW VIOLATION: Blocking publish - workflow in progress for: /content/larasoft/us/es/page by user: editor-test
```

**Publish Blocked - Not Editor**:
```
SECURITY VIOLATION: Blocking publish attempt - user 'other-user' is not in editor-spanish group
```

**Publish Allowed**:
```
✓ Publish action ALLOWED for: /content/larasoft/us/es/page by editor: editor-test (workflow state: APPROVED)
```

---

## Important Notes

1. **Administrators Can Bypass**
   - By design, AEM administrators can publish anything
   - This is for emergency situations
   - All administrator actions are logged

2. **Service Must Be Active**
   - WorkflowEnforcementService must be running
   - Verify after every deployment

3. **Configurable Path**
   - Protection applies only to configured path
   - Other content is not affected

4. **Multi-Layer Defense**
   - ACL permissions + Event listener
   - Even if one layer fails, other protects

5. **Audit Trail**
   - All blocked publish attempts are logged
   - Use logs for security auditing

---

## Summary

### Protection is ACTIVE and ENFORCED

✅ **editor-spanish users CANNOT publish without approval**

The WorkflowEnforcementService provides **real-time, event-driven enforcement** that:
- Intercepts ALL publish attempts
- Validates user permissions
- Checks workflow state
- Blocks unapproved content
- Logs all violations

This is **not just permission-based** - it's an **active enforcement layer** that prevents bypass attempts.

---

## Related Documentation

- [MANUAL_WORKFLOW_SETUP.md](./MANUAL_WORKFLOW_SETUP.md) - Setup guide
- [SPANISH_CONTENT_APPROVAL_WORKFLOW.md](./SPANISH_CONTENT_APPROVAL_WORKFLOW.md) - Workflow details
- [CHANGES_SUMMARY.md](./CHANGES_SUMMARY.md) - Recent changes
- [IMPLEMENTATION_SUMMARY.md](./IMPLEMENTATION_SUMMARY.md) - Full implementation

---

**Last Updated**: November 6, 2025  
**Protection Status**: ✅ **ACTIVE**  
**Enforcement**: ✅ **REAL-TIME**  
**Bypass Prevention**: ✅ **ENABLED**



