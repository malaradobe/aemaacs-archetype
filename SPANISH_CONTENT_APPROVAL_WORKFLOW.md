# Spanish Content Approval Workflow

## Overview

This document describes the Spanish Content Approval Workflow implementation for AEM Sites. This workflow enforces mandatory review and approval before publishing Spanish content under `/content/larasoft/us/es`.

## Architecture

### Components

1. **User Groups** (ACL Tool Configuration)
   - `editor-spanish`: Can edit Spanish content but cannot publish directly
   - `reviewer-spanish`: Can review, approve, reject, and publish Spanish content but cannot edit

2. **Workflow Model**: `spanish-content-approval`
   - Runtime Model: `/var/workflow/models/spanish-content-approval`
   - Design Model: `/conf/global/settings/workflow/models/spanish-content-approval`
   - Automatically triggered when Spanish content is modified

3. **Workflow Processes** (Java OSGi Services)
   - `WorkflowEnforcementProcess`: Marks content as in-workflow to prevent bypass
   - `RejectionNotificationProcess`: Handles content rejection
   - `RequestChangesProcess`: Handles change requests from reviewers
   - `WorkflowEnforcementService`: Event listener that prevents direct publishing

4. **Manual Workflow Initiation**
   - "Request Approval" action in Page Editor (visible only to `editor-spanish` group)
   - Servlet endpoint: `/bin/larasoft/workflow/request-spanish-approval`
   - Configurable Spanish content path via OSGi configuration

## User Groups and Permissions

### editor-spanish

**Description**: Spanish content editors - can edit and publish (only approved) Spanish content

**Member Of**: `contributors`, `workflow-users`

**Permissions**:
- ✅ **ALLOW**: `jcr:read`, `jcr:write`, `jcr:removeNode`, `jcr:versionManagement`, `jcr:lockManagement`, `jcr:modifyProperties`, `crx:replicate` on `/content/larasoft/us/es`
- ❌ **DENY**: All access to `/content/larasoft` (cannot access other language content)
- ✅ **ALLOW**: `jcr:read`, `jcr:write` on `/var/workflow` (for workflow participation)

**What they can do**:
- Create, edit, and delete pages under `/content/larasoft/us/es`
- Create and modify content components
- Save drafts and work-in-progress content
- **Publish pages ONLY AFTER reviewer approval** (when `workflowState` = `APPROVED`)

**What they cannot do**:
- Publish pages without approval (WorkflowEnforcementService blocks unapproved publishes)
- Access or edit English or other language content
- Bypass the approval workflow

**Important**: While editors have `crx:replicate` permission, the `WorkflowEnforcementService` enforces that they can only publish content marked as `APPROVED` by a reviewer.

### reviewer-spanish

**Description**: Spanish content reviewers - can review, approve/reject Spanish content but cannot edit or publish

**Member Of**: `content-authors`, `workflow-users`

**Permissions**:
- ✅ **ALLOW**: `jcr:read`, `jcr:versionManagement` on `/content/larasoft/us/es`
- ❌ **DENY**: `jcr:write`, `jcr:removeNode`, `jcr:lockManagement`, `jcr:modifyProperties`, `crx:replicate` on `/content/larasoft/us/es`
- ✅ **ALLOW**: `jcr:read`, `jcr:write` on `/var/workflow` (for workflow participation)

**What they can do**:
- View Spanish content pages
- Review content submitted through workflow
- Approve content for publication (marks as `APPROVED`)
- Reject content with comments
- Request changes with feedback

**What they cannot do**:
- Edit page content or components
- Make modifications to pages
- Publish pages (editors publish after approval)
- Bypass the approval process

## Workflow Process

### Step-by-Step Flow

1. **Editor Creates/Modifies Content**
   - Editor-spanish user creates or modifies a page under `/content/larasoft/us/es`
   - Content is saved as draft
   - Editor clicks "Request Approval" button in Page Editor toolbar when ready for review

2. **Editor Manually Requests Approval**
   - Editor clicks "Request Approval" button in the Page Editor toolbar
   - Button is only visible for pages under `/content/larasoft/us/es` (configurable)
   - Spanish Content Approval Workflow is initiated
   - Content is marked with `workflowState: IN_PROGRESS`

3. **Workflow Enforcement**
   - `WorkflowEnforcementProcess` sets workflow metadata on the page
   - Content is locked from direct publishing

4. **Review Step**
   - Workflow assigns task to `reviewer-spanish` group
   - Reviewer receives notification (in workflow inbox)
   - Reviewer has three options:
     - **Approve**: Content proceeds to publication
     - **Reject**: Content is rejected with comments
     - **Request Changes**: Content is sent back to editor with feedback

5. **Approval Path**
   - If approved, `ApprovalMarkingProcess` marks content as `APPROVED`
   - ✅ Editor receives **automatic inbox notification**: "Content Approved - Ready to Publish"
   - Editor can now manually publish the content
   - `WorkflowEnforcementService` allows the publish since state is `APPROVED`
   - Workflow completes

6. **Rejection Path**
   - If rejected, `RejectionNotificationProcess` runs
   - Content is marked with `workflowState: REJECTED`
   - Rejection comment is stored in `workflowComment` property
   - ❌ Editor receives **automatic inbox notification**: "Content Rejected - Changes Required"
   - Editor can view rejection reason and make corrections

7. **Request Changes Path**
   - If changes requested, `RequestChangesProcess` runs
   - Content is marked with `workflowState: CHANGES_REQUESTED`
   - Feedback is stored in `workflowFeedback` property
   - 🔄 Editor receives **automatic inbox notification**: "Changes Requested - Action Required"
   - Editor can view feedback and resubmit

### Workflow States

Content pages can have the following workflow states (stored in `jcr:content/workflowState`):

- `IN_PROGRESS`: Workflow is currently active
- `APPROVED`: Content has been approved and published
- `REJECTED`: Content was rejected by reviewer
- `CHANGES_REQUESTED`: Reviewer has requested modifications

## Preventing Workflow Bypass

### How Bypass Prevention Works

1. **ACL Restrictions**
   - `reviewer-spanish` group explicitly denied `crx:replicate` privilege (reviewers cannot publish)
   - `editor-spanish` has `crx:replicate` but it's controlled by workflow enforcement

2. **Workflow State Tracking**
   - Every workflow marks content with state metadata
   - State is checked before allowing any publish action

3. **Event Listener**
   - `WorkflowEnforcementService` listens to all replication events
   - Intercepts publish attempts on `/content/larasoft/us/es`
   - Validates workflow state before allowing publication
   - Blocks publish if:
     - User is not in `editor-spanish` group
     - No workflow state exists
     - Workflow is still in progress (`IN_PROGRESS`)
     - Content is rejected (`REJECTED`)
     - Changes are requested (`CHANGES_REQUESTED`)
     - Content is not approved (`APPROVED`)

4. **User Group Validation**
   - Service checks if user is member of `editor-spanish`
   - Only editors can publish, and only when content is `APPROVED`
   - Reviewers cannot publish (denied `crx:replicate`)
   - All other users are blocked regardless of workflow state

## Implementation Files

### Configuration Files

```
aemaacs-archetype/
├── ui.apps/src/main/content/jcr_root/
│   └── apps/larasoft/actools/
│       └── access.yaml                                    # User groups and ACL configuration
├── ui.content/src/main/content/jcr_root/
│   ├── var/workflow/models/
│   │   └── spanish-content-approval.xml                   # Workflow runtime model
│   └── conf/global/settings/workflow/
│       ├── models/spanish-content-approval/
│       │   └── .content.xml                               # Workflow design model
│       └── launcher/config/spanish-content-approval-launcher/
│           └── .content.xml                               # Workflow launcher configuration
├── ui.config/src/main/content/jcr_root/apps/larasoft/osgiconfig/
│   └── config/
│       ├── org.apache.sling.jcr.repoinit.RepositoryInitializer~larasoft.cfg.json
│       └── org.apache.sling.serviceusermapping.impl.ServiceUserMapperImpl.amended~larasoft.cfg.json
└── core/src/main/java/com/larasoft/core/workflows/
    ├── WorkflowEnforcementProcess.java                    # Marks content in workflow
    ├── WorkflowEnforcementService.java                    # Prevents unauthorized publishing
    ├── ApprovalMarkingProcess.java                        # Marks content as APPROVED
    ├── RejectionNotificationProcess.java                  # Handles rejections
    ├── RequestChangesProcess.java                         # Handles change requests
    └── package-info.java
```

### Test Files

```
aemaacs-archetype/core/src/test/java/com/larasoft/core/workflows/
├── WorkflowEnforcementProcessTest.java                    # Tests workflow enforcement
├── ApprovalMarkingProcessTest.java                        # Tests approval marking
├── RejectionNotificationProcessTest.java                  # Tests rejection handling
├── RequestChangesProcessTest.java                         # Tests change requests
└── SpanishContentUserGroupTest.java                       # Tests user permissions
```

## Testing

### Running Unit Tests

```bash
cd aemaacs-archetype
mvn clean test
```

### Manual Testing Scenarios

#### Test 1: Request Approval Action Visibility

1. Log in as a user in `editor-spanish` group
2. Navigate to `/sites.html/content/larasoft/us/es`
3. Open a Spanish page in Page Editor
4. Verify "Request Approval" button is visible in the toolbar
5. Navigate to a non-Spanish page (e.g., `/content/larasoft/us/en`)
6. Verify "Request Approval" button is NOT visible

**Expected Result**: Button only visible for Spanish content pages and editor-spanish users

#### Test 2: Manual Workflow Initiation and Approval Flow

1. As `editor-spanish`, create/edit a Spanish page
2. Click "Request Approval" button in Page Editor toolbar
3. Verify success notification appears
4. Log in as `reviewer-spanish` user
5. Open workflow inbox: `/aem/inbox`
6. Find the Spanish content approval task
7. Click "Approve"
8. Log back in as `editor-spanish`
9. Check workflow inbox - should see notification that content is approved
10. Navigate to the page and click "Publish"
11. Content should be successfully published

**Expected Result**: Manual workflow initiation works; after reviewer approval, editor can publish the content

#### Test 3: Rejection Flow

1. As `editor-spanish`, create a Spanish page with incomplete content
2. As `reviewer-spanish`, open workflow task
3. Click "Reject" and add comment: "Missing images and description"
4. As `editor-spanish`, check the page properties
5. Verify `workflowState` = `REJECTED`
6. Verify `workflowComment` contains the rejection reason

**Expected Result**: Page is rejected with feedback visible to editor

#### Test 4: Request Changes Flow

1. As `editor-spanish`, create a Spanish page
2. As `reviewer-spanish`, click "Request Changes"
3. Add feedback: "Please add more detail to section 3"
4. As `editor-spanish`, view the feedback
5. Make the requested changes
6. Workflow should restart

**Expected Result**: Editor receives feedback and can resubmit

#### Test 5: Reviewer Cannot Edit or Publish

1. Log in as `reviewer-spanish` user
2. Navigate to a Spanish page
3. Attempt to edit the page content - should be denied
4. Attempt to publish a page (even approved) - should be denied

**Expected Result**: Reviewer can view and approve/reject but cannot edit or publish content

#### Test 6: Bypass Prevention

1. As admin, manually set `workflowState` to `IN_PROGRESS` on a page
2. Attempt to publish the page
3. `WorkflowEnforcementService` should block the publish

**Expected Result**: Publish is blocked while workflow is in progress

### ACL Tool Testing

After deployment, verify ACL configuration:

```bash
# Navigate to AEM Web Console
http://localhost:4502/system/console/configMgr

# Check ACTool logs
http://localhost:4502/system/console/slinglog

# Verify groups were created
http://localhost:4502/useradmin

# Search for:
# - editor-spanish
# - reviewer-spanish

# Check their permissions under Security > Permissions
```

## Deployment

### Building the Package

```bash
cd /Users/malara/Documents/GitHub/Adobe/MarcoLaraProgram-p45262-parent
mvn clean install -PautoInstallSinglePackage
```

### Deployment Steps

1. **Build all modules**:
   ```bash
   mvn clean install
   ```

2. **Deploy to AEM Author**:
   ```bash
   mvn clean install -PautoInstallPackage
   ```

3. **Verify ACL Tool execution**:
   - Check logs at `/system/console/slinglog`
   - Look for ACTool execution messages
   - Verify groups and permissions were created

4. **Verify Workflow Model**:
   - Navigate to `/conf/global/settings/workflow/models`
   - Verify "Spanish Content Approval Workflow" exists
   - Open and verify workflow steps

5. **Verify Workflow Launcher**:
   - Navigate to Tools > Workflow > Launchers
   - Find "Spanish Content Approval Launcher"
   - Verify it's enabled and configured correctly

6. **Test User Groups**:
   - Create test users
   - Add them to respective groups
   - Perform manual testing scenarios

## Troubleshooting

### Workflow Not Triggering

**Problem**: Workflow doesn't start when "Request Approval" button is clicked

**Solutions**:
- Verify user is member of `editor-spanish` group
- Check servlet is registered at `/bin/larasoft/workflow/request-spanish-approval`
- Verify OSGi configuration has correct paths
- Check workflow model path is correct
- Review error logs in `/system/console/slinglog`
- Check browser console for JavaScript errors

### ACL Permissions Not Applied

**Problem**: User groups don't have correct permissions

**Solutions**:
- Check ACTool configuration in OSGi config
- Verify `access.yaml` syntax is correct
- Check ACTool logs for errors
- Manually verify permissions in Security > Permissions

### Publish Still Allowed Without Approval

**Problem**: Users can bypass workflow and publish directly

**Solutions**:
- Verify `WorkflowEnforcementService` is active
- Check service is listening to replication events
- Verify `editor-spanish` group has `crx:replicate` denied
- Check user is actually member of correct group

### Workflow Process Errors

**Problem**: Workflow fails at a specific step

**Solutions**:
- Check workflow instance details in `/etc/workflow/instances`
- Review error logs
- Verify service user `internal-content` has proper permissions
- Check service user mapping configuration

## Security Considerations

1. **Service User**: The `internal-content` service user has read-only access to content and workflow paths
2. **Group Membership**: Users should be added to only ONE of the groups (editor or reviewer)
3. **Admin Bypass**: Administrators can bypass the workflow - this is by design for emergency situations
4. **Workflow History**: All workflow actions are logged and auditable

## Implemented Features

1. ✅ **Inbox Notifications**: Automatic AEM Inbox notifications for all workflow decisions (See [WORKFLOW_NOTIFICATIONS.md](./WORKFLOW_NOTIFICATIONS.md))
2. ✅ **Manual Workflow Trigger**: "Request Approval" button in Page Editor
3. ✅ **Publish Protection**: WorkflowEnforcementService prevents unauthorized publishing
4. ✅ **Configurable Path**: Spanish content path is configurable via OSGi

## Future Enhancements

1. **Email Notifications**: Add email notifications using Day CQ Mail Service
2. **Escalation**: Add time-based escalation if review is not completed
3. **Multi-stage Review**: Add multiple approval stages for sensitive content
4. **Metrics Dashboard**: Track workflow completion times and rejection rates
5. **Slack/Teams Integration**: Send notifications to collaboration tools

## Support

For issues or questions, contact:
- Development Team: [Your Team Email]
- AEM Administrator: [Admin Email]

## References

- [AEM Workflow Documentation](https://experienceleague.adobe.com/docs/experience-manager-65/developing/extending-aem/extending-workflows.html)
- [ACL Tool Documentation](https://github.com/Netcentric/accesscontroltool)
- [AEM Security Best Practices](https://experienceleague.adobe.com/docs/experience-manager-65/administering/security/security-checklist.html)

