(() => {
    "use strict";

    const form = document.getElementById("password-change-form");
    if (!form) return;

    const currentPassword = document.getElementById("currentPassword");
    const newPassword = document.getElementById("newPassword");
    const newPasswordConfirm = document.getElementById("newPasswordConfirm");
    const matchStatus = document.getElementById("password-match-status");
    const submitStatus = document.getElementById("submit-status");
    const changeButton = document.getElementById("change-button");

    document.querySelectorAll(".password-toggle").forEach((button) => {
        button.addEventListener("click", () => {
            const input = document.getElementById(button.dataset.target);
            const show = input.type === "password";
            input.type = show ? "text" : "password";
            button.textContent = show ? "숨김" : "표시";
            button.setAttribute("aria-pressed", String(show));
            button.setAttribute("aria-label", `${input.labels[0].textContent.trim()} ${show ? "숨기기" : "표시"}`);
        });
    });

    newPassword.addEventListener("input", updatePasswordMatch);
    newPasswordConfirm.addEventListener("input", updatePasswordMatch);

    form.addEventListener("submit", (event) => {
        submitStatus.textContent = "";
        const error = validate();
        if (error) {
            event.preventDefault();
            submitStatus.textContent = error.message;
            error.input.focus();
            return;
        }

        changeButton.disabled = true;
        changeButton.textContent = "변경 중...";
    });

    function validate() {
        if (!currentPassword.value) return { input: currentPassword, message: "현재 비밀번호를 입력해 주세요." };
        if (!newPassword.value) return { input: newPassword, message: "새 비밀번호를 입력해 주세요." };
        if (!isValidPassword(newPassword.value)) {
            return { input: newPassword, message: "새 비밀번호 형식을 확인해 주세요." };
        }
        if (!newPasswordConfirm.value) {
            return { input: newPasswordConfirm, message: "새 비밀번호 확인을 입력해 주세요." };
        }
        if (newPassword.value !== newPasswordConfirm.value) {
            return { input: newPasswordConfirm, message: "새 비밀번호와 확인값이 일치하지 않습니다." };
        }
        return null;
    }

    function isValidPassword(value) {
        if (value.length < 8 || value.length > 255 || /\s/.test(value)) return false;
        const categories = [/[A-Za-z]/.test(value), /\d/.test(value), /[^A-Za-z0-9\s]/.test(value)];
        return categories.filter(Boolean).length >= 2;
    }

    function updatePasswordMatch() {
        if (!newPasswordConfirm.value) {
            matchStatus.textContent = "";
            matchStatus.className = "field-status";
            return;
        }
        const matches = newPassword.value === newPasswordConfirm.value;
        matchStatus.textContent = matches ? "비밀번호가 일치합니다." : "비밀번호가 일치하지 않습니다.";
        matchStatus.className = `field-status is-${matches ? "success" : "error"}`;
    }
})();
