import 'dart:math' as math;

import 'package:flutter/foundation.dart';

@immutable
class PlannerRequest {
  const PlannerRequest({
    required this.cityName,
    required this.tripDays,
    required this.totalBudget,
    required this.themes,
    required this.tripDate,
    required this.startTime,
    required this.endTime,
    required this.isRainy,
    required this.isNight,
    required this.walkingLevel,
    required this.companionType,
  });

  factory PlannerRequest.initial() {
    return const PlannerRequest(
      cityName: '成都',
      tripDays: 2,
      totalBudget: 1800,
      themes: ['人文', '美食'],
      tripDate: '2026-05-01',
      startTime: '09:30',
      endTime: '21:30',
      isRainy: false,
      isNight: true,
      walkingLevel: 'medium',
      companionType: 'friends',
    );
  }

  factory PlannerRequest.demo() => PlannerRequest.initial();

  final String cityName;
  final int tripDays;
  final double totalBudget;
  final List<String> themes;
  final String tripDate;
  final String startTime;
  final String endTime;
  final bool isRainy;
  final bool isNight;
  final String walkingLevel;
  final String companionType;

  String get budgetLevel {
    final perDayBudget = totalBudget / math.max(tripDays, 1);
    if (perDayBudget < 700) {
      return 'low';
    }
    if (perDayBudget > 1800) {
      return 'high';
    }
    return 'medium';
  }

  PlannerRequest copyWith({
    String? cityName,
    int? tripDays,
    double? totalBudget,
    List<String>? themes,
    String? tripDate,
    String? startTime,
    String? endTime,
    bool? isRainy,
    bool? isNight,
    String? walkingLevel,
    String? companionType,
  }) {
    return PlannerRequest(
      cityName: cityName ?? this.cityName,
      tripDays: tripDays ?? this.tripDays,
      totalBudget: totalBudget ?? this.totalBudget,
      themes: themes ?? this.themes,
      tripDate: tripDate ?? this.tripDate,
      startTime: startTime ?? this.startTime,
      endTime: endTime ?? this.endTime,
      isRainy: isRainy ?? this.isRainy,
      isNight: isNight ?? this.isNight,
      walkingLevel: walkingLevel ?? this.walkingLevel,
      companionType: companionType ?? this.companionType,
    );
  }

  Map<String, dynamic> toJson() {
    return <String, dynamic>{
      'cityName': cityName,
      'tripDays': tripDays.toDouble(),
      'totalBudget': totalBudget,
      'themes': themes,
      'tripDate': tripDate,
      'startTime': startTime,
      'endTime': endTime,
      'isRainy': isRainy,
      'isNight': isNight,
      'walkingLevel': walkingLevel,
      'companionType': companionType,
      'budgetLevel': budgetLevel,
    };
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) {
      return true;
    }
    return other is PlannerRequest &&
        other.cityName == cityName &&
        other.tripDays == tripDays &&
        other.totalBudget == totalBudget &&
        listEquals(other.themes, themes) &&
        other.tripDate == tripDate &&
        other.startTime == startTime &&
        other.endTime == endTime &&
        other.isRainy == isRainy &&
        other.isNight == isNight &&
        other.walkingLevel == walkingLevel &&
        other.companionType == companionType;
  }

  @override
  int get hashCode => Object.hash(
    cityName,
    tripDays,
    totalBudget,
    Object.hashAll(themes),
    tripDate,
    startTime,
    endTime,
    isRainy,
    isNight,
    walkingLevel,
    companionType,
  );
}
