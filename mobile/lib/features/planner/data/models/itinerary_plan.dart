import 'dart:math' as math;

import 'planner_request.dart';

class PlannerItinerary {
  const PlannerItinerary({
    required this.id,
    required this.title,
    required this.cityName,
    required this.coverImageUrl,
    required this.overview,
    required this.budgetLabel,
    required this.companionLabel,
    required this.travelWindow,
    required this.totalDays,
    required this.totalDurationMinutes,
    required this.totalCost,
    required this.tags,
    required this.alerts,
    required this.days,
    required this.footerInsight,
  });

  final String id;
  final String title;
  final String cityName;
  final String coverImageUrl;
  final String overview;
  final String budgetLabel;
  final String companionLabel;
  final String travelWindow;
  final int totalDays;
  final int totalDurationMinutes;
  final double totalCost;
  final List<String> tags;
  final List<String> alerts;
  final List<PlannerDayItinerary> days;
  final String footerInsight;

  int get totalPoiCount =>
      days.fold<int>(0, (sum, day) => sum + day.nodes.length);

  factory PlannerItinerary.fromBackendJson(
    Map<String, dynamic> json, {
    required PlannerRequest fallbackRequest,
  }) {
    final originalReq = Map<String, dynamic>.from(
      (json['originalReq'] as Map?) ?? fallbackRequest.toJson(),
    );
    final optionMaps = ((json['options'] as List?) ?? const [])
        .whereType<Map>()
        .map((item) => Map<String, dynamic>.from(item))
        .toList(growable: false);
    final selectedOption = _selectOption(
      optionMaps,
      json['selectedOptionKey']?.toString(),
    );
    final primaryPayload = selectedOption ?? json;
    final totalDays = math.max(
      1,
      (_readDouble(originalReq['tripDays']) ?? fallbackRequest.tripDays).ceil(),
    );
    final tripDate = (originalReq['tripDate'] ?? fallbackRequest.tripDate)
        .toString();
    final themeLabels =
        ((originalReq['themes'] as List?) ?? fallbackRequest.themes)
            .map((item) => item.toString())
            .where((item) => item.trim().isNotEmpty)
            .toList(growable: false);
    final nodeMaps = _extractNodeMaps(json, selectedOption);
    final groups = _splitNodes(nodeMaps, totalDays);
    final totalCost =
        _readDouble(primaryPayload['totalCost'] ?? json['totalCost']) ?? 0;
    final totalDurationMinutes =
        _readInt(primaryPayload['totalDuration'] ?? json['totalDuration']) ?? 0;
    final baseDate = DateTime.tryParse(tripDate);
    final walkingLevel =
        (originalReq['walkingLevel'] ?? fallbackRequest.walkingLevel)
            .toString();
    final recommendReason =
        (primaryPayload['recommendReason'] ??
                json['recommendReason'] ??
                '系统已结合你的预算、出行节奏与主题偏好，对热门点位、跨区通勤和停留时长做了一轮平衡。')
            .toString();
    final tips =
        (json['tips'] ?? '建议在核心商圈与热门景区之间预留 20 分钟机动时间，以便应对排队、天气或临时交通波动。')
            .toString();
    final cityName = (originalReq['cityName'] ?? fallbackRequest.cityName)
        .toString();
    final optionTitle = primaryPayload['title']?.toString();

    final dayPlans = List<PlannerDayItinerary>.generate(totalDays, (index) {
      final dayNodes = groups[index]
          .map((item) => PlannerTimelineNode.fromBackendJson(item))
          .toList(growable: false);
      final dayNumber = index + 1;
      final dayCost = dayNodes.fold<double>(
        0,
        (sum, node) => sum + node.estimatedCost,
      );
      final summary = dayNodes.isEmpty
          ? '当前后端返回的是扁平节点结构，这一天先展示为弹性探索日；接入多日接口后可直接替换成真实日程。'
          : '第 $dayNumber 天围绕 ${dayNodes.first.district} 与周边片区展开，优先保证动线顺滑、节奏舒适且便于出片。';

      return PlannerDayItinerary(
        dayNumber: dayNumber,
        title: dayNodes.isEmpty ? '自由机动探索' : 'AI 推荐主线 · 第 $dayNumber 天',
        subtitle: themeLabels.isEmpty ? '城市漫游' : themeLabels.join(' · '),
        dateLabel: _formatDateLabel(baseDate?.add(Duration(days: index))),
        summary: summary,
        estimatedCost: dayCost,
        walkingDistanceKm: _estimateWalkingDistance(
          dayNodes.length,
          walkingLevel,
        ),
        nodes: dayNodes,
      );
    });

    return PlannerItinerary(
      id: (json['id'] ?? '').toString(),
      title: _resolveTitle(
        customTitle: json['customTitle']?.toString(),
        optionTitle: optionTitle,
        cityName: cityName,
      ),
      cityName: cityName,
      coverImageUrl: _coverForThemes(themeLabels),
      overview: recommendReason,
      budgetLabel: _humanizeBudgetLevel(
        (originalReq['budgetLevel'] ?? fallbackRequest.budgetLevel).toString(),
      ),
      companionLabel: _humanizeCompanion(
        (originalReq['companionType'] ?? fallbackRequest.companionType)
            .toString(),
      ),
      travelWindow:
          '${(originalReq['startTime'] ?? fallbackRequest.startTime)} - ${(originalReq['endTime'] ?? fallbackRequest.endTime)}',
      totalDays: totalDays,
      totalDurationMinutes: totalDurationMinutes,
      totalCost: totalCost,
      tags: themeLabels,
      alerts: _extractStringList(primaryPayload['alerts'] ?? json['alerts']),
      days: dayPlans,
      footerInsight: tips,
    );
  }
}

class PlannerDayItinerary {
  const PlannerDayItinerary({
    required this.dayNumber,
    required this.title,
    required this.subtitle,
    required this.dateLabel,
    required this.summary,
    required this.estimatedCost,
    required this.walkingDistanceKm,
    required this.nodes,
  });

  final int dayNumber;
  final String title;
  final String subtitle;
  final String dateLabel;
  final String summary;
  final double estimatedCost;
  final double walkingDistanceKm;
  final List<PlannerTimelineNode> nodes;
}

class PlannerTimelineNode {
  const PlannerTimelineNode({
    required this.startTime,
    required this.endTime,
    required this.name,
    required this.district,
    required this.category,
    required this.imageUrl,
    required this.stayDurationMinutes,
    required this.estimatedCost,
    required this.aiReason,
    required this.transportTip,
    required this.highlightLabel,
  });

  final String startTime;
  final String endTime;
  final String name;
  final String district;
  final String category;
  final String imageUrl;
  final int stayDurationMinutes;
  final double estimatedCost;
  final String aiReason;
  final String transportTip;
  final String highlightLabel;

  String get timeRangeLabel => '$startTime - $endTime';

  factory PlannerTimelineNode.fromBackendJson(Map<String, dynamic> json) {
    final category = (json['category'] ?? '城市体验').toString();
    final name = (json['poiName'] ?? '未命名点位').toString();
    final stayDuration = _readInt(json['stayDuration']) ?? 60;
    final travelTime = _readInt(json['travelTime']) ?? 15;
    final startTime = (json['startTime'] ?? '09:30').toString();
    final endTime = (json['endTime'] ?? '11:00').toString();
    final cost = _readDouble(json['cost']) ?? 0;
    return PlannerTimelineNode(
      startTime: startTime,
      endTime: endTime,
      name: name,
      district: (json['district'] ?? '城市核心区').toString(),
      category: category,
      imageUrl: _imageForCategory(category),
      stayDurationMinutes: stayDuration,
      estimatedCost: cost,
      aiReason:
          (json['sysReason'] ?? '后端暂未返回 AI 推荐文案时，前端会优雅回退到默认解释，确保结果卡片始终完整可读。')
              .toString(),
      transportTip: '与上一站预计通勤 $travelTime 分钟',
      highlightLabel: cost <= 0
          ? '免费优先'
          : (json['operatingStatus'] ?? '值得安排').toString(),
    );
  }
}

List<List<T>> _splitNodes<T>(List<T> source, int parts) {
  if (parts <= 1) {
    return <List<T>>[source];
  }
  if (source.isEmpty) {
    return List<List<T>>.generate(parts, (_) => <T>[]);
  }

  final result = <List<T>>[];
  var start = 0;
  for (var index = 0; index < parts; index++) {
    final remainingItems = source.length - start;
    final remainingGroups = parts - index;
    final take = remainingItems <= 0
        ? 0
        : (remainingItems / remainingGroups).ceil();
    final end = math.min(source.length, start + take);
    result.add(source.sublist(start, end));
    start = end;
  }
  return result;
}

String _formatDateLabel(DateTime? date) {
  if (date == null) {
    return '日期待后端返回';
  }
  const weekDays = ['周一', '周二', '周三', '周四', '周五', '周六', '周日'];
  return '${date.month} 月 ${date.day} 日 ${weekDays[date.weekday - 1]}';
}

Map<String, dynamic>? _selectOption(
  List<Map<String, dynamic>> optionMaps,
  String? selectedOptionKey,
) {
  if (optionMaps.isEmpty) {
    return null;
  }

  for (final option in optionMaps) {
    if (option['optionKey']?.toString() == selectedOptionKey) {
      return option;
    }
  }
  return optionMaps.first;
}

List<Map<String, dynamic>> _extractNodeMaps(
  Map<String, dynamic> rootJson,
  Map<String, dynamic>? selectedOption,
) {
  final rawNodes =
      (rootJson['nodes'] as List?) ??
      (selectedOption == null ? null : selectedOption['nodes'] as List?) ??
      const [];

  return rawNodes
      .whereType<Map>()
      .map((item) => Map<String, dynamic>.from(item))
      .toList(growable: false);
}

List<String> _extractStringList(Object? rawList) {
  if (rawList is! List) {
    return const [];
  }

  return rawList
      .map((item) => item.toString())
      .where((item) => item.trim().isNotEmpty)
      .toList(growable: false);
}

String _resolveTitle({
  required String? customTitle,
  required String? optionTitle,
  required String cityName,
}) {
  if (_hasText(customTitle)) {
    return customTitle!.trim();
  }
  if (_hasText(optionTitle)) {
    return '$cityName · ${optionTitle!.trim()}';
  }
  return '$cityName 智能路线结果';
}

String _humanizeBudgetLevel(String raw) {
  switch (raw) {
    case 'low':
    case '低':
    case '轻':
    case '轻量':
      return '轻量预算';
    case 'high':
    case '高':
    case '高品质':
    case '品质':
      return '高品质预算';
    case '中':
    case '中等':
    case 'medium':
    default:
      return '舒适预算';
  }
}

String _humanizeCompanion(String raw) {
  switch (raw) {
    case 'solo':
    case '独行':
    case '独自':
      return '独自出行';
    case 'couple':
    case '情侣':
      return '情侣约会';
    case 'family':
    case '亲子':
    case '家庭':
      return '亲子家庭';
    case 'friends':
    case '朋友':
    case '好友':
    default:
      return '好友同行';
  }
}

double _estimateWalkingDistance(int nodeCount, String walkingLevel) {
  final multiplier = switch (walkingLevel) {
    'low' || '低' => 1.2,
    'high' || '高' => 2.1,
    _ => 1.6,
  };
  return double.parse((math.max(nodeCount, 1) * multiplier).toStringAsFixed(1));
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

int? _readInt(Object? value) {
  if (value == null) {
    return null;
  }
  if (value is num) {
    return value.toInt();
  }
  return int.tryParse(value.toString());
}

String _coverForThemes(List<String> themes) {
  if (themes.any((item) => item.contains('美食'))) {
    return 'https://images.unsplash.com/photo-1504674900247-0877df9cc836?auto=format&fit=crop&w=1400&q=80';
  }
  if (themes.any((item) => item.contains('自然'))) {
    return 'https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=1400&q=80';
  }
  return 'https://images.unsplash.com/photo-1514565131-fce0801e5785?auto=format&fit=crop&w=1400&q=80';
}

String _imageForCategory(String category) {
  if (category.contains('人文') ||
      category.contains('古迹') ||
      category.contains('科教') ||
      category.contains('文化') ||
      category.contains('博物馆')) {
    return 'https://images.unsplash.com/photo-1526481280695-3c4691a33277?auto=format&fit=crop&w=1200&q=80';
  }
  if (category.contains('购物') ||
      category.contains('商圈') ||
      category.contains('街区')) {
    return 'https://images.unsplash.com/photo-1483985988355-763728e1935b?auto=format&fit=crop&w=1200&q=80';
  }
  if (category.contains('夜') || category.contains('酒吧')) {
    return 'https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?auto=format&fit=crop&w=1200&q=80';
  }
  if (category.contains('公园') || category.contains('自然')) {
    return 'https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=1200&q=80';
  }
  return 'https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?auto=format&fit=crop&w=1200&q=80';
}

bool _hasText(String? value) => value != null && value.trim().isNotEmpty;
