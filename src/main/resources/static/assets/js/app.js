// The staff portal: menu and page routing (#/students, #/attendance, ...).

import { session, get, logout, can, currentRole, label, initials, h, svg, icon, CREST, spinner, errorBox } from "./core.js";

/** Every screen in the portal and the rule (from Access.java) a role must pass to see it. */
const MODULES = [
  { id: "dashboard", label: "Dashboard", icon: "home" },
  { id: "students", label: "Students", icon: "students", group: "Students", rule: "CLASS_LIST",
    blurb: "Class lists, student records and registration" },
  { id: "attendance", label: "Attendance", icon: "attendance", group: "Teaching", rule: "TEACHING",
    blurb: "Take the register and see daily and monthly totals" },
  { id: "grades", label: "Grades", icon: "grades", group: "Teaching", rule: "TEACHING",
    blurb: "Record classwork and exam marks, see GPAs" },
  { id: "reportcards", label: "Report cards", icon: "report", group: "Teaching", rule: "TEACHING",
    blurb: "Grades and attendance for a term, ready to print" },
  { id: "lunch", label: "Lunch", icon: "lunch", group: "School operations", rule: "LUNCH",
    blurb: "Lunch methods, orders and the daily report" },
  { id: "finance", label: "Income & expenses", icon: "finance", group: "School operations", rule: "FINANCE",
    blurb: "The school's books and monthly reports" },
  { id: "staff", label: "Staff", icon: "staff", group: "School operations", rule: "LEADERSHIP",
    blurb: "Staff records, positions and employment" },
];

const el = (id) => document.getElementById(id);

function visibleModules(role = currentRole()) {
  return MODULES.filter((m) => !m.rule || can(role, m.rule));
}

function renderNav() {
  const nav = el("nav");
  nav.replaceChildren();
  let lastGroup = null;
  for (const module of visibleModules()) {
    if (module.group && module.group !== lastGroup) {
      nav.append(h("div", { class: "group" }, module.group));
      lastGroup = module.group;
    }
    nav.append(h("a", { href: `#/${module.id}`, dataset: { route: module.id } }, icon(module.icon), module.label));
  }
  highlightNav();
}

function highlightNav() {
  const route = currentRoute();
  el("nav").querySelectorAll("a").forEach((a) => a.classList.toggle("active", a.dataset.route === route));
}

function renderUser() {
  const { user } = session.get();
  el("avatar").textContent = initials(user.fullName);
  el("user-name").textContent = user.fullName;
  el("user-role").textContent = label(user.role);
}

function currentRoute() {
  const id = location.hash.replace(/^#\/?/, "").split("?")[0];
  return id || "dashboard";
}

let renderToken = 0;

async function route() {
  const id = currentRoute();
  const module = MODULES.find((m) => m.id === id);
  const main = el("main");
  const token = ++renderToken;
  highlightNav();
  el("sidebar").classList.remove("open");
  el("menu-toggle").setAttribute("aria-expanded", "false");

  if (!module) {
    location.hash = "#/dashboard";
    return;
  }
  el("page-title").textContent = module.label;
  document.title = `${module.label} · Mountain View staff portal`;
  main.replaceChildren(spinner());

  if (!visibleModules().some((m) => m.id === id)) {
    main.replaceChildren(notAllowed(module));
    return;
  }

  try {
    const page = await import(`./pages/${id}.js`);
    if (token !== renderToken) return; // the user moved on while this loaded
    main.replaceChildren();
    await page.render(main, { role: currentRole(), user: session.get().user, modules: visibleModules() });
  } catch (error) {
    if (token !== renderToken) return;
    main.replaceChildren(errorBox(error));
    console.error(error);
  }
  main.focus({ preventScroll: true });
}

function notAllowed(module) {
  return h("div", { class: "card" },
    h("div", { class: "empty" },
      h("strong", null, `${module.label} is not part of the ${label(currentRole())} role`),
      h("p", null, "Ask the Principal if you think you should have access."),
      h("a", { class: "btn", href: "#/dashboard" }, "Back to the dashboard")));
}

async function start() {
  el("brand").prepend(svg(CREST));
  el("logout").addEventListener("click", logout);
  el("menu-toggle").addEventListener("click", () => {
    const open = el("sidebar").classList.toggle("open");
    el("menu-toggle").setAttribute("aria-expanded", String(open));
  });
  window.addEventListener("hashchange", route);

  renderUser();
  renderNav();

  // Make sure the saved login still works and the name and role are up to date.
  try {
    const me = await get("/api/auth/me");
    const saved = session.get();
    saved.user = me;
    session.save(saved);
    renderUser();
    renderNav();
  } catch (error) {
    if (error.status === 401) return; // api() has already sent the browser back to the login page
  }
  route();
}

if (session.get()) start();
else location.replace("/login.html");
