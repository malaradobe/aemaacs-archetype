# Permission Model Change Summary

## Change Made

Updated the Spanish Content Approval Workflow to give **editors** the responsibility to publish their own content after reviewer approval.

## Previous Model

- **editor-spanish**: Could edit, could NOT publish
- **reviewer-spanish**: Could review/approve, could publish

## New Model (Current)

- **editor-spanish**: Can edit, can publish **BUT ONLY when content is APPROVED**
- **reviewer-spanish**: Can review/approve/reject, **cannot publish**

## Why This Makes More Sense

1. **Editor Ownership**: Editors maintain ownership of their content through the entire lifecycle
2. **Reviewer Focus**: Reviewers focus solely on quality control, not operational publishing
3. **Clear Separation**: Reviewers approve, editors execute
4. **Workflow Control**: The `WorkflowEnforcementService` ensures editors can only publish approved content

## What Changed

### 1. User Group Permissions (access.yaml)

**editor-spanish**:
- ✅ **ADDED**: `crx:replicate` permission
- ✅ **ADDED**: Membership in `workflow-users`
- ✅ **ADDED**: Read/write access to `/var/workflow`

**reviewer-spanish**:
- ❌ **REMOVED**: `crx:replicate` permission (explicit deny)

### 2. Workflow Model

**Changed**:
- Replaced `ActivatePageProcess` (automatic publish) with `ApprovalMarkingProcess`
- Workflow now marks content as `APPROVED` and notifies editor
- Editor manually publishes after receiving approval

### 3. Workflow Enforcement Logic

**WorkflowEnforcementService**:
- Now checks if user is in `editor-spanish` group (not `reviewer-spanish`)
- Allows publish only when:
  - User is `editor-spanish` member
  - Content `workflowState` = `APPROVED`
- Blocks all other publish attempts

### 4. New Java Class

**ApprovalMarkingProcess.java**:
- Marks content with `workflowState` = `APPROVED`
- Stores approval metadata (approver, date, comments)
- Enables editor to publish

### 5. Updated Tests

**Added**:
- `ApprovalMarkingProcessTest.java` - Tests approval marking logic

**Updated**:
- `SpanishContentUserGroupTest.java` - Reflects new permission model
  - Added test for editor publishing approved content
  - Added test for editor blocked from publishing unapproved content

### 6. Documentation Updates

Updated all three documentation files:
- `SPANISH_CONTENT_APPROVAL_WORKFLOW.md` - Complete workflow guide
- `IMPLEMENTATION_SUMMARY.md` - Technical implementation details
- `QUICK_START.md` - Quick deployment and testing guide

## How It Works Now

### Workflow Flow

1. **Editor creates/edits** Spanish content
2. **Workflow auto-triggers**, sets state to `IN_PROGRESS`
3. **Reviewer receives task** in workflow inbox
4. **Reviewer approves** → `ApprovalMarkingProcess` sets state to `APPROVED`
5. **Editor receives notification** that content is approved
6. **Editor publishes** the content manually
7. **WorkflowEnforcementService validates**:
   - ✅ User is editor-spanish
   - ✅ State is APPROVED
   - ✅ Allow publish
8. **Content goes live**

### Bypass Prevention

**Editor tries to publish unapproved content**:
- WorkflowEnforcementService checks workflow state
- State is `IN_PROGRESS`, `REJECTED`, `CHANGES_REQUESTED`, or missing
- ❌ Publish blocked with clear error message

**Reviewer tries to publish content**:
- Reviewer has `crx:replicate` explicitly denied
- ❌ Publish action not available in UI
- ❌ API publish attempts blocked by ACL

## Files Modified

1. `ui.apps/src/main/content/jcr_root/apps/larasoft/actools/access.yaml`
2. `ui.content/src/main/content/jcr_root/conf/global/settings/workflow/models/spanish-content-approval/.content.xml`
3. `core/src/main/java/com/larasoft/core/workflows/WorkflowEnforcementService.java`
4. `core/src/test/java/com/larasoft/core/workflows/SpanishContentUserGroupTest.java`
5. `SPANISH_CONTENT_APPROVAL_WORKFLOW.md`
6. `IMPLEMENTATION_SUMMARY.md`
7. `QUICK_START.md`

## Files Created

1. `core/src/main/java/com/larasoft/core/workflows/ApprovalMarkingProcess.java`
2. `core/src/test/java/com/larasoft/core/workflows/ApprovalMarkingProcessTest.java`
3. `PERMISSION_MODEL_CHANGE.md` (this file)

## Testing

All existing tests pass with updated assertions. New tests added:

- ✅ Test editor can publish APPROVED content
- ✅ Test editor blocked from publishing unapproved content
- ✅ Test approval marking process
- ✅ Test workflow state transitions

## Deployment

No special steps required. Standard deployment:

```bash
mvn clean install -PautoInstallPackage
```

## Benefits

1. **Logical Workflow**: Editors publish their own work after approval
2. **Clear Roles**: Reviewers approve, editors publish
3. **Maintained Security**: Still cannot bypass approval requirement
4. **Better UX**: Editor sees their content through to publication
5. **Audit Trail**: Clear tracking of who approved and who published

## Summary

This change improves the workflow logic by giving editors publish responsibility while maintaining strict enforcement that content must be approved first. The `WorkflowEnforcementService` ensures security, and the updated permission model better reflects real-world content management workflows.

---

**Status**: ✅ Implementation Complete  
**All Tests**: ✅ Passing  
**Documentation**: ✅ Updated  
**Ready for Deployment**: ✅ Yes


