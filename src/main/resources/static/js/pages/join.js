(() => {
    "use strict";

    const form = document.getElementById("join-form");
    if (!form) {
        return;
    }

    const checkConfig = {
        loginId: {
            endpoint: form.dataset.checkIdUrl,
            validate(value) {
                if (!value) return "아이디를 입력해 주세요.";
                if (/[A-Z]/.test(value)) return "아이디에는 영문 대문자를 사용할 수 없습니다.";
                if (!/^[a-z][a-z0-9]{4,19}$/.test(value)) return "아이디 형식을 확인해 주세요.";
                return "";
            }
        },
        nickname: {
            endpoint: form.dataset.checkNicknameUrl,
            validate(value) {
                if (!value || value.length > 50) return "닉네임 형식을 확인해 주세요.";
                return "";
            }
        },
        email: {
            endpoint: form.dataset.checkEmailUrl,
            validate(value) {
                const email = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
                if (!value || value.length > 255 || !email.test(value)) return "이메일 형식을 확인해 주세요.";
                return "";
            }
        }
    };

    const checkState = {
        loginId: false,
        nickname: false,
        email: false
    };

    Object.keys(checkConfig).forEach((fieldName) => {
        const input = document.getElementById(fieldName);
        const button = form.querySelector(`[data-check="${fieldName}"]`);

        input.addEventListener("input", () => {
            resetCheck(fieldName);
            if (fieldName === "loginId") {
                const message = checkConfig.loginId.validate(input.value.trim());
                if (/[A-Z]/.test(input.value)) {
                    setStatus(fieldName, message, "error");
                }
            }
        });

        button.addEventListener("click", () => checkAvailability(fieldName, input, button));
    });

    async function checkAvailability(fieldName, input, button) {
        const value = input.value.trim();
        const validationMessage = checkConfig[fieldName].validate(value);
        if (validationMessage) {
            setStatus(fieldName, validationMessage, "error");
            input.focus();
            return;
        }

        button.disabled = true;
        setStatus(fieldName, "확인 중입니다.", "");

        try {
            const response = await fetch(`${checkConfig[fieldName].endpoint}?value=${encodeURIComponent(value)}`, {
                headers: { "Accept": "application/json" }
            });
            if (!response.ok) {
                throw new Error("availability request failed");
            }
            const result = await response.json();
            checkState[fieldName] = Boolean(result.available);
            input.dataset.verifiedValue = result.available ? value : "";
            setStatus(fieldName, result.message, result.available ? "success" : "error");
        } catch {
            resetCheck(fieldName);
            setStatus(fieldName, "중복 확인 중 네트워크 오류가 발생했습니다.", "error");
        } finally {
            button.disabled = false;
        }
    }

    function resetCheck(fieldName) {
        checkState[fieldName] = false;
        const input = document.getElementById(fieldName);
        delete input.dataset.verifiedValue;
        setStatus(fieldName, "", "");
    }

    function setStatus(fieldName, message, type) {
        const status = document.getElementById(`${fieldName}-status`);
        status.textContent = message;
        status.className = "field-status";
        if (type) {
            status.classList.add(`is-${type}`);
        }
    }

    const password = document.getElementById("password");
    const passwordConfirm = document.getElementById("passwordConfirm");
    const passwordStatus = document.getElementById("passwordConfirm-status");

    function updatePasswordMatch() {
        if (!passwordConfirm.value) {
            passwordStatus.textContent = "";
            passwordStatus.className = "field-status";
            return;
        }
        const matches = password.value === passwordConfirm.value;
        passwordStatus.textContent = matches ? "비밀번호가 일치합니다." : "비밀번호가 일치하지 않습니다.";
        passwordStatus.className = `field-status is-${matches ? "success" : "error"}`;
    }

    password.addEventListener("input", updatePasswordMatch);
    passwordConfirm.addEventListener("input", updatePasswordMatch);

    const phone = document.getElementById("phone");
    phone.addEventListener("input", () => {
        const digits = phone.value.replace(/\D/g, "").slice(0, 11);
        if (digits.length <= 3) {
            phone.value = digits;
        } else if (digits.length <= 7) {
            phone.value = `${digits.slice(0, 3)}-${digits.slice(3)}`;
        } else {
            phone.value = `${digits.slice(0, 3)}-${digits.slice(3, 7)}-${digits.slice(7)}`;
        }
    });

    const agreeAll = document.getElementById("agreeAll");
    const requiredAgreements = Array.from(form.querySelectorAll(".required-agreement"));

    agreeAll.addEventListener("change", () => {
        requiredAgreements.forEach((checkbox) => {
            checkbox.checked = agreeAll.checked;
        });
    });

    requiredAgreements.forEach((checkbox) => {
        checkbox.addEventListener("change", () => {
            agreeAll.checked = requiredAgreements.every((item) => item.checked);
        });
    });
    agreeAll.checked = requiredAgreements.every((item) => item.checked);

    form.addEventListener("submit", (event) => {
        const submitStatus = document.getElementById("submit-status");
        submitStatus.textContent = "";

        if (/[A-Z]/.test(document.getElementById("loginId").value)) {
            event.preventDefault();
            setStatus("loginId", "아이디에는 영문 대문자를 사용할 수 없습니다.", "error");
            document.getElementById("loginId").focus();
            return;
        }

        const unchecked = Object.keys(checkState).find((fieldName) => {
            const input = document.getElementById(fieldName);
            return !checkState[fieldName] || input.dataset.verifiedValue !== input.value.trim();
        });
        if (unchecked) {
            event.preventDefault();
            setStatus(unchecked, "중복 확인이 필요합니다.", "error");
            document.getElementById(unchecked).focus();
            submitStatus.textContent = "아이디, 닉네임, 이메일 중복 확인을 완료해 주세요.";
            return;
        }

        if (password.value !== passwordConfirm.value) {
            event.preventDefault();
            updatePasswordMatch();
            passwordConfirm.focus();
            return;
        }

        if (!requiredAgreements.every((checkbox) => checkbox.checked)) {
            event.preventDefault();
            submitStatus.textContent = "필수 약관에 동의해 주세요.";
            return;
        }

        if (!form.checkValidity()) {
            event.preventDefault();
            form.reportValidity();
        }
    });
})();
