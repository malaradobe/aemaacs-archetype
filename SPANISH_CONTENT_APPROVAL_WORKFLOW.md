# Spanish Content Approval Workflow Documentation

## Table of Contents
1. [Overview](#overview)
2. [Architecture](#architecture)
3. [User Groups and Permissions](#user-groups-and-permissions)
4. [Workflow States](#workflow-states)
5. [Workflow Process Flow](#workflow-process-flow)
6. [Components](#components)
7. [Configuration](#configuration)
8. [Security and Enforcement](#security-and-enforcement)
9. [Usage Guide](#usage-guide)
10. [Troubleshooting](#troubleshooting)

---

## Overview

The Spanish Content Approval Workflow is an AEM workflow system that enforces a mandatory review and approval process for all Spanish content under `/content/larasoft/us/es`. This workflow ensures that Spanish content is reviewed and approved by qualified reviewers before being published to the live site.

### Key Features

- ✅ **Automatic Workflow Initiation**: Automatically starts when new Spanish pages are created
- ✅ **Role-Based Access Control**: Separates editing and reviewing responsibilities
- ✅ **State Management**: Tracks content through approval lifecycle states
- ✅ **Publish Prevention**: Blocks publishing of unapproved content
- ✅ **Feedback Loop**: Supports change requests and resubmission
- ✅ **Audit Trail**: Records approval metadata (who, when, comments)

### Business Purpose

This workflow ensures quality control and compliance for Spanish content by:
1. Requiring expert review before publication
2. Preventing unauthorized or premature publishing
3. Maintaining separation of duties between content creators and reviewers
4. Creating an auditable approval trail

---

## Architecture

### System Components

```
┌─────────────────────────────────────────────────────────────────┐
│                    Spanish Content Approval System               │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌──────────────┐        ┌──────────────┐        ┌───────────┐ │
│  │   Workflow   │        │   Workflow   │        │  Event    │ │
│  │   Launcher   │───────▶│    Model     │        │ Handlers  │ │
│  └──────────────┘        └──────────────┘        └───────────┘ │
│        │                        │                       │        │
│        │                        ▼                       ▼        │
│        │              ┌──────────────────┐   ┌─────────────────┐│
│        │              │ Workflow Process │   │  Enforcement    ││
│        └─────────────▶│     Steps        │   │    Service      ││
│                       └──────────────────┘   └─────────────────┘│
│                                │                       │        │
│                                ▼                       ▼        │
│                       ┌──────────────────────────────────────┐ │
│                       │    JCR Content Properties           │ │
│                       │  - workflowState                     │ │
│                       │  - workflowApprovedBy               │ │
│                       │  - workflowFeedback                 │ │
│                       └──────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

---

## User Groups and Permissions

### editor-spanish Group

**Purpose**: Spanish content creators and publishers

**Capabilities**:
- ✅ Create, read, update, delete Spanish content under `/content/larasoft/us/es`
- ✅ Request approval for content (manual or automatic)
- ✅ Publish APPROVED content only
- ✅ Participate in workflow as content submitters
- ❌ Cannot approve their own content
- ❌ Cannot bypass workflow enforcement

**Technical Details**:
- Group Path: `/home/groups/larasoft/editor-spanish`
- Member of: `content-authors`, `workflow-users`
- JCR Privileges: `jcr:read`, `jcr:write`, `jcr:removeNode`, `jcr:versionManagement`, `jcr:lockManagement`, `jcr:modifyProperties`, `crx:replicate`

### reviewer-spanish Group

**Purpose**: Spanish content quality reviewers and approvers

**Capabilities**:
- ✅ Review Spanish content
- ✅ Approve content (marks as APPROVED)
- ✅ Request changes with feedback
- ✅ Participate in workflow as reviewers
- ❌ Cannot edit content directly
- ❌ Cannot publish content

**Technical Details**:
- Group Path: `/home/groups/larasoft/reviewer-spanish`
- Member of: `contributor`, `workflow-users`
- JCR Privileges: `jcr:read`, `jcr:versionManagement`
- Denied Privileges: `jcr:write`, `jcr:removeNode`, `jcr:lockManagement`, `jcr:modifyProperties`, `crx:replicate`

---

## Workflow States

The workflow uses JCR properties on `jcr:content` nodes to track approval state:

### State: IN_PROGRESS

**Description**: Content is awaiting reviewer approval or currently under review

**Set By**: 
- `WorkflowEnforcementProcess` (initial workflow step)
- `RequestApprovalProcess` (when editor resubmits after changes)

**Properties Set**:
- `workflowState`: "IN_PROGRESS"
- `workflowId`: Workflow instance ID
- `workflowSubmittedBy`: User who submitted
- `workflowSubmittedDate`: Submission timestamp

**Allowed Actions**:
- ❌ Cannot publish
- ✅ Reviewer can approve or request changes
- ✅ Can be edited by editor-spanish users

### State: APPROVED

**Description**: Content has been reviewed and approved, ready for publishing

**Set By**: `ApprovalMarkingProcess` (when reviewer approves)

**Properties Set**:
- `workflowState`: "APPROVED"
- `workflowApprovedBy`: User who approved
- `workflowApprovedDate`: Approval timestamp
- `workflowApprovalComment`: Optional approval comment

**Allowed Actions**:
- ✅ Can be published by editor-spanish users
- ✅ Can be edited (requires new approval)

### State: CHANGES_REQUESTED

**Description**: Reviewer has requested changes; content cannot be published

**Set By**: `RequestChangesProcess` (when reviewer requests changes)

**Properties Set**:
- `workflowState`: "CHANGES_REQUESTED"
- `workflowFeedback`: Reviewer's feedback comments

**Allowed Actions**:
- ❌ Cannot publish
- ✅ Can be edited by editor-spanish users
- ✅ Editor can resubmit for approval (transitions to IN_PROGRESS)

---

## Workflow Process Flow

### Flow Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                                                                   │
│  [Content Created or "Request Approval" Button Clicked]         │
│                          │                                        │
│                          ▼                                        │
│               ┌──────────────────────┐                          │
│               │  Workflow Launcher   │                          │
│               │    (Automatic)       │                          │
│               └──────────────────────┘                          │
│                          │                                        │
│                          ▼                                        │
│         ┌────────────────────────────────────┐                  │
│         │ 1. Check Workflow State            │                  │
│         │    (WorkflowEnforcementProcess)    │                  │
│         │    Sets: IN_PROGRESS               │                  │
│         └────────────────────────────────────┘                  │
│                          │                                        │
│                          ▼                                        │
│         ┌────────────────────────────────────┐                  │
│         │ 2. Request Changes Process         │                  │
│         │    (Initial Setup - Deprecated)    │                  │
│         └────────────────────────────────────┘                  │
│                          │                                        │
│                          ▼                                        │
│         ┌────────────────────────────────────┐                  │
│         │ 3. Work in Progress                │                  │
│         │    (Participant: editor-spanish)   │                  │
│         │    Editor prepares content         │                  │
│         └────────────────────────────────────┘                  │
│                          │                                        │
│                          ▼                                        │
│         ┌────────────────────────────────────┐                  │
│         │ 4. Request Approval                │                  │
│         │    (RequestApprovalProcess)        │                  │
│         │    Confirms: IN_PROGRESS           │                  │
│         └────────────────────────────────────┘                  │
│                          │                                        │
│                          ▼                                        │
│         ┌────────────────────────────────────┐                  │
│         │ 5. Spanish Content Review          │                  │
│         │    (Participant: reviewer-spanish) │                  │
│         └────────────────────────────────────┘                  │
│                          │                                        │
│              ┌───────────┴───────────┐                          │
│              ▼                       ▼                           │
│    ┌─────────────────┐    ┌──────────────────┐                │
│    │ 6a. APPROVE     │    │ 6b. REQUEST      │                │
│    │ (ApprovalMarking│    │     CHANGES      │                │
│    │  Process)        │    │ (RequestChanges  │                │
│    │ Sets: APPROVED   │    │  Process)        │                │
│    └─────────────────┘    │ Sets: CHANGES_   │                │
│              │             │      REQUESTED   │                │
│              │             └──────────────────┘                │
│              │                       │                           │
│              │                       ▼                           │
│              │             ┌──────────────────┐                │
│              │             │ Goto Step        │                │
│              │             │ (Back to Step 3) │                │
│              │             └──────────────────┘                │
│              │                       │                           │
│              │             ┌─────────┘                          │
│              │             │                                     │
│              │             ▼                                     │
│              │    [Editor Makes Changes]                        │
│              │             │                                     │
│              │             ▼                                     │
│              │    ┌────────────────────┐                       │
│              │    │ 4. Request Approval│                       │
│              │    │ Sets: IN_PROGRESS  │                       │
│              │    └────────────────────┘                       │
│              │             │                                     │
│              │             └─────────────┐                      │
│              ▼                           ▼                      │
│         ┌──────────────────────────────────────┐              │
│         │           OR JOIN                     │              │
│         └──────────────────────────────────────┘              │
│                          │                                        │
│                          ▼                                        │
│                      [END]                                       │
│           (Content ready for publishing)                        │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
```

### Detailed Step Descriptions

#### Step 1: Check Workflow State (WorkflowEnforcementProcess)
- **Purpose**: Initialize workflow tracking on content
- **Actions**: Sets `workflowState=IN_PROGRESS` and `workflowId`
- **Auto-advance**: Yes
- **Next**: Step 2

#### Step 2: Request Changes (Initial Setup)
- **Purpose**: Legacy setup step (deprecated)
- **Actions**: None (pass-through)
- **Auto-advance**: Yes
- **Next**: Step 3

#### Step 3: Work in Progress (Participant Step)
- **Participant**: editor-spanish group
- **Purpose**: Editor prepares content for submission
- **Actions**: Editor creates/edits content
- **Auto-advance**: No (waits for editor action)
- **Next**: Step 4 (when editor clicks "Complete" in workflow inbox)

#### Step 4: Request Approval (RequestApprovalProcess)
- **Purpose**: Submits content for review
- **Actions**: 
  - Ensures `workflowState=IN_PROGRESS`
  - Records submission metadata
  - Clears previous feedback if resubmitted
- **Auto-advance**: Yes
- **Next**: Step 5

#### Step 5: Spanish Content Review (Participant Step)
- **Participant**: reviewer-spanish group
- **Purpose**: Reviewer evaluates content quality
- **Actions**: Reviewer chooses "Approve" or "Request Changes"
- **Auto-advance**: No (waits for reviewer decision)
- **Next**: Step 6a (Approve) or Step 6b (Request Changes)

#### Step 6a: Mark as Approved (ApprovalMarkingProcess)
- **Purpose**: Mark content as approved for publishing
- **Actions**: 
  - Sets `workflowState=APPROVED`
  - Records approval metadata (who, when, comments)
- **Auto-advance**: Yes
- **Next**: End (success)

#### Step 6b: Request Changes (RequestChangesProcess)
- **Purpose**: Send content back to editor with feedback
- **Actions**: 
  - Sets `workflowState=CHANGES_REQUESTED`
  - Records reviewer feedback
- **Auto-advance**: Yes
- **Next**: Goto Step → Returns to Step 3

---

## Components

### 1. Workflow Launcher

**File**: `ui.content/src/main/content/jcr_root/conf/global/settings/workflow/launcher/config/spanish-content-launcher/.content.xml`

**Configuration**:
```xml
enabled: true
eventType: 1 (NODE_ADDED)
nodetype: cq:Page
glob: /content/larasoft/us/es(/.*)?
condition: jcr:content/cq:template
runModes: author
workflow: /var/workflow/models/spanish-content-approval
```

**Purpose**: Automatically starts the Spanish Content Approval Workflow when:
- A new `cq:Page` is created under `/content/larasoft/us/es` or any descendant path
- The page has a template (indicated by `jcr:content/cq:template` property)
- The event occurs on an author instance

**Behavior**:
- Triggers on content creation only (not modification)
- Excludes `jcr:content` sub-node changes
- Works for all descendant paths (e.g., `/content/larasoft/us/es/section/page`)

---

### 2. Workflow Model

**Files**: 
- Modern: `ui.content/src/main/content/jcr_root/conf/global/settings/workflow/models/spanish-content-approval/.content.xml`
- Legacy: `ui.content/src/main/content/jcr_root/var/workflow/models/spanish-content-approval.xml`

**Title**: Spanish Content Approval Workflow

**Description**: Approval workflow for Spanish content under /content/larasoft/us/es - enforces review before publishing

**Steps**: See [Workflow Process Flow](#workflow-process-flow) section

---

### 3. Workflow Process Steps (Java)

#### WorkflowEnforcementProcess

**File**: `core/src/main/java/com/larasoft/core/workflows/WorkflowEnforcementProcess.java`

**OSGi Component**: `process.label=Workflow Enforcement Process`

**Purpose**: Initialize workflow state tracking to prevent publish bypass

**Logic**:
1. Validates payload is under Spanish content path
2. Retrieves `jcr:content` node
3. Sets properties:
   - `workflowState` = "IN_PROGRESS"
   - `workflowId` = current workflow instance ID
4. Commits changes

**Error Handling**: Throws `WorkflowException` if resource not found or cannot be modified

---

#### RequestApprovalProcess

**File**: `core/src/main/java/com/larasoft/core/workflows/RequestApprovalProcess.java`

**OSGi Component**: `process.label=Request Approval Process`

**Purpose**: Initiate or re-initiate approval request (transitions CHANGES_REQUESTED → IN_PROGRESS)

**Logic**:
1. Retrieves current workflow state (for logging)
2. Sets properties:
   - `workflowState` = "IN_PROGRESS"
   - `workflowSubmittedBy` = workflow initiator
   - `workflowSubmittedDate` = current timestamp
   - `workflowSubmissionComment` = optional comment from metadata
3. If resubmission after changes:
   - Clears previous `workflowFeedback` property
4. Commits changes

**Use Cases**:
- Initial submission for approval
- Resubmission after addressing requested changes

---

#### ApprovalMarkingProcess

**File**: `core/src/main/java/com/larasoft/core/workflows/ApprovalMarkingProcess.java`

**OSGi Component**: `process.label=Approval Marking Process`

**Purpose**: Mark content as approved for publishing (IN_PROGRESS → APPROVED)

**Logic**:
1. Sets properties:
   - `workflowState` = "APPROVED"
   - `workflowApprovedBy` = workflow initiator (reviewer)
   - `workflowApprovedDate` = current timestamp
   - `workflowApprovalComment` = optional comment from workflow metadata
2. Commits changes
3. Logs approval success

**Post-Approval**:
- Content is now publishable by editor-spanish users
- Audit trail is complete

---

#### RequestChangesProcess

**File**: `core/src/main/java/com/larasoft/core/workflows/RequestChangesProcess.java`

**OSGi Component**: `process.label=Request Changes Process`

**Purpose**: Handle change requests from reviewer (IN_PROGRESS → CHANGES_REQUESTED)

**Logic**:
1. Sets properties:
   - `workflowState` = "CHANGES_REQUESTED"
   - `workflowFeedback` = reviewer's comment from workflow metadata
2. Commits changes
3. Logs feedback storage

**Post-Action**:
- Workflow returns to editor (via Goto Step)
- Content cannot be published until resubmitted and approved

---

### 4. Request Approval Servlet

**File**: `core/src/main/java/com/larasoft/core/servlets/RequestSpanishApprovalServlet.java`

**Endpoint**: `POST /bin/larasoft/workflow/request-spanish-approval.json`

**Purpose**: Manual workflow initiation via HTTP API (alternative to automatic launcher)

**Request Parameters**:
- `pagePath` (required): Path to the page requiring approval

**Request Example**:
```bash
curl -X POST http://localhost:4502/bin/larasoft/workflow/request-spanish-approval.json \
  -u editor-spanish:password \
  -d "pagePath=/content/larasoft/us/es/my-page"
```

**Response (Success)**:
```json
{
  "success": true,
  "message": "Approval workflow initiated successfully",
  "pagePath": "/content/larasoft/us/es/my-page",
  "pageTitle": "My Page"
}
```

**Response (Error)**:
```json
{
  "success": false,
  "error": "User is not authorized to request approval. Must be in editor-spanish group."
}
```

**Validation**:
1. User must be in `editor-spanish` group
2. Page must be under configured Spanish content path
3. Page must exist
4. Workflow model must be available

**Security**: User authentication via AEM session

---

### 5. Workflow Enforcement Service (Replication Listener)

**File**: `core/src/main/java/com/larasoft/core/workflows/WorkflowEnforcementService.java`

**OSGi Component**: `EventHandler` listening to `ReplicationAction.EVENT_TOPIC`

**Purpose**: Prevent publishing of unapproved Spanish content (real-time enforcement)

**Trigger**: Every replication/publish attempt

**Enforcement Logic**:

```java
if (path under /content/larasoft/us/es && action == ACTIVATE) {
    
    // Check 1: User authorization
    if (user NOT in editor-spanish group) {
        BLOCK: "Only Spanish content editors can publish approved content"
    }
    
    // Check 2: Workflow state exists
    if (no workflowState property) {
        BLOCK: "Content must go through approval workflow before publishing"
    }
    
    // Check 3: State validation
    if (workflowState == IN_PROGRESS) {
        BLOCK: "Cannot publish while workflow is in progress"
    }
    
    if (workflowState == CHANGES_REQUESTED) {
        BLOCK: "Cannot publish content with requested changes"
    }
    
    if (workflowState != APPROVED) {
        BLOCK: "Content must be approved by reviewer before publishing"
    }
    
    // All checks passed
    ALLOW: Publish proceeds
}
```

**Security Exceptions**:
- Throws `SecurityException` to block replication
- Logs all violations with user ID and path
- Exceptions bubble up to halt the replication process

**Service User**: Uses `internal-content` service user for JCR access

---

### 6. Configuration (OSGi)

**File**: `core/src/main/java/com/larasoft/core/workflows/SpanishWorkflowConfig.java`

**Configuration Interface**:
```java
@ObjectClassDefinition(name = "Spanish Content Workflow Configuration")
public @interface SpanishWorkflowConfig {
    
    String spanish_content_path() default "/content/larasoft/us/es";
    
    String workflow_model_path() default "/var/workflow/models/spanish-content-approval";
}
```

**Configuration Location**: 
- OSGi Configuration Console: `http://localhost:4502/system/console/configMgr`
- Search for: "Spanish Content Workflow Configuration"

**Configurable Properties**:
- **Spanish Content Path**: Root path for Spanish content requiring approval
- **Workflow Model Path**: Path to the workflow model

**Components Using Config**:
- `RequestSpanishApprovalServlet`
- `WorkflowEnforcementService`

---

## Configuration

### OSGi Configuration

Navigate to: `http://localhost:4502/system/console/configMgr`

Search for: **Spanish Content Workflow Configuration**

**Default Values**:
- Spanish Content Path: `/content/larasoft/us/es`
- Workflow Model Path: `/var/workflow/models/spanish-content-approval`

**Customization**:
- Change paths to match your site structure
- Create multiple configurations for different language paths
- Restart services after modification

### Workflow Model Deployment

**Package Filter** (`META-INF/vault/filter.xml`):
```xml
<filter root="/var/workflow/models/spanish-content-approval" mode="replace"/>
<filter root="/conf/global/settings/workflow/models/spanish-content-approval" mode="replace"/>
<filter root="/conf/global/settings/workflow/launcher/config/spanish-content-launcher" mode="replace"/>
```

**Deployment**: Included in `ui.content` package

---

## Security and Enforcement

### Multi-Layer Security Model

#### Layer 1: JCR Permissions (actools/access.yaml)

**editor-spanish**:
```yaml
- path: /content/larasoft/us/es
  permission: allow
  privileges: jcr:read,jcr:write,jcr:removeNode,jcr:versionManagement,
              jcr:lockManagement,jcr:modifyProperties,crx:replicate
```

**reviewer-spanish**:
```yaml
- path: /content/larasoft/us/es
  permission: allow
  privileges: jcr:read,jcr:versionManagement

- path: /content/larasoft/us/es
  permission: deny
  privileges: jcr:write,jcr:removeNode,jcr:lockManagement,
              jcr:modifyProperties,crx:replicate
```

**Notes**:
- `editor-spanish` has `crx:replicate` permission (required for UI)
- Actual publish enforcement is done by `WorkflowEnforcementService`
- Reviewers cannot edit or publish (read-only)

#### Layer 2: Workflow Process Validation

- Each process step validates payload path
- Content properties track workflow state
- Audit metadata recorded at each transition

#### Layer 3: Replication Event Listener (WorkflowEnforcementService)

- Real-time interception of all publish attempts
- Validates user group membership
- Validates workflow state property
- Throws exceptions to block unauthorized publishes

### Bypass Prevention

**Cannot Bypass Via**:
- ❌ Direct JCR property modification (state validation on publish)
- ❌ Alternative replication methods (event listener catches all)
- ❌ Workflow instance deletion (state persists on content)
- ❌ Group impersonation (user validation per request)

**Could Bypass Via** (Admin Actions):
- ✅ Manually setting `workflowState=APPROVED` as admin (intentional escape hatch)
- ✅ Disabling WorkflowEnforcementService (requires OSGi console access)

---

## Usage Guide

### For Content Editors (editor-spanish)

#### Creating New Spanish Content

1. **Create Page**:
   - Navigate to `/content/larasoft/us/es` in Sites console
   - Create new page with template
   - Workflow automatically starts upon creation

2. **Edit Content**:
   - Complete content creation/editing
   - Save changes

3. **Submit for Approval**:
   - **Option A (Automatic)**: Workflow already started by launcher
   - **Option B (Manual)**: Click "Request Approval" button in page properties
   - Content enters `IN_PROGRESS` state

4. **Wait for Review**:
   - Check workflow inbox for status
   - Monitor for reviewer feedback

5. **If Changes Requested**:
   - Receive notification (state: `CHANGES_REQUESTED`)
   - Review feedback in `workflowFeedback` property
   - Make requested changes
   - Click "Complete" in workflow inbox to resubmit
   - Content returns to `IN_PROGRESS` state

6. **If Approved**:
   - Receive notification (state: `APPROVED`)
   - Content is now publishable

7. **Publish Content**:
   - Select page in Sites console
   - Click "Quick Publish" or "Manage Publication"
   - ✅ Publish succeeds (content is APPROVED)

#### Common Scenarios

**Scenario: Content Stuck in IN_PROGRESS**
- **Cause**: Reviewer has not yet reviewed
- **Action**: Contact reviewer-spanish group member

**Scenario: Cannot Publish (CHANGES_REQUESTED)**
- **Cause**: Reviewer requested changes
- **Action**: Address feedback and resubmit via workflow inbox

**Scenario: Publish Blocked Despite Approval**
- **Cause**: Workflow state property missing/corrupted
- **Action**: Contact system administrator

---

### For Content Reviewers (reviewer-spanish)

#### Reviewing Content

1. **Access Workflow Inbox**:
   - Navigate to: `http://localhost:4502/aem/inbox`
   - Filter by: "Spanish Content Approval Workflow"

2. **Open Work Item**:
   - Click on pending review item
   - Opens content page for review

3. **Review Content**:
   - Evaluate content quality, accuracy, compliance
   - Check spelling, grammar, formatting
   - Verify adherence to style guide

4. **Make Decision**:
   
   **Option A: Approve**
   - Click "Approve" button
   - (Optional) Add approval comment
   - Submit
   - Content marked as `APPROVED`
   - Editor can now publish
   
   **Option B: Request Changes**
   - Click "Request Changes" button
   - Add detailed feedback in comment field (required)
   - Submit
   - Content marked as `CHANGES_REQUESTED`
   - Returns to editor for revision

#### Reviewer Best Practices

- ✅ Provide specific, actionable feedback
- ✅ Reference style guide violations
- ✅ Suggest corrections when possible
- ✅ Respond to reviews within SLA
- ✅ Document approval decisions
- ❌ Do not edit content directly (read-only access)

---

### For Administrators

#### Monitoring Workflow Health

**Check Running Workflows**:
```
http://localhost:4502/libs/cq/workflow/admin/console/content/instances.html
```

**Check Workflow Models**:
```
http://localhost:4502/libs/cq/workflow/admin/console/content/models.html
```

**Check Launcher Configuration**:
```
http://localhost:4502/libs/cq/workflow/admin/console/content/launchers.html
```

#### Common Administrative Tasks

**Restart Stalled Workflow**:
1. Navigate to workflow instances
2. Find stalled workflow by payload path
3. Click "Terminate" and restart manually

**Emergency Approval (Bypass)**:
1. Use CRX/DE or Groovy Console
2. Set property on `jcr:content`:
   ```groovy
   def page = getResource('/content/larasoft/us/es/emergency-page')
   def content = page.getChild('jcr:content')
   def props = content.adaptTo(ModifiableValueMap.class)
   props.put('workflowState', 'APPROVED')
   props.put('workflowApprovedBy', 'admin-emergency')
   props.put('workflowApprovedDate', Calendar.getInstance())
   content.resourceResolver.commit()
   ```

**Disable Enforcement (Emergency)**:
1. Navigate to OSGi Console: `http://localhost:4502/system/console/components`
2. Search: "WorkflowEnforcementService"
3. Click "Disable"
4. **⚠️ Re-enable after emergency**

---

## Troubleshooting

### Issue: Workflow Does Not Start Automatically

**Symptoms**: New Spanish pages created but no workflow starts

**Diagnosis**:
1. Check launcher is enabled:
   - Navigate to: `http://localhost:4502/libs/cq/workflow/admin/console/content/launchers.html`
   - Verify "Spanish Content Launcher" is enabled
2. Check path pattern matches:
   - Launcher glob: `/content/larasoft/us/es(/.*)?`
   - Content path must match pattern
3. Check event type:
   - Launcher triggers on NODE_ADDED (creation only)
   - Does not trigger on page modification

**Resolution**:
- Enable launcher if disabled
- Manually start workflow via servlet
- Check error logs for launcher failures

---

### Issue: Cannot Publish Approved Content

**Symptoms**: Content shows `APPROVED` state but publish fails

**Diagnosis**:
1. Check user group membership:
   ```bash
   curl -u admin:admin http://localhost:4502/home/groups/larasoft/editor-spanish.rw.json
   ```
   - Verify current user is in `editor-spanish` group members
2. Check workflow state property:
   - Navigate to CRX/DE: `/content/larasoft/us/es/page/jcr:content`
   - Verify property: `workflowState = "APPROVED"`
3. Check WorkflowEnforcementService is running:
   - Navigate to: `http://localhost:4502/system/console/components`
   - Search: "WorkflowEnforcementService"
   - Status should be "Active"

**Resolution**:
- Add user to editor-spanish group
- Set workflowState property manually (admin action)
- Restart WorkflowEnforcementService bundle

---

### Issue: Reviewer Cannot See Work Items

**Symptoms**: Reviewer has empty workflow inbox

**Diagnosis**:
1. Check group membership:
   - User must be in `reviewer-spanish` group
2. Check workflow participant configuration:
   - Workflow model → Step 5 → Participant = "reviewer-spanish"
3. Check workflow state:
   - Content must be in `IN_PROGRESS` state
   - Workflow must be at review step (not editor step)

**Resolution**:
- Add user to reviewer-spanish group
- Verify workflow has progressed to review step
- Check workflow instance for errors

---

### Issue: Changes Requested But Editor Cannot Edit

**Symptoms**: Content in `CHANGES_REQUESTED` but editor receives permission error

**Diagnosis**:
1. Check editor permissions:
   - Editor must have `jcr:write` privilege on `/content/larasoft/us/es`
2. Check page lock status:
   - Page may be locked by workflow

**Resolution**:
- Verify ACL configuration in access.yaml
- Unlock page via Sites console
- Check for conflicting permission denials

---

### Issue: Workflow State Corruption

**Symptoms**: Multiple conflicting workflow instances or incorrect state

**Diagnosis**:
1. Check for multiple running workflow instances:
   - Navigate to: `http://localhost:4502/libs/cq/workflow/admin/console/content/instances.html`
   - Filter by payload path
2. Check property values:
   - CRX/DE → `jcr:content` → verify `workflowState` value

**Resolution**:
1. Terminate all workflow instances for the page
2. Reset workflow properties:
   ```groovy
   def page = getResource('/content/larasoft/us/es/problematic-page/jcr:content')
   def props = page.adaptTo(ModifiableValueMap.class)
   props.remove('workflowState')
   props.remove('workflowId')
   props.remove('workflowFeedback')
   page.resourceResolver.commit()
   ```
3. Restart workflow manually

---

### Issue: Performance Degradation

**Symptoms**: Slow workflow processing or publish delays

**Diagnosis**:
1. Check workflow instance count:
   - High number of active instances may cause slowdown
2. Check WorkflowEnforcementService logs:
   - Look for timeout errors or database connection issues
3. Check replication queue:
   - Navigate to: `http://localhost:4502/etc/replication/agents.author.html`
   - Look for blocked replication agents

**Resolution**:
- Archive/purge old completed workflow instances
- Increase workflow thread pool size (OSGi config)
- Clear blocked replication queues
- Index optimization on workflow-related properties

---

## Error Messages Reference

| Error Message | Cause | User Action |
|--------------|-------|-------------|
| "Only Spanish content editors can publish approved content" | User not in editor-spanish group | Contact administrator to add user to group |
| "Content must go through approval workflow before publishing" | No workflow state property | Request approval before publishing |
| "Cannot publish while workflow is in progress" | Workflow state = IN_PROGRESS | Wait for reviewer approval |
| "Cannot publish content with requested changes" | Workflow state = CHANGES_REQUESTED | Address reviewer feedback and resubmit |
| "User is not authorized to request approval" | User not in editor-spanish group | Contact administrator |
| "Page is not under Spanish content path" | Page outside `/content/larasoft/us/es` | Move page to correct location |
| "Workflow model not found" | Workflow model not deployed | Contact administrator to deploy workflow package |

---

## Audit and Compliance

### Audit Trail Properties

All workflow actions create an audit trail stored as JCR properties on `jcr:content`:

| Property | Description | Set By | Example Value |
|----------|-------------|--------|---------------|
| `workflowState` | Current approval state | All processes | "APPROVED" |
| `workflowId` | Workflow instance ID | WorkflowEnforcementProcess | "2023-11-16_15-30-45_123" |
| `workflowSubmittedBy` | User who submitted | RequestApprovalProcess | "jdoe" |
| `workflowSubmittedDate` | Submission timestamp | RequestApprovalProcess | 2023-11-16T15:30:45.000Z |
| `workflowApprovedBy` | User who approved | ApprovalMarkingProcess | "msmith" |
| `workflowApprovedDate` | Approval timestamp | ApprovalMarkingProcess | 2023-11-16T16:15:22.000Z |
| `workflowApprovalComment` | Approval comment | ApprovalMarkingProcess | "Approved - content meets quality standards" |
| `workflowFeedback` | Requested changes | RequestChangesProcess | "Please correct spelling errors in paragraph 3" |
| `workflowSubmissionComment` | Submission comment | RequestApprovalProcess | "Addressed all feedback from previous review" |

### Compliance Reporting

**Query Approved Content**:
```sql
SELECT * FROM [cq:PageContent] AS content
WHERE ISDESCENDANTNODE(content, '/content/larasoft/us/es')
AND content.[workflowState] = 'APPROVED'
ORDER BY content.[workflowApprovedDate] DESC
```

**Query Pending Reviews**:
```sql
SELECT * FROM [cq:PageContent] AS content
WHERE ISDESCENDANTNODE(content, '/content/larasoft/us/es')
AND content.[workflowState] = 'IN_PROGRESS'
ORDER BY content.[workflowSubmittedDate] ASC
```

**Query Changes Requested**:
```sql
SELECT * FROM [cq:PageContent] AS content
WHERE ISDESCENDANTNODE(content, '/content/larasoft/us/es')
AND content.[workflowState] = 'CHANGES_REQUESTED'
```

---

## Technical Architecture Decisions

### Why Separate Groups for Editing and Reviewing?

**Rationale**: Enforces separation of duties principle
- Prevents authors from approving their own work
- Reduces bias in content review
- Meets compliance requirements for content governance
- Creates accountability through role separation

### Why Event-Based Enforcement vs. Permission-Only?

**Rationale**: Defense in depth
- JCR permissions alone can be bypassed (direct property modification)
- Event listener provides real-time validation
- Can enforce business logic beyond simple permissions
- Allows for graceful error messages vs. silent permission denial

### Why Store State on Content vs. Workflow Instance?

**Rationale**: State survives workflow lifecycle
- Content keeps approval state even if workflow is deleted
- Enables post-publish auditing
- Prevents republishing after workflow completion
- State is query-able via JCR SQL

### Why Auto-Launch on Creation vs. Manual Request?

**Rationale**: Ensures compliance by default
- Removes risk of forgetting to request approval
- Enforces workflow for all Spanish content
- Reduces training burden on editors
- Prevents accidental bypass

---

## Future Enhancements

### Planned Features

1. **Email Notifications**
   - Send email to reviewers when content submitted
   - Send email to editors when reviewed
   - Configurable email templates

2. **SLA Tracking**
   - Track time in each workflow state
   - Alert on overdue reviews
   - Dashboard for workflow metrics

3. **Batch Approval**
   - Approve multiple pages simultaneously
   - Bulk operations UI in workflow inbox

4. **Version Comparison**
   - Show diff between submitted and previous version
   - Help reviewers identify changes

5. **Delegation**
   - Allow reviewers to delegate reviews
   - Out-of-office workflow routing

6. **Localization**
   - Extend to other language paths (French, German, etc.)
   - Shared configuration for multi-language workflows

---

## Appendix

### File Locations

**Java Components**:
- `core/src/main/java/com/larasoft/core/workflows/`
  - `WorkflowEnforcementProcess.java`
  - `RequestApprovalProcess.java`
  - `ApprovalMarkingProcess.java`
  - `RequestChangesProcess.java`
  - `WorkflowEnforcementService.java`
  - `SpanishWorkflowConfig.java`
- `core/src/main/java/com/larasoft/core/servlets/`
  - `RequestSpanishApprovalServlet.java`

**Workflow Models**:
- `ui.content/src/main/content/jcr_root/conf/global/settings/workflow/models/spanish-content-approval/.content.xml`
- `ui.content/src/main/content/jcr_root/var/workflow/models/spanish-content-approval.xml`

**Launcher Configuration**:
- `ui.content/src/main/content/jcr_root/conf/global/settings/workflow/launcher/config/spanish-content-launcher/.content.xml`

**Access Control**:
- `ui.apps/src/main/content/jcr_root/apps/larasoft/actools/access.yaml`

**Package Filter**:
- `ui.content/src/main/content/META-INF/vault/filter.xml`

### Key Workflow Properties

| Workflow Property | Location | Purpose |
|------------------|----------|---------|
| `workflowState` | `jcr:content` | Tracks approval lifecycle state |
| `workflowId` | `jcr:content` | References workflow instance |
| `workflowSubmittedBy` | `jcr:content` | Records submitting user |
| `workflowSubmittedDate` | `jcr:content` | Records submission timestamp |
| `workflowApprovedBy` | `jcr:content` | Records approving user |
| `workflowApprovedDate` | `jcr:content` | Records approval timestamp |
| `workflowFeedback` | `jcr:content` | Stores reviewer feedback |
| `workflowApprovalComment` | `jcr:content` | Stores approval comment |
| `workflowSubmissionComment` | `jcr:content` | Stores submission comment |

### Glossary

- **Editor**: Member of `editor-spanish` group; creates and publishes Spanish content
- **Reviewer**: Member of `reviewer-spanish` group; approves or requests changes
- **Workflow State**: Property indicating where content is in approval lifecycle
- **Launcher**: AEM component that auto-starts workflows based on events
- **Workflow Model**: Visual/XML definition of workflow steps and routing
- **Process Step**: Java class that executes business logic during workflow
- **Participant Step**: Workflow step requiring human action/decision
- **Event Handler**: Java service listening for system events (e.g., replication)
- **Service User**: Technical user with elevated JCR permissions for system operations

---

## Support and Contact

### For Issues

- **Workflow Issues**: Contact AEM Administrator
- **Permission Issues**: Contact Security Team
- **Content Issues**: Contact Spanish Content Manager
- **System Issues**: Contact IT Support

### Documentation Updates

- **Last Updated**: November 16, 2025
- **Version**: 1.0
- **Maintained By**: AEM Development Team
- **Review Cycle**: Quarterly

---

**End of Documentation**

