import {
  get, put, query, label, today, monthBounds, formatDate, h, table, tabs, badge,
  spinner, emptyState, errorBox, toast,
} from "../core.js";

const STATUSES = ["PRESENT", "ABSENT", "LATE"];

export async function render(main) {
  main.append(h("p", { class: "page-intro" },
    "Take the morning and afternoon register for a class, then see the day's totals and the monthly report for the Ministry."));

  let classes = [];
  try {
    const students = await get("/api/students");
    classes = [...new Set(students.map((s) => s.className))].sort();
  } catch (error) {
    main.append(errorBox(error));
    return;
  }

  main.append(tabs([
    { id: "register", label: "Take the register", render: (panel) => registerTab(panel, classes) },
    { id: "daily", label: "Daily summary", render: dailyTab },
    { id: "monthly", label: "Monthly report", render: monthlyTab },
    { id: "history", label: "Student history", render: historyTab },
  ]));
}

function statusPicker(value, onChange) {
  const box = h("div", { class: "register-status", role: "group" });
  const buttons = STATUSES.map((status) => h("button", {
    type: "button", class: status.toLowerCase(), "aria-pressed": String(status === value),
    onclick: () => {
      buttons.forEach((b, i) => b.setAttribute("aria-pressed", String(STATUSES[i] === status)));
      onChange(status);
    },
  }, label(status)));
  box.append(...buttons);
  box.set = (status) => buttons.forEach((b, i) => b.setAttribute("aria-pressed", String(STATUSES[i] === status)));
  return box;
}

function registerTab(panel, classes) {
  const classSelect = h("select", null, classes.map((c) => h("option", { value: c }, c)));
  const date = h("input", { type: "date", value: today(), max: today() });
  const status = h("span");
  const body = h("div");
  const saveBtn = h("button", { class: "btn", type: "button", hidden: true }, "Save register");
  const allPresent = h("button", { class: "btn btn-ghost", type: "button", hidden: true }, "Mark everyone present");

  panel.append(h("div", { class: "toolbar" },
    h("label", { class: "field" }, h("span", null, "Class"), classSelect),
    h("label", { class: "field" }, h("span", null, "Date"), date),
    h("div", { class: "spacer" }), status, allPresent, saveBtn), body);

  let entries = [];
  let pickers = [];

  async function load() {
    body.replaceChildren(spinner());
    saveBtn.hidden = true;
    allPresent.hidden = true;
    status.replaceChildren();
    try {
      const rows = await get(`/api/attendance/classes/${encodeURIComponent(classSelect.value)}${query({ date: date.value })}`);
      const taken = rows.some((r) => r.recordId);
      status.replaceChildren(taken ? badge("Already taken: you can correct it", "green") : badge("Not taken yet", "gold"));
      entries = rows.map((r) => ({ studentId: r.studentId, morning: r.morning || "PRESENT", afternoon: r.afternoon || "PRESENT" }));
      pickers = [];
      body.replaceChildren(table([
        { label: "Student", value: (r) => h("strong", null, r.studentName) },
        { label: "Morning", value: (r, i = rows.indexOf(r)) => keep(statusPicker(entries[i].morning, (s) => { entries[i].morning = s; }), i, "morning") },
        { label: "Afternoon", value: (r, i = rows.indexOf(r)) => keep(statusPicker(entries[i].afternoon, (s) => { entries[i].afternoon = s; }), i, "afternoon") },
      ], rows));
      saveBtn.hidden = false;
      allPresent.hidden = false;
    } catch (error) {
      body.replaceChildren(errorBox(error));
    }
  }

  function keep(picker, index, half) {
    pickers.push({ picker, index, half });
    return picker;
  }

  allPresent.addEventListener("click", () => {
    entries.forEach((e) => { e.morning = "PRESENT"; e.afternoon = "PRESENT"; });
    pickers.forEach(({ picker }) => picker.set("PRESENT"));
  });

  saveBtn.addEventListener("click", async () => {
    saveBtn.disabled = true;
    try {
      await put(`/api/attendance/classes/${encodeURIComponent(classSelect.value)}`, { date: date.value, entries });
      toast(`Register saved for ${classSelect.value} on ${formatDate(date.value)}.`);
      await load();
    } catch (error) {
      toast(error.message, "error");
    } finally {
      saveBtn.disabled = false;
    }
  });

  classSelect.addEventListener("change", load);
  date.addEventListener("change", load);
  if (classes.length) load();
  else body.append(h("div", { class: "card" }, emptyState("No classes yet", "Register students first.")));
}

function dailyTab(panel) {
  const date = h("input", { type: "date", value: today(), max: today() });
  const body = h("div");
  panel.append(h("div", { class: "toolbar" }, h("label", { class: "field" }, h("span", null, "Date"), date)), body);

  async function load() {
    body.replaceChildren(spinner());
    try {
      const summary = await get(`/api/attendance/summary/daily${query({ date: date.value })}`);
      const totalBoys = summary.divisions.reduce((sum, d) => sum + d.boysPresent, 0);
      const totalGirls = summary.divisions.reduce((sum, d) => sum + d.girlsPresent, 0);
      body.replaceChildren(
        h("div", { class: "stat-grid" },
          summary.divisions.map((d) => h("div", { class: "stat" }, h("div", { class: "label" }, `${label(d.division)} department`),
            h("div", { class: "value" }, d.boysPresent + d.girlsPresent), h("div", { class: "sub" }, `${d.boysPresent} boys · ${d.girlsPresent} girls`))),
          h("div", { class: "stat" }, h("div", { class: "label" }, "Whole school"), h("div", { class: "value" }, totalBoys + totalGirls),
            h("div", { class: "sub" }, `${totalBoys} boys · ${totalGirls} girls`))),
        summary.classes.length
          ? table([
            { label: "Class", value: (c) => h("strong", null, c.className) },
            { label: "Department", value: (c) => label(c.division) },
            { label: "Boys present", value: (c) => c.boysPresent, num: true },
            { label: "Girls present", value: (c) => c.girlsPresent, num: true },
            { label: "Total", value: (c) => c.boysPresent + c.girlsPresent, num: true },
          ], summary.classes, { footer: ["Total", "", totalBoys, totalGirls, totalBoys + totalGirls] })
          : h("div", { class: "card" }, emptyState("No registers taken", `Nobody has taken a register for ${formatDate(date.value)} yet.`)));
    } catch (error) {
      body.replaceChildren(errorBox(error));
    }
  }
  date.addEventListener("change", load);
  load();
}

function monthlyTab(panel) {
  const now = new Date();
  const month = h("input", { type: "month", value: `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}` });
  const body = h("div");
  const print = h("button", { class: "btn btn-ghost no-print", type: "button", onclick: () => window.print() }, "Print");
  panel.append(h("div", { class: "toolbar" }, h("label", { class: "field" }, h("span", null, "Month"), month), h("div", { class: "spacer" }), print), body);

  async function load() {
    if (!month.value) return;
    const [year, m] = month.value.split("-").map(Number);
    body.replaceChildren(spinner());
    try {
      const report = await get(`/api/attendance/reports/monthly${query({ year, month: m })}`);
      const monthName = new Date(year, m - 1, 1).toLocaleDateString(undefined, { month: "long", year: "numeric" });
      body.replaceChildren(
        h("div", { class: "card-head" }, h("h3", null, `Attendance for ${monthName}`),
          h("span", { class: "muted small" }, `${report.schoolDaysRecorded} school days recorded`)),
        report.schoolDaysRecorded
          ? table([
            { label: "Department", value: (r) => h("strong", null, label(r.division)) },
            { label: "Boys' attendances", value: (r) => `${r.boysAttendances} of ${r.boysMarked}`, num: true },
            { label: "Boys %", value: (r) => `${r.boysPercent}%`, num: true },
            { label: "Girls' attendances", value: (r) => `${r.girlsAttendances} of ${r.girlsMarked}`, num: true },
            { label: "Girls %", value: (r) => `${r.girlsPercent}%`, num: true },
          ], report.divisions)
          : h("div", { class: "card" }, emptyState("Nothing recorded", "No registers were taken this month.")));
    } catch (error) {
      body.replaceChildren(errorBox(error));
    }
  }
  month.addEventListener("change", load);
  load();
}

function historyTab(panel) {
  const bounds = monthBounds();
  const name = h("input", { type: "search", placeholder: "e.g. Brown" });
  const from = h("input", { type: "date", value: bounds.from });
  const to = h("input", { type: "date", value: today() });
  const body = h("div", null, h("div", { class: "card" }, emptyState("Search for a student", "Type part of a name to see their attendance.")));
  const form = h("form", { class: "toolbar" },
    h("label", { class: "field grow" }, h("span", null, "Student name"), name),
    h("label", { class: "field" }, h("span", null, "From"), from),
    h("label", { class: "field" }, h("span", null, "To"), to),
    h("button", { class: "btn", type: "submit" }, "Search"));
  panel.append(form, body);

  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    if (!name.value.trim()) return;
    body.replaceChildren(spinner());
    try {
      const histories = await get(`/api/attendance/students${query({ name: name.value.trim(), from: from.value, to: to.value })}`);
      body.replaceChildren(...histories.map((hist) => h("div", { class: "card" },
        h("div", { class: "card-head" },
          h("div", null, h("h3", null, hist.studentName), h("span", { class: "muted small" }, `${hist.className} · ${formatDate(hist.from)} to ${formatDate(hist.to)}`)),
          h("div", { style: "display:flex;gap:.4rem;flex-wrap:wrap" },
            badge(`${hist.daysPresent} present`, "green"), badge(`${hist.daysAbsent} absent`, "red"), badge(`${hist.daysLate} late`, "gold"))),
        hist.records.length
          ? table([
            { label: "Date", value: (r) => formatDate(r.date) },
            { label: "Morning", value: (r) => statusBadge(r.morning) },
            { label: "Afternoon", value: (r) => statusBadge(r.afternoon) },
          ], hist.records)
          : emptyState("No registers in this period"))));
    } catch (error) {
      body.replaceChildren(errorBox(error));
    }
  });
}

function statusBadge(status) {
  const tone = { PRESENT: "green", ABSENT: "red", LATE: "gold" }[status];
  return status ? badge(label(status), tone) : h("span", { class: "muted" }, "—");
}
