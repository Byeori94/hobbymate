(() => {
    document.querySelectorAll("[data-profile-image-fallback]").forEach((image) => {
        image.addEventListener("error", () => {
            const defaultSource = image.dataset.defaultSrc;
            image.removeAttribute("data-profile-image-fallback");
            if (defaultSource && image.src !== defaultSource) {
                image.src = defaultSource;
            }
        }, { once: true });
    });

    const menus = document.querySelectorAll(".profile-menu");

    const closeMenu = (menu, restoreFocus = false) => {
        const toggle = menu.querySelector(".profile-menu-toggle");
        const dropdown = menu.querySelector(".profile-dropdown");
        toggle.setAttribute("aria-expanded", "false");
        dropdown.hidden = true;
        if (restoreFocus) {
            toggle.focus();
        }
    };

    menus.forEach((menu) => {
        const toggle = menu.querySelector(".profile-menu-toggle");
        const dropdown = menu.querySelector(".profile-dropdown");
        const menuItems = dropdown.querySelectorAll("[role='menuitem']");

        toggle.addEventListener("click", () => {
            const willOpen = toggle.getAttribute("aria-expanded") !== "true";
            menus.forEach((otherMenu) => closeMenu(otherMenu));
            toggle.setAttribute("aria-expanded", String(willOpen));
            dropdown.hidden = !willOpen;
        });

        toggle.addEventListener("keydown", (event) => {
            if (event.key === "ArrowDown") {
                event.preventDefault();
                toggle.setAttribute("aria-expanded", "true");
                dropdown.hidden = false;
                menuItems[0]?.focus();
            }
        });

        menu.addEventListener("keydown", (event) => {
            if (event.key === "Escape") {
                event.preventDefault();
                closeMenu(menu, true);
            }
        });
    });

    document.addEventListener("click", (event) => {
        menus.forEach((menu) => {
            if (!menu.contains(event.target)) {
                closeMenu(menu);
            }
        });
    });
})();
