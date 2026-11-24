import { get, query, can, label, today, monthBounds, formatMoney, h, icon } from "../core.js";

export async function render(main, ctx) {
  const dateText = new Date().toLocaleDateString(undefined, { weekday: "long", day: "numeric", month: "long", year: "numeric" });

  main.append(h("div", { class: "welcome" },
    h("div", null,
      h("h2", null, `Good ${partOfDay()}, ${ctx.user.fullName}`),
      h("p", null, `${dateText} · ${label(ctx.role)}`))));

  const stats = h("div", { class: "stat-grid" });
  main.append(stats);
  loadStats(stats, ctx.role);

  const modules = ctx.modules.filter((m) => m.id !== "dashboard");
  main.append(h("div", { class: "card-head" }, h("h2", null, "Your tools")));
  main.append(h("div", { class: "module-grid" }, modules.map((m) => h("a", { class: "module", href: `#/${m.id}` },
    h("div", { class: "ico" }, icon(m.icon)), h("h3", null, m.label), h("p", null, m.blurb)))));
}

function partOfDay() {
  const hour = new Date().getHours();
  if (hour < 12) return "morning";
  if (hour < 17) return "afternoon";
  return "evening";
}

function stat(labelText, value, sub, tone) {
  return h("div", { class: `stat${tone ? ` ${tone}` : ""}` },
    h("div", { class: "label" }, labelText), h("div", { class: "value" }, value), sub ? h("div", { class: "sub" }, sub) : null);
}

async function loadStats(box, role) {
  const date = today();
  const month = monthBounds();
  const jobs = [];

  if (can(role, "CLASS_LIST")) {
    jobs.push(get("/api/students").then((students) => {
      const classes = new Set(students.map((s) => s.className));
      return [stat("Students", students.length, `${classes.size} classes`)];
    }));
  }
  if (can(role, "TEACHING")) {
    jobs.push(get(`/api/attendance/summary/daily${query({ date })}`).then((summary) => {
      const present = summary.divisions.reduce((sum, d) => sum + d.boysPresent + d.girlsPresent, 0);
      return [stat("Present today", present, present ? "From today's registers" : "No register taken yet")];
    }));
  }
  if (can(role, "LUNCH")) {
    jobs.push(get(`/api/lunch/reports/daily${query({ date })}`).then((report) => [
      stat("Lunch orders today", report.totalOrders, `${formatMoney(report.totalPayments)} paid`),
    ]));
  }
  if (can(role, "FINANCE")) {
    jobs.push(get(`/api/expenditure/summary${query(month)}`).then((s) => [
      stat("Income this month", formatMoney(s.totalIncome)),
      stat("Expenses this month", formatMoney(s.totalExpenses)),
      stat("Balance this month", formatMoney(s.balance), null, Number(s.balance) < 0 ? "negative" : "positive"),
    ]));
  }
  if (can(role, "LEADERSHIP")) {
    jobs.push(get(`/api/staff${query({ status: "ACTIVE" })}`).then((staff) => [stat("Active staff", staff.length)]));
  }

  if (!jobs.length) {
    box.remove();
    return;
  }
  const results = await Promise.allSettled(jobs);
  for (const result of results) {
    if (result.status === "fulfilled") box.append(...result.value);
  }
}
