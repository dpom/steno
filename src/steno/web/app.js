(() => {
  "use strict";

  const els = {
    path: document.getElementById("path"),
    open: document.getElementById("open"),
    original: document.getElementById("glyph-original"),
    processed: document.getElementById("glyph-processed"),
    caption: document.getElementById("glyph-caption"),
    empty: document.getElementById("glyph-empty"),
    grid: document.getElementById("glyph-grid"),
    table: document.getElementById("lsigns"),
    letters: document.getElementById("letters"),
    save: document.getElementById("save"),
    prev: document.getElementById("prev"),
    next: document.getElementById("next"),
    delete: document.getElementById("delete"),
    progress: document.getElementById("progress"),
    status: document.getElementById("status"),
    position: document.getElementById("position"),
  };

  let busy = false;
  let state = {
    count: 0,
    idx: 0,
    imagefile: null,
    rows: [],
    letters: [],
    status: "Ready",
    glyph: null,
  };

  function setStatus(text) {
    els.status.textContent = text || "";
  }

  function setBusy(value) {
    busy = value;
    els.progress.hidden = !value;
    document.querySelectorAll("button, input").forEach((control) => {
      control.disabled = value;
    });
  }

  function applyAvailability() {
    els.save.disabled = busy || state.rows.length === 0;
    els.delete.disabled = busy || !state.imagefile;
  }

  async function request(url, options) {
    const response = await fetch(url, options);
    let data = null;
    try {
      data = await response.json();
    } catch (_) {
      throw new Error(`HTTP ${response.status}`);
    }
    if (!response.ok) {
      throw new Error(data && data.error ? data.error : `HTTP ${response.status}`);
    }
    return data;
  }

  function post(url, body) {
    return request(url, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body === undefined ? {} : body),
    });
  }

  function renderCells(row) {
    const tr = document.createElement("tr");
    tr.dataset.pos = String(row.pos);

    const thumbCell = document.createElement("td");
    const thumb = document.createElement("img");
    thumb.className = "thumb";
    thumb.src = row.thumb;
    thumb.alt = `lsign ${row.pos}`;
    thumbCell.appendChild(thumb);

    const startCell = document.createElement("td");
    startCell.textContent = `[${(row.start || []).join(", ")}]`;

    const results = row.results || {};
    const resultCells = ["freq", "diff", "knn"].map((name) => {
      const cell = document.createElement("td");
      cell.textContent = results[name] || "";
      return cell;
    });

    const selectCell = document.createElement("td");
    const select = document.createElement("input");
    select.type = "text";
    select.className = "select";
    select.value = row.select || "";
    select.setAttribute("list", "letters");
    select.dataset.pos = String(row.pos);
    selectCell.appendChild(select);

    const saveCell = document.createElement("td");
    const save = document.createElement("input");
    save.type = "checkbox";
    save.className = "save";
    save.checked = Boolean(row["save?"]);
    save.dataset.pos = String(row.pos);
    saveCell.appendChild(save);

    tr.append(thumbCell, startCell, ...resultCells, selectCell, saveCell);
    return tr;
  }

  function renderGlyph(glyph, imagefile) {
    if (glyph) {
      els.empty.hidden = true;
      els.grid.hidden = false;
      els.original.src = glyph.original;
      els.processed.src = glyph.processed;
      els.caption.textContent = imagefile || "processed";
    } else {
      els.empty.hidden = false;
      els.grid.hidden = true;
      els.original.removeAttribute("src");
      els.processed.removeAttribute("src");
      els.caption.textContent = "processed";
    }
  }

  function render(data) {
    state = data;
    els.position.textContent = data.count ? `${data.idx + 1}/${data.count}` : "0/0";
    setStatus(data.status);

    renderGlyph(data.glyph, data.imagefile);

    const options = [];
    (data.letters || []).forEach((letter) => {
      const option = document.createElement("option");
      option.value = letter;
      options.push(option);
    });
    els.letters.replaceChildren(...options);

    const body = els.table.tBodies[0];
    body.replaceChildren(...(data.rows || []).map(renderCells));
    applyAvailability();
  }

  async function run(action, activity) {
    if (busy) return;
    setBusy(true);
    setStatus(activity);
    try {
      const data = await action();
      setBusy(false);
      render(data);
      applyAvailability();
    } catch (error) {
      setBusy(false);
      applyAvailability();
      setStatus(`Request failed: ${error.message}`);
    }
  }

  function collectRows() {
    return Array.from(els.table.tBodies[0].rows).map((tr) => {
      const select = tr.querySelector("input.select");
      const save = tr.querySelector("input.save");
      return {
        pos: Number(tr.dataset.pos),
        select: select ? select.value : "",
        "save?": save ? save.checked : false,
      };
    });
  }

  function saveChecked() {
    run(() => post("/api/save", { rows: collectRows() }), "Saving…");
  }

  function navigate(direction) {
    run(
      () => post("/api/nav", { direction }),
      direction === "next" ? "Loading next glyph…" : "Loading previous glyph…",
    );
  }

  function openInput() {
    const path = els.path.value.trim();
    if (!path) {
      setStatus("Enter a glyph image or directory path");
      return;
    }
    run(() => post("/api/open", { path }), "Opening…");
  }

  function deleteGlyph() {
    if (!state.imagefile) {
      setStatus("No glyph to delete");
      return;
    }
    if (!window.confirm(`Delete ${state.imagefile}?`)) return;
    run(() => post("/api/delete"), "Deleting…");
  }

  els.open.addEventListener("click", openInput);
  els.path.addEventListener("keydown", (event) => {
    if (event.key === "Enter") {
      event.preventDefault();
      openInput();
    }
  });
  els.save.addEventListener("click", saveChecked);
  els.prev.addEventListener("click", () => navigate("prev"));
  els.next.addEventListener("click", () => navigate("next"));
  els.delete.addEventListener("click", deleteGlyph);

  document.addEventListener("keydown", (event) => {
    if (!event.ctrlKey) return;
    const key = event.key.toLowerCase();
    if (key === "s") {
      event.preventDefault();
      saveChecked();
    } else if (event.key === "ArrowLeft") {
      event.preventDefault();
      navigate("prev");
    } else if (event.key === "ArrowRight") {
      event.preventDefault();
      navigate("next");
    } else if (key === "d") {
      event.preventDefault();
      deleteGlyph();
    }
  });

  request("/api/state")
    .then((data) => {
      render(data);
      applyAvailability();
    })
    .catch((error) => setStatus(`Request failed: ${error.message}`));
})();
