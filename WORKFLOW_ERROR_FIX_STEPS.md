# Workflow Error Fix - Action Required

## Problem
You're seeing the error on an **existing workflow instance** (`spanish-content-approval_3`) that was created before the fix. This old instance still references the broken workflow model.

## Solution Steps

### Step 1: Complete or Terminate Old Workflow Instances

**Option A: Complete the workflow through the UI**
1. Go to: http://localhost:4502/aem/inbox
2. Find the workflow task for `spanish-content-approval_3`
3. Select any action (Approve/Reject/Request Changes) to complete it
4. OR click "Terminate" to force-close it

**Option B: Terminate via JCR**
1. Go to: http://localhost:4502/crx/de/index.jsp
2. Navigate to: `/var/workflow/instances/server0/2025-11-05/spanish-content-approval_3`
3. Delete the entire node
4. Save

**Option C: Terminate All Running Instances (Clean Slate)**
1. Go to: http://localhost:4502/etc/workflow/instances.html
2. Find all "Spanish Content Approval Workflow" instances with status "RUNNING"
3. Select them and click "Terminate"

### Step 2: Deploy the Fix

```bash
cd /Users/malara/Documents/GitHub/Adobe/MarcoLaraProgram-p45262-parent/aemaacs-archetype
mvn clean install -PautoInstallPackage
```

### Step 3: Sync the Workflow Model

**Important**: This ensures the runtime workflow model is updated

1. Go to: http://localhost:4502/libs/cq/workflow/admin/console/content/models.html
2. Find "Spanish Content Approval Workflow" in the list
3. Click on the workflow name to select it
4. Click the **"Sync"** button in the toolbar (top of page)
5. Wait for confirmation message: "Workflow Model synced successfully"

### Step 4: Start a New Workflow Test

**Create a new workflow instance:**

1. As `editor-spanish` user, go to: http://localhost:4502/sites.html/content/larasoft/es
2. Create a new page OR edit an existing page
3. Save the changes
4. Workflow should auto-trigger
5. As `reviewer-spanish`, go to: http://localhost:4502/aem/inbox
6. Open the **NEW** workflow task
7. **Expected**: You should see "Approve", "Reject", and "Request Changes" buttons WITHOUT errors

### Step 5: Verify the Fix

Check that the error is gone:

1. Open the new workflow task in the inbox
2. The task detail panel should load without errors
3. Check AEM logs at: http://localhost:4502/system/console/slinglog
4. Filter by "NullPointerException" or "RuleEngineAdminImpl"
5. **Expected**: No new errors related to workflow rules

---

## Why This Happened

- **Old Workflow Instances**: Workflow instances store a snapshot of the workflow model at creation time
- **Model Changes**: Changes to the workflow model only affect NEW instances
- **Sync Required**: The editable model (`/conf/...`) must be synced to the runtime model (`/var/...`)

## Quick Check: Is the Model Synced?

View the runtime model to verify the fix was applied:

1. Go to: http://localhost:4502/crx/de/index.jsp
2. Navigate to: `/var/workflow/models/spanish-content-approval`
3. Open the `spanish-content-approval.xml` file
4. Check line ~112: Should **NOT** have `rule="approve"`
5. Should look like this:
   ```xml
   <node2_to_node3_approve
       jcr:primaryType="cq:WorkflowTransition"
       from="node2"
       to="node3">
   ```

## Troubleshooting

### Still seeing the error on NEW workflows?

1. **Clear Browser Cache**: Hard refresh with Cmd+Shift+R (Mac) or Ctrl+Shift+R (Windows)
2. **Check Sync**: Verify the sync completed successfully
3. **Check Runtime Model**: Navigate to `/var/workflow/models/spanish-content-approval.xml` in CRXDE and verify no `rule` attributes exist
4. **Restart AEM**: As a last resort, restart the AEM instance

### How to verify it's a NEW instance vs OLD?

Check the workflow instance path:
- OLD: `/var/workflow/instances/server0/2025-11-05/spanish-content-approval_3` (or _1, _2)
- NEW: Will have a higher number or different date

---

## Summary

✅ **Files Fixed**: 
- `ui.content/.../var/workflow/models/spanish-content-approval.xml`
- `ui.content/.../conf/global/settings/workflow/models/spanish-content-approval/.content.xml`

✅ **What Changed**: Removed `rule` attributes from workflow transitions and updated actions to use `route` instead

⚠️ **Action Required**: 
1. Terminate old workflow instances
2. Deploy the fix
3. Sync the workflow model
4. Test with a NEW workflow instance

---

**After completing these steps, the error should be resolved for all NEW workflow instances.**

