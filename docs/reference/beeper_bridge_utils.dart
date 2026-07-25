// REFERENCE IMPLEMENTATION — FluffyBeep (Flutter/Dart)
// Source: fluffychat_src/lib/utils/beeper_bridge_utils.dart
//
// DO NOT copy this code directly. It must be reimplemented in Kotlin.
// This file serves as design reference for:
//   - BeeperRoomData model
//   - BeeperBridgeService logic
//   - BeeperNetwork / networkMap
//   - Fake DM detection heuristic
//   - Cache system design
//   - com.beeper.labels Account Data format
//   - im.ponies.user_emotes sticker pack format
//
// Kotlin equivalents go in:
//   features/beeperbridge/api/      → interfaces/models
//   features/beeperbridge/impl/     → implementation
// ============================================================

import 'dart:typed_data';

import 'package:fluffychat/widgets/future_loading_dialog.dart';
import 'package:flutter/material.dart';
import 'package:matrix/matrix.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'file_logger.dart';

class BeeperRoomData {
  final String? contactId;
  final bool isFakeDM;
  final Uri? avatarUrl;
  final String? displayName;
  final String? networkKey;
  final bool fromCache;

  const BeeperRoomData({
    this.contactId,
    this.isFakeDM = false,
    this.avatarUrl,
    this.displayName,
    this.networkKey,
    this.fromCache = false,
  });
}

class BeeperBridgeUtils {

  // KEY ALGORITHM: Bot detection
  // A user is a Beeper bridge bot if:
  //   - their localpart ends with 'bot'
  //   - AND their MXID contains 'beeper'
  static bool isBeeperBot(String userId) {
    final localpart = userId.split(':').first.toLowerCase();
    return localpart.endsWith('bot') && userId.contains('beeper');
  }

  // KEY ALGORITHM: Network map
  // Maps MXID prefixes to their display name and brand color.
  // Both 'whatsapp' and 'whatsappgo' (Go bridge) map to WhatsApp.
  static const Map<String, BeeperNetSpec> networkMap = {
    'whatsapp': BeeperNetSpec('WhatsApp', 0xFF25D366),
    'whatsappgo': BeeperNetSpec('WhatsApp', 0xFF25D366),
    'instagram': BeeperNetSpec('Instagram', 0xFFE1306C),
    'instagramgo': BeeperNetSpec('Instagram', 0xFFE1306C),
    'telegram': BeeperNetSpec('Telegram', 0xFF2AABEE),
    'telegramgo': BeeperNetSpec('Telegram', 0xFF2AABEE),
    'signal': BeeperNetSpec('Signal', 0xFF3A76F0),
    'signalgo': BeeperNetSpec('Signal', 0xFF3A76F0),
    'discord': BeeperNetSpec('Discord', 0xFF5865F2),
    'discordgo': BeeperNetSpec('Discord', 0xFF5865F2),
    'facebook': BeeperNetSpec('Facebook', 0xFF00B2FF),
    'facebookgo': BeeperNetSpec('Facebook', 0xFF00B2FF),
    'slack': BeeperNetSpec('Slack', 0xFF4A154B),
    'slackgo': BeeperNetSpec('Slack', 0xFF4A154B),
    'googlechat': BeeperNetSpec('Google Chat', 0xFF34A853),
    'googlechatgo': BeeperNetSpec('Google Chat', 0xFF34A853),
  };

  // KEY ALGORITHM: Normalize 'whatsappgo' → 'whatsapp'
  static String getBaseNetworkKey(String key) {
    if (key.endsWith('go')) {
      return key.substring(0, key.length - 2);
    }
    return key;
  }

  // KEY ALGORITHM: Compute room data
  // Single pass over m.room.member states to detect:
  //   - network (from MXID prefix)
  //   - bot vs real contact
  //   - isFakeDM (bridged 1:1)
  //   - displayName (contact's displayname, not room name)
  //   - avatarUrl (contact's avatar, not room avatar or bot avatar)
  static BeeperRoomData _computeRoomData(Room room) {
    final memberStates = room.states['m.room.member'];
    String? contactId;
    String? networkKey;
    int activeMembers = 0;
    bool onlyBotLoaded = false;

    // Single iteration over memberStates
    if (memberStates != null && memberStates.isNotEmpty) {
      bool foundBot = false;
      for (final entry in memberStates.entries) {
        final mxid = entry.key;
        final membership = entry.value.content.tryGet<String>('membership');
        if (membership != 'join' && membership != 'invite') continue;
        activeMembers++;
        if (mxid == room.client.userID) continue;

        // Detect network from participant's MXID prefix
        if (networkKey == null) {
          final localpart = mxid.split(':').first.toLowerCase();
          if (localpart.startsWith('@')) {
            final rawPrefix = localpart.substring(1);
            for (final netEntry in networkMap.entries) {
              if (rawPrefix.startsWith('${netEntry.key}_') ||
                  rawPrefix == '${netEntry.key}bot') {
                networkKey = getBaseNetworkKey(netEntry.key);
                break;
              }
            }
          }
        }

        // Classify: bot or real contact
        if (isBeeperBot(mxid)) {
          foundBot = true;
        } else {
          contactId = mxid;
        }
      }

      // Incomplete data detection (first sync, bot loaded before contact)
      if (foundBot && contactId == null && activeMembers <= 2) {
        onlyBotLoaded = true;
      }
    }

    // FAKE DM HEURISTIC:
    //   - no m.room.name state event (unnamed room)
    //   - ≤ 3 active members
    //   - at least one real contact identified
    final hasRoomName = room.states['m.room.name']
        ?.values.firstOrNull?.content
        .tryGet<String>('name')?.trim().isNotEmpty == true;

    final isFakeDM = !hasRoomName && activeMembers > 0
        && activeMembers <= 3 && contactId != null;

    // Avatar: prefer contact's avatar over room avatar
    // Important: if it's a fake DM with no avatar, return null to avoid
    // showing the bridge bot's logo.
    Uri? avatarUrl;
    // ... (see full source for details)

    // DisplayName: prefer contact's displayname over room name
    String? displayName;
    // ... (see full source for details)

    return BeeperRoomData(
      contactId: contactId,
      isFakeDM: isFakeDM,
      avatarUrl: avatarUrl,
      displayName: displayName,
      networkKey: networkKey,
    );
  }

  // KEY ALGORITHM: com.beeper.labels Account Data format
  // Structure: Map<String uuid, Map labelData>
  // labelData = {
  //   'title': String,
  //   'emoji': String?,
  //   'rooms': List<String roomId>,
  //   'isShownInInbox': bool,  // false = contained (hidden from main inbox)
  //   'createdAt': int,        // Unix seconds
  // }
  // Special key '_hidden_networks': { 'rooms': List<String networkKey> }
  static Map<String, dynamic> getBeeperLabels(Client client) {
    try {
      final accountDataEvent = client.accountData['com.beeper.labels'];
      if (accountDataEvent != null && accountDataEvent.content is Map) {
        return Map<String, dynamic>.from(accountDataEvent.content);
      }
    } catch (e) {
      Logs().e('Error reading com.beeper.labels: $e');
    }
    return {};
  }

  // KEY ALGORITHM: im.ponies.user_emotes sticker pack format
  // Structure:
  // {
  //   'images': {
  //     'sticker_<timestamp>': {
  //       'url': 'mxc://...',
  //       'body': String,
  //       'info': { 'w': int, 'h': int, 'mimetype': String },
  //       'usage': ['sticker'],
  //     }
  //   },
  //   'pack': {
  //     'display_name': 'Mis stickers',
  //     'usage': ['sticker'],
  //   }
  // }

  // KEY ALGORITHM: Last real message
  // Skip com.beeper.* events and state events.
  // Only return: m.room.message, m.room.encrypted, m.sticker
  static Event? getLastRealMessage(Room room) {
    const realMessageTypes = {
      EventTypes.Message,    // m.room.message
      EventTypes.Encrypted,  // m.room.encrypted
      EventTypes.Sticker,    // m.sticker
    };
    final lastEvent = room.lastEvent;
    if (lastEvent != null && realMessageTypes.contains(lastEvent.type)) {
      return lastEvent;
    }
    return null;
  }
}

class BeeperNetwork {
  final String name;
  final int colorHex;
  final String key;
  const BeeperNetwork(this.name, this.colorHex, this.key);
}

class BeeperNetSpec {
  final String name;
  final int colorHex;
  const BeeperNetSpec(this.name, this.colorHex);
}
