// REFERENCE IMPLEMENTATION — FluffyBeep (Flutter/Dart)
// Source: fluffychat_src/lib/pages/settings/patches_settings.dart (excerpts)
//
// Muestra qué settings existen en la pantalla "Parches de Beeper".
// Kotlin equivalente: features/beeperbridge/impl/ui/BeeperSettingsNode.kt
// ============================================================

// SETTINGS ITEMS (in order as shown in the UI):
//
// Section: "Acciones rápidas"
//   - Re-sincronizar chats (Beeper) — triggers client.clearCache() [OUT OF SCOPE]
//
// Section: "Depuración y Diagnóstico"
//   - Toggle: Activar logging de depuración                [OUT OF SCOPE]
//   - Toggle: Forzar pasarela pública UnifiedPush          [OUT OF SCOPE]
//   - Toggle: Lectura manual de mensajes                   [IN SCOPE - Category B]
//   - Action: Re-solicitar claves de cifrado               [OUT OF SCOPE]
//   - Action: Ver logs de depuración                       [OUT OF SCOPE]
//   - List: Pushers activos en el servidor                 [OUT OF SCOPE]
//   - Action: Forzar re-registro de notificaciones         [OUT OF SCOPE]
//
// Section: "Contener Bridges (Ocultar de la lista principal)"
//   - For each detected network:
//     - Toggle: Contener <NetworkName> [IN SCOPE]
//     - Toggle: Ocultar <NetworkName> de la barra lateral  [IN SCOPE]
//
// Section: "Etiquetas de Beeper (Labels)"
//   - Add button (+)
//   - For each label:
//     - Card with: title, emoji, room count
//     - Edit button → LabelEditorDialog
//     - Delete button
//     - Toggle: Contener chats                             [IN SCOPE]
//     - Toggle: Ocultar de la barra lateral               [IN SCOPE]
//
// Section: "Etiquetas personalizadas Matrix (u.xxx)"
//   - For each Matrix custom tag:
//     - Toggle: Contener etiqueta                          [IN SCOPE]
//
// ============================================================
// LABEL EDITOR DIALOG (LabelEditorDialog)
// Fields:
//   - Title (text input)
//   - Emoji (emoji picker)
//   - isShownInInbox toggle (opposite of "contained")
//   - Room selection (multi-select from room list)
//
// On save:
//   client.setAccountData(userId, 'com.beeper.labels', updatedLabels)
//
// ============================================================
// MANUAL READ RECEIPTS
// When enabled (stored in local DataStore/SharedPreferences):
//   - Do NOT call client.setReadMarker() when opening a room
//   - Show a "Mark as read" button in the chat UI
//   - Only send read receipt when user explicitly taps that button
//
// This is useful to avoid accidentally marking messages as read
// on a bridge that then marks them as read on the external platform.
