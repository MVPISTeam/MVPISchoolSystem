import {
  get, post, put, query, label, today, formatDate, h, table, drawer, buildForm, details, badge,
  spinner, emptyState, errorBox, toast,
} from "../core.js";

const POSITIONS = ["PRINCIPAL", "VICE_PRINCIPAL", "TEACHER", "TEACHERS_AIDE", "GUIDANCE_COUNSELLOR",
  "ADMIN_ASSISTANT", "BURSAR", "TUCKSHOP_MANAGER", "SECURITY_GUARD", "JANITOR"];
const TYPES = ["FULL_TIME", "PART_TIME", "CONTRACT"];
const STATUSES = ["ACTIVE", "ON_LEAVE", "FORMER"];
const statusTone = { ACTIVE: "green", ON_LEAVE: "gold", FORMER: null };

export async function render(main) {
  main.append(h("p", { class: "page-intro" },
    "Staff records are private to the Principal and Vice Principal. Records are never deleted: someone who leaves is marked as former staff."));

  const name = h("input", { type: "search", placeholder: "Search by name" });
  const position = h("select", null, h("option", { value: "" }, "All positions"), POSITIONS.map((p) => h("option", { value: p }, label(p))));
  const status = h("select", null, h("option", { value: "" }, "Any status"), STATUSES.map((s) => h("option", { value: s, selected: s === "ACTIVE" }, label(s))));
  const body = h("div");

  main.append(h("div", { class: "toolbar" },
    h("label", { class: "field grow" }, h("span", null, "Name"), name),
    h("label", { class: "field" }, h("span", null, "Position"), position),
    h("label", { class: "field" }, h("span", null, "Status"), status),
    h("div", { class: "spacer" }),
    h("button", { class: "btn", type: "button", onclick: () => openForm(null) }, "+ Add a staff member")), body);

  let timer;
  async function load() {
    body.replaceChildren(spinner());
    try {
      const staff = await get(`/api/staff${query({ name: name.value.trim(), position: position.value, status: status.value })}`);
      body.replaceChildren(staff.length ? table([
        { label: "Name", value: (s) => h("strong", null, s.fullName) },
        { label: "Position", value: (s) => label(s.position) },
        { label: "Class", value: (s) => s.className || h("span", { class: "muted" }, "—") },
        { label: "Employment", value: (s) => label(s.employmentType) },
        { label: "Since", value: (s) => formatDate(s.hireDate) },
        { label: "Status", value: (s) => badge(label(s.employmentStatus), statusTone[s.employmentStatus]) },
      ], staff, { onRowClick: (s) => openDetails(s) }) : h("div", { class: "card" }, emptyState("No staff match", "Try another name, position or status.")));
    } catch (error) {
      body.replaceChildren(errorBox(error));
    }
  }

  function openDetails(s) {
    const panel = drawer(s.fullName);
    panel.body.append(
      h("div", { class: "card-head" },
        h("div", null, h("h3", { style: "font-size:1.3rem;margin:0" }, s.fullName),
          h("span", { class: "muted" }, `${label(s.position)}${s.className ? ` · ${s.className}` : ""}`)),
        h("button", { class: "btn btn-ghost", type: "button", onclick: () => { panel.close(); openForm(s); } }, "Edit")),
      details([
        ["Status", badge(label(s.employmentStatus), statusTone[s.employmentStatus])],
        ["Employment", label(s.employmentType)],
        ["Hired", formatDate(s.hireDate)],
        ["Date of birth", formatDate(s.dateOfBirth)],
        ["Phone", s.phone],
        ["Email", s.email],
        ["Address", s.address],
      ]));
  }

  function openForm(existing) {
    const panel = drawer(existing ? `Edit ${existing.fullName}` : "Add a staff member");
    const v = existing || {};
    const { form } = buildForm([
      { name: "firstName", label: "First name", required: true, value: v.firstName, max: 100 },
      { name: "lastName", label: "Last name", required: true, value: v.lastName, max: 100 },
      { name: "position", label: "Position", type: "select", required: true, options: POSITIONS, value: v.position, placeholder: "Choose…" },
      { name: "className", label: "Class", value: v.className, hint: "Teachers and aides only, e.g. 4A" },
      { name: "employmentType", label: "Employment", type: "select", required: true, options: TYPES, value: v.employmentType, placeholder: "Choose…" },
      existing ? { name: "employmentStatus", label: "Status", type: "select", required: true, options: STATUSES, value: v.employmentStatus } : null,
      { name: "hireDate", label: "Hire date", type: "date", required: true, value: v.hireDate || today(), max: today() },
      { name: "dateOfBirth", label: "Date of birth", type: "date", required: true, value: v.dateOfBirth, max: today() },
      { name: "phone", label: "Phone", type: "tel", required: true, value: v.phone, hint: "Like 876-555-0123" },
      { name: "email", label: "Email", type: "email", value: v.email },
      { name: "address", label: "Address", required: true, value: v.address, span: "span-all" },
    ].filter(Boolean), {
      submitLabel: existing ? "Save changes" : "Add staff member",
      onCancel: () => panel.close(),
      onSubmit: async (values) => {
        const saved = existing ? await put(`/api/staff/${existing.id}`, values) : await post("/api/staff", values);
        panel.close();
        toast(existing ? "Staff record updated." : `${saved.fullName} has been added.`);
        load();
      },
    });
    panel.body.append(form);
  }

  name.addEventListener("input", () => { clearTimeout(timer); timer = setTimeout(load, 250); });
  position.addEventListener("change", load);
  status.addEventListener("change", load);
  load();
}
