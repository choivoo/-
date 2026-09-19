(() => {
  const images = window.GAUNTLET_IMAGES || [];
  const labels = ["정면","후면"];
  const hero = document.getElementById("heroImage");
  const main = document.getElementById("galleryMain");
  const detail = document.getElementById("detailImage");
  const thumbs = document.getElementById("thumbs");
  const label = document.getElementById("galleryLabel");
  const lightbox = document.getElementById("lightbox");
  const lightboxImage = document.getElementById("lightboxImage");
  const lightboxCaption = document.getElementById("lightboxCaption");

  if (images.length) {
    hero.src = images[0];
    main.src = images[0];
    detail.src = images[1] || images[0];

    images.forEach((src, i) => {
      const btn = document.createElement("button");
      btn.className = "thumb" + (i === 0 ? " active" : "");
      btn.setAttribute("aria-label", labels[i] || ("사진 " + (i+1)));
      const img = document.createElement("img");
      img.src = src;
      img.alt = labels[i] || ("건틀렛 사진 " + (i+1));
      btn.appendChild(img);
      btn.addEventListener("click", () => {
        main.src = src;
        label.textContent = labels[i] || ("사진 " + (i+1));
        document.querySelectorAll(".thumb").forEach(el => el.classList.remove("active"));
        btn.classList.add("active");
      });
      thumbs.appendChild(btn);
    });
  }

  document.querySelector(".main-shot")?.addEventListener("click", () => {
    lightboxImage.src = main.src;
    lightboxCaption.textContent = label.textContent;
    lightbox.classList.add("open");
    lightbox.setAttribute("aria-hidden","false");
  });
  const close = () => {
    lightbox.classList.remove("open");
    lightbox.setAttribute("aria-hidden","true");
  };
  document.querySelector(".close")?.addEventListener("click", close);
  lightbox?.addEventListener("click", e => { if (e.target === lightbox) close(); });
  window.addEventListener("keydown", e => { if (e.key === "Escape") close(); });

  const obs = new IntersectionObserver(entries => entries.forEach(entry => {
    if (entry.isIntersecting) entry.target.animate(
      [{opacity:0, transform:"translateY(20px)"},{opacity:1, transform:"translateY(0)"}],
      {duration:620, easing:"cubic-bezier(.2,.7,.2,1)", fill:"both"}
    );
  }), {threshold:.08});
  document.querySelectorAll(".section").forEach(el => obs.observe(el));
})();