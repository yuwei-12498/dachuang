import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:planner/core/network/api_exception.dart';

import '../../data/models/profile_trip_item.dart';
import '../../data/models/profile_trip_type.dart';
import '../../data/models/profile_view_state.dart';
import '../../data/repositories/profile_repository.dart';

class ProfileController extends AsyncNotifier<ProfileViewState> {
  @override
  FutureOr<ProfileViewState> build() async {
    final user = await ref.read(profileRepositoryProvider).fetchCurrentUser();
    return ProfileViewState(user: user);
  }

  Future<void> refreshAll() async {
    ref.invalidate(profileTripsProvider(ProfileTripType.generated));
    ref.invalidate(profileTripsProvider(ProfileTripType.saved));
    state = const AsyncLoading();
    state = await AsyncValue.guard(() async {
      final user = await ref.read(profileRepositoryProvider).fetchCurrentUser();
      return ProfileViewState(user: user);
    });
  }

  Future<void> refreshTrips(ProfileTripType type) async {
    ref.invalidate(profileTripsProvider(type));
    await ref.read(profileTripsProvider(type).future);
  }
}

final profileControllerProvider =
    AsyncNotifierProvider<ProfileController, ProfileViewState>(
      ProfileController.new,
    );

final profileTripsProvider =
    FutureProvider.family<List<ProfileTripItem>, ProfileTripType>((
      ref,
      type,
    ) async {
      return ref.watch(profileRepositoryProvider).fetchTrips(type);
    });

String profileFriendlyMessage(Object error) {
  if (error is ApiException) {
    return error.message;
  }
  final text = error.toString().replaceFirst('Exception: ', '').trim();
  if (text.isEmpty) {
    return '页面数据暂时不可用，请稍后重试。';
  }
  return text;
}
