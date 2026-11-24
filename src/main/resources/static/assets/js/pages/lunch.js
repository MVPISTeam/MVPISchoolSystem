import {
  get, post, put, query, label, today, formatDate, formatMoney, h, table, tabs, drawer, buildForm, badge,
  spinner, emptyState, errorBox, toast,
} from "../core.js";
import { gradeName } from "./students.js";

const METHODS = ["CANTEEN", "TUCKSHOP", "PATH"];
const STATUSES = ["ORDERED", "SERVED", "CANCELLED"];
const methodTone = { CANTEEN: "green", TUCKSHOP: "blue", PATH: "gold" };

export async function render(main) {
  main.append(h("p", { class: "page-intro" },
    "Record lunch orders and payments, keep each student's usual lunch method up to date, and see the day's totals. PATH lunches are always free."));

  let students;
  try {
    students = await get("/api/lunch/students");
  } catch (error) {
    main.append(errorBox(error));
    return;
  }

  main.append(tabs([
    { id: "orders", label: "Orders", render: (panel) => ordersTab(panel, students) },
    { id: "methods", label: "Lunch methods", render: (panel) => methodsTab(panel, students) },
    { id: "report", label: "Daily report", render: reportTab },
  ]));
}

function ordersTab(panel, students) {
  const date = h("input", { type: "date", value: today() });
  const grade = h("select", null, h("option", { value: "" }, "All grades"), [0, 1, 2, 3, 4, 5, 6].map((g) => h("option", { value: g }, gradeName(g))));
  const body = h("div");
  panel.append(h("div", { class: "toolbar" },
    h("label", { class: "field" }, h("span", null, "Date"), date),
    h("label", { class: "field" }, h("span", null, "Grade"), grade),
    h("div", { class: "spacer" }),
    h("button", { class: "btn", type: "button", onclick: () => openForm(null) }, "+ Record an order")), body);

  async function load() {
    body.replaceChildren(spinner());
    try {
      const orders = await get(`/api/lunch/orders${query({ date: date.value, gradeLevel: grade.value })}`);
      if (!orders.length) {
        body.replaceChildren(h("div", { class: "card" }, emptyState("No orders", `No lunch orders recorded for ${formatDate(date.value)}.`)));
        return;
      }
      const paid = orders.filter((o) => o.status !== "CANCELLED").reduce((sum, o) => sum + Number(o.amountPaid || 0), 0);
      body.replaceChildren(table([
        { label: "Student", value: (o) => h("strong", null, o.studentName) },
        { label: "Class", value: (o) => o.className },
        { label: "Method", value: (o) => badge(label(o.method), methodTone[o.method]) },
        { label: "Item", value: (o) => o.itemDescription || h("span", { class: "muted" }, "—") },
        { label: "Paid", value: (o) => formatMoney(o.amountPaid), num: true },
        { label: "Status", value: (o) => statusSelect(o) },
        { label: "", value: (o) => h("button", { class: "btn btn-ghost btn-sm", type: "button", onclick: () => openForm(o) }, "Edit") },
      ], orders, { footer: [`${orders.length} orders`, "", "", "", formatMoney(paid), "", ""] }));
    } catch (error) {
      body.replaceChildren(errorBox(error));
    }
  }

  function bodyFor(order, changes) {
    return {
      studentId: order.studentId, orderDate: order.orderDate, method: order.method, status: order.status,
      itemDescription: order.itemDescription, amountPaid: order.amountPaid, ...changes,
    };
  }

  function statusSelect(order) {
    const select = h("select", { "aria-label": `Status for ${order.studentName}` },
      STATUSES.map((s) => h("option", { value: s, selected: s === order.status }, label(s))));
    select.addEventListener("change", async () => {
      try {
        await put(`/api/lunch/orders/${order.id}`, bodyFor(order, { status: select.value }));
        order.status = select.value;
        toast(`${order.studentName}: ${label(select.value).toLowerCase()}.`);
      } catch (error) {
        select.value = order.status;
        toast(error.message, "error");
      }
    });
    return select;
  }

  function openForm(order) {
    const panel = drawer(order ? `Edit order for ${order.studentName}` : "Record a lunch order");
    const sorted = [...students].sort((a, b) => a.className.localeCompare(b.className) || a.studentName.localeCompare(b.studentName));
    const { form, inputs } = buildForm([
      { name: "studentId", label: "Student", type: "select", required: true, value: order && order.studentId, placeholder: "Choose…", span: "span-all",
        options: sorted.map((s) => ({ value: s.studentId, label: `${s.studentName} (${s.className}, usually ${label(s.lunchMethod).toLowerCase()})` })) },
      { name: "orderDate", label: "Date", type: "date", required: true, value: order ? order.orderDate : date.value },
      { name: "method", label: "Method", type: "select", options: METHODS, value: order && order.method, placeholder: "Student's usual method",
        hint: "Leave blank to use the student's usual method" },
      { name: "status", label: "Status", type: "select", required: true, options: STATUSES, value: order ? order.status : "ORDERED" },
      { name: "amountPaid", label: "Amount paid (JMD)", type: "number", min: 0, step: "0.01", value: order ? order.amountPaid : "",
        hint: "PATH lunches are saved as $0" },
      { name: "itemDescription", label: "Item", value: order && order.itemDescription, span: "span-all", max: 255,
        placeholder: "e.g. Chicken and rice" },
    ], {
      submitLabel: order ? "Save changes" : "Record order",
      onCancel: () => panel.close(),
      onSubmit: async (values) => {
        values.studentId = Number(values.studentId);
        if (order) await put(`/api/lunch/orders/${order.id}`, values);
        else await post("/api/lunch/orders", values);
        panel.close();
        toast(order ? "Order updated." : "Order recorded.");
        if (values.orderDate !== date.value) date.value = values.orderDate;
        load();
      },
    });
    const syncAmount = () => {
      const student = students.find((s) => String(s.studentId) === inputs.studentId.value);
      const method = inputs.method.value || (student && student.lunchMethod);
      inputs.amountPaid.disabled = method === "PATH";
      if (method === "PATH") inputs.amountPaid.value = "0";
    };
    inputs.studentId.addEventListener("change", syncAmount);
    inputs.method.addEventListener("change", syncAmount);
    syncAmount();
    panel.body.append(form);
  }

  date.addEventListener("change", load);
  grade.addEventListener("change", load);
  load();
}

function methodsTab(panel, students) {
  const filter = h("select", null, h("option", { value: "" }, "All methods"), METHODS.map((m) => h("option", { value: m }, label(m))));
  const body = h("div");
  panel.append(h("div", { class: "toolbar" }, h("label", { class: "field" }, h("span", null, "Show"), filter),
    h("span", { class: "muted small" }, "Changes save as soon as you pick a new method.")), body);

  function draw() {
    const rows = students.filter((s) => !filter.value || s.lunchMethod === filter.value)
      .sort((a, b) => a.className.localeCompare(b.className) || a.studentName.localeCompare(b.studentName));
    body.replaceChildren(rows.length ? table([
      { label: "Student", value: (s) => h("strong", null, s.studentName) },
      { label: "Class", value: (s) => s.className },
      { label: "Grade", value: (s) => gradeName(s.gradeLevel) },
      { label: "Usual lunch", value: (s) => methodSelect(s) },
    ], rows) : h("div", { class: "card" }, emptyState("No students use this method")));
  }

  function methodSelect(student) {
    const select = h("select", { "aria-label": `Lunch method for ${student.studentName}` },
      METHODS.map((m) => h("option", { value: m, selected: m === student.lunchMethod }, label(m))));
    select.addEventListener("change", async () => {
      try {
        const saved = await put(`/api/lunch/students/${student.studentId}/method`, { method: select.value });
        student.lunchMethod = saved.lunchMethod;
        toast(`${student.studentName} now has ${label(saved.lunchMethod).toLowerCase()} lunch.`);
      } catch (error) {
        select.value = student.lunchMethod;
        toast(error.message, "error");
      }
    });
    return select;
  }

  filter.addEventListener("change", draw);
  draw();
}

function reportTab(panel) {
  const date = h("input", { type: "date", value: today() });
  const body = h("div");
  panel.append(h("div", { class: "toolbar" }, h("label", { class: "field" }, h("span", null, "Date"), date)), body);

  async function load() {
    body.replaceChildren(spinner());
    try {
      const report = await get(`/api/lunch/reports/daily${query({ date: date.value })}`);
      body.replaceChildren(
        h("div", { class: "stat-grid" },
          h("div", { class: "stat" }, h("div", { class: "label" }, "Orders"), h("div", { class: "value" }, report.totalOrders), h("div", { class: "sub" }, "Cancelled orders not counted")),
          h("div", { class: "stat positive" }, h("div", { class: "label" }, "Payments"), h("div", { class: "value" }, formatMoney(report.totalPayments)))),
        table([
          { label: "Method", value: (m) => badge(label(m.method), methodTone[m.method]) },
          { label: "Orders", value: (m) => m.orderCount, num: true },
          { label: "Payments", value: (m) => formatMoney(m.paymentTotal), num: true },
        ], report.byMethod, { footer: ["Total", report.totalOrders, formatMoney(report.totalPayments)] }));
    } catch (error) {
      body.replaceChildren(errorBox(error));
    }
  }
  date.addEventListener("change", load);
  load();
}
