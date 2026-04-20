class UserSession {
  const UserSession({
    required this.id,
    required this.username,
    required this.nickname,
    required this.role,
    required this.token,
  });

  final int id;
  final String username;
  final String nickname;
  final int role;
  final String token;

  factory UserSession.fromJson(Map<String, dynamic> json) {
    return UserSession(
      id: (json['id'] as num?)?.toInt() ?? 0,
      username: (json['username'] ?? '').toString(),
      nickname: (json['nickname'] ?? '').toString(),
      role: (json['role'] as num?)?.toInt() ?? 0,
      token: (json['token'] ?? '').toString(),
    );
  }
}
