import 'package:flutter/foundation.dart';
import 'package:planner/features/auth/data/models/user_session.dart';

@immutable
class ProfileViewState {
  const ProfileViewState({required this.user});

  final UserSession user;
}
