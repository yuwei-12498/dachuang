import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:planner/core/auth/auth_session.dart';
import 'package:planner/core/constants/app_constants.dart';
import 'package:planner/features/auth/presentation/pages/login_page.dart';
import 'package:planner/features/community/presentation/pages/community_detail_page.dart';
import 'package:planner/features/community/presentation/pages/community_page.dart';
import 'package:planner/features/home/presentation/pages/main_shell_page.dart';
import 'package:planner/features/home/presentation/pages/profile_page.dart';
import 'package:planner/features/planner/presentation/pages/planner_input_page.dart';
import 'package:planner/features/planner/presentation/pages/planner_page.dart';

final routerRefreshNotifierProvider = Provider<RouterRefreshNotifier>((ref) {
  final notifier = RouterRefreshNotifier(ref);
  ref.onDispose(notifier.dispose);
  return notifier;
});

final appRouterProvider = Provider<GoRouter>((ref) {
  final refreshNotifier = ref.watch(routerRefreshNotifierProvider);

  return GoRouter(
    initialLocation: AppConstants.plannerRoutePath,
    refreshListenable: refreshNotifier,
    redirect: (context, state) {
      final session = ref.read(authSessionProvider);
      final isLoggedIn = session.isAuthenticated;
      final isGoingToLogin =
          state.matchedLocation == AppConstants.loginRoutePath;

      if (!isLoggedIn && !isGoingToLogin) {
        return AppConstants.loginRoutePath;
      }

      if (isLoggedIn && isGoingToLogin) {
        return AppConstants.plannerRoutePath;
      }

      return null;
    },
    routes: [
      GoRoute(
        path: AppConstants.loginRoutePath,
        name: AppConstants.loginRouteName,
        builder: (context, state) => const LoginPage(),
      ),
      StatefulShellRoute.indexedStack(
        builder: (context, state, navigationShell) {
          return MainShellPage(navigationShell: navigationShell);
        },
        branches: [
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: AppConstants.plannerRoutePath,
                name: AppConstants.plannerRouteName,
                builder: (context, state) => const PlannerInputPage(),
                routes: [
                  GoRoute(
                    path: AppConstants.plannerResultRoutePath,
                    name: AppConstants.plannerResultRouteName,
                    builder: (context, state) => const PlannerPage(),
                  ),
                ],
              ),
            ],
          ),
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: AppConstants.communityRoutePath,
                name: AppConstants.communityRouteName,
                builder: (context, state) => const CommunityPage(),
                routes: [
                  GoRoute(
                    path: AppConstants.communityDetailRoutePath,
                    name: AppConstants.communityDetailRouteName,
                    builder: (context, state) {
                      final communityId = int.tryParse(
                        state.pathParameters[
                                AppConstants.communityDetailPathParameter] ??
                            '',
                      );
                      if (communityId == null) {
                        return const CommunityDetailPage.invalid();
                      }
                      return CommunityDetailPage(communityId: communityId);
                    },
                  ),
                ],
              ),
            ],
          ),
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: AppConstants.profileRoutePath,
                name: AppConstants.profileRouteName,
                builder: (context, state) => const ProfilePage(),
              ),
            ],
          ),
        ],
      ),
    ],
  );
});

class RouterRefreshNotifier extends ChangeNotifier {
  RouterRefreshNotifier(this.ref) {
    ref.listen<AuthSessionState>(
      authSessionProvider,
      (_, next) => notifyListeners(),
    );
  }

  final Ref ref;
}
