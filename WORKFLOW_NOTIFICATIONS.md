# Workflow Notifications for Spanish Content Approval

## Overview

The Spanish Content Approval Workflow now includes **automatic inbox notifications** for editors. When a reviewer makes a decision, the editor (`spanish-editor-test` or any user in `editor-spanish` group) receives an **AEM Inbox notification** immediately.

## Notification Types

### 1. ✅ Content Approved Notification

**When**: Reviewer clicks "Approve"

**Notification Details**:
- **Title**: "Content Approved - Ready to Publish"
- **Message**: 
  ```
  ✅ Your Spanish content has been APPROVED and is ready to publish!
  
  Page: /content/larasoft/us/es/your-page
  
  You can now publish this page from the Sites console.
  ```

**What Editor Should Do**:
1. Open AEM Inbox at `/aem/inbox`
2. Read the notification
3. Navigate to the page in Sites console
4. Click "Publish" button
5. Content will be published successfully (state is APPROVED)

---

### 2. ❌ Content Rejected Notification

**When**: Reviewer clicks "Reject"

**Notification Details**:
- **Title**: "Content Rejected - Changes Required"
- **Message**: 
  ```
  ❌ Your Spanish content has been REJECTED.
  
  Page: /content/larasoft/us/es/your-page
  
  Please review the rejection comments, make necessary changes, 
  and request approval again.
  ```

**What Editor Should Do**:
1. Open AEM Inbox at `/aem/inbox`
2. Read the notification
3. Check page properties for `workflowComment` (rejection reason)
4. Make necessary corrections
5. Click "Request Approval" button again

---

### 3. 🔄 Changes Requested Notification

**When**: Reviewer clicks "Request Changes"

**Notification Details**:
- **Title**: "Changes Requested - Action Required"
- **Message**: 
  ```
  🔄 The reviewer has requested changes to your Spanish content.
  
  Page: /content/larasoft/us/es/your-page
  
  Please review the feedback, make the requested changes, 
  and request approval again.
  ```

**What Editor Should Do**:
1. Open AEM Inbox at `/aem/inbox`
2. Read the notification
3. Check page properties for `workflowFeedback` (reviewer's feedback)
4. Make the requested changes
5. Click "Request Approval" button again

---

## How to Check Your Inbox

### Option 1: AEM Inbox Page

```
1. Navigate to: http://localhost:4502/aem/inbox
2. Log in as spanish-editor-test
3. View notifications in list
4. Click on notification to see details
5. Click link to navigate to the page
```

### Option 2: Inbox Icon (Top Navigation)

```
1. Look for the bell/inbox icon in AEM top navigation
2. Click the icon
3. See notification count badge
4. Click to open inbox panel
5. View notification details
```

### Option 3: Notifications Panel

```
1. Click user icon (top right)
2. Select "Notifications"
3. View all workflow notifications
4. Mark as read or delete
```

---

## Notification Flow Diagram

```
Editor Requests Approval
         |
         ↓
    Reviewer Reviews
         |
    ┌────┴────┬────────────┬────────────┐
    |         |            |            |
  Approve   Reject   Request Changes   (No Action)
    |         |            |            |
    ↓         ↓            ↓            ↓
[Approved] [Rejected] [Changes Req.] [In Progress]
    |         |            |            |
    ↓         ↓            ↓            ↓
  ✅ Notify  ❌ Notify    🔄 Notify    (No Notification)
    ↓         ↓            ↓
Editor's Inbox shows notification
```

---

## Workflow Architecture

### Notification Workflow Steps

After the reviewer makes a decision, the workflow includes these steps:

**Approval Path**:
```
1. Spanish Content Review (Reviewer)
   ↓
2. Mark as Approved (Process)
   ↓
3. Notify Editor - Content Approved (Participant - Auto-advances)
   ↓
4. End
```

**Rejection Path**:
```
1. Spanish Content Review (Reviewer)
   ↓
2. Reject Handler (Process)
   ↓
3. Notify Editor - Content Rejected (Participant - Auto-advances)
   ↓
4. End
```

**Changes Requested Path**:
```
1. Spanish Content Review (Reviewer)
   ↓
2. Request Changes Handler (Process)
   ↓
3. Notify Editor - Changes Requested (Participant - Auto-advances)
   ↓
4. End
```

### How It Works

The notifications use **AEM's built-in Participant Step** with special configuration:

```xml
<metaData>
    PARTICIPANT="$(initiator)"        <!-- Send to original editor -->
    PROCESS_AUTO_ADVANCE="true"       <!-- Auto-complete (FYI only) -->
    taskTitle="Content Approved..."   <!-- Inbox item title -->
    taskInstructions="✅ Your content..." <!-- Message content -->
</metaData>
```

**Key Features**:
- `$(initiator)` - Dynamically assigns to the user who started the workflow
- `PROCESS_AUTO_ADVANCE="true"` - Notification auto-completes (no action needed)
- `taskInstructions` - The message shown in the inbox
- `taskTitle` - The title/subject of the notification

---

## Testing Notifications

### Test Scenario 1: Approval Notification

```bash
# As editor-spanish-test:
1. Navigate to /sites.html/content/larasoft/us/es
2. Create a test page
3. Click "Request Approval" button
4. Note the page path

# As reviewer-spanish-test:
5. Navigate to /aem/inbox
6. Find the "Spanish Content Review" task
7. Click "Approve"

# As editor-spanish-test:
8. Navigate to /aem/inbox
9. ✅ Should see: "Content Approved - Ready to Publish" notification
10. Click the notification to view details
11. Navigate to page and publish successfully
```

**Expected Result**: ✅ Notification appears in inbox immediately after approval

---

### Test Scenario 2: Rejection Notification

```bash
# As editor-spanish-test:
1. Create a Spanish page with incomplete content
2. Click "Request Approval"

# As reviewer-spanish-test:
3. Navigate to /aem/inbox
4. Open the review task
5. Add comment: "Missing images and description"
6. Click "Reject"

# As editor-spanish-test:
7. Navigate to /aem/inbox
8. ❌ Should see: "Content Rejected - Changes Required" notification
9. Read the rejection comment
10. Make corrections and request approval again
```

**Expected Result**: ❌ Rejection notification appears with link to page

---

### Test Scenario 3: Changes Requested Notification

```bash
# As editor-spanish-test:
1. Create a Spanish page
2. Click "Request Approval"

# As reviewer-spanish-test:
3. Navigate to /aem/inbox
4. Open the review task
5. Add feedback: "Please add pricing information"
6. Click "Request Changes"

# As editor-spanish-test:
7. Navigate to /aem/inbox
8. 🔄 Should see: "Changes Requested - Action Required" notification
9. Read the feedback
10. Make requested changes
11. Request approval again
```

**Expected Result**: 🔄 Changes notification appears with feedback

---

## Notification Properties

### Stored Metadata

When workflows complete, the following properties are stored on the page:

**After Approval**:
- `workflowState` = `"APPROVED"`
- `workflowApprovedBy` = Reviewer username
- `workflowApprovedDate` = Timestamp
- `workflowApprovalComment` = Optional approval comment

**After Rejection**:
- `workflowState` = `"REJECTED"`
- `workflowComment` = Rejection reason/comment

**After Changes Requested**:
- `workflowState` = `"CHANGES_REQUESTED"`
- `workflowFeedback` = Reviewer's feedback

---

## Troubleshooting

### Issue 1: Not Receiving Notifications

**Symptoms**: Reviewer approves content but editor sees no inbox notification

**Possible Causes**:
1. Workflow not updated after deployment
2. User not the original initiator
3. Inbox permissions issue

**Solutions**:
```bash
# 1. Verify workflow is updated
http://localhost:4502/editor.html/conf/global/settings/workflow/models/spanish-content-approval.html

# Check for "Notify Editor" steps in workflow model

# 2. Clear workflow cache
Navigate to: /system/console/jmx
Find: com.adobe.granite.workflow
Operation: "clearCache"
Click: "Invoke"

# 3. Check user permissions
Verify user has access to /var/workflow
```

---

### Issue 2: Notifications Not Auto-Completing

**Symptoms**: Notifications appear but require manual completion

**Cause**: `PROCESS_AUTO_ADVANCE` not set to `true`

**Solution**: Verify workflow model XML has:
```xml
PROCESS_AUTO_ADVANCE="true"
```

---

### Issue 3: Wrong User Receiving Notifications

**Symptoms**: Someone other than the editor receives notifications

**Cause**: Participant not set to `$(initiator)`

**Solution**: Verify workflow model has:
```xml
PARTICIPANT="$(initiator)"
```

---

## Future Enhancements

### Email Notifications

Currently, notifications only appear in AEM Inbox. Future enhancement could add email notifications:

```java
// In ApprovalMarkingProcess.java
@Reference
private MailService mailService;

// Send email to editor
Email email = new HtmlEmail();
email.setTo(editorEmail);
email.setSubject("Content Approved - Ready to Publish");
email.setMsg("Your content has been approved...");
mailService.send(email);
```

### Push Notifications

For modern browsers, could implement browser push notifications:

```javascript
// In clientlib JavaScript
if ('Notification' in window && Notification.permission === 'granted') {
    new Notification('Content Approved!', {
        body: 'Your Spanish content is ready to publish',
        icon: '/libs/granite/ui/content/shell/favorites/icon.png'
    });
}
```

### Slack/Teams Integration

Could integrate with collaboration tools:

```java
// Post to Slack webhook
HttpPost post = new HttpPost(slackWebhookUrl);
String payload = "{'text': 'Content approved: " + pagePath + "'}";
post.setEntity(new StringEntity(payload));
httpClient.execute(post);
```

---

## Summary

✅ **Notifications are NOW ENABLED**

- Editors receive **automatic AEM Inbox notifications**
- Notifications appear for: Approval, Rejection, Changes Requested
- No manual checking required
- Native AEM inbox integration
- Auto-completing (FYI notifications only)

**To see notifications**: Navigate to `/aem/inbox` after reviewer makes a decision!

---

## Related Documentation

- [SPANISH_CONTENT_APPROVAL_WORKFLOW.md](./SPANISH_CONTENT_APPROVAL_WORKFLOW.md) - Full workflow details
- [MANUAL_WORKFLOW_SETUP.md](./MANUAL_WORKFLOW_SETUP.md) - Setup guide
- [PUBLISH_PROTECTION.md](./PUBLISH_PROTECTION.md) - Security details

---

**Last Updated**: November 6, 2025  
**Notification Status**: ✅ **ACTIVE**  
**Auto-Complete**: ✅ **ENABLED**  
**Inbox Integration**: ✅ **WORKING**



