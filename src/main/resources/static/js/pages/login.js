(() => {
    const form = document.querySelector("#login-form");
    const loginId = document.querySelector("#loginId");
    const password = document.querySelector("#password");
    const loginIdError = document.querySelector("#loginId-error");
    const passwordError = document.querySelector("#password-error");
    const submitError = document.querySelector("#submit-error");
    const submitButton = document.querySelector("#login-button");
    const passwordToggle = document.querySelector("#password-toggle");
    const loginIdPattern = /^[a-z][a-z0-9]{4,19}$/;

    if (!form) {
        return;
    }

    passwordToggle.addEventListener("click", () => {
        const showPassword = password.type === "password";
        password.type = showPassword ? "text" : "password";
        passwordToggle.textContent = showPassword ? "숨김" : "표시";
        passwordToggle.setAttribute("aria-label", showPassword ? "비밀번호 숨기기" : "비밀번호 표시");
    });

    form.addEventListener("submit", (event) => {
        loginId.value = loginId.value.trim();
        loginIdError.textContent = "";
        passwordError.textContent = "";
        submitError.textContent = "";

        let valid = true;
        if (!loginId.value) {
            loginIdError.textContent = "아이디를 입력해 주세요.";
            valid = false;
        } else if (!loginIdPattern.test(loginId.value)) {
            loginIdError.textContent = "영문 소문자와 숫자로 된 올바른 아이디를 입력해 주세요.";
            valid = false;
        }

        if (!password.value) {
            passwordError.textContent = "비밀번호를 입력해 주세요.";
            valid = false;
        }

        if (!valid) {
            event.preventDefault();
            submitError.textContent = "입력 내용을 확인해 주세요.";
            return;
        }

        submitButton.disabled = true;
        submitButton.textContent = "로그인 중...";
    });
})();
