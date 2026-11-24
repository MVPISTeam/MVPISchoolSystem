// Shared code for the login page and the staff portal: the login session, API calls, roles and small UI helpers.

/* ------------------------------------------------------------------ */
/* Session                                                            */
/* ------------------------------------------------------------------ */

// The API uses HTTP Basic, so the browser must send the username and password with every request.
// They are kept in sessionStorage: they vanish when the tab is closed and never go into a cookie.
const SESSION_KEY = "mvpi.session";

export const session = {
  get() {
    try {
      return JSON.parse(sessionStorage.getItem(SESSION_KEY));
    } catch {
      return null;
    }
  },
  save(value) {
    sessionStorage.setItem(SESSION_KEY, JSON.stringify(value));
  },
  clear() {
    sessionStorage.removeItem(SESSION_KEY);
  },
};

function basicToken(username, password) {
  const bytes = new TextEncoder().encode(`${username}:${password}`);
  let binary = "";
  bytes.forEach((b) => { binary += String.fromCharCode(b); });
  return btoa(binary);
}

export class ApiError extends Error {
  constructor(status, message, fieldErrors) {
    super(message);
    this.status = status;
    this.fieldErrors = fieldErrors || {};
  }
}

/** Checks a username and password against /api/auth/me and, if they work, starts the session. */
export async function login(username, password) {
  const token = basicToken(username.trim(), password);
  const response = await fetch("/api/auth/me", { headers: { Authorization: `Basic ${token}` } });
  if (response.status === 401) throw new ApiError(401, "That username and password do not match.");
  if (!response.ok) throw new ApiError(response.status, "The server could not log you in. Try again in a moment.");
  const user = await response.json();
  session.save({ token, user });
  return user;
}

export function logout() {
  session.clear();
  location.href = "/login.html";
}

/** Calls the API as the logged-in user. The server decides what each role may do. */
export async function api(method, path, body) {
  const current = session.get();
  if (!current) {
    location.href = "/login.html";
    throw new ApiError(401, "Not logged in");
  }
  const headers = { Authorization: `Basic ${current.token}` };
  if (body !== undefined) headers["Content-Type"] = "application/json";

  let response;
  try {
    response = await fetch(path, { method, headers, body: body !== undefined ? JSON.stringify(body) : undefined });
  } catch {
    throw new ApiError(0, "Could not reach the server. Check your connection.");
  }

  if (response.status === 401) {
    session.clear();
    location.href = "/login.html?expired=1";
    throw new ApiError(401, "Your login has expired.");
  }
  const text = await response.text();
  let data = null;
  if (text) {
    try { data = JSON.parse(text); } catch { data = null; }
  }
  if (!response.ok) {
    const message = response.status === 403
      ? "Your role is not allowed to do this."
      : (data && data.message) || `The server answered ${response.status}.`;
    throw new ApiError(response.status, message, data && data.fieldErrors);
  }
  return data;
}

export const get = (path) => api("GET", path);
export const post = (path, body) => api("POST", path, body);
export const put = (path, body) => api("PUT", path, body);

/** Builds a query string, leaving out empty values. */
export function query(params) {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") search.set(key, value);
  });
  const text = search.toString();
  return text ? `?${text}` : "";
}

/* ------------------------------------------------------------------ */
/* Roles and what each one may use                                    */
/* ------------------------------------------------------------------ */

export const ROLES = {
  PRINCIPAL: { label: "Principal", username: "principal" },
  VICE_PRINCIPAL: { label: "Vice Principal", username: "viceprincipal" },
  ADMIN_STAFF: { label: "Administrative staff", username: "admin" },
  TEACHER: { label: "Teacher", username: "teacher" },
  BURSAR: { label: "Bursar", username: "bursar" },
  TUCKSHOP_MANAGER: { label: "Tuckshop Manager", username: "tuckshop" },
};

// These mirror the rules in Access.java. The server always has the final say; the menu only hides what would fail.
const LEADERSHIP = ["PRINCIPAL", "VICE_PRINCIPAL"];
export const ACCESS = {
  CLASS_LIST: [...LEADERSHIP, "ADMIN_STAFF", "TEACHER"],
  STUDENT_RECORDS: [...LEADERSHIP, "ADMIN_STAFF"],
  TEACHING: [...LEADERSHIP, "TEACHER"],
  LUNCH: [...LEADERSHIP, "TEACHER", "TUCKSHOP_MANAGER"],
  FINANCE: [...LEADERSHIP, "BURSAR"],
  LEADERSHIP,
};

export function can(role, rule) {
  return Boolean(role) && ACCESS[rule].includes(role);
}

/** The logged-in user's role. */
export function currentRole(current = session.get()) {
  return current ? current.user.role : null;
}

/* ------------------------------------------------------------------ */
/* Formatting                                                         */
/* ------------------------------------------------------------------ */

/** VICE_PRINCIPAL -> "Vice principal", TERM_1 -> "Term 1" */
export function label(value) {
  if (value === null || value === undefined || value === "") return "";
  const known = ROLES[value];
  if (known) return known.label;
  const text = String(value).toLowerCase().replace(/_/g, " ");
  return text.charAt(0).toUpperCase() + text.slice(1);
}

export function today() {
  return new Date().toLocaleDateString("en-CA"); // YYYY-MM-DD in the user's time zone
}

export function monthBounds(date = new Date()) {
  const first = new Date(date.getFullYear(), date.getMonth(), 1);
  const last = new Date(date.getFullYear(), date.getMonth() + 1, 0);
  return { from: first.toLocaleDateString("en-CA"), to: last.toLocaleDateString("en-CA") };
}

/** "2026-2027" for a date between September and August */
export function academicYear(date = new Date()) {
  const start = date.getMonth() >= 8 ? date.getFullYear() : date.getFullYear() - 1;
  return `${start}-${start + 1}`;
}

export function formatDate(iso) {
  if (!iso) return "";
  const [y, m, d] = iso.split("-").map(Number);
  return new Date(y, m - 1, d).toLocaleDateString(undefined, { day: "numeric", month: "short", year: "numeric" });
}

const money = new Intl.NumberFormat("en-JM", { style: "currency", currency: "JMD", minimumFractionDigits: 2 });
export function formatMoney(value) {
  return money.format(Number(value || 0));
}

export function initials(name) {
  return (name || "?").split(/\s+/).filter(Boolean).slice(0, 2).map((part) => part[0].toUpperCase()).join("");
}

/* ------------------------------------------------------------------ */
/* DOM helpers. Text always goes in through textContent, never HTML.  */
/* ------------------------------------------------------------------ */

export function h(tag, attrs, ...children) {
  const node = document.createElement(tag);
  if (attrs) {
    for (const [key, value] of Object.entries(attrs)) {
      if (value === undefined || value === null || value === false) continue;
      if (key === "class") node.className = value;
      else if (key === "dataset") Object.assign(node.dataset, value);
      else if (key.startsWith("on") && typeof value === "function") node.addEventListener(key.slice(2), value);
      else if (key === "value") node.value = value;
      else if (key === "checked" || key === "selected" || key === "disabled" || key === "hidden") node[key] = Boolean(value);
      else node.setAttribute(key, value === true ? "" : value);
    }
  }
  append(node, children);
  return node;
}

function append(node, children) {
  for (const child of children.flat(Infinity)) {
    if (child === null || child === undefined || child === false) continue;
    node.append(child instanceof Node ? child : document.createTextNode(String(child)));
  }
}

/** Parses trusted SVG markup written in this file (icons, crest). Never pass it data from the server. */
export function svg(markup) {
  const template = document.createElement("template");
  template.innerHTML = markup.trim();
  return template.content.firstElementChild;
}

export function spinner() {
  return h("div", { class: "spinner", role: "status", "aria-label": "Loading" });
}

export function emptyState(title, text) {
  return h("div", { class: "empty" }, h("strong", null, title), text ? h("span", null, text) : null);
}

export function errorBox(error) {
  return h("div", { class: "alert alert-error", role: "alert" }, error.message || String(error));
}

export function toast(message, kind) {
  let box = document.getElementById("toasts");
  if (!box) {
    box = h("div", { id: "toasts", "aria-live": "polite" });
    document.body.append(box);
  }
  const item = h("div", { class: `toast${kind === "error" ? " error" : ""}` }, message);
  box.append(item);
  setTimeout(() => item.remove(), 4200);
}

export function badge(text, tone) {
  return h("span", { class: `badge${tone ? ` badge-${tone}` : ""}` }, text);
}

/**
 * A table from rows and column definitions.
 * columns: [{ label, value: row => text or Node, num: true }]
 */
export function table(columns, rows, { onRowClick, footer } = {}) {
  const head = h("thead", null, h("tr", null, columns.map((c) => h("th", { class: c.num ? "num" : null }, c.label))));
  const body = h("tbody", null, rows.map((row) => h("tr", {
    class: onRowClick ? "clickable" : null,
    tabindex: onRowClick ? "0" : null,
    onclick: onRowClick ? () => onRowClick(row) : null,
    onkeydown: onRowClick ? (e) => { if (e.key === "Enter") onRowClick(row); } : null,
  }, columns.map((c) => h("td", { class: c.num ? "num" : null }, c.value(row))))));
  const foot = footer ? h("tfoot", null, h("tr", null, footer.map((cell, i) => h("td", { class: columns[i] && columns[i].num ? "num" : null }, cell)))) : null;
  return h("div", { class: "table-wrap" }, h("table", { class: "data" }, head, body, foot));
}

/** Tabs: [{ id, label, render: (panel) => void }] */
export function tabs(items, start) {
  const panel = h("div");
  const bar = h("div", { class: "tabs", role: "tablist" });
  const buttons = items.map((item) => h("button", {
    class: "tab", role: "tab", type: "button",
    onclick: () => select(item.id),
  }, item.label));
  bar.append(...buttons);
  function select(id) {
    items.forEach((item, i) => buttons[i].setAttribute("aria-selected", String(item.id === id)));
    panel.replaceChildren();
    items.find((item) => item.id === id).render(panel);
  }
  select(start || items[0].id);
  return h("div", null, bar, panel);
}

/* ---------- Forms ---------- */

/**
 * Builds a form from field definitions and returns { form, values(), showErrors(fieldErrors) }.
 * field: { name, label, type, options: [value] | [{value,label}], required, value, hint, span, min, max, step }
 */
export function buildForm(fields, { onSubmit, submitLabel = "Save", onCancel } = {}) {
  const inputs = {};
  const wrappers = {};
  const errorTop = h("div");

  const grid = h("div", { class: "form-grid" }, fields.map((field) => {
    let control;
    const id = `f-${field.name}-${Math.random().toString(36).slice(2, 7)}`;
    if (field.type === "select") {
      control = h("select", { id, name: field.name, required: field.required },
        field.required ? null : h("option", { value: "" }, field.placeholder || "—"),
        (field.options || []).map((opt) => {
          const value = typeof opt === "object" ? opt.value : opt;
          const text = typeof opt === "object" ? opt.label : label(opt);
          return h("option", { value, selected: String(field.value ?? "") === String(value) }, text);
        }));
      if (field.required && field.value === undefined && field.placeholder) {
        control.prepend(h("option", { value: "", selected: true, disabled: true }, field.placeholder));
      }
    } else if (field.type === "textarea") {
      control = h("textarea", { id, name: field.name, required: field.required, maxlength: field.max }, field.value ?? "");
    } else if (field.type === "checkbox") {
      control = h("input", { id, type: "checkbox", name: field.name, checked: Boolean(field.value) });
      const wrap = h("label", { class: `check${field.span ? ` ${field.span}` : ""}`, for: id }, control, field.label);
      inputs[field.name] = control;
      wrappers[field.name] = wrap;
      return wrap;
    } else {
      control = h("input", {
        id, name: field.name, type: field.type || "text", required: field.required,
        value: field.value ?? "", min: field.min, max: field.max, step: field.step,
        placeholder: field.placeholder, autocomplete: "off",
      });
    }
    inputs[field.name] = control;
    const wrap = h("label", { class: `field${field.span ? ` ${field.span}` : ""}`, for: id },
      h("span", null, field.label, field.required ? " *" : ""), control,
      field.hint ? h("small", { class: "hint" }, field.hint) : null);
    wrappers[field.name] = wrap;
    return wrap;
  }));

  const submit = h("button", { class: "btn", type: "submit" }, submitLabel);
  const actions = h("div", { class: "form-actions" },
    onCancel ? h("button", { class: "btn btn-ghost", type: "button", onclick: onCancel }, "Cancel") : null,
    submit);
  const form = h("form", { novalidate: true }, errorTop, grid, actions);

  function values() {
    const out = {};
    for (const field of fields) {
      const control = inputs[field.name];
      if (field.type === "checkbox") out[field.name] = control.checked;
      else if (field.type === "number") out[field.name] = control.value === "" ? null : Number(control.value);
      else out[field.name] = control.value.trim() === "" ? null : control.value.trim();
    }
    return out;
  }

  function clearErrors() {
    errorTop.replaceChildren();
    Object.values(wrappers).forEach((wrap) => {
      wrap.classList.remove("invalid");
      wrap.querySelectorAll(".error").forEach((e) => e.remove());
    });
  }

  function showErrors(error) {
    clearErrors();
    const fieldErrors = (error && error.fieldErrors) || {};
    let placed = false;
    for (const [name, message] of Object.entries(fieldErrors)) {
      const wrap = wrappers[name];
      if (!wrap) continue;
      wrap.classList.add("invalid");
      wrap.append(h("small", { class: "error" }, message));
      placed = true;
    }
    errorTop.append(h("div", { class: "alert alert-error", role: "alert" },
      placed ? "Please fix the highlighted fields." : error.message || "Something went wrong."));
  }

  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    clearErrors();
    const missing = fields.filter((f) => f.required && f.type !== "checkbox" && !inputs[f.name].value.trim());
    if (missing.length) {
      showErrors({ fieldErrors: Object.fromEntries(missing.map((f) => [f.name, "This field is required"])) });
      return;
    }
    submit.disabled = true;
    try {
      await onSubmit(values());
    } catch (error) {
      showErrors(error);
    } finally {
      submit.disabled = false;
    }
  });

  return { form, values, showErrors, inputs };
}

/** A panel that slides in from the right. Returns { body, close }. */
export function drawer(title, { onClose } = {}) {
  const body = h("div", { class: "drawer-body" });
  const backdrop = h("div", { class: "drawer-backdrop" });
  const close = () => {
    backdrop.remove();
    document.removeEventListener("keydown", onKey);
    if (onClose) onClose();
  };
  const onKey = (e) => { if (e.key === "Escape") close(); };
  const panel = h("div", { class: "drawer", role: "dialog", "aria-modal": "true", "aria-label": title },
    h("div", { class: "drawer-head" }, h("h2", null, title),
      h("button", { class: "icon-btn", type: "button", "aria-label": "Close", onclick: close }, "×")),
    body);
  backdrop.append(panel);
  backdrop.addEventListener("click", (e) => { if (e.target === backdrop) close(); });
  document.addEventListener("keydown", onKey);
  document.body.append(backdrop);
  setTimeout(() => {
    const first = panel.querySelector("input, select, textarea, button.btn");
    if (first) first.focus();
  }, 50);
  return { body, close };
}

export function details(pairs) {
  return h("dl", { class: "details" }, pairs.flatMap(([term, value]) => [
    h("dt", null, term),
    h("dd", null, value === null || value === undefined || value === "" ? h("span", { class: "muted" }, "Not recorded") : value),
  ]));
}

/* ------------------------------------------------------------------ */
/* Artwork                                                            */
/* ------------------------------------------------------------------ */

export const CREST = `
<svg class="crest" viewBox="0 0 48 48" aria-hidden="true">
  <path d="M24 2 43 9v14c0 11.5-8 19.7-19 23C13 42.7 5 34.5 5 23V9z" fill="#145039"/>
  <path d="M24 5.5 40 11.4V23c0 9.6-6.6 16.6-16 19.6C14.6 39.6 8 32.6 8 23V11.4z" fill="none" stroke="#f4bf2e" stroke-width="1.6"/>
  <path d="M11 31 19 20l5 6 4-4 9 9z" fill="#f4bf2e"/>
  <path d="M19 20l3 3.6-3 1.6-3-1.4z" fill="#fff"/>
  <circle cx="31" cy="15" r="3" fill="#fff" opacity=".9"/>
  <path d="M14 34.5h20" stroke="#fff" stroke-width="1.6" stroke-linecap="round" opacity=".7"/>
</svg>`;

const ICON_PATHS = {
  home: '<path d="M3 11.5 12 4l9 7.5"/><path d="M5 10v10h14V10"/><path d="M10 20v-6h4v6"/>',
  students: '<circle cx="9" cy="8" r="3.2"/><path d="M3 20c0-3.3 2.7-6 6-6s6 2.7 6 6"/><circle cx="17" cy="9" r="2.4"/><path d="M16 14.2c2.8.2 5 2.6 5 5.8"/>',
  attendance: '<rect x="4" y="5" width="16" height="15" rx="2"/><path d="M8 3v4M16 3v4M4 10h16"/><path d="m9 15 2 2 4-4"/>',
  grades: '<path d="M4 20V10M10 20V4M16 20v-7M22 20H2"/>',
  report: '<path d="M7 3h8l4 4v14H7z"/><path d="M15 3v4h4"/><path d="M10 12h6M10 16h6"/>',
  lunch: '<path d="M4 3v8a3 3 0 0 0 6 0V3M7 3v18"/><path d="M17 21V3c-2 1-3 3.5-3 7 0 2 1 3 3 3"/>',
  finance: '<rect x="3" y="6" width="18" height="13" rx="2"/><path d="M3 10h18"/><circle cx="16" cy="14.5" r="1.4"/>',
  staff: '<rect x="3" y="7" width="18" height="13" rx="2"/><path d="M9 7V5a2 2 0 0 1 2-2h2a2 2 0 0 1 2 2v2"/><path d="M3 13h18"/>',
  register: '<path d="M9 5h10M9 12h10M9 19h10"/><path d="m3 5 1.5 1.5L7 4M3 12l1.5 1.5L7 11M3 19l1.5 1.5L7 18"/>',
};

export function icon(name) {
  return svg(`<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${ICON_PATHS[name] || ""}</svg>`);
}
