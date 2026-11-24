import {
  get, post, put, query, label, today, monthBounds, formatDate, formatMoney, h, table, tabs, drawer, buildForm, badge,
  spinner, emptyState, errorBox, toast,
} from "../core.js";

const TYPES = ["INCOME", "EXPENSE"];
const INCOME_CATEGORIES = ["MINISTRY_GRANT", "PATH", "FUNDRAISER", "TUCKSHOP", "CANTEEN", "OTHER"];
const EXPENSE_CATEGORIES = ["MAINTENANCE", "UTILITIES", "SALARIES", "SUPPLIES", "OTHER"];
const ALL_CATEGORIES = [...new Set([...INCOME_CATEGORIES, ...EXPENSE_CATEGORIES])];

export async function render(main) {
  main.append(h("p", { class: "page-intro" },
    "Record the school's income and expenses, and see where the money comes from and where it goes."));
  main.append(tabs([
    { id: "overview", label: "Overview", render: overviewTab },
    { id: "monthly", label: "Monthly report", render: monthlyTab },
  ]));
}

function totalsAndBreakdown(summary) {
  const balanceTone = Number(summary.balance) < 0 ? "negative" : "positive";
  return [
    h("div", { class: "stat-grid" },
      h("div", { class: "stat" }, h("div", { class: "label" }, "Income"), h("div", { class: "value" }, formatMoney(summary.totalIncome))),
      h("div", { class: "stat" }, h("div", { class: "label" }, "Expenses"), h("div", { class: "value" }, formatMoney(summary.totalExpenses))),
      h("div", { class: `stat ${balanceTone}` }, h("div", { class: "label" }, "Balance"), h("div", { class: "value" }, formatMoney(summary.balance)),
        h("div", { class: "sub" }, `${formatDate(summary.from)} to ${formatDate(summary.to)}`))),
    h("div", { class: "split", style: "margin-bottom:1.25rem" },
      breakdown("Income by category", summary.incomeByCategory, summary.totalIncome, false),
      breakdown("Expenses by category", summary.expensesByCategory, summary.totalExpenses, true)),
  ];
}

function breakdown(title, rows, total, expense) {
  const max = Number(total) || 1;
  return h("div", { class: "card" }, h("div", { class: "card-head" }, h("h3", null, title)),
    rows.length
      ? h("div", { style: "display:grid;gap:.7rem" }, rows.map((row) => h("div", null,
        h("div", { style: "display:flex;justify-content:space-between;font-size:.88rem;margin-bottom:.25rem" },
          h("span", null, label(row.category)), h("strong", null, formatMoney(row.total))),
        h("div", { class: `bar${expense ? " expense" : ""}` }, h("i", { style: `width:${Math.max(2, (Number(row.total) / max) * 100)}%` })))))
      : h("p", { class: "muted small", style: "margin:0" }, "Nothing recorded in this period."));
}

function overviewTab(panel) {
  const bounds = monthBounds();
  const from = h("input", { type: "date", value: bounds.from });
  const to = h("input", { type: "date", value: bounds.to });
  const type = h("select", null, h("option", { value: "" }, "Income and expenses"), TYPES.map((t) => h("option", { value: t }, label(t))));
  const category = h("select", null, h("option", { value: "" }, "All categories"), ALL_CATEGORIES.map((c) => h("option", { value: c }, label(c))));
  const summaryBox = h("div");
  const entriesBox = h("div");

  panel.append(h("div", { class: "toolbar" },
    h("label", { class: "field" }, h("span", null, "From"), from),
    h("label", { class: "field" }, h("span", null, "To"), to),
    h("div", { class: "spacer" }),
    h("button", { class: "btn btn-ghost", type: "button", onclick: () => openForm(null, "INCOME") }, "+ Record income"),
    h("button", { class: "btn", type: "button", onclick: () => openForm(null, "EXPENSE") }, "+ Record an expense")),
  summaryBox,
  h("div", { class: "card-head" }, h("h3", null, "Entries"),
    h("div", { class: "toolbar", style: "margin:0" }, type, category)),
  entriesBox);

  async function load() {
    summaryBox.replaceChildren(spinner());
    entriesBox.replaceChildren();
    try {
      const summary = await get(`/api/expenditure/summary${query({ from: from.value, to: to.value })}`);
      summaryBox.replaceChildren(...totalsAndBreakdown(summary));
      await loadEntries();
    } catch (error) {
      summaryBox.replaceChildren(errorBox(error));
    }
  }

  async function loadEntries() {
    entriesBox.replaceChildren(spinner());
    try {
      const entries = await get(`/api/expenditure/entries${query({ from: from.value, to: to.value, type: type.value, category: category.value })}`);
      entriesBox.replaceChildren(entries.length ? table([
        { label: "Date", value: (e) => formatDate(e.entryDate) },
        { label: "Type", value: (e) => badge(label(e.type), e.type === "INCOME" ? "green" : "red") },
        { label: "Category", value: (e) => label(e.category) },
        { label: "Description", value: (e) => e.description || h("span", { class: "muted" }, "—") },
        { label: "Payee / payer", value: (e) => e.payee || h("span", { class: "muted" }, "—") },
        { label: "Reference", value: (e) => e.referenceNumber || h("span", { class: "muted" }, "—") },
        { label: "Amount", value: (e) => h("strong", null, `${e.type === "EXPENSE" ? "−" : ""}${formatMoney(e.amount)}`), num: true },
        { label: "", value: (e) => h("button", { class: "btn btn-ghost btn-sm", type: "button", onclick: () => openForm(e) }, "Edit") },
      ], entries) : h("div", { class: "card" }, emptyState("No entries", "Nothing matches this period and filter.")));
    } catch (error) {
      entriesBox.replaceChildren(errorBox(error));
    }
  }

  function openForm(entry, newType) {
    const kind = entry ? entry.type : newType;
    const panelBox = drawer(entry ? "Edit entry" : kind === "INCOME" ? "Record income" : "Record an expense");
    const v = entry || {};
    const { form } = buildForm([
      { name: "type", label: "Type", type: "select", required: true, options: TYPES, value: kind },
      { name: "category", label: "Category", type: "select", required: true, value: v.category, placeholder: "Choose…",
        options: kind === "INCOME" ? INCOME_CATEGORIES : entry ? ALL_CATEGORIES : EXPENSE_CATEGORIES },
      { name: "amount", label: "Amount (JMD)", type: "number", required: true, min: "0.01", step: "0.01", value: v.amount },
      { name: "entryDate", label: "Date", type: "date", required: true, value: v.entryDate || today() },
      { name: "description", label: "Description", value: v.description, span: "span-all", max: 255 },
      { name: "payee", label: kind === "INCOME" ? "Received from" : "Paid to", value: v.payee, max: 255 },
      { name: "referenceNumber", label: "Receipt or cheque number", value: v.referenceNumber, max: 100 },
    ], {
      submitLabel: entry ? "Save changes" : "Save entry",
      onCancel: () => panelBox.close(),
      onSubmit: async (values) => {
        if (entry) await put(`/api/expenditure/entries/${entry.id}`, values);
        else await post("/api/expenditure/entries", values);
        panelBox.close();
        toast(entry ? "Entry updated." : "Entry saved.");
        load();
      },
    });
    panelBox.body.append(form);
  }

  from.addEventListener("change", load);
  to.addEventListener("change", load);
  type.addEventListener("change", loadEntries);
  category.addEventListener("change", loadEntries);
  load();
}

function monthlyTab(panel) {
  const now = new Date();
  const month = h("input", { type: "month", value: `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}` });
  const body = h("div");
  panel.append(h("div", { class: "toolbar" }, h("label", { class: "field" }, h("span", null, "Month"), month), h("div", { class: "spacer" }),
    h("button", { class: "btn btn-ghost no-print", type: "button", onclick: () => window.print() }, "Print")), body);

  async function load() {
    if (!month.value) return;
    const [year, m] = month.value.split("-").map(Number);
    body.replaceChildren(spinner());
    try {
      const report = await get(`/api/expenditure/reports/monthly${query({ year, month: m })}`);
      body.replaceChildren(...totalsAndBreakdown(report),
        report.entries.length ? table([
          { label: "Date", value: (e) => formatDate(e.entryDate) },
          { label: "Type", value: (e) => label(e.type) },
          { label: "Category", value: (e) => label(e.category) },
          { label: "Description", value: (e) => e.description || "" },
          { label: "Amount", value: (e) => `${e.type === "EXPENSE" ? "−" : ""}${formatMoney(e.amount)}`, num: true },
        ], report.entries) : h("div", { class: "card" }, emptyState("Nothing recorded this month")));
    } catch (error) {
      body.replaceChildren(errorBox(error));
    }
  }
  month.addEventListener("change", load);
  load();
}
