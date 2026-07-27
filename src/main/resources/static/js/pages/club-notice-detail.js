(() => {
    document.querySelectorAll("[data-notice-unavailable]").forEach((button) => {
        button.addEventListener("click", () => {
            window.alert(button.dataset.message || "준비 중인 기능입니다.");
        });
    });
})();
