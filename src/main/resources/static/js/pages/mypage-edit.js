(() => {
    "use strict";

    const form = document.getElementById("profile-edit-form");
    if (!form) {
        return;
    }

    const nicknameInput = document.getElementById("nickname");
    const emailInput = document.getElementById("email");
    const checkButton = document.getElementById("nickname-check-button");
    const saveButton = document.getElementById("save-button");
    const originalNickname = form.dataset.originalNickname.trim();
    let checkedNickname = "";

    nicknameInput.addEventListener("input", () => {
        checkedNickname = "";
        setNicknameStatus("", "");
    });

    emailInput.addEventListener("input", () => {
        setEmailStatus("");
    });

    checkButton.addEventListener("click", async () => {
        const nickname = nicknameInput.value.trim();
        const validationMessage = validateNickname(nickname);
        if (validationMessage) {
            setNicknameStatus(validationMessage, "error");
            nicknameInput.focus();
            return;
        }

        if (nickname === originalNickname) {
            checkedNickname = nickname;
            setNicknameStatus("현재 사용 중인 닉네임입니다.", "success");
            return;
        }

        checkButton.disabled = true;
        setNicknameStatus("확인 중입니다.", "");
        try {
            const url = new URL(form.dataset.checkNicknameUrl, window.location.origin);
            url.search = new URLSearchParams({ value: nickname }).toString();
            const response = await fetch(url, { headers: { "Accept": "application/json" } });
            if (!response.ok) {
                throw new Error("nickname availability request failed");
            }

            const result = await response.json();
            checkedNickname = result.available ? nickname : "";
            setNicknameStatus(result.message, result.available ? "success" : "error");
        } catch {
            checkedNickname = "";
            setNicknameStatus("중복 확인 중 네트워크 오류가 발생했습니다.", "error");
        } finally {
            checkButton.disabled = false;
        }
    });

    form.addEventListener("submit", (event) => {
        const nickname = nicknameInput.value.trim();
        const email = emailInput.value.trim();
        const nicknameMessage = validateNickname(nickname);
        const emailMessage = validateEmail(email);

        nicknameInput.value = nickname;
        emailInput.value = email;
        document.getElementById("submit-status").textContent = "";

        if (nicknameMessage) {
            event.preventDefault();
            setNicknameStatus(nicknameMessage, "error");
            nicknameInput.focus();
            return;
        }
        if (emailMessage) {
            event.preventDefault();
            setEmailStatus(emailMessage);
            emailInput.focus();
            return;
        }
        if (nickname !== originalNickname && checkedNickname !== nickname) {
            event.preventDefault();
            setNicknameStatus("닉네임 중복확인이 필요합니다.", "error");
            nicknameInput.focus();
            return;
        }

        saveButton.disabled = true;
        saveButton.textContent = "저장 중...";
    });

    function validateNickname(value) {
        if (!value) return "닉네임을 입력해 주세요.";
        if (value.length > 50) return "닉네임은 50자 이하로 입력해 주세요.";
        return "";
    }

    function validateEmail(value) {
        const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!value) return "이메일을 입력해 주세요.";
        if (value.length > 255) return "이메일은 255자 이하로 입력해 주세요.";
        if (!emailPattern.test(value)) return "이메일 형식이 올바르지 않습니다.";
        return "";
    }

    function setNicknameStatus(message, type) {
        const status = document.getElementById("nickname-status");
        status.textContent = message;
        status.className = "field-status";
        if (type) {
            status.classList.add(`is-${type}`);
        }
    }

    function setEmailStatus(message) {
        const status = document.getElementById("email-status");
        status.textContent = message;
        status.className = message ? "field-status is-error" : "field-status";
    }
})();
