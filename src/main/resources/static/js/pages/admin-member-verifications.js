(() => {
    document.querySelectorAll("[data-verification-form]").forEach((form) => {
        const selectAll = form.querySelector("[data-select-all]");
        const rowCheckboxes = [...form.querySelectorAll("[data-row-checkbox]")];
        const selectedCount = form.querySelector("[data-selected-count]");
        const mode = form.dataset.verificationForm;

        const updateSelection = () => {
            const count = rowCheckboxes.filter((checkbox) => checkbox.checked).length;
            selectedCount.textContent = String(count);
            selectAll.checked = rowCheckboxes.length > 0 && count === rowCheckboxes.length;
            selectAll.indeterminate = count > 0 && count < rowCheckboxes.length;
        };

        selectAll.addEventListener("change", () => {
            rowCheckboxes.forEach((checkbox) => {
                checkbox.checked = selectAll.checked;
            });
            updateSelection();
        });

        rowCheckboxes.forEach((checkbox) => {
            checkbox.addEventListener("change", updateSelection);
        });

        form.addEventListener("submit", (event) => {
            const count = rowCheckboxes.filter((checkbox) => checkbox.checked).length;
            if (count === 0) {
                event.preventDefault();
                alert(mode === "process"
                    ? "임시 본인인증 처리할 회원을 선택해주세요."
                    : "임시 본인인증을 취소할 회원을 선택해주세요.");
                return;
            }

            const reason = form.querySelector("[name='reason']");
            if (!reason.value.trim()) {
                event.preventDefault();
                alert(mode === "process"
                    ? "인증 처리 사유를 입력해주세요."
                    : "취소 사유를 입력해주세요.");
                reason.focus();
                return;
            }

            const message = mode === "process"
                ? `선택한 ${count}명의 회원을 임시 본인인증 완료 상태로 처리하시겠습니까?\n회원가입 시 입력된 이름, 생년월일, 성별 및 휴대폰 번호가 본인인증 정보로 사용됩니다.`
                : `선택한 ${count}명의 임시 본인인증을 취소하시겠습니까?\n본인인증이 취소된 회원은 모임 개설 및 가입이 제한될 수 있습니다.`;
            if (!confirm(message)) {
                event.preventDefault();
            }
        });

        updateSelection();
    });
})();
