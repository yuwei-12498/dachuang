import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:planner/core/network/api_exception.dart';

import '../../data/models/community_card.dart';
import '../../data/models/community_detail.dart';
import '../../data/models/community_page_payload.dart';
import '../../data/repositories/community_repository.dart';

@immutable
class CommunityFeedState {
  const CommunityFeedState({
    required this.items,
    required this.page,
    required this.size,
    required this.total,
    this.isLoadingMore = false,
    this.transientMessage,
  });

  factory CommunityFeedState.fromPayload(CommunityPagePayload payload) {
    return CommunityFeedState(
      items: payload.records,
      page: payload.page,
      size: payload.size,
      total: payload.total,
    );
  }

  final List<CommunityCard> items;
  final int page;
  final int size;
  final int total;
  final bool isLoadingMore;
  final String? transientMessage;

  bool get isEmpty => items.isEmpty;
  bool get hasMore => items.length < total;

  CommunityFeedState copyWith({
    List<CommunityCard>? items,
    int? page,
    int? size,
    int? total,
    bool? isLoadingMore,
    String? transientMessage,
    bool clearTransientMessage = false,
  }) {
    return CommunityFeedState(
      items: items ?? this.items,
      page: page ?? this.page,
      size: size ?? this.size,
      total: total ?? this.total,
      isLoadingMore: isLoadingMore ?? this.isLoadingMore,
      transientMessage:
          clearTransientMessage ? null : transientMessage ?? this.transientMessage,
    );
  }
}

class CommunityFeedController extends AsyncNotifier<CommunityFeedState> {
  static const int _defaultPageSize = 12;

  CommunityRepository get _repository => ref.read(communityRepositoryProvider);

  @override
  FutureOr<CommunityFeedState> build() async {
    final payload = await _repository.fetchCommunityPage(
      page: 1,
      size: _defaultPageSize,
    );
    return CommunityFeedState.fromPayload(payload);
  }

  Future<void> refreshFeed() async {
    final previous = state.asData?.value;
    final size = previous?.size ?? _defaultPageSize;

    try {
      final payload = await _repository.fetchCommunityPage(page: 1, size: size);
      state = AsyncData(CommunityFeedState.fromPayload(payload));
    } catch (error, stackTrace) {
      if (previous != null) {
        state = AsyncData(
          previous.copyWith(transientMessage: _friendlyMessage(error)),
        );
        return;
      }
      state = AsyncError(error, stackTrace);
    }
  }

  Future<void> loadMore() async {
    final current = state.asData?.value;
    if (current == null || current.isLoadingMore || !current.hasMore) {
      return;
    }

    state = AsyncData(
      current.copyWith(isLoadingMore: true, clearTransientMessage: true),
    );

    try {
      final payload = await _repository.fetchCommunityPage(
        page: current.page + 1,
        size: current.size,
      );

      state = AsyncData(
        current.copyWith(
          items: [...current.items, ...payload.records],
          page: payload.page,
          size: payload.size,
          total: payload.total,
          isLoadingMore: false,
          clearTransientMessage: true,
        ),
      );
    } catch (error) {
      state = AsyncData(
        current.copyWith(
          isLoadingMore: false,
          transientMessage: _friendlyMessage(error),
        ),
      );
    }
  }

  void consumeTransientMessage() {
    final current = state.asData?.value;
    if (current == null || current.transientMessage == null) {
      return;
    }
    state = AsyncData(current.copyWith(clearTransientMessage: true));
  }
}

final communityFeedControllerProvider =
    AsyncNotifierProvider<CommunityFeedController, CommunityFeedState>(
  CommunityFeedController.new,
);

final communityDetailProvider =
    FutureProvider.family<CommunityDetail, int>((ref, itineraryId) async {
  return ref.watch(communityRepositoryProvider).fetchCommunityDetail(itineraryId);
});

String _friendlyMessage(Object error) {
  if (error is ApiException) {
    return error.message;
  }

  final text = error.toString().replaceFirst('Exception: ', '').trim();
  if (text.isEmpty) {
    return '社区服务暂时不可用，请稍后再试。';
  }
  return text;
}
