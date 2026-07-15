const fs = require('fs');
const path = require('path');

const logoIcon = fs.readFileSync(
  path.join(__dirname, '../app/src/main/res/drawable/logo_icon.xml'),
  'utf8'
);
const pathMatch = logoIcon.match(/android:pathData="([^"]+)"/);
if (!pathMatch) throw new Error('Could not extract logo path');
const logoPath = pathMatch[1];

function launcherVector(fillColor, fileName) {
  return `<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <group
        android:scaleX="1.56"
        android:scaleY="1.56"
        android:translateX="24.34"
        android:translateY="22">
        <path
            android:fillColor="${fillColor}"
            android:fillType="evenOdd"
            android:pathData="${logoPath}" />
    </group>
</vector>
`;
}

const drawableDir = path.join(__dirname, '../app/src/main/res/drawable');
fs.writeFileSync(
  path.join(drawableDir, 'ic_launcher_foreground.xml'),
  launcherVector('#394149', 'ic_launcher_foreground.xml'),
  'utf8'
);
fs.writeFileSync(
  path.join(drawableDir, 'ic_launcher_monochrome.xml'),
  launcherVector('#000000', 'ic_launcher_monochrome.xml'),
  'utf8'
);
console.log('Generated launcher icon vectors');
