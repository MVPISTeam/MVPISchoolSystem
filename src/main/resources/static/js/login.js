document.addEventListener("DOMContentLoaded", function () {
    const form = document.getElementById("login-form");
    const message = document.getElementById("message");

    form.addEventListener("submit", function (event) {
        event.preventDefault();

        const username = document.getElementById("username").value.trim();
        const password = document.getElementById("password").value;

        if (username === "" || password === "") {
            message.textContent = "Please enter your username and password.";
            return;
        }

        // The API uses HTTP Basic login, so we send the username and password in a header
        const auth = btoa(username + ":" + password);

        fetch("/api/auth/me", {
            headers: { "Authorization": "Basic " + auth }
        })
        .then(response => {
            if (response.status === 401) {
                throw new Error("Wrong username or password.");
            }
            if (!response.ok) {
                throw new Error("Something went wrong, please try again.");
            }
            return response.json();
        })
        .then(user => {
            // Remember the login until the tab is closed
            sessionStorage.setItem("auth", auth);
            sessionStorage.setItem("fullName", user.fullName);
            sessionStorage.setItem("role", user.role);
            window.location.href = "portal/dashboard.html";
        })
        .catch(error => {
            message.textContent = error.message;
        });
    });
});
