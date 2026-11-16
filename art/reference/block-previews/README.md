# Scribery & Crafts – Block Texture Draft v1

This package contains first-pass 16×16 PNG tile textures for the planned blocks.

## Included blocks
- Scribe's Lectern
- Letter Cabinet (separate block; includes optional inkwell and roller detail textures)
- Printing Press (separate lower table and upper mechanism texture groups)
- Translator's Desk
- Manuscript Archive
- Bookshelf

## Technical details
- Every game texture is 16×16 pixels, PNG, nearest-neighbour pixel art.
- Textures use only the palette recorded in `manifest.json` (from the current UI requirements) plus transparency.
- No words, manuscript content, inventory counts, UI states, or animation are baked into textures.
- Preview images are enlarged for inspection; do not use previews as in-game textures.

## Integration boundary
These are texture tiles only. They are not block models, blockstates, item models, Java registrations, recipes, or placement logic. The eventual model JSONs must map the appropriate `front`, `side`, and `top` surfaces and any separate details to the geometry. The printing press model must define how its lower table and upper mechanism occupy the world.

## Suggested next review
Check the front-face designs and silhouette details before building models around them. In particular, the Letter Cabinet's compartments and the Press upper mechanism may need more geometry than a single cube face can express.
