(() => {
    const uploadForm = document.querySelector(".profile-image-upload-form");
    const deleteForm = document.querySelector(".profile-image-delete-form");
    const fileInput = document.querySelector("#profileImage");
    const preview = document.querySelector("#profile-image-preview");
    const fileName = document.querySelector(".profile-file-name");
    const errorMessage = document.querySelector(".profile-file-error");
    const saveButton = document.querySelector(".profile-image-save");
    const allowedTypes = new Set(["image/jpeg", "image/png", "image/webp"]);
    const maxFileSize = 5 * 1024 * 1024;
    let previewUrl = null;

    const releasePreviewUrl = () => {
        if (previewUrl) {
            URL.revokeObjectURL(previewUrl);
            previewUrl = null;
        }
    };

    const restorePreview = () => {
        releasePreviewUrl();
        if (preview) {
            preview.src = preview.dataset.originalSrc || preview.dataset.defaultSrc;
            preview.alt = "현재 프로필 사진";
        }
    };

    const resetInvalidSelection = (message) => {
        if (fileInput) {
            fileInput.value = "";
        }
        if (fileName) {
            fileName.textContent = "선택된 파일 없음";
        }
        if (errorMessage) {
            errorMessage.textContent = message;
        }
        if (saveButton) {
            saveButton.disabled = true;
        }
        restorePreview();
    };

    fileInput?.addEventListener("change", () => {
        const file = fileInput.files?.[0];
        if (!file) {
            resetInvalidSelection("");
            return;
        }
        if (file.size === 0) {
            resetInvalidSelection("빈 파일은 등록할 수 없습니다.");
            return;
        }
        if (file.size > maxFileSize) {
            resetInvalidSelection("5MB 이하의 이미지만 등록할 수 있습니다.");
            return;
        }
        if (!allowedTypes.has(file.type)) {
            resetInvalidSelection("JPG, JPEG, PNG, WEBP 파일만 등록할 수 있습니다.");
            return;
        }

        releasePreviewUrl();
        previewUrl = URL.createObjectURL(file);
        preview.src = previewUrl;
        preview.alt = "선택한 프로필 사진 미리보기";
        fileName.textContent = file.name;
        errorMessage.textContent = "";
        saveButton.disabled = false;
    });

    uploadForm?.addEventListener("submit", (event) => {
        if (!fileInput.files?.[0]) {
            event.preventDefault();
            resetInvalidSelection("프로필 사진을 선택해 주세요.");
            return;
        }
        saveButton.disabled = true;
        saveButton.textContent = "저장 중...";
    });

    deleteForm?.addEventListener("submit", (event) => {
        if (!window.confirm(deleteForm.dataset.confirmMessage)) {
            event.preventDefault();
            return;
        }
        const deleteButton = deleteForm.querySelector("button[type='submit']");
        deleteButton.disabled = true;
        deleteButton.textContent = "삭제 중...";
    });

    window.addEventListener("beforeunload", releasePreviewUrl);
})();
