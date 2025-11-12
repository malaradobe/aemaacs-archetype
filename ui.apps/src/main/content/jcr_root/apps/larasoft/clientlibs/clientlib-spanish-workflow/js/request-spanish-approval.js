(function (document, $, Granite) {
    "use strict";

    var SPANISH_CONTENT_PATH_PATTERN = "/content/larasoft/us/es";
    var SERVLET_PATH = "/bin/larasoft/workflow/request-spanish-approval";
    var EDITOR_SPANISH_GROUP = "editor-spanish";

    /**
     * Check if current page is under Spanish content path
     */
    function isSpanishContentPage() {
        var pagePath = Granite.author.page.path;
        return pagePath && pagePath.indexOf(SPANISH_CONTENT_PATH_PATTERN) === 0;
    }

    /**
     * Check if user is in editor-spanish group
     */
    function isEditorSpanishUser(callback) {
        $.ajax({
            url: "/libs/granite/security/currentuser.json",
            type: "GET",
            success: function(data) {
                var userGroups = data.authorizableId ? data.memberOf : [];
                var isMember = userGroups.indexOf(EDITOR_SPANISH_GROUP) !== -1;
                callback(isMember);
            },
            error: function() {
                callback(false);
            }
        });
    }

    /**
     * Request approval workflow for current page
     */
    function requestApproval() {
        var pagePath = Granite.author.page.path;
        
        var ui = $(window).adaptTo("foundation-ui");
        
        ui.wait();

        $.ajax({
            url: SERVLET_PATH,
            type: "POST",
            data: {
                pagePath: pagePath
            },
            success: function(response) {
                ui.clearWait();
                
                if (response.success) {
                    ui.notify(null, "Approval workflow initiated successfully for: " + response.pageTitle, "success");
                    
                    // Refresh the page editor to show workflow status
                    setTimeout(function() {
                        Granite.author.ContentFrame.reload();
                    }, 1000);
                } else {
                    ui.alert("Error", response.error || "Failed to initiate approval workflow", "error");
                }
            },
            error: function(xhr) {
                ui.clearWait();
                var errorMsg = "Failed to request approval";
                
                try {
                    var response = JSON.parse(xhr.responseText);
                    errorMsg = response.error || errorMsg;
                } catch (e) {
                    errorMsg = xhr.statusText || errorMsg;
                }
                
                ui.alert("Error", errorMsg, "error");
            }
        });
    }

    /**
     * Initialize the "Request Approval" action
     */
    $(document).on("cq-layer-activated", function(event) {
        // Only activate in Edit mode
        if (event.layer === "Edit") {
            
            // Check if current page is Spanish content
            if (!isSpanishContentPage()) {
                return;
            }

            // Check if user is in editor-spanish group
            isEditorSpanishUser(function(isMember) {
                if (!isMember) {
                    return;
                }

                // Add the "Request Approval" button to the page toolbar
                addRequestApprovalButton();
            });
        }
    });

    /**
     * Add "Request Approval" button to the page info toolbar
     */
    function addRequestApprovalButton() {
        // Wait for page toolbar to be available
        var checkToolbar = setInterval(function() {
            var pageInfoButton = $('button[data-foundation-collection-action*="page-properties"]').first();
            
            if (pageInfoButton.length) {
                clearInterval(checkToolbar);
                
                // Check if button already exists
                if ($('#request-spanish-approval-action').length === 0) {
                    // Create the request approval button
                    var requestApprovalButton = $('<button>', {
                        'id': 'request-spanish-approval-action',
                        'class': 'coral-Button coral-Button--square coral-Button--secondary',
                        'type': 'button',
                        'title': 'Request Spanish Content Approval',
                        'aria-label': 'Request Approval'
                    }).append(
                        $('<coral-icon>', {
                            'icon': 'workflow',
                            'size': 'S'
                        })
                    ).append(
                        $('<coral-button-label>').text('Request Approval')
                    );

                    // Add click handler
                    requestApprovalButton.on('click', function(e) {
                        e.preventDefault();
                        requestApproval();
                    });

                    // Insert button in the toolbar (after page properties button)
                    pageInfoButton.parent().after(
                        $('<div>', {'class': 'editor-toolbar-item'}).append(requestApprovalButton)
                    );
                }
            }
        }, 100);

        // Clear the interval after 5 seconds to prevent infinite loop
        setTimeout(function() {
            clearInterval(checkToolbar);
        }, 5000);
    }

})(document, Granite.$, Granite);



