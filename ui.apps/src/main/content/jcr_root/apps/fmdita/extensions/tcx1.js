var __defProp = Object.defineProperty;
var __defProps = Object.defineProperties;
var __getOwnPropDescs = Object.getOwnPropertyDescriptors;
var __getOwnPropSymbols = Object.getOwnPropertySymbols;
var __hasOwnProp = Object.prototype.hasOwnProperty;
var __propIsEnum = Object.prototype.propertyIsEnumerable;
var __defNormalProp = (obj, key, value) => key in obj ? __defProp(obj, key, { enumerable: true, configurable: true, writable: true, value }) : obj[key] = value;
var __spreadValues = (a2, b2) => {
  for (var prop in b2 || (b2 = {}))
    if (__hasOwnProp.call(b2, prop))
      __defNormalProp(a2, prop, b2[prop]);
  if (__getOwnPropSymbols)
    for (var prop of __getOwnPropSymbols(b2)) {
      if (__propIsEnum.call(b2, prop))
        __defNormalProp(a2, prop, b2[prop]);
    }
  return a2;
};
var __spreadProps = (a2, b2) => __defProps(a2, __getOwnPropDescs(b2));
var __async = (__this, __arguments, generator) => {
  return new Promise((resolve, reject) => {
    var fulfilled = (value) => {
      try {
        step(generator.next(value));
      } catch (e2) {
        reject(e2);
      }
    };
    var rejected = (value) => {
      try {
        step(generator.throw(value));
      } catch (e2) {
        reject(e2);
      }
    };
    var step = (x2) => x2.done ? resolve(x2.value) : Promise.resolve(x2.value).then(fulfilled, rejected);
    step((generator = generator.apply(__this, __arguments)).next());
  });
};
var e = ((e2) => (e2.APPEND = "append", e2.PREPEND = "prepend", e2.REPLACE = "replace", e2))(e || {});
const t = { id: "review_comment", view: { items: [{ component: "label", label: "@extraProps.commentUniqId", extraclass: "commentUniqId", target: { key: "extraclass", value: "user-image", viewState: "prepend" } }, { component: "div", extraclass: "user-info", items: [{ component: "label", label: "@extraProps.userInfo", extraclass: "reviewer-name" }, { component: "button", icon: "email", extraclass: "mailto-icon", "on-click": "openMailTo" }], target: { key: "extraclass", value: "reviewer-name", viewState: "replace" } }, { component: "div", extraclass: "comment-details", items: [{ component: "div", extraclass: "comment-type-text", items: [{ component: "label", label: "Comment Type: ", extraclass: "severity-label" }, { component: "label", label: "@extraProps.severity" }] }, { component: "div", extraclass: "comment-rationale", items: [{ component: "label", label: "Comment Rationale: ", extraclass: "comment-rationale-label" }, { component: "label", label: "@extraProps.commentRationale" }] }], target: { key: "id", value: "attachment_tiles", viewState: "prepend" } }, { component: "div", items: [{ component: "div", extraclass: "edit-comment-type", items: [{ component: "label", label: "Comment Type" }, { component: "comboBox", data: "@extraProps.labels", extraclass: "severity-combobox", multiple: false, placeholder: "", value: "@extraProps.severity", "on-change": "changeSeverity", "on-keyup": { name: "changeSeverity", eventArgs: { keys: ["ENTER"] } } }] }, { component: "div", extraclass: "edit-comment-rationale", items: [{ component: "label", label: "Comment Rationale" }, { component: "textarea", extraclass: "edit-textfield", id: "edit_comment_rationale", data: "@extraProps.commentRationale", "on-keyup": { name: "submitEditComment", eventArgs: { keys: ["ENTER"] } }, stopKeyPropagation: true }] }], target: { key: "class", value: "comment-block", viewState: "append" } }, { component: "button", icon: "MultipleAdd", variant: "action", quiet: true, extraclass: "hover-item", title: "Accept with Modifications", "on-click": "acceptWithModification", target: { key: "title", value: "Reject comment", viewState: "append" } }] }, controller: { init: function() {
  const e2 = tcx.commentStore.getComment(this.getValue("commentId"));
  this.setValue("extraProps", e2.extraProps), this.setValue("labels", ["None", "CRITICAL", "MAJOR", "SUBSTANTATIVE", "ADMINISTRATIVE"]);
}, sendAcceptWithModificationProps(e2) {
  this.next("updateExtraProps", e2);
}, changeSeverity: function(e2) {
  this.setValue("severity", e2.data), this.next("updateExtraProps", { severity: this.getValue("severity") });
}, changeCommentRationale: function() {
  this.next("updateExtraProps", { commentRationale: this.getValue("commentRationale") });
}, submitEditComment({ domEvent: e2 } = {}) {
  "Enter" === (e2 == null ? void 0 : e2.key) && this.setValue("commentRationale", _.trim(this.getValue("commentRationale"))), this.getValue("originalCommentRationale") !== this.getValue("commentRationale") && (this.setValue("originalCommentRationale", this.getValue("commentRationale")), this.next("changeCommentRationale"));
}, openMailTo() {
  const e2 = `mailto:${this.getValue("userEmail")}`;
  tcx.util.openLink(e2);
}, acceptWithModification() {
  tcx.eventHandler.next(tcx.eventHandler.KEYS.APP_SHOW_DIALOG, { id: "accept_with_modification_dialog", args: { onSuccess: (e2) => this.next("sendAcceptWithModificationProps", e2) } });
} } }, n = function(e2, t2) {
  const n2 = ["highlight", "strikethrough", "addcomment", "insertext"];
  _.each(e2, (e3) => {
    -1 !== _.findIndex(n2, (t3) => t3 === e3.eventType) && this.next("setCommentId", { event: e3, topicIndex: t2 });
  });
}, i = { id: "inline_review_panel", model: { deps: ["commentCount"] }, controller: { init: function() {
  this.setValue("commentCount", {}), tcx.model.subscribeVal(tcx.model.KEYS.REVIEW_DATA, (e2) => {
    for (let t2 of e2.topicsinReview)
      t2 = t2.toString(), tcx.commentStore.onProcessEvent(t2, (e3) => n.call(this, e3, t2));
  });
}, onNewCommentEvent(e2) {
  const t2 = _.get(e2, "events"), n2 = tcx.model.getValue(tcx.model.KEYS.REVIEW_CURR_TOPIC) || this.getValue("currTopicIndex") || "0", i2 = _.get(_.get(t2, n2), "0"), a2 = _.get(e2, "newComment"), o2 = _.get(e2, "newReply");
  (a2 || o2) && i2 && this.next("setUserInfo", i2);
}, setUserInfo(e2) {
  var t2;
  (t2 = e2.user, $.ajax({ url: "/bin/dxml/xmleditor/userinfo", data: { username: t2 }, success: (e3) => e3 })).done((t3) => {
    const n2 = { userFirstName: (t3 == null ? void 0 : t3.givenName) || "", userLastName: (t3 == null ? void 0 : t3.familyName) || "", userTitle: (t3 == null ? void 0 : t3.title) || "", userJobTitle: (t3 == null ? void 0 : t3.jobTitle) || "", userEmail: (t3 == null ? void 0 : t3.email) || "" }, i2 = `${n2.userFirstName} ${n2.userLastName}, ${n2.userJobTitle}`;
    "," === _.trim(i2) ? n2.userInfo = t3.displayName : n2.userInfo = i2;
    const a2 = __spreadProps(__spreadValues({}, e2), { extraProps: n2 });
    this.next("sendExtraProps", a2);
  });
}, setCommentId({ event: e2, topicIndex: t2 }) {
  var _a;
  const n2 = this.getValue("processingComments"), i2 = _.find(n2, { commentId: e2.commentId }), a2 = tcx.commentStore.getComment(e2.commentId), o2 = this.getValue("commentCount");
  if (_.has(this.getValue("commentCount"), t2) ? (o2[t2] += 1, this.setValue("commentCount", o2)) : o2[t2] = 1, a2) {
    this.setValue("commentCount", o2);
    const e3 = `${Number(t2) + 1}.${o2[t2]}`;
    a2.extraProps.set("commentUniqId", e3), (_a = i2 == null ? void 0 : i2.extraProps) == null ? void 0 : _a.set("commentUniqId", e3);
  }
} } }, a = { id: "topic_reviews", model: { deps: [] }, controller: __spreadProps(__spreadValues({}, i.controller), { init: function() {
  this.setValue("commentCount", {}), tcx.model.subscribeVal(tcx.model.KEYS.REVIEW_DATA, (e2) => {
    for (let t2 of e2.topicsinReview)
      t2 = t2.toString(), tcx.commentStore.onProcessEvent(t2, (e3) => n.call(this, e3, t2));
  });
} }) }, o = { id: "comment_reply", view: { items: [{ component: "div", extraclass: "user-info", items: [{ component: "label", label: "@extraProps.userInfo", extraclass: "user-name" }, { component: "button", icon: "email", extraclass: "mailto-icon", "on-click": "openMailTo" }], target: { key: "extraclass", value: "user-name", viewState: e.REPLACE } }] }, model: { deps: [] }, controller: { init: function() {
  const e2 = tcx.commentStore.getComment(this.getValue("commentId")).findReply(this.getValue("replyId"));
  this.setValue("extraProps", e2.extraProps);
}, openMailTo() {
  const e2 = `mailto:${this.getValue("userEmail")}`;
  tcx.util.openLink(e2);
} } }, l = { id: "accept_with_modification_dialog", view: { component: "dialog", header: { items: [{ component: "label", extraclass: "header", label: "Accept With Modifications" }] }, content: { items: [{ component: "div", extraclass: "revised-text", items: [{ component: "label", label: "Revised Text (Required)", extraclass: "revised-text-label" }, { component: "textarea", extraclass: "revised-text-textarea", data: "@extraProps.revisedText", stopKeyPropagation: true }] }, { component: "div", extraclass: "adjudication-rationale", items: [{ component: "label", label: "Adjudicator Comment Rationale (Required)", extraclass: "adjudication-rationale-label" }, { component: "textarea", extraclass: "adjudication-rationale-textarea", data: "@extraProps.adjudicationRationale", "on-keyup": { name: "", eventArgs: { keys: ["ENTER"] } }, stopKeyPropagation: true }] }] }, footer: { items: [{ component: "button", label: "Cancel", "on-click": "handleClose", variant: "secondary" }, { component: "button", label: "Submit", variant: "cta", "on-click": "submitAcceptWithModification" }] } }, model: { deps: [] }, controller: { init: function() {
}, submitAcceptWithModification: function() {
  const e2 = { revisedText: this.getValue("revisedText"), adjudicationRationale: this.getValue("adjudicationRationale") };
  this.args.onSuccess(e2), this.next("handleClose");
}, handleClose() {
  tcx.eventHandler.next(tcx.eventHandler.KEYS.APP_HIDE_DIALOG, { id: "accept_with_modification_dialog" });
} } }, s = { id: "toolbar", view: { items: [{ component: "div", target: { key: "title", value: "Insert Element", viewState: e.REPLACE } }, { component: "div", target: { key: "title", value: "Insert Paragraph", viewState: e.REPLACE } }, { component: "div", target: { key: "title", value: "Insert Numbered List", viewState: e.REPLACE } }, { component: "div", target: { key: "title", value: "Insert Bulleted List", viewState: e.REPLACE } }, { component: "button", extraclass: "insert-multimedia", icon: "more", variant: "action", quiet: true, holdAffordance: true, title: "More Insert Options", elementID: "toolbar_insert", "on-click": { name: "APP_SHOW_OPTIONS_POPOVER", args: { target: "toolbar_insert", extraclass: "new_options_buttons", items: [{ component: "button", icon: "add", variant: "action", quiet: true, title: "Insert Element", "on-click": "AUTHOR_SHOW_INSERT_ELEMENT_UI" }, { component: "button", icon: "textParagraph", variant: "action", quiet: true, title: "Insert Paragraph", "on-click": "INSERT_P" }, { component: "button", icon: "textNumbered", variant: "action", quiet: true, title: "Insert Numbered List", "on-click": "AUTHOR_INSERT_REMOVE_NUMBERED_LIST" }, { component: "button", icon: "textBulleted", variant: "action", quiet: true, title: "Insert Bulleted List", "on-click": "AUTHOR_INSERT_REMOVE_BULLETED_LIST" }, { component: "button", icon: "table", variant: "action", quiet: true, title: "Insert Table", "on-click": "AUTHOR_INSERT_ELEMENT" }] } }, target: { key: "title", value: "Insert Table", viewState: e.REPLACE } }] }, controller: { init() {
  console.log(this.proxy.getValue("canUndo")), this.proxy.subscribeAppEvent({ key: "editor.preview_rendered", next: function(e2) {
    return __async(this, null, function* () {
      console.log(this.proxy.getValue("canUndo"));
    });
  }.bind(this) });
}, INSERT_P() {
  this.next("AUTHOR_INSERT_ELEMENT", "p");
} } }, c = { id: "file_options", contextMenuWidget: "repository_panel", view: { items: [{ component: "div", target: { key: "displayName", value: "Delete", viewState: e.REPLACE } }, { component: "div", target: { key: "displayName", value: "Edit", viewState: e.REPLACE } }, { displayName: "Download", data: { eventid: "downloadFile" }, icon: "downloadFromCloud", class: "menu-separator", target: { key: "displayName", value: "Duplicate", viewState: e.REPLACE } }] }, controller: { downloadFile() {
  console.log("args: ", this.args), console.log("view confgi: ", this.viewConfig), console.log("subject: ", this.subject), this.subscribe({ key: "rename", next: () => {
    console.log("extenson sub rename");
  } }), this.subscribeAppEvent({ key: "app.active_document_changed", next: () => {
    console.log("active doc changed subs");
  } }), this.subscribeAppModel("download + console", () => {
    console.log("app mode subs");
  }), this.subscribeParentEvent({ key: "tabChange", next: () => {
    console.log("tab change subs");
  } }), this.parentEventHandlerNext("tabChange", { data: "repository_panel" }), this.appModelNext("app.mode", "author"), this.appEventHandlerNext("app.active_document_changed", "active doc changed");
  const e2 = this.getValue("selectedItems")[0].path;
  var t2, n2;
  (t2 = e2, n2 = true, $.ajax({ type: "POST", url: "/bin/referencelistener", data: { operation: "getdita", path: t2, type: n2 ? "UUID" : "PATH", cache: false } })).then((t3) => {
    !function(e3, t4) {
      const n3 = new Blob([t4], { type: "text/plain" }), i2 = document.createElement("a");
      i2.download = e3, i2.href = window.URL.createObjectURL(n3), i2.onclick = function() {
        const e4 = this;
        setTimeout(function() {
          window.URL.revokeObjectURL(e4.href);
        }, 1500);
      }, i2.click(), i2.remove();
    }(e2, t3.xml);
  });
} } }, r = { id: "left_panel_container", tabView: { id: "left_panel_container", tabs: [{ component: "tab", id: "new_tab_extension", extraclass: "collection-panel-tab", showClass: "@visibleTabs.collection_panel", "on-click": "tabClick", icon: "collection", title: "TEST EXTENSION", label: "TEST EXTENSION", prevTabID: "condition_panel" }], tabPanels: [{ component: "tabPanel", tabId: "new_tab_extension", showClass: "@visibleTabs.citation_panel", items: [{ id: "annotation_toolbox" }] }] } };
function m(e2) {
  let t2 = e2.version;
  const n2 = true === e2.versionDirty;
  return t2 && n2 && (t2 = `${t2} * `), t2 || "none";
}
const d = { id: "annotation_toolbox", view: { items: [{ component: "button", icon: "linkOut", title: "openTopicInAEM", "on-click": "openTopicInAEM", target: { key: "value", value: "addcomment", viewState: e.APPEND } }, { component: "widget", id: "save_as_new_version", inputModel: { versionNumber: "@extraProps.versionNumber", isNotLatest: "@extraProps.isNotLatest", isSideBySideViewOn: "@extraProps.isSideBySideViewOn", isNotReadOnly: "@extraProps.isNotReadOnly", tabContent: "@extraProps.tabContent" }, target: { key: "value", value: "addcomment", viewState: e.APPEND } }] }, controller: { init: function() {
  this.subscribeAppEvent({ key: "app.active_document_changed", next: () => {
    _.defer(() => {
      var _a;
      let e2 = (_a = tcx.appGet("tabControllers")) == null ? void 0 : _a.author, t2 = e2.tabItems.items.find((t3) => _.isEqual(e2 == null ? void 0 : e2.selectedTabId, t3.id));
      this.setValue("versionNumber", m(t2)), this.setValue("isNotLatest", !(t2 == null ? void 0 : t2.isLatest)), this.setValue("isSideBySideViewOn", function(e3) {
        const t3 = e3.sideBySideVersion;
        return !(!t3 || "" === t3) && t3;
      }(t2)), this.setValue("isNotReadOnly", !(t2 == null ? void 0 : t2.readOnly)), this.setValue("tabContent", t2 == null ? void 0 : t2.content);
    });
  } }), this.subscribeAppModel("page.file.current_version", (e2) => {
    var _a;
    const t2 = (_a = tcx.appGet("tabControllers")) == null ? void 0 : _a.author, n2 = t2.tabItems.items.find((e3) => _.isEqual(t2 == null ? void 0 : t2.selectedTabId, e3.id));
    n2 ? (this.setValue("isNotLatest", !(n2 == null ? void 0 : n2.isLatest)), this.setValue("versionNumber", m(n2))) : this.setValue("versionNumber", e2 == null ? void 0 : e2.version);
  });
}, openTopicInAEM: function(e2) {
  const t2 = tcx.model.getValue(tcx.model.KEYS.REVIEW_CURR_TOPIC), { allTopics: n2 = {} } = tcx.model.getValue(tcx.model.KEYS.REVIEW_DATA) || {};
  tcx.appGet("util").openInAEM(n2[t2]);
} } }, u = { id: "right_panel_container", tabView: { id: "right_panel_container_tab", tabs: [{ component: "tab", id: "mapref_highlight_tab", "on-click": "tabClick", icon: "highlight", title: "Mapref Tools" }], tabPanels: [{ component: "tabPanel", tabId: "mapref_highlight_tab", items: [{ component: "button", label: "@buttonLabel", title: "Toggle highlight on all mapref elements in the editor", "on-click": "toggleHighlight", extraclass: "@highlightActiveClass" }] }] }, model: { deps: ["buttonLabel", "highlightActiveClass"] }, controller: { init: function() {
  this.setValue("buttonLabel", "Highlight mapref"), this.setValue("highlightActiveClass", ""), this.setValue("isHighlighted", false);
}, toggleHighlight: function() {
  var _a, _b;
  const e2 = this.getValue("isHighlighted") || false, t2 = Array.from(document.querySelectorAll("iframe"));
  let n2 = 0;
  for (const i3 of t2)
    try {
      const t3 = (_b = i3.contentDocument) != null ? _b : (_a = i3.contentWindow) == null ? void 0 : _a.document;
      if (!t3)
        continue;
      const a2 = t3.querySelectorAll("mapref");
      if (0 === a2.length)
        continue;
      n2 += a2.length, a2.forEach((t4) => {
        const n3 = t4;
        e2 ? (n3.style.removeProperty("background-color"), n3.style.removeProperty("outline"), n3.style.removeProperty("border-radius")) : (n3.style.backgroundColor = "rgba(255, 210, 0, 0.45)", n3.style.outline = "2px solid #f97316", n3.style.borderRadius = "2px");
      });
    } catch (e3) {
    }
  const i2 = !e2;
  this.setValue("isHighlighted", i2), this.setValue("buttonLabel", i2 ? `Remove Highlight (${n2})` : "Highlight mapref"), this.setValue("highlightActiveClass", i2 ? "mapref-highlight-active" : "");
} } };
var p = ((e2) => (e2.APPEND = "append", e2.PREPEND = "prepend", e2.REPLACE = "replace", e2))(p || {});
const h = { id: "save_revision", view: { items: [{ component: "button", label: "publish", target: { key: "label", value: "Save", viewState: p.APPEND } }] } }, v = { id: "other_attribute_list_item", view: { items: [{ component: "widget", id: "loading_shimmer", target: { key: "extraclass", value: "editable-attribute-value", viewState: "append" } }, { component: "button", target: { key: "extraclass", value: "editable-attribute-value", viewState: "append" }, title: "mybutton", "on-click": "handleClick" }] }, controller: { handleClick: function() {
  this.getValue("name"), this.getValue("value"), this.getValue("xpath");
}, init: function() {
  this.getValue("name"), this.getValue("value");
} } }, g = { id: "map_translation_view", view: {}, controller: { rowSelectionChanged: function(e2) {
  e2.data.rowIndex.find((e3) => {
    e3.cols.find((e4) => "doc_state" === e4.property.propName).items[0];
  });
} } }, b = { id: "editor_toolbar", view: { items: [{ component: "div", target: { key: "title", value: "Insert Numbered List", viewState: e.REPLACE } }, { component: "button", icon: "textParagraph", variant: "action", quiet: true, title: "Insert Paragraph", "on-click": "INSERT_P", target: { key: "title", value: "Insert Paragraph", viewState: e.REPLACE } }, { component: "button", icon: "fileHTML", variant: "action", quiet: true, title: "URL Link Customisation", "on-click": "openExternalLinkDialog", target: { key: "title", value: "Insert Bulleted List", viewState: e.REPLACE } }] }, controller: { init: function() {
  console.log(this.getValue("canUndo")), this.subscribeAppEvent({ key: "editor.preview_rendered", next: function(e2) {
    return __async(this, null, function* () {
      console.log(this.getValue("canUndo"));
    });
  }.bind(this) });
}, INSERT_P() {
  this.appEventHandlerNext("AUTHOR_INSERT_ELEMENT", "p");
}, openExternalLinkDialog() {
  this.appEventHandlerNext("AUTHOR_INSERT_ELEMENT", { args: "<xref href='' scope='external' format = 'dita' ></xref>", activeTabId: "conkey_reference" });
} } }, x = { id: "html5_preset_general", view: { items: [{ component: "textfield", disabled: true, text: "Added some display text", data: "@ditaOTCommandLineArguement", "aria-label": "@ditaOTCommandLineArguement", "on-change": "ditaOTCommandLineArguementChanged", target: { key: "on-change", value: "ditaOTCommandLineArguementChanged", viewState: "replace" } }] } }, E = { id: "author_outline_element", contextMenuWidget: "dita_editor_menu", view: { items: [{ displayName: "Custom wrap element", data: { eventid: "customWrapClicked" }, icon: "textSpaceAfter", readOnly: true, target: { key: "displayName", value: "Wrap Element", viewState: e.APPEND } }, { displayName: "Custom Cut", data: { eventid: "AUTHOR_CUT" }, icon: "cut", target: { key: "displayName", value: "Cut", viewState: e.REPLACE } }] }, controller: { customWrapClicked() {
  console.log("Custom context menu clicked");
} } };
window.extension = { [t.id]: t, [i.id]: i, [a.id]: a, [o.id]: o, [l.id]: l, [s.id]: s, [c.id]: c, [r.id]: r, [u.id]: u, [d.id]: d, [h.id]: h, [v.id]: v, [g.id]: g, [b.id]: b, [x.id]: x, [E.id]: E };
