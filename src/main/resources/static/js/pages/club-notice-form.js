document.addEventListener("DOMContentLoaded", () => {
    const form = document.querySelector(
        "[data-notice-create-form], [data-notice-update-form]"
    );
    if (!form) {
        return;
    }

    const content = form.querySelector("#content");
    const counter = form.querySelector("[data-content-count]");
    const submitButton = form.querySelector("[data-submit-button]");

    const updateContentCount = () => {
        if (content && counter) {
            counter.textContent = `${content.value.length.toLocaleString("ko-KR")} / 10,000`;
        }
    };

    content?.addEventListener("input", updateContentCount);
    updateContentCount();

    form.addEventListener("submit", (event) => {
        if (form.dataset.submitting === "true") {
            event.preventDefault();
            return;
        }
        form.dataset.submitting = "true";
        if (submitButton) {
            submitButton.disabled = true;
            submitButton.textContent =
                form.dataset.submittingText || "처리 중...";
        }
    });
});
