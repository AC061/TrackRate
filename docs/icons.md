# Iconografía MDI — TrackRate

Todos los iconos de la app provienen de [Material Design Icons (Pictogrammers)](https://pictogrammers.com/library/mdi/).

## Convención

- Archivos en `app/src/main/res/drawable/` con prefijo `ic_mdi_`
- Viewport 24×24, tinte vía `?attr/colorControlNormal` (excepto FAB, que usa color del tema primario)
- No usar `@android:drawable/*`, Material Symbols ni drawables de la plantilla Android Studio

## Mapa de iconos

| Archivo | Icono MDI | Uso |
|---------|-----------|-----|
| `ic_mdi_home.xml` | `home` | Bottom nav / drawer — Inicio |
| `ic_mdi_magnify.xml` | `magnify` | Bottom nav / drawer — Buscar |
| `ic_mdi_book_open_variant.xml` | `book-open-variant` | Bottom nav / drawer — Diario |
| `ic_mdi_account.xml` | `account` | Perfil (futuro) |
| `ic_mdi_cog.xml` | `cog` | Ajustes (drawer / overflow) |
| `ic_mdi_plus.xml` | `plus` | FAB principal |
| `ic_mdi_album.xml` | `album` | Tipo entidad — álbum |
| `ic_mdi_music_note.xml` | `music-note` | Tipo entidad — canción |
| `ic_mdi_account_music.xml` | `account-music` | Tipo entidad — artista |
| `ic_mdi_star.xml` | `star` | Rating completo |
| `ic_mdi_star_half_full.xml` | `star-half-full` | Rating medio |
| `ic_mdi_star_outline.xml` | `star-outline` | Rating vacío |
| `ic_mdi_clock_outline.xml` | `clock-outline` | Moderación pendiente |
| `ic_mdi_check_circle.xml` | `check-circle` | Moderación aprobada |
| `ic_mdi_close_circle.xml` | `close-circle` | Moderación rechazada |
| `ic_mdi_shield_account.xml` | `shield-account` | Cola admin |

## Añadir un icono nuevo

1. Buscar el icono en [pictogrammers.com/library/mdi](https://pictogrammers.com/library/mdi/)
2. Copiar el path SVG y convertirlo a Vector Drawable (Android Studio: File → New → Vector Asset → Local SVG)
3. Nombrar `ic_mdi_<nombre_mdi>.xml`
4. Documentar en esta tabla
