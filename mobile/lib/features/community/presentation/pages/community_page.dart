import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_staggered_grid_view/flutter_staggered_grid_view.dart';
import 'package:go_router/go_router.dart';
import 'package:planner/core/constants/app_constants.dart';

import '../../data/models/community_card.dart';
import '../providers/community_provider.dart';
import '../widgets/community_itinerary_card.dart';

class CommunityPage extends ConsumerStatefulWidget {
  const CommunityPage({super.key});

  @override
  ConsumerState<CommunityPage> createState() => _CommunityPageState();
}

class _CommunityPageState extends ConsumerState<CommunityPage> {
  late final ScrollController _scrollController;

  @override
  void initState() {
    super.initState();
    _scrollController = ScrollController()..addListener(_handleScroll);
  }

  @override
  void dispose() {
    _scrollController
      ..removeListener(_handleScroll)
      ..dispose();
    super.dispose();
  }

  void _handleScroll() {
    if (!_scrollController.hasClients) {
      return;
    }

    if (_scrollController.position.extentAfter < 520) {
      ref.read(communityFeedControllerProvider.notifier).loadMore();
    }
  }

  @override
  Widget build(BuildContext context) {
    ref.listen<AsyncValue<CommunityFeedState>>(
      communityFeedControllerProvider,
      (previous, next) {
        final currentMessage = next.asData?.value.transientMessage;
        final previousMessage = previous?.asData?.value.transientMessage;
        if (currentMessage == null || currentMessage == previousMessage) {
          return;
        }

        WidgetsBinding.instance.addPostFrameCallback((_) {
          if (!mounted) {
            return;
          }
          ScaffoldMessenger.of(context)
            ..hideCurrentSnackBar()
            ..showSnackBar(SnackBar(content: Text(currentMessage)));
          ref
              .read(communityFeedControllerProvider.notifier)
              .consumeTransientMessage();
        });
      },
    );

    final feedAsync = ref.watch(communityFeedControllerProvider);

    return DecoratedBox(
      decoration: const BoxDecoration(
        gradient: LinearGradient(
          colors: [Color(0xFFF8FAFC), Color(0xFFF1F5F9)],
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
        ),
      ),
      child: SafeArea(
        bottom: false,
        child: feedAsync.when(
          loading: () => const _CommunityLoadingView(),
          error: (error, stackTrace) => _CommunityErrorView(
            message: _friendlyMessage(error),
            onRetry: () => ref.invalidate(communityFeedControllerProvider),
          ),
          data: (state) => _CommunityFeedView(
            state: state,
            scrollController: _scrollController,
            onRefresh: () =>
                ref.read(communityFeedControllerProvider.notifier).refreshFeed(),
            onCardTap: (card) {
              context.pushNamed(
                AppConstants.communityDetailRouteName,
                pathParameters: {
                  AppConstants.communityDetailPathParameter: '${card.id}',
                },
              );
            },
          ),
        ),
      ),
    );
  }
}

class _CommunityFeedView extends StatelessWidget {
  const _CommunityFeedView({
    required this.state,
    required this.scrollController,
    required this.onRefresh,
    required this.onCardTap,
  });

  final CommunityFeedState state;
  final ScrollController scrollController;
  final RefreshCallback onRefresh;
  final ValueChanged<CommunityCard> onCardTap;

  @override
  Widget build(BuildContext context) {
    return RefreshIndicator.adaptive(
      color: const Color(0xFF2563EB),
      onRefresh: onRefresh,
      child: CustomScrollView(
        controller: scrollController,
        physics: const AlwaysScrollableScrollPhysics(
          parent: BouncingScrollPhysics(),
        ),
        slivers: [
          const SliverToBoxAdapter(child: SizedBox(height: 10)),
          const SliverPadding(
            padding: EdgeInsets.fromLTRB(20, 8, 20, 0),
            sliver: SliverToBoxAdapter(child: _CommunityHeader()),
          ),
          if (state.isEmpty)
            SliverFillRemaining(
              hasScrollBody: false,
              child: _CommunityEmptyView(onRefresh: onRefresh),
            )
          else ...[
            SliverPadding(
              padding: const EdgeInsets.fromLTRB(20, 22, 20, 0),
              sliver: SliverToBoxAdapter(
                child: _CommunityMetricsBar(state: state),
              ),
            ),
            SliverPadding(
              padding: const EdgeInsets.fromLTRB(20, 18, 20, 20),
              sliver: SliverMasonryGrid.count(
                crossAxisCount: 2,
                mainAxisSpacing: 16,
                crossAxisSpacing: 16,
                childCount: state.items.length,
                itemBuilder: (context, index) {
                  final card = state.items[index];
                  return _CommunityCardTile(
                    card: card,
                    onTap: () => onCardTap(card),
                  );
                },
              ),
            ),
            SliverToBoxAdapter(
              child: AnimatedSwitcher(
                duration: const Duration(milliseconds: 220),
                child: state.isLoadingMore
                    ? const Padding(
                        padding: EdgeInsets.only(bottom: 24),
                        child: Center(
                          child: CircularProgressIndicator.adaptive(
                            valueColor: AlwaysStoppedAnimation<Color>(
                              Color(0xFF2563EB),
                            ),
                          ),
                        ),
                      )
                    : const SizedBox(height: 24),
              ),
            ),
          ],
        ],
      ),
    );
  }
}

class _CommunityHeader extends StatelessWidget {
  const _CommunityHeader();

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    '社区发现',
                    style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                          fontWeight: FontWeight.w800,
                          color: const Color(0xFF0F172A),
                        ),
                  ),
                  const SizedBox(height: 8),
                  const Text(
                    '浏览真实用户公开的路线卡片，找到高点赞、高完成度的城市微度假方案。',
                    style: TextStyle(
                      fontSize: 14,
                      height: 1.7,
                      color: Color(0xFF475569),
                    ),
                  ),
                ],
              ),
            ),
            Container(
              width: 52,
              height: 52,
              decoration: BoxDecoration(
                gradient: const LinearGradient(
                  colors: [Color(0xFF2563EB), Color(0xFF7C3AED)],
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                ),
                borderRadius: BorderRadius.circular(18),
              ),
              child: const Icon(
                Icons.auto_awesome_rounded,
                color: Colors.white,
              ),
            ),
          ],
        ),
        const SizedBox(height: 18),
        Container(
          width: double.infinity,
          padding: const EdgeInsets.all(20),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(28),
            gradient: const LinearGradient(
              colors: [Color(0xFF0F172A), Color(0xFF1D4ED8)],
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
            ),
            boxShadow: const [
              BoxShadow(
                color: Color(0x1F0F172A),
                blurRadius: 28,
                offset: Offset(0, 16),
              ),
            ],
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: const [
              _HeaderPill(text: '真实公开路线'),
              SizedBox(height: 14),
              Text(
                '灵感流正在更新',
                style: TextStyle(
                  fontSize: 24,
                  fontWeight: FontWeight.w800,
                  color: Colors.white,
                ),
              ),
              SizedBox(height: 10),
              Text(
                '下拉即可刷新最新路线，上滑会自动加载更多社区作品；进入详情后还能一键收藏到你的私有行程库。',
                style: TextStyle(
                  fontSize: 14,
                  height: 1.7,
                  color: Color(0xFFE2E8F0),
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class _CommunityMetricsBar extends StatelessWidget {
  const _CommunityMetricsBar({required this.state});

  final CommunityFeedState state;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Expanded(
          child: _MetricTile(
            label: '已加载',
            value: '${state.items.length}',
            icon: Icons.grid_view_rounded,
          ),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: _MetricTile(
            label: '社区总量',
            value: '${state.total}',
            icon: Icons.travel_explore_rounded,
          ),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: _MetricTile(
            label: '分页状态',
            value: state.hasMore ? '更多' : '到底',
            icon: Icons.swap_vert_circle_rounded,
          ),
        ),
      ],
    );
  }
}

class _CommunityCardTile extends StatelessWidget {
  const _CommunityCardTile({required this.card, required this.onTap});

  final CommunityCard card;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return CommunityItineraryCard(
      title: card.title,
      coverImageUrl: card.coverImageUrl,
      topLeftLabel: card.cityName,
      badgeLabel: '${card.likeCount}',
      description: card.shareNote,
      tagLabels: card.highlights,
      primaryLabel: card.authorLabel,
      secondaryLabel: '${card.tripDate} · ${_duration(card.totalDuration)}',
      priceLabel: _currency(card.totalCost),
      footerLabel: '${card.commentCount} 评论 · ${card.routeSummary}',
      onTap: onTap,
    );
  }
}

class _CommunityLoadingView extends StatelessWidget {
  const _CommunityLoadingView();

  @override
  Widget build(BuildContext context) {
    return CustomScrollView(
      physics: const AlwaysScrollableScrollPhysics(
        parent: BouncingScrollPhysics(),
      ),
      slivers: [
        const SliverPadding(
          padding: EdgeInsets.fromLTRB(20, 18, 20, 0),
          sliver: SliverToBoxAdapter(child: _LoadingHeaderCard()),
        ),
        SliverPadding(
          padding: const EdgeInsets.fromLTRB(20, 20, 20, 24),
          sliver: SliverMasonryGrid.count(
            crossAxisCount: 2,
            mainAxisSpacing: 16,
            crossAxisSpacing: 16,
            childCount: 6,
            itemBuilder: (context, index) => Container(
              height: index.isEven ? 280 : 340,
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

class _CommunityErrorView extends StatelessWidget {
  const _CommunityErrorView({required this.message, required this.onRetry});

  final String message;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    return ListView(
      physics: const AlwaysScrollableScrollPhysics(
        parent: BouncingScrollPhysics(),
      ),
      padding: const EdgeInsets.fromLTRB(20, 32, 20, 24),
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
                  color: const Color(0xFFDBEAFE),
                  borderRadius: BorderRadius.circular(24),
                ),
                child: const Icon(
                  Icons.travel_explore_rounded,
                  color: Color(0xFF2563EB),
                  size: 34,
                ),
              ),
              const SizedBox(height: 18),
              const Text(
                '社区暂时加载失败',
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
                  '重新拉取数据',
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

class _CommunityEmptyView extends StatelessWidget {
  const _CommunityEmptyView({required this.onRefresh});

  final RefreshCallback onRefresh;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(20, 20, 20, 40),
      child: Container(
        padding: const EdgeInsets.all(24),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(28),
          border: Border.all(color: const Color(0xFFE2E8F0)),
        ),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Container(
              width: 72,
              height: 72,
              decoration: BoxDecoration(
                color: const Color(0xFFEFF6FF),
                borderRadius: BorderRadius.circular(24),
              ),
              child: const Icon(
                Icons.public_off_rounded,
                color: Color(0xFF2563EB),
                size: 34,
              ),
            ),
            const SizedBox(height: 18),
            const Text(
              '社区内容还在准备中',
              style: TextStyle(
                fontSize: 22,
                fontWeight: FontWeight.w800,
                color: Color(0xFF0F172A),
              ),
            ),
            const SizedBox(height: 10),
            const Text(
              '当前还没有公开路线，稍后下拉刷新即可获取最新社区数据。',
              textAlign: TextAlign.center,
              style: TextStyle(
                fontSize: 14,
                height: 1.7,
                color: Color(0xFF475569),
              ),
            ),
            const SizedBox(height: 20),
            FilledButton.icon(
              onPressed: onRefresh,
              style: FilledButton.styleFrom(
                minimumSize: const Size.fromHeight(52),
                backgroundColor: const Color(0xFF2563EB),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(18),
                ),
              ),
              icon: const Icon(Icons.refresh_rounded),
              label: const Text(
                '刷新社区',
                style: TextStyle(fontWeight: FontWeight.w700),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _MetricTile extends StatelessWidget {
  const _MetricTile({
    required this.label,
    required this.value,
    required this.icon,
  });

  final String label;
  final String value;
  final IconData icon;

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
          Icon(icon, size: 18, color: const Color(0xFF2563EB)),
          const SizedBox(height: 10),
          Text(
            value,
            style: const TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.w800,
              color: Color(0xFF0F172A),
            ),
          ),
          const SizedBox(height: 4),
          Text(
            label,
            style: const TextStyle(
              fontSize: 12,
              fontWeight: FontWeight.w600,
              color: Color(0xFF64748B),
            ),
          ),
        ],
      ),
    );
  }
}

class _HeaderPill extends StatelessWidget {
  const _HeaderPill({required this.text});

  final String text;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      decoration: BoxDecoration(
        color: const Color(0x1FFFFFFF),
        borderRadius: BorderRadius.circular(999),
      ),
      child: Text(
        text,
        style: const TextStyle(
          fontSize: 12,
          fontWeight: FontWeight.w700,
          color: Colors.white,
        ),
      ),
    );
  }
}

class _LoadingHeaderCard extends StatelessWidget {
  const _LoadingHeaderCard();

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 190,
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(28),
      ),
    );
  }
}

String _currency(double value) => value == value.roundToDouble()
    ? '¥${value.toStringAsFixed(0)}'
    : '¥${value.toStringAsFixed(1)}';

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

String _friendlyMessage(Object error) {
  final text = error.toString().replaceFirst('Exception: ', '').trim();
  if (text.isEmpty) {
    return '请检查网络后重试。';
  }
  return text;
}
