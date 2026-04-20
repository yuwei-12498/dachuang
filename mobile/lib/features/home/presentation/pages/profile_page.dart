import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:planner/core/constants/app_constants.dart';
import 'package:planner/core/network/api_exception.dart';
import 'package:planner/features/auth/data/models/user_session.dart';
import 'package:planner/features/auth/presentation/controllers/auth_controller.dart';
import 'package:planner/features/community/presentation/widgets/community_itinerary_card.dart';
import 'package:planner/features/profile/data/models/profile_trip_item.dart';
import 'package:planner/features/profile/data/models/profile_trip_type.dart';
import 'package:planner/features/profile/data/models/profile_view_state.dart';
import 'package:planner/features/profile/presentation/providers/profile_provider.dart';

class ProfilePage extends ConsumerWidget {
  const ProfilePage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    ref.listen<AsyncValue<void>>(authControllerProvider, (previous, next) {
      if (!next.hasError || previous?.error == next.error) {
        return;
      }

      final error = next.error;
      final message = error is ApiException ? error.message : '退出登录失败，请稍后重试。';

      ScaffoldMessenger.of(context)
        ..hideCurrentSnackBar()
        ..showSnackBar(
          SnackBar(
            behavior: SnackBarBehavior.floating,
            content: Text(message),
            backgroundColor: Theme.of(context).colorScheme.error,
          ),
        );
    });

    final profileAsync = ref.watch(profileControllerProvider);
    final authState = ref.watch(authControllerProvider);
    final isLoggingOut = authState.isLoading;

    return DefaultTabController(
      length: ProfileTripType.values.length,
      child: DecoratedBox(
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            colors: [Color(0xFFF8FAFC), Color(0xFFF1F5F9)],
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
          ),
        ),
        child: SafeArea(
          bottom: false,
          child: Column(
            children: [
              Padding(
                padding: const EdgeInsets.fromLTRB(20, 12, 20, 0),
                child: _ProfileTopBar(
                  onRefresh: () =>
                      ref.read(profileControllerProvider.notifier).refreshAll(),
                  onLogout: () =>
                      ref.read(authControllerProvider.notifier).logout(),
                  isLoggingOut: isLoggingOut,
                ),
              ),
              Expanded(
                child: profileAsync.when(
                  loading: () => const _ProfilePageLoadingView(),
                  error: (error, _) => _ProfilePageErrorView(
                    message: profileFriendlyMessage(error),
                    onRetry: () => ref
                        .read(profileControllerProvider.notifier)
                        .refreshAll(),
                  ),
                  data: (state) => _ProfileContent(state: state),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _ProfileContent extends StatelessWidget {
  const _ProfileContent({required this.state});

  final ProfileViewState state;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(20, 18, 20, 0),
          child: _ProfileHeaderCard(user: state.user),
        ),
        Padding(
          padding: const EdgeInsets.fromLTRB(20, 16, 20, 0),
          child: _ProfileStatsRow(user: state.user),
        ),
        Padding(
          padding: const EdgeInsets.fromLTRB(20, 16, 20, 0),
          child: Container(
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(24),
              border: Border.all(color: const Color(0xFFE2E8F0)),
            ),
            child: TabBar(
              dividerColor: Colors.transparent,
              indicatorSize: TabBarIndicatorSize.tab,
              indicator: BoxDecoration(
                borderRadius: BorderRadius.circular(18),
                gradient: const LinearGradient(
                  colors: [Color(0xFF2563EB), Color(0xFF1D4ED8)],
                ),
              ),
              labelColor: Colors.white,
              unselectedLabelColor: const Color(0xFF475569),
              labelStyle: const TextStyle(
                fontSize: 14,
                fontWeight: FontWeight.w700,
              ),
              unselectedLabelStyle: const TextStyle(
                fontSize: 14,
                fontWeight: FontWeight.w600,
              ),
              padding: const EdgeInsets.all(6),
              tabs: const [
                Tab(text: '我的规划'),
                Tab(text: '我的收藏'),
              ],
            ),
          ),
        ),
        const SizedBox(height: 8),
        const Expanded(
          child: TabBarView(
            children: [
              _ProfileTripsTabView(type: ProfileTripType.generated),
              _ProfileTripsTabView(type: ProfileTripType.saved),
            ],
          ),
        ),
      ],
    );
  }
}

class _ProfileTopBar extends StatelessWidget {
  const _ProfileTopBar({
    required this.onRefresh,
    required this.onLogout,
    required this.isLoggingOut,
  });

  final VoidCallback onRefresh;
  final VoidCallback onLogout;
  final bool isLoggingOut;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                '我的',
                style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                  fontWeight: FontWeight.w800,
                  color: const Color(0xFF0F172A),
                ),
              ),
              const SizedBox(height: 6),
              const Text(
                '管理你的个人资料、规划资产与收藏路线',
                style: TextStyle(
                  fontSize: 14,
                  height: 1.6,
                  color: Color(0xFF475569),
                ),
              ),
            ],
          ),
        ),
        const SizedBox(width: 12),
        _CircleActionButton(
          icon: Icons.refresh_rounded,
          tooltip: '刷新个人数据',
          onTap: isLoggingOut ? null : onRefresh,
        ),
        const SizedBox(width: 10),
        AnimatedSwitcher(
          duration: const Duration(milliseconds: 220),
          child: isLoggingOut
              ? Container(
                  key: const ValueKey('logout-loading'),
                  width: 48,
                  height: 48,
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(16),
                    border: Border.all(color: const Color(0xFFE2E8F0)),
                  ),
                  alignment: Alignment.center,
                  child: const SizedBox(
                    width: 20,
                    height: 20,
                    child: CircularProgressIndicator(
                      strokeWidth: 2.4,
                      color: Color(0xFF1D4ED8),
                    ),
                  ),
                )
              : FilledButton.tonalIcon(
                  key: const ValueKey('logout-button'),
                  onPressed: onLogout,
                  style: FilledButton.styleFrom(
                    minimumSize: const Size(0, 48),
                    backgroundColor: const Color(0xFFEFF6FF),
                    foregroundColor: const Color(0xFF1D4ED8),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(16),
                    ),
                  ),
                  icon: const Icon(Icons.logout_rounded),
                  label: const Text(
                    '退出',
                    style: TextStyle(fontWeight: FontWeight.w700),
                  ),
                ),
        ),
      ],
    );
  }
}

class _CircleActionButton extends StatelessWidget {
  const _CircleActionButton({
    required this.icon,
    required this.tooltip,
    required this.onTap,
  });

  final IconData icon;
  final String tooltip;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    return Tooltip(
      message: tooltip,
      child: Material(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        child: InkWell(
          borderRadius: BorderRadius.circular(16),
          onTap: onTap,
          child: Ink(
            width: 48,
            height: 48,
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: const Color(0xFFE2E8F0)),
            ),
            child: Icon(
              icon,
              size: 22,
              color: onTap == null
                  ? const Color(0xFF94A3B8)
                  : const Color(0xFF1D4ED8),
            ),
          ),
        ),
      ),
    );
  }
}

class _ProfileHeaderCard extends StatelessWidget {
  const _ProfileHeaderCard({required this.user});

  final UserSession user;

  @override
  Widget build(BuildContext context) {
    final displayName = _displayName(user);
    final username = user.username.trim().isEmpty
        ? '未绑定用户名'
        : '@${user.username.trim()}';
    final avatarText = _avatarText(displayName);

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(22),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(30),
        gradient: const LinearGradient(
          colors: [Color(0xFF0F172A), Color(0xFF1D4ED8)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        boxShadow: const [
          BoxShadow(
            color: Color(0x1A0F172A),
            blurRadius: 32,
            offset: Offset(0, 18),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                width: 68,
                height: 68,
                decoration: BoxDecoration(
                  color: Colors.white.withValues(alpha: 0.16),
                  borderRadius: BorderRadius.circular(22),
                  border: Border.all(
                    color: Colors.white.withValues(alpha: 0.18),
                  ),
                ),
                alignment: Alignment.center,
                child: Text(
                  avatarText,
                  style: const TextStyle(
                    fontSize: 28,
                    fontWeight: FontWeight.w800,
                    color: Colors.white,
                  ),
                ),
              ),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      displayName,
                      style: Theme.of(context).textTheme.headlineSmall
                          ?.copyWith(
                            color: Colors.white,
                            fontWeight: FontWeight.w800,
                          ),
                    ),
                    const SizedBox(height: 6),
                    Text(
                      username,
                      style: const TextStyle(
                        fontSize: 14,
                        color: Color(0xFFE2E8F0),
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 18),
          Wrap(
            spacing: 10,
            runSpacing: 10,
            children: [
              _ProfilePill(
                icon: Icons.verified_user_rounded,
                label: _roleLabel(user.role),
              ),
              const _ProfilePill(icon: Icons.lock_rounded, label: 'JWT 已托管'),
              const _ProfilePill(
                icon: Icons.cloud_done_rounded,
                label: '资产已同步',
              ),
            ],
          ),
          const SizedBox(height: 16),
          const Text(
            '这里会集中沉淀你的智能规划结果与社区收藏路线。退出登录时，本地 Token 会被彻底清空，避免账号资产残留在设备中。',
            style: TextStyle(
              fontSize: 14,
              height: 1.7,
              color: Color(0xFFE2E8F0),
            ),
          ),
        ],
      ),
    );
  }
}

class _ProfilePill extends StatelessWidget {
  const _ProfilePill({required this.icon, required this.label});

  final IconData icon;
  final String label;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      decoration: BoxDecoration(
        color: Colors.white.withValues(alpha: 0.12),
        borderRadius: BorderRadius.circular(999),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 14, color: Colors.white),
          const SizedBox(width: 6),
          Text(
            label,
            style: const TextStyle(
              fontSize: 12,
              fontWeight: FontWeight.w700,
              color: Colors.white,
            ),
          ),
        ],
      ),
    );
  }
}

class _ProfileStatsRow extends ConsumerWidget {
  const _ProfileStatsRow({required this.user});

  final UserSession user;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final generatedTrips = ref.watch(
      profileTripsProvider(ProfileTripType.generated),
    );
    final savedTrips = ref.watch(profileTripsProvider(ProfileTripType.saved));

    return Row(
      children: [
        Expanded(
          child: _StatsCard(
            label: '我的规划',
            value: _countLabel(generatedTrips),
            hint: _countHint(generatedTrips),
            icon: Icons.route_rounded,
            accentColor: const Color(0xFF2563EB),
          ),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: _StatsCard(
            label: '我的收藏',
            value: _countLabel(savedTrips),
            hint: _countHint(savedTrips),
            icon: Icons.bookmark_rounded,
            accentColor: const Color(0xFF7C3AED),
          ),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: _StatsCard(
            label: '账户角色',
            value: _roleShortLabel(user.role),
            hint: '当前登录态',
            icon: Icons.shield_rounded,
            accentColor: const Color(0xFF0F766E),
          ),
        ),
      ],
    );
  }

  String _countLabel(AsyncValue<List<ProfileTripItem>> value) {
    return value.when(
      data: (items) => '${items.length}',
      loading: () => '--',
      error: (error, stackTrace) => '!',
    );
  }

  String _countHint(AsyncValue<List<ProfileTripItem>> value) {
    return value.when(
      data: (items) => items.isEmpty ? '等待沉淀' : '条路线',
      loading: () => '同步中',
      error: (error, stackTrace) => '可重试',
    );
  }
}

class _StatsCard extends StatelessWidget {
  const _StatsCard({
    required this.label,
    required this.value,
    required this.hint,
    required this.icon,
    required this.accentColor,
  });

  final String label;
  final String value;
  final String hint;
  final IconData icon;
  final Color accentColor;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(22),
        border: Border.all(color: const Color(0xFFE2E8F0)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 36,
            height: 36,
            decoration: BoxDecoration(
              color: accentColor.withValues(alpha: 0.12),
              borderRadius: BorderRadius.circular(12),
            ),
            alignment: Alignment.center,
            child: Icon(icon, size: 18, color: accentColor),
          ),
          const SizedBox(height: 12),
          Text(
            value,
            style: const TextStyle(
              fontSize: 22,
              fontWeight: FontWeight.w800,
              color: Color(0xFF0F172A),
            ),
          ),
          const SizedBox(height: 4),
          Text(
            label,
            style: const TextStyle(
              fontSize: 12,
              fontWeight: FontWeight.w700,
              color: Color(0xFF475569),
            ),
          ),
          const SizedBox(height: 2),
          Text(
            hint,
            style: const TextStyle(
              fontSize: 11,
              fontWeight: FontWeight.w600,
              color: Color(0xFF94A3B8),
            ),
          ),
        ],
      ),
    );
  }
}

class _ProfileTripsTabView extends ConsumerWidget {
  const _ProfileTripsTabView({required this.type});

  final ProfileTripType type;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final tripsAsync = ref.watch(profileTripsProvider(type));

    return tripsAsync.when(
      loading: () => const _ProfileTripsLoadingView(),
      error: (error, _) => _ProfileTripsErrorView(
        type: type,
        message: profileFriendlyMessage(error),
        onRetry: () => _refresh(ref),
      ),
      data: (items) {
        if (items.isEmpty) {
          return _ProfileTripsEmptyView(
            type: type,
            onRefresh: () => _refresh(ref),
          );
        }

        return RefreshIndicator.adaptive(
          color: const Color(0xFF2563EB),
          onRefresh: () => _refresh(ref),
          child: ListView.separated(
            physics: const AlwaysScrollableScrollPhysics(
              parent: BouncingScrollPhysics(),
            ),
            padding: const EdgeInsets.fromLTRB(20, 18, 20, 32),
            itemCount: items.length,
            separatorBuilder: (context, index) => const SizedBox(height: 16),
            itemBuilder: (context, index) {
              final item = items[index];
              return CommunityItineraryCard(
                title: item.title,
                coverImageUrl: item.coverImageUrl,
                topLeftLabel: item.cityName,
                badgeIcon: type == ProfileTripType.generated
                    ? Icons.auto_awesome_rounded
                    : Icons.bookmark_rounded,
                badgeLabel: type.badgeLabel,
                description: item.description(),
                tagLabels: item.tags(type),
                primaryLabel: item.timeWindowLabel(),
                secondaryLabel:
                    '${_duration(item.totalDuration)} · ${item.nodeCount} 个节点',
                priceLabel: _currency(item.totalCost),
                footerLabel: _footerLabel(item),
                onTap: () => _showTripPreview(context, item, type),
              );
            },
          ),
        );
      },
    );
  }

  Future<void> _refresh(WidgetRef ref) async {
    await ref.read(profileControllerProvider.notifier).refreshTrips(type);
  }
}

class _ProfileTripsLoadingView extends StatelessWidget {
  const _ProfileTripsLoadingView();

  @override
  Widget build(BuildContext context) {
    return ListView.separated(
      physics: const AlwaysScrollableScrollPhysics(
        parent: BouncingScrollPhysics(),
      ),
      padding: const EdgeInsets.fromLTRB(20, 18, 20, 32),
      itemCount: 3,
      separatorBuilder: (context, index) => const SizedBox(height: 16),
      itemBuilder: (context, index) {
        return Container(
          height: 360,
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(28),
            border: Border.all(color: const Color(0xFFE2E8F0)),
          ),
        );
      },
    );
  }
}

class _ProfileTripsErrorView extends StatelessWidget {
  const _ProfileTripsErrorView({
    required this.type,
    required this.message,
    required this.onRetry,
  });

  final ProfileTripType type;
  final String message;
  final Future<void> Function() onRetry;

  @override
  Widget build(BuildContext context) {
    return RefreshIndicator.adaptive(
      color: const Color(0xFF2563EB),
      onRefresh: onRetry,
      child: ListView(
        physics: const AlwaysScrollableScrollPhysics(
          parent: BouncingScrollPhysics(),
        ),
        padding: const EdgeInsets.fromLTRB(20, 18, 20, 32),
        children: [
          Container(
            padding: const EdgeInsets.all(24),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(28),
              border: Border.all(color: const Color(0xFFE2E8F0)),
            ),
            child: Column(
              children: [
                Container(
                  width: 72,
                  height: 72,
                  decoration: BoxDecoration(
                    color: const Color(0xFFEFF6FF),
                    borderRadius: BorderRadius.circular(24),
                  ),
                  child: const Icon(
                    Icons.wifi_off_rounded,
                    size: 34,
                    color: Color(0xFF2563EB),
                  ),
                ),
                const SizedBox(height: 18),
                Text(
                  '${type.tabLabel}加载失败',
                  style: const TextStyle(
                    fontSize: 22,
                    fontWeight: FontWeight.w800,
                    color: Color(0xFF0F172A),
                  ),
                ),
                const SizedBox(height: 10),
                Text(
                  message,
                  textAlign: TextAlign.center,
                  style: const TextStyle(
                    fontSize: 14,
                    height: 1.7,
                    color: Color(0xFF475569),
                  ),
                ),
                const SizedBox(height: 20),
                FilledButton.icon(
                  onPressed: () => onRetry(),
                  style: FilledButton.styleFrom(
                    minimumSize: const Size.fromHeight(52),
                    backgroundColor: const Color(0xFF2563EB),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(18),
                    ),
                  ),
                  icon: const Icon(Icons.refresh_rounded),
                  label: const Text(
                    '重新获取',
                    style: TextStyle(fontWeight: FontWeight.w700),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _ProfileTripsEmptyView extends StatelessWidget {
  const _ProfileTripsEmptyView({required this.type, required this.onRefresh});

  final ProfileTripType type;
  final Future<void> Function() onRefresh;

  @override
  Widget build(BuildContext context) {
    final isGenerated = type == ProfileTripType.generated;

    return ListView(
      physics: const AlwaysScrollableScrollPhysics(
        parent: BouncingScrollPhysics(),
      ),
      padding: const EdgeInsets.fromLTRB(20, 18, 20, 32),
      children: [
        Container(
          padding: const EdgeInsets.all(24),
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(28),
            border: Border.all(color: const Color(0xFFE2E8F0)),
          ),
          child: Column(
            children: [
              Container(
                width: 72,
                height: 72,
                decoration: BoxDecoration(
                  color: const Color(0xFFEFF6FF),
                  borderRadius: BorderRadius.circular(24),
                ),
                child: Icon(
                  isGenerated
                      ? Icons.route_rounded
                      : Icons.bookmark_border_rounded,
                  size: 34,
                  color: const Color(0xFF2563EB),
                ),
              ),
              const SizedBox(height: 18),
              Text(
                type.emptyTitle,
                style: const TextStyle(
                  fontSize: 22,
                  fontWeight: FontWeight.w800,
                  color: Color(0xFF0F172A),
                ),
              ),
              const SizedBox(height: 10),
              Text(
                type.emptyDescription,
                textAlign: TextAlign.center,
                style: const TextStyle(
                  fontSize: 14,
                  height: 1.7,
                  color: Color(0xFF475569),
                ),
              ),
              const SizedBox(height: 20),
              FilledButton.icon(
                onPressed: () {
                  if (isGenerated) {
                    context.go(AppConstants.plannerRoutePath);
                    return;
                  }
                  context.go(AppConstants.communityRoutePath);
                },
                style: FilledButton.styleFrom(
                  minimumSize: const Size.fromHeight(52),
                  backgroundColor: const Color(0xFF2563EB),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(18),
                  ),
                ),
                icon: Icon(
                  isGenerated
                      ? Icons.auto_awesome_rounded
                      : Icons.explore_rounded,
                ),
                label: Text(
                  isGenerated ? '去智能规划' : '去社区发现',
                  style: const TextStyle(fontWeight: FontWeight.w700),
                ),
              ),
              const SizedBox(height: 10),
              TextButton.icon(
                onPressed: () => onRefresh(),
                icon: const Icon(Icons.refresh_rounded),
                label: const Text('重新拉取数据'),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class _ProfilePageLoadingView extends StatelessWidget {
  const _ProfilePageLoadingView();

  @override
  Widget build(BuildContext context) {
    return ListView(
      physics: const AlwaysScrollableScrollPhysics(
        parent: BouncingScrollPhysics(),
      ),
      padding: const EdgeInsets.fromLTRB(20, 18, 20, 32),
      children: [
        Container(
          height: 220,
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(30),
          ),
        ),
        const SizedBox(height: 16),
        Row(
          children: List.generate(
            3,
            (index) => Expanded(
              child: Padding(
                padding: EdgeInsets.only(right: index == 2 ? 0 : 12),
                child: Container(
                  height: 120,
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(22),
                  ),
                ),
              ),
            ),
          ),
        ),
        const SizedBox(height: 16),
        Container(
          height: 64,
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(24),
          ),
        ),
        const SizedBox(height: 16),
        ...List.generate(
          2,
          (_) => Padding(
            padding: const EdgeInsets.only(bottom: 16),
            child: Container(
              height: 320,
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(28),
              ),
            ),
          ),
        ),
      ],
    );
  }
}

class _ProfilePageErrorView extends StatelessWidget {
  const _ProfilePageErrorView({required this.message, required this.onRetry});

  final String message;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    return ListView(
      physics: const AlwaysScrollableScrollPhysics(
        parent: BouncingScrollPhysics(),
      ),
      padding: const EdgeInsets.fromLTRB(20, 18, 20, 32),
      children: [
        Container(
          padding: const EdgeInsets.all(24),
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(28),
            border: Border.all(color: const Color(0xFFE2E8F0)),
          ),
          child: Column(
            children: [
              Container(
                width: 72,
                height: 72,
                decoration: BoxDecoration(
                  color: const Color(0xFFEFF6FF),
                  borderRadius: BorderRadius.circular(24),
                ),
                child: const Icon(
                  Icons.person_search_rounded,
                  size: 34,
                  color: Color(0xFF2563EB),
                ),
              ),
              const SizedBox(height: 18),
              const Text(
                '个人资料加载失败',
                style: TextStyle(
                  fontSize: 22,
                  fontWeight: FontWeight.w800,
                  color: Color(0xFF0F172A),
                ),
              ),
              const SizedBox(height: 10),
              Text(
                message,
                textAlign: TextAlign.center,
                style: const TextStyle(
                  fontSize: 14,
                  height: 1.7,
                  color: Color(0xFF475569),
                ),
              ),
              const SizedBox(height: 20),
              FilledButton.icon(
                onPressed: onRetry,
                style: FilledButton.styleFrom(
                  minimumSize: const Size.fromHeight(52),
                  backgroundColor: const Color(0xFF2563EB),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(18),
                  ),
                ),
                icon: const Icon(Icons.refresh_rounded),
                label: const Text(
                  '重试加载',
                  style: TextStyle(fontWeight: FontWeight.w700),
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

Future<void> _showTripPreview(
  BuildContext context,
  ProfileTripItem item,
  ProfileTripType type,
) {
  return showModalBottomSheet<void>(
    context: context,
    isScrollControlled: true,
    backgroundColor: Colors.transparent,
    builder: (context) {
      return Container(
        decoration: const BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.vertical(top: Radius.circular(32)),
        ),
        child: SafeArea(
          top: false,
          child: Padding(
            padding: EdgeInsets.fromLTRB(
              20,
              14,
              20,
              20 + MediaQuery.of(context).viewInsets.bottom,
            ),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Center(
                  child: Container(
                    width: 44,
                    height: 5,
                    decoration: BoxDecoration(
                      color: const Color(0xFFE2E8F0),
                      borderRadius: BorderRadius.circular(999),
                    ),
                  ),
                ),
                const SizedBox(height: 20),
                Text(
                  item.title,
                  style: const TextStyle(
                    fontSize: 24,
                    fontWeight: FontWeight.w800,
                    color: Color(0xFF0F172A),
                  ),
                ),
                const SizedBox(height: 12),
                Wrap(
                  spacing: 8,
                  runSpacing: 8,
                  children: [
                    _SheetChip(label: item.cityName),
                    _SheetChip(label: type.badgeLabel),
                    _SheetChip(label: _duration(item.totalDuration)),
                    _SheetChip(label: _currency(item.totalCost)),
                  ],
                ),
                const SizedBox(height: 18),
                const Text(
                  '路线摘要',
                  style: TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.w800,
                    color: Color(0xFF0F172A),
                  ),
                ),
                const SizedBox(height: 8),
                Text(
                  item.routeSummary,
                  style: const TextStyle(
                    fontSize: 14,
                    height: 1.7,
                    color: Color(0xFF475569),
                  ),
                ),
                const SizedBox(height: 18),
                _SheetInfoRow(
                  icon: Icons.schedule_rounded,
                  label: '出行时段',
                  value: item.timeWindowLabel(),
                ),
                const SizedBox(height: 12),
                _SheetInfoRow(
                  icon: Icons.place_rounded,
                  label: '路线骨架',
                  value: item.footerLabel(),
                ),
                const SizedBox(height: 12),
                _SheetInfoRow(
                  icon: Icons.category_rounded,
                  label: '标签特征',
                  value: item.tags(type).isEmpty
                      ? '待后端补充主题标签'
                      : item.tags(type).join(' · '),
                ),
                const SizedBox(height: 24),
                SizedBox(
                  width: double.infinity,
                  child: FilledButton(
                    onPressed: () => Navigator.of(context).pop(),
                    style: FilledButton.styleFrom(
                      minimumSize: const Size.fromHeight(52),
                      backgroundColor: const Color(0xFF2563EB),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(18),
                      ),
                    ),
                    child: const Text(
                      '知道了',
                      style: TextStyle(fontWeight: FontWeight.w700),
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      );
    },
  );
}

class _SheetChip extends StatelessWidget {
  const _SheetChip({required this.label});

  final String label;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      decoration: BoxDecoration(
        color: const Color(0xFFEFF6FF),
        borderRadius: BorderRadius.circular(999),
      ),
      child: Text(
        label,
        style: const TextStyle(
          fontSize: 12,
          fontWeight: FontWeight.w700,
          color: Color(0xFF1D4ED8),
        ),
      ),
    );
  }
}

class _SheetInfoRow extends StatelessWidget {
  const _SheetInfoRow({
    required this.icon,
    required this.label,
    required this.value,
  });

  final IconData icon;
  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
          width: 36,
          height: 36,
          decoration: BoxDecoration(
            color: const Color(0xFFEFF6FF),
            borderRadius: BorderRadius.circular(12),
          ),
          alignment: Alignment.center,
          child: Icon(icon, size: 18, color: const Color(0xFF1D4ED8)),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                label,
                style: const TextStyle(
                  fontSize: 12,
                  fontWeight: FontWeight.w700,
                  color: Color(0xFF64748B),
                ),
              ),
              const SizedBox(height: 4),
              Text(
                value,
                style: const TextStyle(
                  fontSize: 14,
                  height: 1.6,
                  fontWeight: FontWeight.w600,
                  color: Color(0xFF0F172A),
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

String _displayName(UserSession user) {
  final nickname = user.nickname.trim();
  if (nickname.isNotEmpty) {
    return nickname;
  }
  final username = user.username.trim();
  if (username.isNotEmpty) {
    return username;
  }
  return '城市旅行家';
}

String _avatarText(String displayName) {
  final trimmed = displayName.trim();
  if (trimmed.isEmpty) {
    return '行';
  }
  return trimmed.substring(0, 1).toUpperCase();
}

String _roleLabel(int role) {
  if (role >= 9) {
    return '管理员账号';
  }
  if (role >= 1) {
    return '认证用户';
  }
  return '普通用户';
}

String _roleShortLabel(int role) {
  if (role >= 9) {
    return '管理';
  }
  if (role >= 1) {
    return '认证';
  }
  return '普通';
}

String _currency(double value) {
  if (value == value.roundToDouble()) {
    return '¥${value.toStringAsFixed(0)}';
  }
  return '¥${value.toStringAsFixed(1)}';
}

String _duration(int totalMinutes) {
  final hours = totalMinutes ~/ 60;
  final minutes = totalMinutes % 60;
  if (hours <= 0) {
    return '$minutes 分钟';
  }
  if (minutes == 0) {
    return '$hours 小时';
  }
  return '$hours 小时 $minutes 分钟';
}

String _footerLabel(ProfileTripItem item) {
  final updatedAt = item.updatedAt;
  if (updatedAt == null) {
    return item.footerLabel();
  }
  return '最近更新 ${_formatDate(updatedAt)} · ${item.footerLabel()}';
}

String _formatDate(DateTime value) {
  final month = value.month.toString().padLeft(2, '0');
  final day = value.day.toString().padLeft(2, '0');
  return '$month-$day';
}
