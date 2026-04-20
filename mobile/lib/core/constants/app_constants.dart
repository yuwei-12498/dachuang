abstract final class AppConstants {
  static const String appName = '\u884c\u57ce\u6709\u6570';
  static const String appSubtitle =
      '\u57ce\u5e02\u5fae\u5ea6\u5047\u667a\u80fd\u51b3\u7b56\u5e73\u53f0';

  /// Android Application ID / iOS Bundle ID
  static const String appId = 'com.citytrip.planner';

  static const String loginRouteName = 'login';
  static const String plannerRouteName = 'planner';
  static const String plannerResultRouteName = 'plannerResult';
  static const String communityRouteName = 'community';
  static const String communityDetailRouteName = 'communityDetail';
  static const String profileRouteName = 'profile';

  static const String loginRoutePath = '/login';
  static const String plannerRoutePath = '/planner';
  static const String plannerResultRoutePath = 'result';
  static const String plannerResultRouteLocation = '/planner/result';
  static const String communityRoutePath = '/community';
  static const String communityDetailPathParameter = 'communityId';
  static const String communityDetailRoutePath =
      ':$communityDetailPathParameter';
  static const String profileRoutePath = '/profile';

  static String communityDetailRouteLocation(int communityId) =>
      '$communityRoutePath/$communityId';
}
