import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:planner/core/auth/auth_session.dart';
import 'package:planner/features/auth/data/models/login_request.dart';
import 'package:planner/features/auth/data/repositories/auth_repository.dart';
import 'package:planner/features/profile/data/models/profile_trip_type.dart';
import 'package:planner/features/profile/presentation/providers/profile_provider.dart';

final authControllerProvider = AsyncNotifierProvider<AuthController, void>(
  AuthController.new,
);

class AuthController extends AsyncNotifier<void> {
  @override
  FutureOr<void> build() {}

  Future<void> login({
    required String username,
    required String password,
  }) async {
    state = const AsyncLoading();
    state = await AsyncValue.guard(() async {
      final session = await ref
          .read(authRepositoryProvider)
          .login(LoginRequest(username: username, password: password));

      await ref
          .read(authSessionProvider.notifier)
          .saveAccessToken(session.token);
      _clearProfileScope();
    });
  }

  Future<void> logout() async {
    state = const AsyncLoading();
    state = await AsyncValue.guard(() async {
      try {
        await ref.read(authRepositoryProvider).logout();
      } catch (_) {
        // 用户主动退出时，以本地会话清理为最高优先级。
      }

      await ref.read(authSessionProvider.notifier).clearSession();
      _clearProfileScope();
    });
  }

  void _clearProfileScope() {
    ref.invalidate(profileControllerProvider);
    ref.invalidate(profileTripsProvider(ProfileTripType.generated));
    ref.invalidate(profileTripsProvider(ProfileTripType.saved));
  }
}
