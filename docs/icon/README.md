# LegacyMinecraftModTemplate icon

## What this is

`icon.png` — the LegacyMinecraftModTemplate mod icon, 1024 x 1024 PNG, SHA-256
`859c634f4499a96627cb4e04c54c4f57509c542fb5af5d724c1c2efd6a6fc112`.

Copied byte-identically from `.local-icon-variants/provenance/from-round3/blender-q/legacy-hub-down-left-1.png`
(SHA-256 verified at the source, and again on the copy). The hash matches `png_sha256` in
`provenance/legacy-hub-down-left-1-metadata.json` and `provenance/png-verification.json`.

## How it was made

Blender render of the approved template hub, not a screenshot.

* Blender 5.1.1, headless `--background -noaudio --threads 3`, Cycles CPU (no GPU), 32 samples,
  1024 x 1024, 1.484 s. No display server, no audio sink, no shader pack.
* Composition: the approved legacy hub, unchanged — seven separated workstations
  (`crafting_table` centre, `furnace` top, `enchanting_table` top-right, `anvil` bottom-right,
  `chest` bottom, `ender_chest` bottom-left, `brewing_stand` top-left), same positions, canonical
  meshes and UVs, same lighting formula, camera, framing and background as the approved source
  scene, and the approved enchanting-book entity pose is retained.
* The only change is blockstate facing: every block with a legal `facing` property is set to `east`.
  Minecraft `+X` maps to Blender `(0, -1, 0)` under the recorded convention
  `(x, y, z) -> (-z, -x, y)` and projects down-left at the retained camera yaw of 45 deg, so the
  furnace front and the chest/ender-chest latches now sit on the screen-left visible face.
  `crafting_table`, `enchanting_table` and `brewing_stand` have no facing blockstate and stay
  canonical. Per-station proof, camera-relative directions and screen deltas are in
  `provenance/facing-table.json` / `facing-table.csv`.
* Imagery — **extracted from the real Minecraft 1.8.9 client**, not re-drawn and not shipped as a jar.
  Source jar: `.local-icon-variants/round3/minecraft-1.8.9-client.jar`. Every texture the scene uses is
  packed into the saved `.blend` and is byte-identical to its jar member; `asset-verification.json`
  lists each packed texture with its jar member, SHA-256 and exact byte match. `source/asset-provenance.json`
  (29 entries, `era: legacy`) records the exact member list; `source/blockstate-provenance.json` and
  `evidence/legacy/*.json` are the extracted blockstate files that prove the facing rules.
* The extracted asset tree (`source/assets/legacy/...`) is not present in round-3 any more; the
  re-extraction command is below. The `.blend` does not need it to render, because all images are packed.

## Provenance files

* `legacy-hub-down-left-1.py` — entry point (`render_down_left.render('legacy')`); `render_down_left.py` —
  the cutover script that loads the approved packed scene and applies the facing change.
* `source/render_hubs.py`, `source/minecraft.py` — the approved hub author and the vanilla model/atlas
  helpers it uses (`minecraft.py` reads client assets from `source/assets/<era>/...`).
* `source/legacy-hub-1.blend` — the immutable approved source scene
  (SHA-256 `146a2d29537fb1a8756b3880266f1df2e6bfd2894e29ed5dfa4a02d92e338142`).
* `legacy-hub-down-left-1.blend` (packed, embeds `minecraft.py`, `render_hubs.py` and the entry point),
  `-metadata.json`, `-packed-verification.json` (reopens the saved scene and re-proves geometry, facing
  and packed textures), `facing-table.*`, `evidence/legacy/*` blockstate extracts.
* `asset-verification.json`, `source/asset-provenance.json`, `source/blockstate-provenance.json`,
  `source-snapshot.json`, `png-verification.json`, `visual-review.json`, `cleanup.json` — curated to the
  legacy render (`CURATION.json` records the dropped rows).
* `verify_assets.py`, `verify_pngs.py` — the verification scripts.
* `CURATION.json` — what was copied, what was filtered, and what was left in round-3.

## How to regenerate

The scripts read client assets from `source/assets/<era>/...`, which must exist first. Re-extract them
from the recorded jar with the recorded member list (paths and members are taken from
`source/asset-provenance.json`, so nothing is guessed):

```sh
cd <repo>/docs/icon/provenance
python3 - <<'PY'
import json, pathlib, zipfile
for e in json.load(open('source/asset-provenance.json')):
    out = pathlib.Path(e['path'])
    out.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(e['source']) as z:
        out.write_bytes(z.read(e['member']))
PY
```

Then render and verify:

```sh
taskset -c 0,2,4 nix shell nixpkgs#blender --command blender --background -noaudio \
  --threads 3 --python-exit-code 1 --python legacy-hub-down-left-1.py
python3 verify_pngs.py
```

`render_down_left.py` asserts that it has been migrated into the pre-existing systemd user unit
`render-blender.service` (CPUWeight 20, quota <= 3 cores) within 90 seconds, and fails loudly otherwise;
`wait_for_shared_cpu_cap()` is at the top of the script. Re-running overwrites
`legacy-hub-down-left-1.png` and `-metadata.json`.

## Notes

* Only this template's files are here. The sibling `ModernMinecraftModTemplate` icon was made from the
  same round-3 `blender-q` folder; its scene, metadata, evidence and source snapshots live in that
  repository, not this one. The shared tooling (`render_down_left.py`, `minecraft.py`, `render_hubs.py`,
  `facing-table.*`, `verify_*.py`) is intentionally present in both.
* The approved source render `blender-m/legacy-hub-1.png` is previous-round icon work and is not copied;
  its SHA-256 is recorded in `source-snapshot.json`.
* No commits were made. In the original round-3 workspace these scripts were deliberately saved
  untracked (local-only policy); they are now part of this repository's `docs/icon/` as delivered files.
* Nothing else in the mod repository was modified.

## Working-tree note

The round-3 working tree that produced this icon was cleaned up after integration. Every file needed to regenerate the icon was copied into `provenance/`; the copies live under `provenance/from-round3/` when they came from the working tree. Any remaining `round3/...` mention records where something came from, not a path that still exists.
