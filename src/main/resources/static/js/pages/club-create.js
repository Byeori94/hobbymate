(() => {
    const form = document.querySelector(".club-create-form");
    if (!form) {
        return;
    }

    const imageInput = form.querySelector("#representativeImage");
    const preview = form.querySelector("[data-image-preview]");
    const previewImage = preview?.querySelector("img");
    const previewText = preview?.querySelector("span");
    const description = form.querySelector("#clubDescription");
    const descriptionCount = form.querySelector("[data-description-count]");
    const joinTypeInputs = [...form.querySelectorAll('input[name="joinType"]')];
    const joinGuideField = form.querySelector("[data-join-guide-field]");
    const joinGuide = form.querySelector("#joinGuide");
    const submitButton = form.querySelector("[data-submit-button]");
    let previewUrl;

    const updateDescriptionCount = () => {
        if (description && descriptionCount) {
            descriptionCount.textContent =
                `${description.value.length.toLocaleString()} / 2,000`;
        }
    };

    const updateJoinGuide = () => {
        const approval = joinTypeInputs.some(
            (input) => input.checked && input.value === "APPROVAL");
        if (joinGuideField) {
            joinGuideField.hidden = !approval;
        }
        if (joinGuide) {
            joinGuide.disabled = !approval;
        }
    };

    imageInput?.addEventListener("change", () => {
        if (previewUrl) {
            URL.revokeObjectURL(previewUrl);
            previewUrl = undefined;
        }
        const [file] = imageInput.files;
        if (!file || !previewImage || !previewText) {
            if (previewImage) {
                previewImage.hidden = true;
            }
            if (previewText) {
                previewText.hidden = false;
            }
            return;
        }
        if (file.size > 5 * 1024 * 1024) {
            imageInput.setCustomValidity("대표 이미지는 5MB 이하만 등록할 수 있습니다.");
        } else {
            imageInput.setCustomValidity("");
        }
        previewUrl = URL.createObjectURL(file);
        previewImage.src = previewUrl;
        previewImage.hidden = false;
        previewText.hidden = true;
    });

    description?.addEventListener("input", updateDescriptionCount);
    joinTypeInputs.forEach((input) => input.addEventListener("change", updateJoinGuide));

    form.addEventListener("submit", (event) => {
        const minAge = Number(form.querySelector("#minAge")?.value);
        const maxAge = Number(form.querySelector("#maxAge")?.value);
        const maxAgeInput = form.querySelector("#maxAge");
        if (maxAgeInput) {
            maxAgeInput.setCustomValidity(
                minAge && maxAge && minAge > maxAge
                    ? "최대 연령은 최소 연령보다 작을 수 없습니다."
                    : "");
        }
        if (!form.checkValidity()) {
            event.preventDefault();
            form.reportValidity();
            return;
        }
        if (submitButton) {
            submitButton.disabled = true;
            submitButton.textContent = "개설 중...";
        }
    });

    updateDescriptionCount();
    updateJoinGuide();
})();
