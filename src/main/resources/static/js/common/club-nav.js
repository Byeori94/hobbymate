(() => {
    document.querySelectorAll("[data-club-nav-unavailable]").forEach((menuButton) => {
        menuButton.addEventListener("click", () => {
            const message = menuButton.dataset.unavailableMessage || "준비 중인 기능입니다.";
            window.alert(message);
        });
    });
})();
