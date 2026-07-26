(() => {
    document.querySelectorAll("[data-individual-confirm]").forEach((form) => {
        form.addEventListener("submit", (event) => {
            const mode = form.dataset.individualConfirm;
            const message = mode === "process"
                ? "해당 회원을 임시 본인인증 완료 상태로 처리하시겠습니까?"
                : "해당 회원의 임시 본인인증을 취소하시겠습니까?\n본인인증이 취소되면 모임 개설 및 가입이 제한될 수 있습니다.";
            if (!confirm(message)) {
                event.preventDefault();
            }
        });
    });
})();
