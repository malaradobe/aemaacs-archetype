let t = false;
const e = { id: "right_panel_container", tabView: { id: "right_panel_container_tab", tabs: [{ component: "tab", id: "mapref_highlight_tab", "on-click": "tabClick", icon: "highlight", title: "Mapref Tools" }], tabPanels: [{ component: "tabPanel", tabId: "mapref_highlight_tab", items: [{ component: "button", label: "Highlight mapref", title: "Toggle highlight on all mapref elements in the editor", "on-click": "toggleHighlight", extraclass: "mapref-toggle-btn" }, { component: "label", label: "", extraclass: "mapref-count-label" }] }] }, controller: { toggleHighlight: function() {
  var _a, _b;
  const e2 = [document];
  for (const t2 of Array.from(document.querySelectorAll("iframe")))
    try {
      const o2 = (_b = t2.contentDocument) != null ? _b : (_a = t2.contentWindow) == null ? void 0 : _a.document;
      o2 && e2.push(o2);
    } catch (e3) {
    }
  let o = 0;
  for (const n2 of e2) {
    const e3 = /* @__PURE__ */ new Set();
    Array.from(n2.querySelectorAll("span")).filter((t2) => "mapref" === t2.textContent).forEach((t2) => {
      var _a2;
      const o2 = (_a2 = t2.closest(".cm-line")) != null ? _a2 : t2.parentElement;
      o2 && e3.add(o2);
    }), o += e3.size, e3.forEach((e4) => {
      t ? (e4.style.removeProperty("background-color"), e4.style.removeProperty("outline")) : (e4.style.backgroundColor = "rgba(255, 210, 0, 0.45)", e4.style.outline = "1px solid #f97316");
    });
  }
  t = !t;
  const n = document.querySelector(".mapref-toggle-btn");
  if (n) {
    n.classList.toggle("mapref-highlight-active", t);
    const e3 = n.querySelector("span");
    e3 && (e3.textContent = t ? "Remove Highlight" : "Highlight mapref");
  }
  const l = document.querySelector(".mapref-count-label");
  if (l) {
    const e3 = t ? `${o} mapref element${1 !== o ? "s" : ""} found` : "";
    l.setAttribute("label", e3), l.textContent = e3;
  }
} } };
window.extension = { [e.id]: e };
