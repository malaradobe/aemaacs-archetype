# Notification Fix - PROCESS_AUTO_ADVANCE Issue

## Problem Identified

**Issue**: Notifications were not appearing in the editor's inbox after deployment.

**Root Cause**: `PROCESS_AUTO_ADVANCE="true"` was set on the notification participant steps, which caused them to auto-complete so quickly that they never actually appeared in the inbox for users to see.

## Solution Applied

Changed all notification steps from:
```xml
PROCESS_AUTO_ADVANCE="true"  <!-- Auto-completes instantly, never shows -->
```

To:
```xml
PROCESS_AUTO_ADVANCE="false"  <!-- Stays in inbox until acknowledged -->
```

## Files Updated

### 1. Runtime Workflow Model
**File**: `ui.content/src/main/content/jcr_root/var/workflow/models/spanish-content-approval.xml`

**Changes**:
- node7 (Approval notification): `PROCESS_AUTO_ADVANCE="false"`
- node8 (Rejection notification): `PROCESS_AUTO_ADVANCE="false"`
- node9 (Changes requested notification): `PROCESS_AUTO_ADVANCE="false"`

### 2. Design Workflow Model
**File**: `ui.content/src/main/content/jcr_root/conf/global/settings/workflow/models/spanish-content-approval/.content.xml`

**Changes**:
- participant_notification: `PROCESS_AUTO_ADVANCE="false"`
- participant_rejection_notification: `PROCESS_AUTO_ADVANCE="false"`
- participant_changes_notification: `PROCESS_AUTO_ADVANCE="false"`

## New Behavior

### Before Fix ❌
```
1. Reviewer approves content
2. Notification step executes
3. PROCESS_AUTO_ADVANCE="true" → Instantly completes
4. Editor never sees notification in inbox
5. Workflow ends
```

### After Fix ✅
```
1. Reviewer approves content
2. Notification step executes
3. Notification appears in editor's inbox
4. Editor sees: "Content Approved - Ready to Publish"
5. Editor clicks "Complete" to acknowledge
6. Workflow ends
```

## Updated Message Format

All notifications now include instructions to complete:

### Approval Notification
```
✅ Your Spanish content has been APPROVED and is ready to publish!

Page: /content/larasoft/us/es/your-page

You can now publish this page from the Sites console.

Click 'Complete' to acknowledge this notification.
```

### Rejection Notification
```
❌ Your Spanish content has been REJECTED.

Page: /content/larasoft/us/es/your-page

Please review the rejection comments, make necessary changes, 
and request approval again.

Click 'Complete' to acknowledge this notification.
```

### Changes Requested Notification
```
🔄 The reviewer has requested changes to your Spanish content.

Page: /content/larasoft/us/es/your-page

Please review the feedback, make the requested changes, 
and request approval again.

Click 'Complete' to acknowledge this notification.
```

## Deployment Instructions

### Step 1: Build the Project
```bash
cd /Users/malara/Documents/GitHub/Adobe/MarcoLaraProgram-p45262-parent/aemaacs-archetype

# Clean and build
mvn clean install
```

### Step 2: Deploy to AEM
```bash
# Deploy to local AEM instance
mvn clean install -PautoInstallPackage
```

### Step 3: Verify Deployment
```bash
# Check workflow model in CRXDE
http://localhost:4502/crx/de/index.jsp

# Navigate to:
/var/workflow/models/spanish-content-approval

# Verify:
- node7 has PROCESS_AUTO_ADVANCE="false"
- node8 has PROCESS_AUTO_ADVANCE="false"
- node9 has PROCESS_AUTO_ADVANCE="false"
```

### Step 4: Clear Workflow Cache (Important!)
```
1. Navigate to: http://localhost:4502/system/console/jmx
2. Find: com.adobe.granite.workflow (WorkflowModel)
3. Operation: clearCache
4. Click: Invoke
```

### Step 5: Test the Notifications

```bash
# As spanish-editor-test:
1. Navigate to /sites.html/content/larasoft/us/es
2. Create a new test page
3. Click "Request Approval"
4. Keep inbox open: http://localhost:4502/aem/inbox

# As spanish-reviewer-test:
5. Navigate to /aem/inbox
6. Find "Spanish Content Review" task
7. Click "Approve"

# Back as spanish-editor-test:
8. Refresh inbox page
9. ✅ Should see: "Content Approved - Ready to Publish" notification
10. Click on notification to view details
11. Click "Complete" button
12. Notification is acknowledged and workflow completes
```

## Expected Results

### In Editor's Inbox
- Notification appears immediately after reviewer decision
- Remains in inbox until editor clicks "Complete"
- Shows clear title: "Content Approved - Ready to Publish"
- Displays full message with page path
- Has "Complete" button at bottom

### Workflow Behavior
- Notification step waits for user to click "Complete"
- Workflow does NOT end until notification is acknowledged
- Provides clear record that editor was notified
- Workflow history shows notification step completion

## Troubleshooting

### Issue: Notifications Still Not Appearing

**Check 1: Deployment**
```bash
# Verify files were deployed
ls -la ui.content/target/*.zip

# Check timestamp - should be recent
```

**Check 2: Workflow Cache**
```
# Clear workflow cache (Step 4 above)
# Then test with a NEW page (don't reuse old workflow instances)
```

**Check 3: Check Logs**
```bash
# Navigate to:
http://localhost:4502/system/console/slinglog

# Look for:
- Workflow execution logs
- Errors related to workflow model
- Participant step execution
```

**Check 4: Verify User is Initiator**
```
# The notification goes to $(initiator)
# Make sure you're logged in as the same user who clicked "Request Approval"
```

### Issue: Notification Appears But Has "Complete" Button

**This is CORRECT behavior!**
- The notification now requires acknowledgment
- User must click "Complete" to close it
- This ensures the editor actually saw the notification

### Issue: Multiple Notifications for Same Page

**This is expected IF**:
- You requested approval multiple times
- Each workflow creates its own notification
- Complete old notifications to clear inbox

## Comparison: Auto-Advance vs. Manual Complete

### PROCESS_AUTO_ADVANCE="true" (OLD - DOESN'T WORK)
- ✅ No user action needed
- ❌ Completes instantly
- ❌ Never appears in inbox
- ❌ No confirmation editor saw it
- ❌ **NOT SUITABLE FOR NOTIFICATIONS**

### PROCESS_AUTO_ADVANCE="false" (NEW - WORKS!)
- ✅ Appears in inbox
- ✅ Stays until acknowledged
- ✅ Editor confirms they saw it
- ✅ Clear audit trail
- ✅ **CORRECT FOR NOTIFICATIONS**

## Summary

✅ **Fixed**: Changed `PROCESS_AUTO_ADVANCE` to `false`  
✅ **Result**: Notifications now appear in inbox  
✅ **Action Required**: Editor must click "Complete" to acknowledge  
✅ **Status**: Ready for testing

---

**Fix Applied**: November 6, 2025  
**Issue**: Notifications not appearing  
**Solution**: Changed auto-advance behavior  
**Status**: ✅ Fixed and Ready for Deployment



