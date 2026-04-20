import 'package:flutter/foundation.dart';

import 'community_card.dart';

@immutable
class CommunityPagePayload {
  const CommunityPagePayload({
    required this.page,
    required this.size,
    required this.total,
    required this.records,
  });

  factory CommunityPagePayload.fromJson(Map<String, dynamic> json) {
    final records = ((json['records'] as List?) ?? const [])
        .whereType<Map>()
        .map((item) => CommunityCard.fromJson(Map<String, dynamic>.from(item)))
        .toList(growable: false);

    return CommunityPagePayload(
      page: _readInt(json['page']) ?? 1,
      size: _readInt(json['size']) ?? records.length,
      total: _readInt(json['total']) ?? records.length,
      records: records,
    );
  }

  final int page;
  final int size;
  final int total;
  final List<CommunityCard> records;
}

int? _readInt(Object? value) {
  if (value == null) {
    return null;
  }
  if (value is num) {
    return value.toInt();
  }
  return int.tryParse(value.toString());
}
