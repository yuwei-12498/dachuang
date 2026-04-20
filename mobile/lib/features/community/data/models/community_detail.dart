import 'package:flutter/foundation.dart';

@immutable
class CommunityDetail {
  const CommunityDetail({
    required this.id,
    required this.title,
    required this.cityName,
    required this.coverImageUrl,
    required this.shareNote,
    required this.authorLabel,
    required this.tripDate,
    required this.startTime,
    required this.endTime,
    required this.themes,
    required this.totalDuration,
    required this.totalCost,
    required this.nodeCount,
    required this.routeSummary,
    required this.recommendReason,
    required this.selectedOptionKey,
    required this.highlights,
    required this.alerts,
    required this.nodes,
    required this.likeCount,
    required this.liked,
    required this.commentCount,
    required this.updatedAt,
  });

  factory CommunityDetail.fromJson(Map<String, dynamic> json) {
    final themes = _readStringList(json['themes']);
    final nodes = ((json['nodes'] as List?) ?? const [])
        .whereType<Map>()
        .map((item) => CommunityDetailNode.fromJson(Map<String, dynamic>.from(item)))
        .toList(growable: false);
    final cityName = _readString(json['cityName'], fallback: '城市公开路线');
    final coverImageUrl = _readString(json['coverImageUrl']);

    return CommunityDetail(
      id: _readInt(json['id']) ?? 0,
      title: _readString(json['title'], fallback: '社区路线详情'),
      cityName: cityName,
      coverImageUrl: coverImageUrl.isNotEmpty
          ? coverImageUrl
          : _fallbackCoverImage([cityName, ...themes, ...nodes.map((node) => node.category)]),
      shareNote: _readString(json['shareNote']),
      authorLabel: _readString(json['authorLabel'], fallback: '城市体验官'),
      tripDate: _readString(json['tripDate'], fallback: '近期可玩'),
      startTime: _readString(json['startTime'], fallback: '09:00'),
      endTime: _readString(json['endTime'], fallback: '18:00'),
      themes: themes,
      totalDuration: _readInt(json['totalDuration']) ?? 0,
      totalCost: _readDouble(json['totalCost']) ?? 0,
      nodeCount: _readInt(json['nodeCount']) ?? nodes.length,
      routeSummary: _readString(json['routeSummary'], fallback: '等待路线摘要'),
      recommendReason: _readString(
        json['recommendReason'],
        fallback: '这条公开路线在可执行性、打卡效率与整体体验之间取得了不错的平衡。',
      ),
      selectedOptionKey: _readOptionalString(json['selectedOptionKey']),
      highlights: _readStringList(json['highlights']),
      alerts: _readStringList(json['alerts']),
      nodes: nodes,
      likeCount: _readInt(json['likeCount']) ?? 0,
      liked: _readBool(json['liked']),
      commentCount: _readInt(json['commentCount']) ?? 0,
      updatedAt: _readDateTime(json['updatedAt']),
    );
  }

  final int id;
  final String title;
  final String cityName;
  final String coverImageUrl;
  final String shareNote;
  final String authorLabel;
  final String tripDate;
  final String startTime;
  final String endTime;
  final List<String> themes;
  final int totalDuration;
  final double totalCost;
  final int nodeCount;
  final String routeSummary;
  final String recommendReason;
  final String? selectedOptionKey;
  final List<String> highlights;
  final List<String> alerts;
  final List<CommunityDetailNode> nodes;
  final int likeCount;
  final bool liked;
  final int commentCount;
  final DateTime? updatedAt;

  bool get hasShareNote => shareNote.trim().isNotEmpty;
}

@immutable
class CommunityDetailNode {
  const CommunityDetailNode({
    required this.stepOrder,
    required this.poiId,
    required this.poiName,
    required this.category,
    required this.district,
    required this.address,
    required this.startTime,
    required this.endTime,
    required this.stayDuration,
    required this.travelTime,
    required this.cost,
    required this.reason,
    required this.operatingStatus,
    required this.statusNote,
  });

  factory CommunityDetailNode.fromJson(Map<String, dynamic> json) {
    return CommunityDetailNode(
      stepOrder: _readInt(json['stepOrder']) ?? 0,
      poiId: _readInt(json['poiId']) ?? 0,
      poiName: _readString(json['poiName'], fallback: '未命名点位'),
      category: _readString(json['category'], fallback: '城市体验'),
      district: _readString(json['district'], fallback: '核心城区'),
      address: _readString(json['address'], fallback: '地址待后端补充'),
      startTime: _readString(json['startTime'], fallback: '09:00'),
      endTime: _readString(json['endTime'], fallback: '10:30'),
      stayDuration: _readInt(json['stayDuration']) ?? 60,
      travelTime: _readInt(json['travelTime']) ?? 15,
      cost: _readDouble(json['cost']) ?? 0,
      reason: _readString(
        json['sysReason'],
        fallback: '系统已结合整体动线与停留节奏，为你保留了这一个关键点位。',
      ),
      operatingStatus: _readString(json['operatingStatus'], fallback: 'OPEN'),
      statusNote: _readString(
        json['statusNote'],
        fallback: '营业状态正常，建议以出行当日公告为准。',
      ),
    );
  }

  final int stepOrder;
  final int poiId;
  final String poiName;
  final String category;
  final String district;
  final String address;
  final String startTime;
  final String endTime;
  final int stayDuration;
  final int travelTime;
  final double cost;
  final String reason;
  final String operatingStatus;
  final String statusNote;

  String get timeRangeLabel => '$startTime - $endTime';

  String get imageUrl {
    if (category.contains('美食') || category.contains('夜市')) {
      return 'https://images.unsplash.com/photo-1504674900247-0877df9cc836?auto=format&fit=crop&w=1200&q=80';
    }
    if (category.contains('公园') || category.contains('自然')) {
      return 'https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=1200&q=80';
    }
    if (category.contains('街区') || category.contains('购物') || category.contains('商圈')) {
      return 'https://images.unsplash.com/photo-1483985988355-763728e1935b?auto=format&fit=crop&w=1200&q=80';
    }
    if (category.contains('人文') ||
        category.contains('文化') ||
        category.contains('博物馆') ||
        category.contains('古迹') ||
        category.contains('宗教')) {
      return 'https://images.unsplash.com/photo-1526481280695-3c4691a33277?auto=format&fit=crop&w=1200&q=80';
    }
    return 'https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?auto=format&fit=crop&w=1200&q=80';
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

String? _readOptionalString(Object? value) {
  final text = value?.toString().trim();
  if (text == null || text.isEmpty) {
    return null;
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

String _fallbackCoverImage(Iterable<String> hints) {
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
