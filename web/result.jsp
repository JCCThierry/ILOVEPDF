<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!doctype html>
<html lang="fr">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>Conversion réussie — PDF Tools</title>
  <link rel="stylesheet" href="style.css" type="text/css">
</head>
<body>

<header class="topbar">
  <div class="container topbar__inner">
    <a class="brand" href="index.html">
      <span class="brand__logo" aria-hidden="true">PDF</span>
      <span class="brand__name">PDF Tools</span>
    </a>
    <nav class="nav">
      <a href="index.html" class="nav__link">Convertir</a>
    </nav>
    <button class="burger" aria-label="Menu">
      <span></span><span></span><span></span>
    </button>
  </div>
</header>

<main class="main">
  <div class="container">
    <section class="hero">
      <div class="hero__badge">Résultat</div>
      <h1 class="hero__title">Conversion réussie ✅</h1>
      <p class="hero__subtitle">Ou kapab telechaje, wè, oswa pataje fichye a.</p>
    </section>

    <section class="card">
      <p style="margin:0 0 12px 0;">
        <strong>Fichier :</strong> <span id="outName">...</span>
      </p>

      <div class="actions-row">
        <a id="downloadLink" class="btn btn--primary" href="#">Télécharger</a>
        <a id="previewLink" class="btn btn--secondary" href="#" target="_blank">Voir (Aperçu)</a>
        <button id="shareFileBtn" class="btn btn--secondary" type="button">Partager (fichier)</button>
      </div>

      <div class="actions-row" style="margin-top:10px;">
        <a id="waLink" class="btn btn--secondary" href="#" target="_blank">Partager WhatsApp</a>
        <a id="tgLink" class="btn btn--secondary" href="#" target="_blank">Partager Telegram</a>
      </div>

      <p style="margin-top:12px; font-size:.95rem; opacity:.85">
        Nòt: WhatsApp/Telegram “Partager” ap pataje <b>yon lyen</b>. Si sit la pa piblik sou entènèt,
        lòt moun p ap ka louvri lyen an. Nan ka sa, itilize <b>Partager (fichier)</b> sou mobil (Web Share),
        oswa telechaje epi pataje fichye a soti nan Downloads.
      </p>
    </section>
  </div>
</main>

<footer class="footer">
  <div class="container footer__inner">
    <p>© 2026 — Votre éditeur PDF</p>
    <div class="footer__links">
      <a href="#">Confidentialité</a>
      <a href="#">Conditions</a>
      <a href="#">Aide</a>
    </div>
  </div>
</footer>

<script>
  const token = new URLSearchParams(location.search).get("token");

  const outName = document.getElementById("outName");
  const downloadLink = document.getElementById("downloadLink");
  const previewLink = document.getElementById("previewLink");
  const waLink = document.getElementById("waLink");
  const tgLink = document.getElementById("tgLink");
  const shareFileBtn = document.getElementById("shareFileBtn");

  if (!token) {
    outName.textContent = "Token manquant";
  } else {
    const downloadUrl = "download?token=" + encodeURIComponent(token);
    const previewUrl  = "preview?token=" + encodeURIComponent(token);

    downloadLink.href = downloadUrl;
    previewLink.href  = previewUrl;

    fetch("result-info?token=" + encodeURIComponent(token))
      .then(r => r.ok ? r.json() : Promise.reject())
      .then(d => outName.textContent = d.filename || "Fichier converti")
      .catch(() => outName.textContent = "Fichier converti");

    // Share link
    const publicUrl = location.origin + "/" + downloadUrl;
    const msg = "Voici mon fichier converti : " + publicUrl;

    waLink.href = "https://wa.me/?text=" + encodeURIComponent(msg);
    tgLink.href = "https://t.me/share/url?url=" + encodeURIComponent(publicUrl) +
                  "&text=" + encodeURIComponent("Fichier converti");

    // Share file (Android / iOS support varies)
    shareFileBtn.addEventListener("click", async () => {
      try {
        if (!navigator.share || !navigator.canShare) {
          alert("Partage fichier non supporté ici. Télécharge puis partage depuis Downloads.");
          return;
        }

        const resp = await fetch(downloadUrl);
        if (!resp.ok) { alert("Impossible de récupérer le fichier."); return; }

        const blob = await resp.blob();
        const name = outName.textContent || "converted_file";
        const file = new File([blob], name, { type: blob.type || "application/octet-stream" });

        if (!navigator.canShare({ files: [file] })) {
          alert("Partage fichier non supporté sur ce navigateur.");
          return;
        }

        await navigator.share({ files: [file], title: "Fichier converti" });
      } catch (e) {
        alert("Partage annulé ou erreur.");
      }
    });
  }
</script>

</body>
</html>
