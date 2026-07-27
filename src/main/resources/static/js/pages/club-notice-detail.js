document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll("[data-notice-delete-form]").forEach((form) => {
        const deleteButton = form.querySelector("[data-delete-button]");

        form.addEventListener("submit", (event) => {
            if (form.dataset.submitting === "true") {
                event.preventDefault();
                return;
            }
            if (!window.confirm("공지사항을 삭제하시겠습니까?")) {
                event.preventDefault();
                return;
            }

            form.dataset.submitting = "true";
            if (deleteButton) {
                deleteButton.disabled = true;
                deleteButton.textContent = "삭제 중...";
            }
        });
    });
});
