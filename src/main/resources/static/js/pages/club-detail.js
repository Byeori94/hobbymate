(() => {
    document.querySelectorAll("[data-club-image-fallback]").forEach((image) => {
        image.addEventListener("error", () => {
            const defaultSource = image.dataset.defaultSrc;
            image.removeAttribute("data-club-image-fallback");
            if (defaultSource && image.src !== defaultSource) {
                image.src = defaultSource;
                image.classList.add("club-default-image");
            }
        }, { once: true });
    });
})();
