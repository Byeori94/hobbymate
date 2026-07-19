(() => {
    "use strict";

    const form = document.getElementById("withdraw-form");
    if (!form) return;

    const password = document.getElementById("currentPassword");
    const consent = document.getElementById("withdrawAgreed");
    const passwordToggle = document.getElementById("password-toggle");
    const submitStatus = document.getElementById("submit-status");
    const withdrawButton = document.getElementById("withdraw-button");

    passwordToggle.addEventListener("click", () => {
        const show = password.type === "password";
        password.type = show ? "text" : "password";
        passwordToggle.textContent = show ? "숨김" : "표시";
        passwordToggle.setAttribute("aria-pressed", String(show));
        passwordToggle.setAttribute("aria-label", show ? "현재 비밀번호 숨기기" : "현재 비밀번호 표시");
    });

    form.addEventListener("submit", (event) => {
        submitStatus.textContent = "";
        if (!password.value) {
            event.preventDefault();
            submitStatus.textContent = "현재 비밀번호를 입력해 주세요.";
            password.focus();
            return;
        }
        if (!consent.checked) {
            event.preventDefault();
            submitStatus.textContent = "회원 탈퇴 안내를 확인하고 동의해 주세요.";
            consent.focus();
            return;
        }
        if (!window.confirm("정말 회원 탈퇴를 진행하시겠습니까?")) {
            event.preventDefault();
            return;
        }

        withdrawButton.disabled = true;
        withdrawButton.textContent = "회원 탈퇴 중...";
    });
})();
