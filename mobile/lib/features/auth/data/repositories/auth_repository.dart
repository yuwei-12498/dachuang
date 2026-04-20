import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:planner/core/network/api_exception.dart';
import 'package:planner/core/network/dio_client.dart';
import 'package:planner/features/auth/data/models/login_request.dart';
import 'package:planner/features/auth/data/models/user_session.dart';

final authRepositoryProvider = Provider<AuthRepository>((ref) {
  return AuthRepository(ref.watch(apiClientProvider));
});

class AuthRepository {
  AuthRepository(this._apiClient);

  final ApiClient _apiClient;

  static const String _loginPath = '/api/auth/login';
  static const String _logoutPath = '/api/auth/logout';

  Future<UserSession> login(LoginRequest request) {
    return _apiClient.postObject<UserSession>(
      _loginPath,
      data: request.toJson(),
      requiresAuth: false,
      decoder: UserSession.fromJson,
    );
  }

  Future<void> logout() async {
    try {
      await _apiClient.deleteVoid(_logoutPath);
    } on ApiException catch (error) {
      if (!error.isUnauthorized) {
        rethrow;
      }
    }
  }
}
