import {
  get, post, put, query, label, academicYear, h, table, drawer, buildForm, badge,
  spinner, emptyState, errorBox, toast,
} from "../core.js";

const TERMS = ["DIAGNOSTIC", "TERM_1", "TERM_2", "TERM_3"];

/** Grades 1 to 3 take Integrated Studies; Grades 4 to 6 take Science and Social Studies instead. */
export function subjectsFor(gradeLevel) {
  const common = ["MATHEMATICS", "LANGUAGE_ARTS", "READING", "PHYSICAL_EDUCATION"];
  if (gradeLevel >= 4) return [...common, "SCIENCE", "SOCIAL_STUDIES"];
  if (gradeLevel >= 1) return [...common, "INTEGRATED_STUDIES"];
  return [...common, "INTEGRATED_STUDIES", "SCIENCE", "SOCIAL_STUDIES"];
}

export function letterTone(letter) {
  return { A: "green", B: "green", C: "blue", D: "gold", F: "red" }[letter] || null;
}

/** Class and student dropdowns built from the class list. Calls onChange(student) when a student is picked. */
export function studentPicker(students, onChange) {
  const classes = [...new Set(students.map((s) => s.className))].sort();
  const classSelect = h("select", null, classes.map((c) => h("option", { value: c }, c)));
  const studentSelect = h("select");

  function fillStudents() {
    const inClass = students.filter((s) => s.className === classSelect.value)
      .sort((a, b) => a.lastName.localeCompare(b.lastName));
    studentSelect.replaceChildren(h("option", { value: "" }, "Choose a student…"),
      ...inClass.map((s) => h("option", { value: s.id }, `${s.lastName}, ${s.firstName}`)));
  }
  classSelect.addEventListener("change", () => { fillStudents(); onChange(null); });
  studentSelect.addEventListener("change", () => onChange(students.find((s) => String(s.id) === studentSelect.value) || null));
  fillStudents();

  return {
    nodes: [
      h("label", { class: "field" }, h("span", null, "Class"), classSelect),
      h("label", { class: "field grow" }, h("span", null, "Student"), studentSelect),
    ],
  };
}

export async function render(main) {
  main.append(h("p", { class: "page-intro" },
    "Pick a student to see their marks and GPA. The final mark is the average of the classwork and exam marks."));

  let students;
  try {
    students = await get("/api/students");
  } catch (error) {
    main.append(errorBox(error));
    return;
  }

  let student = null;
  const year = h("input", { value: academicYear(), pattern: "\\d{4}-\\d{4}", style: "width:130px" });
  const term = h("select", null, h("option", { value: "" }, "All terms"), TERMS.map((t) => h("option", { value: t }, label(t))));
  const addBtn = h("button", { class: "btn", type: "button", disabled: true, onclick: () => openAdd() }, "+ Add a grade");
  const body = h("div", null, h("div", { class: "card" }, emptyState("Choose a student", "Pick a class and then a student.")));

  const picker = studentPicker(students, (s) => { student = s; addBtn.disabled = !s; load(); });
  main.append(h("div", { class: "toolbar" }, picker.nodes,
    h("label", { class: "field" }, h("span", null, "Academic year"), year),
    h("label", { class: "field" }, h("span", null, "Term"), term),
    addBtn), body);
  year.addEventListener("change", load);
  term.addEventListener("change", load);

  async function load() {
    if (!student) {
      body.replaceChildren(h("div", { class: "card" }, emptyState("Choose a student", "Pick a class and then a student.")));
      return;
    }
    body.replaceChildren(spinner());
    try {
      const report = await get(`/api/academics/students/${student.id}${query({ academicYear: year.value.trim(), term: term.value })}`);
      body.replaceChildren(
        h("div", { class: "stat-grid" },
          h("div", { class: "stat" }, h("div", { class: "label" }, "GPA"), h("div", { class: "value" }, report.gpa.toFixed(2)), h("div", { class: "sub" }, "Out of 4.00")),
          h("div", { class: "stat" }, h("div", { class: "label" }, "Grades recorded"), h("div", { class: "value" }, report.grades.length)),
          h("div", { class: "stat" }, h("div", { class: "label" }, "Class"), h("div", { class: "value" }, report.className), h("div", { class: "sub" }, report.studentName))),
        report.grades.length
          ? table([
            { label: "Subject", value: (g) => h("strong", null, label(g.subject)) },
            { label: "Term", value: (g) => label(g.term) },
            { label: "Year", value: (g) => g.academicYear },
            { label: "Classwork", value: (g) => g.classworkGrade, num: true },
            { label: "Exam", value: (g) => g.examGrade, num: true },
            { label: "Final", value: (g) => h("strong", null, g.finalMark), num: true },
            { label: "Letter", value: (g) => badge(g.letter, letterTone(g.letter)) },
            { label: "", value: (g) => h("button", { class: "btn btn-ghost btn-sm", type: "button", onclick: () => openEdit(g) }, "Edit") },
          ], report.grades)
          : h("div", { class: "card" }, emptyState("No grades yet", "Use \"Add a grade\" to record this student's first mark.")));
    } catch (error) {
      body.replaceChildren(errorBox(error));
    }
  }

  function openAdd() {
    const panel = drawer(`Add a grade for ${student.firstName} ${student.lastName}`);
    const { form } = buildForm([
      { name: "subject", label: "Subject", type: "select", required: true, options: subjectsFor(student.gradeLevel), placeholder: "Choose…" },
      { name: "term", label: "Term", type: "select", required: true, options: TERMS, value: term.value || "TERM_1" },
      { name: "academicYear", label: "Academic year", required: true, value: year.value.trim(), hint: "Like 2026-2027" },
      { name: "classworkGrade", label: "Classwork mark", type: "number", required: true, min: 0, max: 100, step: "0.5", hint: "0 to 100" },
      { name: "examGrade", label: "Exam mark", type: "number", required: true, min: 0, max: 100, step: "0.5", hint: "0 to 100" },
    ], {
      submitLabel: "Save grade",
      onCancel: () => panel.close(),
      onSubmit: async (values) => {
        const saved = await post("/api/academics/grades", { ...values, studentId: student.id });
        panel.close();
        toast(`${label(saved.subject)}: ${saved.finalMark} (${saved.letter}) saved.`);
        load();
      },
    });
    panel.body.append(form);
  }

  function openEdit(grade) {
    const panel = drawer(`${label(grade.subject)} · ${label(grade.term)} ${grade.academicYear}`);
    const { form } = buildForm([
      { name: "classworkGrade", label: "Classwork mark", type: "number", required: true, min: 0, max: 100, step: "0.5", value: grade.classworkGrade },
      { name: "examGrade", label: "Exam mark", type: "number", required: true, min: 0, max: 100, step: "0.5", value: grade.examGrade },
    ], {
      submitLabel: "Save changes",
      onCancel: () => panel.close(),
      onSubmit: async (values) => {
        await put(`/api/academics/grades/${grade.id}`, values);
        panel.close();
        toast("Grade updated.");
        load();
      },
    });
    panel.body.append(form);
  }
}
