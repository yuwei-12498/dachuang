import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:planner/core/storage/token_storage.dart';

class AuthSessionState {
  const AuthSessionState({
    required this.isHydrated,
    this.accessToken,
  });

  const AuthSessionState.unknown()
      : isHydrated = false,
        accessToken = null;

  const AuthSessionState.unauthenticated()
      : isHydrated = true,
        accessToken = null;

  const AuthSessionState.authenticated(this.accessToken) : isHydrated = true;

  final bool isHydrated;
  final String? accessToken;

  bool get isAuthenticated => accessToken?.isNotEmpty ?? false;
}

class AuthSessionController extends Notifier<AuthSessionState> {
  late final TokenStorage _tokenStorage;

  @override
  AuthSessionState build() {
    _tokenStorage = ref.read(tokenStorageProvider);
    return const AuthSessionState.unknown();
  }

  Future<void> restoreSession() async {
    final token = await _tokenStorage.readAccessToken();
    if (token == null || token.isEmpty) {
      state = const AuthSessionState.unauthenticated();
      return;
    }

    state = AuthSessionState.authenticated(token);
  }

  Future<void> saveAccessToken(String token) async {
    await _tokenStorage.saveAccessToken(token);
    state = AuthSessionState.authenticated(token);
  }

  Future<void> clearSession() async {
    await _tokenStorage.clearSession();
    state = const AuthSessionState.unauthenticated();
  }
}

final authSessionProvider =
    NotifierProvider<AuthSessionController, AuthSessionState>(
  AuthSessionController.new,
);
