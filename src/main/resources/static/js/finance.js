document.addEventListener("DOMContentLoaded", function () {
    if (!setupPage("finance.html")) {
        return;
    }

    const INCOME_CATEGORIES = ["MINISTRY_GRANT", "PATH", "FUNDRAISER", "TUCKSHOP", "CANTEEN", "OTHER"];
    const EXPENSE_CATEGORIES = ["MAINTENANCE", "UTILITIES", "SALARIES", "SUPPLIES", "OTHER"];

    const from = document.getElementById("from");
    const to = document.getElementById("to");
    const typeSelect = document.getElementById("type");
    const categorySelect = document.getElementById("category");
    const summaryResult = document.getElementById("summary-result");
    const form = document.getElementById("entry-form");

    let entries = [];
    let editingId = null;

    // Start with this month
    const today = todayDate();
    from.value = today.substring(0, 8) + "01";
    to.value = today;
    document.getElementById("entry-date").value = today;
    document.getElementById("month").value = today.substring(0, 7);

    // The category list changes for income and expenses
    function fillCategories(selected) {
        const list = typeSelect.value === "INCOME" ? INCOME_CATEGORIES : EXPENSE_CATEGORIES;
        categorySelect.innerHTML = makeOptions(list, selected);
    }
    typeSelect.addEventListener("change", function () {
        fillCategories();
    });
    fillCategories();

    // Builds the totals table and the category tables
    function totalsHtml(summary) {
        let html = "<p><strong>Income:</strong> " + money(summary.totalIncome) +
            " | <strong>Expenses:</strong> " + money(summary.totalExpenses) +
            " | <strong>Balance:</strong> " + money(summary.balance) + "</p>";

        html += "<table><tr><th>Income by category</th><th>Amount</th></tr>";
        summary.incomeByCategory.forEach(function (c) {
            html += "<tr><td>" + niceText(c.category) + "</td><td>" + money(c.total) + "</td></tr>";
        });
        html += "</table>";

        html += "<table><tr><th>Expenses by category</th><th>Amount</th></tr>";
        summary.expensesByCategory.forEach(function (c) {
            html += "<tr><td>" + niceText(c.category) + "</td><td>" + money(c.total) + "</td></tr>";
        });
        html += "</table>";
        return html;
    }

    function entriesHtml(list, withEditButtons) {
        if (list.length === 0) {
            return "<p>No entries for these dates.</p>";
        }
        let html = '<div class="table-scroll"><table><tr><th>Date</th><th>Type</th><th>Category</th><th>Description</th><th>Paid to / from</th><th>Ref #</th><th>Amount</th>';
        html += withEditButtons ? "<th></th></tr>" : "</tr>";
        list.forEach(function (e) {
            html += "<tr><td>" + clean(e.entryDate) + "</td><td>" + niceText(e.type) + "</td><td>" + niceText(e.category) +
                "</td><td>" + clean(e.description) + "</td><td>" + clean(e.payee) + "</td><td>" + clean(e.referenceNumber) +
                "</td><td>" + money(e.amount) + "</td>";
            if (withEditButtons) {
                html += '<td><button class="small grey edit-button" data-id="' + e.id + '">Edit</button></td>';
            }
            html += "</tr>";
        });
        html += "</table></div>";
        return html;
    }

    document.getElementById("load-summary").addEventListener("click", loadSummary);

    function loadSummary() {
        api("GET", "/api/expenditure/summary?from=" + from.value + "&to=" + to.value).then(summary => {
            let url = "/api/expenditure/entries?from=" + from.value + "&to=" + to.value;
            const type = document.getElementById("type-filter").value;
            if (type !== "") {
                url += "&type=" + type;
            }
            return api("GET", url).then(list => {
                entries = list;
                summaryResult.innerHTML = totalsHtml(summary) + "<h3>Entries</h3>" + entriesHtml(entries, true);
            });
        }).catch(error => {
            summaryResult.innerHTML = '<p class="error">' + clean(error.message) + "</p>";
        });
    }

    // Edit puts the entry into the form
    summaryResult.addEventListener("click", function (event) {
        if (!event.target.classList.contains("edit-button")) return;
        const entry = entries.find(e => String(e.id) === event.target.dataset.id);

        editingId = entry.id;
        typeSelect.value = entry.type;
        fillCategories(entry.category);
        document.getElementById("amount").value = entry.amount;
        document.getElementById("entry-date").value = entry.entryDate;
        document.getElementById("description").value = entry.description || "";
        document.getElementById("payee").value = entry.payee || "";
        document.getElementById("reference").value = entry.referenceNumber || "";
        document.getElementById("form-title").textContent = "Edit Entry";
        document.getElementById("cancel-button").hidden = false;
        form.scrollIntoView();
    });

    function resetForm() {
        editingId = null;
        form.reset();
        fillCategories();
        document.getElementById("entry-date").value = todayDate();
        document.getElementById("form-title").textContent = "Add Income or Expense";
        document.getElementById("cancel-button").hidden = true;
    }

    document.getElementById("cancel-button").addEventListener("click", resetForm);

    form.addEventListener("submit", function (event) {
        event.preventDefault();

        const entry = {
            type: typeSelect.value,
            category: categorySelect.value,
            amount: Number(document.getElementById("amount").value),
            entryDate: document.getElementById("entry-date").value,
            description: document.getElementById("description").value.trim(),
            payee: document.getElementById("payee").value.trim(),
            referenceNumber: document.getElementById("reference").value.trim()
        };

        if (!entry.amount) {
            showMessage("form-message", "Please enter an amount.", true);
            return;
        }

        let request;
        if (editingId) {
            request = api("PUT", "/api/expenditure/entries/" + editingId, entry);
        } else {
            request = api("POST", "/api/expenditure/entries", entry);
        }

        request.then(() => {
            showMessage("form-message", "Entry saved.", false);
            resetForm();
            loadSummary();
        }).catch(error => {
            showMessage("form-message", error.message, true);
        });
    });

    document.getElementById("load-monthly").addEventListener("click", function () {
        const parts = document.getElementById("month").value.split("-");
        const result = document.getElementById("monthly-result");

        api("GET", "/api/expenditure/reports/monthly?year=" + parts[0] + "&month=" + Number(parts[1])).then(report => {
            result.innerHTML = totalsHtml(report) + "<h3>Entries</h3>" + entriesHtml(report.entries, false);
        }).catch(error => {
            result.innerHTML = '<p class="error">' + clean(error.message) + "</p>";
        });
    });

    loadSummary();
});
