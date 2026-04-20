import 'package:flutter/foundation.dart';

import 'profile_trip_type.dart';

@immutable
class ProfileTripItem {
  const ProfileTripItem({
    required this.id,
    required this.title,
    required this.cityName,
    required this.routeSummary,
    required this.coverImageUrl,
    required this.tripDate,
    required this.startTime,
    required this.endTime,
    required this.nodeCount,
    required this.totalDuration,
    required this.totalCost,
    required this.firstPoiName,
    required this.lastPoiName,
    required this.budgetLevel,
    required this.companionType,
    required this.rainy,
    required this.night,
    required this.favorited,
    required this.themes,
    required this.updatedAt,
  });

  factory ProfileTripItem.fromJson(Map<String, dynamic> json) {
    final themes = _readStringList(json['themes']);
    final cityName = _readString(json['cityName'], fallback: '城市精选');
    final routeSummary = _readString(
      json['routeSummary'],
      fallback: '系统正在为这条路线补充摘要描述。',
    );
    final coverImageUrl = _readString(json['coverImageUrl']);

    return ProfileTripItem(
      id: _readInt(json['id']) ?? 0,
      title: _readString(json['title'], fallback: '未命名路线'),
      cityName: cityName,
      routeSummary: routeSummary,
      coverImageUrl: coverImageUrl.isNotEmpty
          ? coverImageUrl
          : _fallbackCover([cityName, routeSummary, ...themes]),
      tripDate: _readString(json['tripDate'], fallback: '近期可用'),
      startTime: _readString(json['startTime'], fallback: '09:00'),
      endTime: _readString(json['endTime'], fallback: '18:00'),
      nodeCount: _readInt(json['nodeCount']) ?? 0,
      totalDuration: _readInt(json['totalDuration']) ?? 0,
      totalCost: _readDouble(json['totalCost']) ?? 0,
      firstPoiName: _readString(json['firstPoiName']),
      lastPoiName: _readString(json['lastPoiName']),
      budgetLevel: _readString(json['budgetLevel']),
      companionType: _readString(json['companionType']),
      rainy: _readBool(json['rainy']),
      night: _readBool(json['night']),
      favorited: _readBool(json['favorited']),
      themes: themes,
      updatedAt: _readDateTime(json['updatedAt']),
    );
  }

  final int id;
  final String title;
  final String cityName;
  final String routeSummary;
  final String coverImageUrl;
  final String tripDate;
  final String startTime;
  final String endTime;
  final int nodeCount;
  final int totalDuration;
  final double totalCost;
  final String firstPoiName;
  final String lastPoiName;
  final String budgetLevel;
  final String companionType;
  final bool rainy;
  final bool night;
  final bool favorited;
  final List<String> themes;
  final DateTime? updatedAt;

  String timeWindowLabel() => '$tripDate · $startTime - $endTime';

  String footerLabel() {
    if (firstPoiName.isNotEmpty && lastPoiName.isNotEmpty) {
      return '$firstPoiName → $lastPoiName';
    }
    return routeSummary;
  }

  String description() {
    final traits = <String>[];
    if (budgetLevel.isNotEmpty) {
      traits.add(_humanizeBudget(budgetLevel));
    }
    if (companionType.isNotEmpty) {
      traits.add(_humanizeCompanion(companionType));
    }
    if (rainy) {
      traits.add('雨天友好');
    }
    if (night) {
      traits.add('夜游可玩');
    }
    if (traits.isEmpty) {
      return routeSummary;
    }
    return '${traits.join(' · ')} · 共 $nodeCount 个节点，适合继续细化这条路线';
  }

  List<String> tags(ProfileTripType type) {
    final values = <String>[];
    values.addAll(themes.take(2));
    if (budgetLevel.isNotEmpty) {
      values.add(_humanizeBudget(budgetLevel));
    }
    if (favorited || type == ProfileTripType.saved) {
      values.add('已收藏');
    }
    return values.toSet().take(3).toList(growable: false);
  }
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

double? _readDouble(Object? value) {
  if (value == null) {
    return null;
  }
  if (value is num) {
    return value.toDouble();
  }
  return double.tryParse(value.toString());
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

DateTime? _readDateTime(Object? value) {
  final text = _readString(value);
  if (text.isEmpty) {
    return null;
  }
  return DateTime.tryParse(text);
}

List<String> _readStringList(Object? rawList) {
  if (rawList is! List) {
    return const [];
  }
  return rawList
      .map((item) => item.toString().trim())
      .where((item) => item.isNotEmpty)
      .toList(growable: false);
}

String _humanizeBudget(String raw) {
  switch (raw.toLowerCase()) {
    case 'low':
    case 'economy':
    case '经济':
      return '预算友好';
    case 'high':
    case 'premium':
    case '高':
      return '品质优先';
    default:
      return '均衡预算';
  }
}

String _humanizeCompanion(String raw) {
  switch (raw.toLowerCase()) {
    case 'solo':
    case 'single':
    case '独自':
      return '独自出行';
    case 'couple':
    case '情侣':
      return '情侣同行';
    case 'family':
    case '亲子':
      return '亲子家庭';
    case 'friends':
    case '好友':
      return '好友结伴';
    default:
      return '轻松同行';
  }
}

String _fallbackCover(Iterable<String> hints) {
  final merged = hints.join(' ');
  if (merged.contains('美食') || merged.contains('夜市')) {
    return 'https://images.unsplash.com/photo-1504674900247-0877df9cc836?auto=format&fit=crop&w=1400&q=80';
  }
  if (merged.contains('自然') || merged.contains('公园') || merged.contains('绿道')) {
    return 'https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=1400&q=80';
  }
  if (merged.contains('街区') || merged.contains('购物') || merged.contains('商圈')) {
    return 'https://images.unsplash.com/photo-1483985988355-763728e1935b?auto=format&fit=crop&w=1400&q=80';
  }
  if (merged.contains('人文') ||
      merged.contains('文化') ||
      merged.contains('博物馆') ||
      merged.contains('古迹')) {
    return 'https://images.unsplash.com/photo-1526481280695-3c4691a33277?auto=format&fit=crop&w=1400&q=80';
  }
  return 'https://images.unsplash.com/photo-1514565131-fce0801e5785?auto=format&fit=crop&w=1400&q=80';
}
