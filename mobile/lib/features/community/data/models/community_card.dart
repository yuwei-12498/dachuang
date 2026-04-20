import 'package:flutter/foundation.dart';

@immutable
class CommunityCard {
  const CommunityCard({
    required this.id,
    required this.title,
    required this.cityName,
    required this.tripDate,
    required this.coverImageUrl,
    required this.authorLabel,
    required this.shareNote,
    required this.routeSummary,
    required this.themes,
    required this.highlights,
    required this.totalDuration,
    required this.totalCost,
    required this.nodeCount,
    required this.likeCount,
    required this.commentCount,
    required this.liked,
    required this.updatedAt,
  });

  factory CommunityCard.fromJson(Map<String, dynamic> json) {
    final themes = _readStringList(json['themes']);
    final highlights = _readStringList(json['highlights']);
    final cityName = _readString(json['cityName'], fallback: '城市精选');
    final coverImageUrl = _readString(json['coverImageUrl']);

    return CommunityCard(
      id: _readInt(json['id']) ?? 0,
      title: _readString(json['title'], fallback: '社区路线'),
      cityName: cityName,
      tripDate: _readString(json['tripDate'], fallback: '近期可玩'),
      coverImageUrl: coverImageUrl.isNotEmpty
          ? coverImageUrl
          : _fallbackCoverImage([cityName, ...themes, _readString(json['routeSummary'])]),
      authorLabel: _readString(json['authorLabel'], fallback: '城市体验官'),
      shareNote: _readString(
        json['shareNote'],
        fallback: '这条公开路线正在社区持续升温，适合直接收藏后二次编辑。',
      ),
      routeSummary: _readString(json['routeSummary'], fallback: '等待后端返回路线摘要'),
      themes: themes,
      highlights: highlights,
      totalDuration: _readInt(json['totalDuration']) ?? 0,
      totalCost: _readDouble(json['totalCost']) ?? 0,
      nodeCount: _readInt(json['nodeCount']) ?? 0,
      likeCount: _readInt(json['likeCount']) ?? 0,
      commentCount: _readInt(json['commentCount']) ?? 0,
      liked: _readBool(json['liked']),
      updatedAt: _readDateTime(json['updatedAt']),
    );
  }

  final int id;
  final String title;
  final String cityName;
  final String tripDate;
  final String coverImageUrl;
  final String authorLabel;
  final String shareNote;
  final String routeSummary;
  final List<String> themes;
  final List<String> highlights;
  final int totalDuration;
  final double totalCost;
  final int nodeCount;
  final int likeCount;
  final int commentCount;
  final bool liked;
  final DateTime? updatedAt;
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
