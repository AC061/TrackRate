const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '..');
const drawableDir = path.join(root, 'app', 'src', 'main', 'res', 'drawable');

function parsePathTag(tag) {
  const dMatch = tag.match(/\bd="([^"]+)"/);
  const fillMatch = tag.match(/(?<![-\w])fill="([^"]+)"/);
  if (!dMatch) return null;
  return { d: dMatch[1], fill: fillMatch?.[1] ?? '#394149' };
}

function parseTranslate(transform) {
  const match = transform.match(/translate\(\s*([^,\s]+)\s*,\s*([^)]+)\)/);
  if (!match) return null;
  return { x: parseFloat(match[1]), y: parseFloat(match[2]) };
}

function extractContent(svgPath) {
  const text = fs.readFileSync(svgPath, 'utf8');
  const viewBoxMatch = text.match(/viewBox="([^"]+)"/);
  const viewBox = viewBoxMatch[1].split(/\s+/);
  const nodes = [];

  const groupRegex = /<g[^>]*id="([^"]+)"[^>]*transform="([^"]*)"[^>]*>([\s\S]*?)<\/g>/g;
  let groupMatch;
  while ((groupMatch = groupRegex.exec(text)) !== null) {
    const translate = parseTranslate(groupMatch[2]);
    const pathRegex = /<path[^>]*\/?>/g;
    let pathMatch;
    while ((pathMatch = pathRegex.exec(groupMatch[3])) !== null) {
      const parsed = parsePathTag(pathMatch[0]);
      if (parsed) {
        nodes.push({ ...parsed, translate });
      }
    }
  }

  if (nodes.length === 0) {
    const pathRegex = /<path[^>]*\/?>/g;
    let pathMatch;
    while ((pathMatch = pathRegex.exec(text)) !== null) {
      const parsed = parsePathTag(pathMatch[0]);
      if (parsed) nodes.push({ ...parsed, translate: null });
    }
  }

  return { viewBox, nodes };
}

function writeVector(name) {
  const { viewBox, nodes } = extractContent(path.join(root, `${name}.svg`));
  const width = parseFloat(viewBox[2]);
  const height = parseFloat(viewBox[3]);
  const lines = [
    '<?xml version="1.0" encoding="utf-8"?>',
    '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
    `    android:width="${width}dp"`,
    `    android:height="${height}dp"`,
    `    android:viewportWidth="${width}"`,
    `    android:viewportHeight="${height}">`,
  ];

  for (const node of nodes) {
    const fill = node.fill ?? '#394149';
    if (node.translate) {
      lines.push('    <group');
      lines.push(`        android:translateX="${node.translate.x}"`);
      lines.push(`        android:translateY="${node.translate.y}">`);
    }
    lines.push('        <path');
    lines.push(`            android:fillColor="${fill}"`);
    lines.push('            android:fillType="evenOdd"');
    lines.push(`            android:pathData="${node.d}" />`);
    if (node.translate) {
      lines.push('    </group>');
    }
  }

  lines.push('</vector>');
  const output = path.join(drawableDir, `${name}.xml`);
  fs.writeFileSync(output, lines.join('\n') + '\n', 'utf8');
  console.log(`Wrote ${output} (${nodes.length} paths)`);
}

for (const logo of ['logo_icon', 'logo_full']) {
  writeVector(logo);
}
