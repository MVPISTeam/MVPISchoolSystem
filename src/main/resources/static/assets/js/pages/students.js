import {
  get, post, put, can, label, today, formatDate, h, table, drawer, buildForm, details, badge,
  spinner, emptyState, errorBox, toast,
} from "../core.js";

export function gradeName(level) {
  return Number(level) === 0 ? "Infant" : `Grade ${level}`;
}

export async function render(main, ctx) {
  const canEdit = can(ctx.role, "STUDENT_RECORDS");

  main.append(h("p", { class: "page-intro" }, canEdit
    ? "Search the class lists, open a student to see their full record, or register a new student."
    : "The class lists for the school. Personal details such as addresses and health notes are kept to the Principal, Vice Principal and administration."));

  const search = h("input", { type: "search", placeholder: "Search by name", "aria-label": "Search by name" });
  const classFilter = h("select", { "aria-label": "Class" }, h("option", { value: "" }, "All classes"));
  const gradeFilter = h("select", { "aria-label": "Grade" }, h("option", { value: "" }, "All grades"),
    [0, 1, 2, 3, 4, 5, 6].map((g) => h("option", { value: g }, gradeName(g))));
  const count = h("span", { class: "muted small" });

  main.append(h("div", { class: "toolbar" },
    h("label", { class: "field grow" }, h("span", null, "Name"), search),
    h("label", { class: "field" }, h("span", null, "Class"), classFilter),
    h("label", { class: "field" }, h("span", null, "Grade"), gradeFilter),
    h("div", { class: "spacer" }),
    canEdit ? h("button", { class: "btn", type: "button", onclick: () => openForm(null) }, "+ Register a student") : null));

  const results = h("div", null, spinner());
  main.append(results, h("p", { class: "small", style: "margin-top:.6rem" }, count));

  let students = [];

  async function load() {
    try {
      students = await get("/api/students");
      const classes = [...new Set(students.map((s) => s.className))].sort();
      const chosen = classFilter.value;
      classFilter.replaceChildren(h("option", { value: "" }, "All classes"),
        ...classes.map((c) => h("option", { value: c, selected: c === chosen }, c)));
      draw();
    } catch (error) {
      results.replaceChildren(errorBox(error));
    }
  }

  function draw() {
    const text = search.value.trim().toLowerCase();
    const rows = students
      .filter((s) => !text || `${s.firstName} ${s.lastName}`.toLowerCase().includes(text))
      .filter((s) => !classFilter.value || s.className === classFilter.value)
      .filter((s) => gradeFilter.value === "" || s.gradeLevel === Number(gradeFilter.value))
      .sort((a, b) => a.className.localeCompare(b.className) || a.lastName.localeCompare(b.lastName));
    count.textContent = `${rows.length} of ${students.length} students`;
    if (!rows.length) {
      results.replaceChildren(h("div", { class: "card" }, emptyState("No students match", "Try a different name, class or grade.")));
      return;
    }
    results.replaceChildren(table([
      { label: "Name", value: (s) => h("strong", null, `${s.firstName} ${s.lastName}`) },
      { label: "Class", value: (s) => s.className },
      { label: "Grade", value: (s) => gradeName(s.gradeLevel) },
      { label: "Gender", value: (s) => label(s.gender) },
      { label: "Department", value: (s) => badge(label(s.division), s.division === "INFANT" ? "gold" : "green") },
    ], rows, { onRowClick: canEdit ? (s) => openDetails(s.id) : null }));
  }

  [search, classFilter, gradeFilter].forEach((control) => control.addEventListener("input", draw));

  async function openDetails(id) {
    const panel = drawer("Student record");
    panel.body.append(spinner());
    try {
      const s = await get(`/api/students/${id}`);
      panel.body.replaceChildren(
        h("div", { class: "card-head" },
          h("div", null, h("h3", { style: "font-size:1.3rem;margin:0" }, `${s.firstName} ${s.lastName}`),
            h("span", { class: "muted" }, `${s.className} · ${gradeName(s.gradeLevel)} · ${label(s.division)} department`)),
          h("button", { class: "btn btn-ghost", type: "button", onclick: () => { panel.close(); openForm(s); } }, "Edit")),
        details([
          ["Gender", label(s.gender)],
          ["Date of birth", s.dateOfBirth ? `${formatDate(s.dateOfBirth)} (age ${s.age})` : null],
          ["Address", s.address],
          ["Guardian", s.guardianName],
          ["Guardian phone", s.guardianPhone],
          ["Immunisations", s.immunizationUpToDate === null || s.immunizationUpToDate === undefined ? null
            : s.immunizationUpToDate ? badge("Up to date", "green") : badge("Not up to date", "red")],
          ["Health notes", s.healthInfo],
          ["Registered", formatDate(s.registrationDate)],
        ]));
    } catch (error) {
      panel.body.replaceChildren(errorBox(error));
    }
  }

  function openForm(existing) {
    const panel = drawer(existing ? `Edit ${existing.firstName} ${existing.lastName}` : "Register a student");
    const v = existing || {};
    const { form } = buildForm([
      { name: "firstName", label: "First name", required: true, value: v.firstName },
      { name: "lastName", label: "Last name", required: true, value: v.lastName },
      { name: "gender", label: "Gender", type: "select", options: ["FEMALE", "MALE"], required: true, value: v.gender, placeholder: "Choose…" },
      { name: "dateOfBirth", label: "Date of birth", type: "date", required: true, value: v.dateOfBirth, max: today() },
      { name: "gradeLevel", label: "Grade", type: "select", required: true, value: v.gradeLevel, placeholder: "Choose…",
        options: [0, 1, 2, 3, 4, 5, 6].map((g) => ({ value: g, label: gradeName(g) })) },
      { name: "className", label: "Class", required: true, value: v.className, hint: "For example K2, 1M or 4A" },
      { name: "division", label: "Department", type: "select", options: ["INFANT", "PRIMARY"], required: true, value: v.division, placeholder: "Choose…" },
      { name: "registrationDate", label: "Registration date", type: "date", value: v.registrationDate || (existing ? null : today()) },
      { name: "address", label: "Address", required: true, value: v.address, span: "span-all" },
      { name: "guardianName", label: "Guardian's name", value: v.guardianName },
      { name: "guardianPhone", label: "Guardian's phone", value: v.guardianPhone, type: "tel" },
      { name: "healthInfo", label: "Health notes", type: "textarea", value: v.healthInfo, span: "span-all", max: 1000,
        hint: "Allergies, conditions or medication the school should know about" },
      { name: "immunizationUpToDate", label: "Immunisations are up to date", type: "checkbox", value: v.immunizationUpToDate, span: "span-all" },
    ], {
      submitLabel: existing ? "Save changes" : "Register student",
      onCancel: () => panel.close(),
      onSubmit: async (values) => {
        values.gradeLevel = values.gradeLevel === null ? null : Number(values.gradeLevel);
        const saved = existing ? await put(`/api/students/${existing.id}`, values) : await post("/api/students", values);
        panel.close();
        toast(existing ? "Student record updated." : `${saved.firstName} ${saved.lastName} has been registered.`);
        await load();
      },
    });
    panel.body.append(form);
  }

  load();
}
