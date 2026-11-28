// Code that every staff portal page uses

// The pages in the portal and which roles can use them (same rules as Access.java)
const PAGES = [
    { file: "dashboard.html", name: "Home", roles: ["PRINCIPAL", "VICE_PRINCIPAL", "ADMIN_STAFF", "TEACHER", "BURSAR", "TUCKSHOP_MANAGER"] },
    { file: "students.html", name: "Students", roles: ["PRINCIPAL", "VICE_PRINCIPAL", "ADMIN_STAFF", "TEACHER"] },
    { file: "attendance.html", name: "Attendance", roles: ["PRINCIPAL", "VICE_PRINCIPAL", "TEACHER"] },
    { file: "grades.html", name: "Grades", roles: ["PRINCIPAL", "VICE_PRINCIPAL", "TEACHER"] },
    { file: "reportcard.html", name: "Report Cards", roles: ["PRINCIPAL", "VICE_PRINCIPAL", "TEACHER"] },
    { file: "lunch.html", name: "Lunch", roles: ["PRINCIPAL", "VICE_PRINCIPAL", "TEACHER", "TUCKSHOP_MANAGER"] },
    { file: "finance.html", name: "Finance", roles: ["PRINCIPAL", "VICE_PRINCIPAL", "BURSAR"] },
    { file: "staff.html", name: "Staff", roles: ["PRINCIPAL", "VICE_PRINCIPAL"] }
];

// Only these roles can see a student's personal details
const STUDENT_RECORD_ROLES = ["PRINCIPAL", "VICE_PRINCIPAL", "ADMIN_STAFF"];


// Checks the user is logged in, builds the menu and checks they can use this page.
// Returns false if they are not allowed on the page.
function setupPage(pageFile) {
    const role = sessionStorage.getItem("role");

    if (!sessionStorage.getItem("auth")) {
        window.location.href = "../login.html";
        return false;
    }

    // Build the menu with only the pages this role can use
    const nav = document.getElementById("nav");
    let menu = "";
    PAGES.forEach(function (page) {
        if (page.roles.includes(role)) {
            const current = page.file === pageFile ? ' class="current"' : "";
            menu += '<li><a href="' + page.file + '"' + current + ">" + page.name + "</a></li>";
        }
    });
    menu += '<li><a href="#" id="logout">Log Out</a></li>';
    nav.innerHTML = menu;

    document.getElementById("logout").addEventListener("click", function (event) {
        event.preventDefault();
        logout();
    });

    document.getElementById("user-info").textContent =
        "Logged in as " + sessionStorage.getItem("fullName") + " (" + niceText(role) + ")";

    const thisPage = PAGES.find(function (page) { return page.file === pageFile; });
    if (!thisPage.roles.includes(role)) {
        document.getElementById("page-content").innerHTML =
            '<p class="error">Your role does not have access to this page.</p>';
        return false;
    }
    return true;
}

function logout() {
    sessionStorage.clear();
    window.location.href = "../login.html";
}

// Sends a request to our Spring Boot API and gives back the JSON.
// method is "GET", "POST" or "PUT". body is only needed for POST and PUT.
function api(method, url, body) {
    const options = {
        method: method,
        headers: { "Authorization": "Basic " + sessionStorage.getItem("auth") }
    };
    if (body) {
        options.headers["Content-Type"] = "application/json";
        options.body = JSON.stringify(body);
    }

    return fetch(url, options).then(response => {
        if (response.status === 401) {
            logout();
            throw new Error("Please log in again.");
        }
        if (response.status === 403) {
            throw new Error("Your role is not allowed to do this.");
        }
        return response.text().then(text => {
            const data = text ? JSON.parse(text) : null;
            if (!response.ok) {
                let msg = (data && data.message) ? data.message : "Something went wrong.";
                // Show which fields were wrong if the server told us
                if (data && data.fieldErrors) {
                    const problems = [];
                    for (const field in data.fieldErrors) {
                        problems.push(field + " " + data.fieldErrors[field]);
                    }
                    if (problems.length > 0) {
                        msg += ": " + problems.join(", ");
                    }
                }
                throw new Error(msg);
            }
            return data;
        });
    });
}

// Stops names with < or > from breaking the page when we use innerHTML
function clean(text) {
    if (text === null || text === undefined) {
        return "";
    }
    return String(text)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;");
}

// Turns VICE_PRINCIPAL into Vice Principal
function niceText(text) {
    if (!text) {
        return "";
    }
    return text.toLowerCase().split("_").map(function (word) {
        return word.charAt(0).toUpperCase() + word.slice(1);
    }).join(" ");
}

function gradeName(level) {
    return level === 0 ? "Infant" : "Grade " + level;
}

// Today's date as YYYY-MM-DD
function todayDate() {
    const d = new Date();
    const month = String(d.getMonth() + 1).padStart(2, "0");
    const day = String(d.getDate()).padStart(2, "0");
    return d.getFullYear() + "-" + month + "-" + day;
}

function money(amount) {
    return "$" + Number(amount || 0).toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, ",");
}

// Shows a message in a <p>. Pass true for an error (red), false for success (green)
function showMessage(id, text, isError) {
    const p = document.getElementById(id);
    p.textContent = text;
    p.className = isError ? "message error" : "message success";
}

// Makes <option> tags from a list of values.
// Set keepText to true to show the value as it is (for class names like 1M)
function makeOptions(values, selected, keepText) {
    let html = "";
    values.forEach(function (value) {
        const isSelected = String(value) === String(selected) ? " selected" : "";
        const text = keepText ? value : niceText(String(value));
        html += '<option value="' + clean(value) + '"' + isSelected + ">" + clean(text) + "</option>";
    });
    return html;
}

// The academic year for today, like 2026-2027 (the school year starts in September)
function currentAcademicYear() {
    const d = new Date();
    const start = d.getMonth() >= 8 ? d.getFullYear() : d.getFullYear() - 1;
    return start + "-" + (start + 1);
}
