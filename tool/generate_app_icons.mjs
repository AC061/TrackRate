import path from 'node:path';
import { fileURLToPath } from 'node:url';
import sharp from 'sharp';

const root = path.join(path.dirname(fileURLToPath(import.meta.url)), '..', 'assets', 'brand');
const icon = path.join(root, 'logo_icon.svg');

async function writeIcon(name, background) {
  const resized = sharp(icon).resize(900, 900, { fit: 'contain' });

  if (background) {
    await sharp({
      create: {
        width: 1024,
        height: 1024,
        channels: 4,
        background,
      },
    })
      .composite([{ input: await resized.toBuffer(), gravity: 'center' }])
      .png()
      .toFile(path.join(root, `${name}.png`));
    return;
  }

  await sharp(await resized.toBuffer())
    .extend({
      top: 62,
      bottom: 62,
      left: 62,
      right: 62,
      background: { r: 0, g: 0, b: 0, alpha: 0 },
    })
    .png()
    .toFile(path.join(root, `${name}.png`));
}

await writeIcon('app_icon', '#FFFFFF');
await writeIcon('app_icon_foreground', null);
console.log('Generated app_icon.png and app_icon_foreground.png');
