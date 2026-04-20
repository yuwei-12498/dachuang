import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../data/models/itinerary_plan.dart';
import '../../data/models/planner_request.dart';
import '../../data/repositories/planner_repository.dart';

const plannerThinkingMessages = <String>[
  '正在分析城市数据...',
  '正在结合预算与主题偏好...',
  '正在避开高拥挤时段...',
  '正在推演最优动线与停留时长...',
  '正在生成你的专属微度假路线...',
];

enum PlannerFlowPhase { editing, planning, planned }

@immutable
class PlannerFlowState {
  const PlannerFlowState({
    required this.phase,
    required this.thinkingMessageIndex,
    this.activeRequest,
    this.errorMessage,
  });

  const PlannerFlowState.initial()
    : phase = PlannerFlowPhase.editing,
      thinkingMessageIndex = 0,
      activeRequest = null,
      errorMessage = null;

  final PlannerFlowPhase phase;
  final PlannerRequest? activeRequest;
  final int thinkingMessageIndex;
  final String? errorMessage;

  bool get isPlanning => phase == PlannerFlowPhase.planning;

  PlannerFlowState copyWith({
    PlannerFlowPhase? phase,
    PlannerRequest? activeRequest,
    bool keepActiveRequest = true,
    int? thinkingMessageIndex,
    String? errorMessage,
    bool clearError = false,
  }) {
    return PlannerFlowState(
      phase: phase ?? this.phase,
      activeRequest: keepActiveRequest
          ? (activeRequest ?? this.activeRequest)
          : activeRequest,
      thinkingMessageIndex: thinkingMessageIndex ?? this.thinkingMessageIndex,
      errorMessage: clearError ? null : errorMessage ?? this.errorMessage,
    );
  }
}

class PlannerFlowController extends Notifier<PlannerFlowState> {
  Timer? _thinkingTimer;

  @override
  PlannerFlowState build() {
    ref.onDispose(_cancelThinkingTimer);
    return const PlannerFlowState.initial();
  }

  Future<void> startPlanning(PlannerRequest request) async {
    if (state.isPlanning) {
      return;
    }

    _cancelThinkingTimer();
    state = PlannerFlowState(
      phase: PlannerFlowPhase.planning,
      activeRequest: request,
      thinkingMessageIndex: 0,
      errorMessage: null,
    );

    _thinkingTimer = Timer.periodic(const Duration(milliseconds: 620), (_) {
      final nextIndex =
          (state.thinkingMessageIndex + 1) % plannerThinkingMessages.length;
      state = state.copyWith(thinkingMessageIndex: nextIndex, clearError: true);
    });

    try {
      ref.invalidate(plannerItineraryProvider);
      final itineraryFuture = ref.read(plannerItineraryProvider.future);

      await Future.wait<Object?>([
        itineraryFuture,
        Future<void>.delayed(const Duration(milliseconds: 2800)),
      ]);

      _cancelThinkingTimer();
      state = state.copyWith(
        phase: PlannerFlowPhase.planned,
        thinkingMessageIndex: plannerThinkingMessages.length - 1,
        clearError: true,
      );
    } catch (error) {
      _cancelThinkingTimer();
      state = state.copyWith(
        phase: PlannerFlowPhase.editing,
        errorMessage: _friendlyMessage(error),
      );
    }
  }

  void resetToEditing({bool keepRequest = true}) {
    _cancelThinkingTimer();
    state = state.copyWith(
      phase: PlannerFlowPhase.editing,
      activeRequest: keepRequest ? state.activeRequest : null,
      keepActiveRequest: keepRequest,
      thinkingMessageIndex: 0,
      clearError: true,
    );
  }

  void clearError() {
    state = state.copyWith(clearError: true);
  }

  void _cancelThinkingTimer() {
    _thinkingTimer?.cancel();
    _thinkingTimer = null;
  }
}

final plannerFlowControllerProvider =
    NotifierProvider<PlannerFlowController, PlannerFlowState>(
      PlannerFlowController.new,
    );

final plannerCurrentRequestProvider = Provider<PlannerRequest?>((ref) {
  return ref.watch(
    plannerFlowControllerProvider.select((state) => state.activeRequest),
  );
});

final plannerThinkingMessageProvider = Provider<String>((ref) {
  final index = ref.watch(
    plannerFlowControllerProvider.select((state) => state.thinkingMessageIndex),
  );
  return plannerThinkingMessages[index % plannerThinkingMessages.length];
});

final plannerItineraryProvider = FutureProvider<PlannerItinerary>((ref) async {
  final repository = ref.watch(plannerRepositoryProvider);
  final request = ref.watch(plannerCurrentRequestProvider);

  if (request == null) {
    throw StateError('请先完成出行条件填写，再生成路线结果。');
  }

  return repository.generatePlan(request);
});

String _friendlyMessage(Object error) {
  final text = error.toString().replaceFirst('Exception: ', '').trim();
  if (text.isEmpty) {
    return '智能规划暂时失败，请稍后重试。';
  }
  return text;
}
