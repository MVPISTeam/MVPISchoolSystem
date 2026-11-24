import { get, query, label, academicYear, formatDate, h, table, badge, svg, CREST, spinner, emptyState, errorBox } from "../core.js";
import { studentPicker, letterTone } from "./grades.js";
import { gradeName } from "./students.js";

const TERMS = ["TERM_1", "TERM_2", "TERM_3", "DIAGNOSTIC"];

/** Usual term dates, used to fill in the attendance period. They can be changed before making the card. */
function termDates(term, year) {
  const [start, end] = year.split("-").map(Number);
  switch (term) {
    case "TERM_2": return { from: `${end}-01-05`, to: `${end}-04-10` };
    case "TERM_3": return { from: `${end}-04-20`, to: `${end}-07-10` };
    case "DIAGNOSTIC": return { from: `${start}-09-01`, to: `${start}-09-30` };
    default: return { from: `${start}-09-01`, to: `${start}-12-20` };
  }
}

export async function render(main) {
  main.append(h("p", { class: "page-intro no-print" },
    "Make a student's report card for a term: their marks, GPA and attendance for the same period. Print it or save it as a PDF."));

  let students;
  try {
    students = await get("/api/students");
  } catch (error) {
    main.append(errorBox(error));
    return;
  }

  let student = null;
  const year = h("input", { value: academicYear(), style: "width:130px" });
  const term = h("select", null, TERMS.map((t) => h("option", { value: t }, label(t))));
  const from = h("input", { type: "date" });
  const to = h("input", { type: "date" });
  const makeBtn = h("button", { class: "btn", type: "button", disabled: true }, "Make report card");
  const body = h("div", null, h("div", { class: "card" }, emptyState("Choose a student", "Pick a class and a student, then make the card.")));

  function fillDates() {
    if (!/^\d{4}-\d{4}$/.test(year.value.trim())) return;
    const dates = termDates(term.value, year.value.trim());
    from.value = dates.from;
    to.value = dates.to;
  }
  fillDates();
  year.addEventListener("change", fillDates);
  term.addEventListener("change", fillDates);

  const picker = studentPicker(students, (s) => { student = s; makeBtn.disabled = !s; });
  main.append(h("div", { class: "toolbar no-print" }, picker.nodes,
    h("label", { class: "field" }, h("span", null, "Academic year"), year),
    h("label", { class: "field" }, h("span", null, "Term"), term),
    h("label", { class: "field" }, h("span", null, "Attendance from"), from),
    h("label", { class: "field" }, h("span", null, "to"), to),
    makeBtn), body);

  makeBtn.addEventListener("click", async () => {
    if (!student) return;
    body.replaceChildren(spinner());
    try {
      const card = await get(`/api/reports/students/${student.id}/report-card${query({
        academicYear: year.value.trim(), term: term.value, from: from.value, to: to.value,
      })}`);
      body.replaceChildren(reportCard(card, term.value, year.value.trim()));
    } catch (error) {
      body.replaceChildren(errorBox(error));
    }
  });
}

function reportCard(card, term, year) {
  const { academics: a, attendance: att } = card;
  const daysMarked = att.daysPresent + att.daysAbsent + att.daysLate;
  const rate = daysMarked ? Math.round(((att.daysPresent + att.daysLate) / daysMarked) * 100) : null;

  return h("div", null,
    h("div", { class: "toolbar no-print", style: "justify-content:flex-end" },
      h("button", { class: "btn btn-ghost", type: "button", onclick: () => window.print() }, "Print or save as PDF")),
    h("article", { class: "report-card" },
      h("header", null, svg(CREST),
        h("div", null, h("h2", null, "Mountain View Primary and Infant School"),
          h("div", { class: "muted" }, `Report card · ${label(term)} · ${year}`))),
      h("div", { class: "meta" },
        h("div", null, h("span", null, "Student"), h("strong", null, a.studentName)),
        h("div", null, h("span", null, "Class"), h("strong", null, `${a.className} · ${gradeName(a.gradelevel)}`)),
        h("div", null, h("span", null, "GPA"), h("strong", null, a.gpa.toFixed(2))),
        h("div", null, h("span", null, "Attendance"), h("strong", null, rate === null ? "Not recorded" : `${rate}%`))),
      h("h3", null, "Marks"),
      a.grades.length
        ? table([
          { label: "Subject", value: (g) => label(g.subject) },
          { label: "Classwork", value: (g) => g.classworkGrade, num: true },
          { label: "Exam", value: (g) => g.examGrade, num: true },
          { label: "Final mark", value: (g) => h("strong", null, g.finalMark), num: true },
          { label: "Grade", value: (g) => badge(g.letter, letterTone(g.letter)) },
        ], a.grades)
        : emptyState("No marks for this term", "Add them on the Grades screen."),
      h("h3", { style: "margin-top:1.5rem" }, "Attendance"),
      h("p", null, `${formatDate(att.from)} to ${formatDate(att.to)}: `,
        h("strong", null, `${att.daysPresent} days present`), `, ${att.daysLate} late, ${att.daysAbsent} absent.`),
      h("div", { style: "display:grid;grid-template-columns:1fr 1fr;gap:2rem;margin-top:3rem" },
        h("div", { style: "border-top:1px solid var(--line-strong);padding-top:.4rem" }, h("span", { class: "muted small" }, "Class teacher")),
        h("div", { style: "border-top:1px solid var(--line-strong);padding-top:.4rem" }, h("span", { class: "muted small" }, "Principal")))));
}
