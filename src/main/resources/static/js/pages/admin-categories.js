document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll("[data-confirm-form]").forEach((form) => {
        form.addEventListener("submit", (event) => {
            const message = form.dataset.confirmForm;
            if (message && !window.confirm(message)) {
                event.preventDefault();
            }
        });
    });
});
