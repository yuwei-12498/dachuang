import 'package:flutter/foundation.dart';

@immutable
class CommunitySavedResult {
  const CommunitySavedResult({
    required this.itineraryId,
    required this.title,
    required this.favorited,
  });

  factory CommunitySavedResult.fromJson(Map<String, dynamic> json) {
    return CommunitySavedResult(
      itineraryId: _readInt(json['id']) ?? 0,
      title: _readString(json['customTitle'], fallback: '?????'),
      favorited: _readBool(json['favorited']),
    );
  }

  final int itineraryId;
  final String title;
  final bool favorited;
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

bool _readBool(Object? value) {
  if (value is bool) {
    return value;
  }
  if (value is num) {
    return value != 0;
  }
  final text = value?.toString().toLowerCase().trim();
  return text == 'true' || text == '1';
}

String _readString(Object? value, {String fallback = ''}) {
  final text = value?.toString().trim();
  if (text == null || text.isEmpty) {
    return fallback;
  }
  return text;
}
