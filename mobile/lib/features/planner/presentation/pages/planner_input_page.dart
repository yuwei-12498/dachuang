import 'dart:ui';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:planner/core/constants/app_constants.dart';
import 'package:planner/features/planner/data/models/planner_request.dart';

import '../providers/planner_provider.dart';

class PlannerInputPage extends ConsumerStatefulWidget {
  const PlannerInputPage({super.key});

  @override
  ConsumerState<PlannerInputPage> createState() => _PlannerInputPageState();
}

class _PlannerInputPageState extends ConsumerState<PlannerInputPage> {
  static const _cityOptions = <String>['成都', '杭州', '厦门', '重庆'];
  static const _themeOptions = <String>[
    '人文',
    '自然',
    '美食',
    '拍照',
    '夜游',
    '治愈',
    '特种兵',
    '亲子',
  ];
  static const _companionOptions = <MapEntry<String, String>>[
    MapEntry('friends', '好友'),
    MapEntry('couple', '情侣'),
    MapEntry('family', '亲子'),
    MapEntry('solo', '独行'),
  ];
  static const _walkingOptions = <MapEntry<String, String>>[
    MapEntry('low', '轻松'),
    MapEntry('medium', '平衡'),
    MapEntry('high', '高能'),
  ];

  late String _selectedCity;
  late double _tripDays;
  late double _budget;
  late Set<String> _selectedThemes;
  late String _companionType;
  late String _walkingLevel;

  @override
  void initState() {
    super.initState();
    final initialRequest =
        ref.read(plannerCurrentRequestProvider) ?? PlannerRequest.initial();
    _applyRequest(initialRequest);
  }

  void _applyRequest(PlannerRequest request) {
    _selectedCity = request.cityName;
    _tripDays = request.tripDays.toDouble();
    _budget = request.totalBudget;
    _selectedThemes = request.themes.toSet();
    _companionType = request.companionType;
    _walkingLevel = request.walkingLevel;
  }

  @override
  Widget build(BuildContext context) {
    final flowState = ref.watch(plannerFlowControllerProvider);
    final thinkingMessage = ref.watch(plannerThinkingMessageProvider);
    final theme = Theme.of(context);

    ref.listen<PlannerFlowState>(plannerFlowControllerProvider, (
      previous,
      next,
    ) {
      if (!mounted) {
        return;
      }

      final messenger = ScaffoldMessenger.of(context);

      if (previous?.phase == PlannerFlowPhase.planning &&
          next.phase == PlannerFlowPhase.planned) {
        context.goNamed(AppConstants.plannerResultRouteName);
      }

      if (previous?.phase == PlannerFlowPhase.planning &&
          next.phase == PlannerFlowPhase.editing &&
          next.errorMessage != null &&
          next.errorMessage!.isNotEmpty) {
        messenger
          ..hideCurrentSnackBar()
          ..showSnackBar(
            SnackBar(
              behavior: SnackBarBehavior.floating,
              backgroundColor: const Color(0xFF0F172A),
              content: Text(next.errorMessage!),
            ),
          );
        ref.read(plannerFlowControllerProvider.notifier).clearError();
      }
    });

    return DecoratedBox(
      decoration: const BoxDecoration(
        gradient: LinearGradient(
          colors: [Color(0xFFF8FBFF), Color(0xFFF3F5FF), Color(0xFFF8FAFC)],
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
        ),
      ),
      child: Scaffold(
        backgroundColor: Colors.transparent,
        body: SafeArea(
          child: Stack(
            children: [
              CustomScrollView(
                physics: const BouncingScrollPhysics(),
                slivers: [
                  SliverPadding(
                    padding: const EdgeInsets.fromLTRB(20, 12, 20, 24),
                    sliver: SliverList(
                      delegate: SliverChildListDelegate([
                        Container(
                          padding: const EdgeInsets.all(24),
                          decoration: BoxDecoration(
                            gradient: const LinearGradient(
                              colors: [Color(0xFF0F172A), Color(0xFF2563EB)],
                              begin: Alignment.topLeft,
                              end: Alignment.bottomRight,
                            ),
                            borderRadius: BorderRadius.circular(32),
                            boxShadow: const [
                              BoxShadow(
                                color: Color(0x220F172A),
                                blurRadius: 30,
                                offset: Offset(0, 18),
                              ),
                            ],
                          ),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Container(
                                padding: const EdgeInsets.symmetric(
                                  horizontal: 12,
                                  vertical: 8,
                                ),
                                decoration: BoxDecoration(
                                  color: const Color(0x1FFFFFFF),
                                  borderRadius: BorderRadius.circular(999),
                                ),
                                child: const Text(
                                  'AI 智能规划器',
                                  style: TextStyle(
                                    color: Colors.white,
                                    fontSize: 12,
                                    fontWeight: FontWeight.w700,
                                  ),
                                ),
                              ),
                              const SizedBox(height: 18),
                              Text(
                                '先告诉我你的想法，\n再给你一条真正像 AI 生成的路线。',
                                style: theme.textTheme.headlineSmall?.copyWith(
                                  color: Colors.white,
                                  fontWeight: FontWeight.w800,
                                  height: 1.25,
                                ),
                              ),
                              const SizedBox(height: 12),
                              const Text(
                                '通过目的地、天数、预算和偏好标签，先完成你的旅行意图采样；随后再进入 AI 思考态与结果页，体验会更像一个真实的智能决策产品。',
                                style: TextStyle(
                                  color: Color(0xFFE2E8F0),
                                  fontSize: 14,
                                  height: 1.65,
                                ),
                              ),
                              const SizedBox(height: 20),
                              Wrap(
                                spacing: 10,
                                runSpacing: 10,
                                children: [
                                  _TopMetric(
                                    label: '已选城市',
                                    value: _selectedCity,
                                  ),
                                  _TopMetric(
                                    label: '预计天数',
                                    value: '${_tripDays.round()} 天',
                                  ),
                                  _TopMetric(
                                    label: '预算区间',
                                    value: _budgetLabel(
                                      _budget,
                                      _tripDays.round(),
                                    ),
                                  ),
                                ],
                              ),
                            ],
                          ),
                        ),
                        const SizedBox(height: 18),
                        _SectionCard(
                          title: '1. 选择目的地',
                          subtitle: '先锁定城市，AI 才能做区域热度、景点分布与交通动线推演。',
                          child: Wrap(
                            spacing: 10,
                            runSpacing: 10,
                            children: _cityOptions
                                .map(
                                  (city) => ChoiceChip(
                                    label: Text(city),
                                    selected: _selectedCity == city,
                                    onSelected: (_) {
                                      setState(() {
                                        _selectedCity = city;
                                      });
                                    },
                                    labelStyle: TextStyle(
                                      color: _selectedCity == city
                                          ? Colors.white
                                          : const Color(0xFF334155),
                                      fontWeight: FontWeight.w700,
                                    ),
                                    selectedColor: const Color(0xFF2563EB),
                                    backgroundColor: const Color(0xFFF8FAFC),
                                    side: BorderSide(
                                      color: _selectedCity == city
                                          ? Colors.transparent
                                          : const Color(0xFFE2E8F0),
                                    ),
                                    shape: RoundedRectangleBorder(
                                      borderRadius: BorderRadius.circular(18),
                                    ),
                                    padding: const EdgeInsets.symmetric(
                                      horizontal: 14,
                                      vertical: 12,
                                    ),
                                  ),
                                )
                                .toList(growable: false),
                          ),
                        ),
                        const SizedBox(height: 16),
                        _SectionCard(
                          title: '2. 设定游玩天数',
                          subtitle: '城市微度假更适合 1-4 天，AI 会据此控制动线密度与路线长度。',
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Row(
                                children: [
                                  Text(
                                    '${_tripDays.round()} 天',
                                    style: const TextStyle(
                                      fontSize: 26,
                                      fontWeight: FontWeight.w800,
                                      color: Color(0xFF0F172A),
                                    ),
                                  ),
                                  const Spacer(),
                                  Container(
                                    padding: const EdgeInsets.symmetric(
                                      horizontal: 12,
                                      vertical: 8,
                                    ),
                                    decoration: BoxDecoration(
                                      color: const Color(0xFFEEF4FF),
                                      borderRadius: BorderRadius.circular(999),
                                    ),
                                    child: Text(
                                      _tripDays.round() <= 2 ? '短时高效' : '节奏舒展',
                                      style: const TextStyle(
                                        color: Color(0xFF1D4ED8),
                                        fontWeight: FontWeight.w700,
                                      ),
                                    ),
                                  ),
                                ],
                              ),
                              const SizedBox(height: 8),
                              SliderTheme(
                                data: SliderTheme.of(context).copyWith(
                                  activeTrackColor: const Color(0xFF2563EB),
                                  inactiveTrackColor: const Color(0xFFE2E8F0),
                                  thumbColor: const Color(0xFF2563EB),
                                  overlayColor: const Color(0x332563EB),
                                  trackHeight: 6,
                                ),
                                child: Slider(
                                  value: _tripDays,
                                  min: 1,
                                  max: 4,
                                  divisions: 3,
                                  label: '${_tripDays.round()} 天',
                                  onChanged: (value) {
                                    setState(() {
                                      _tripDays = value;
                                    });
                                  },
                                ),
                              ),
                              const SizedBox(height: 4),
                              const Row(
                                children: [
                                  Text(
                                    '1 天闪游',
                                    style: TextStyle(
                                      color: Color(0xFF64748B),
                                      fontSize: 12,
                                    ),
                                  ),
                                  Spacer(),
                                  Text(
                                    '4 天深玩',
                                    style: TextStyle(
                                      color: Color(0xFF64748B),
                                      fontSize: 12,
                                    ),
                                  ),
                                ],
                              ),
                            ],
                          ),
                        ),
                        const SizedBox(height: 16),
                        _SectionCard(
                          title: '3. 设定总预算',
                          subtitle: '预算会直接影响餐饮、商圈和打车比例，Mock 结果页会实时体现这种差异。',
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                '¥${_budget.round()}',
                                style: const TextStyle(
                                  fontSize: 28,
                                  fontWeight: FontWeight.w800,
                                  color: Color(0xFF0F172A),
                                ),
                              ),
                              const SizedBox(height: 6),
                              Text(
                                _budgetHelper(_budget, _tripDays.round()),
                                style: const TextStyle(
                                  fontSize: 13,
                                  height: 1.6,
                                  color: Color(0xFF64748B),
                                ),
                              ),
                              const SizedBox(height: 10),
                              SliderTheme(
                                data: SliderTheme.of(context).copyWith(
                                  activeTrackColor: const Color(0xFF7C3AED),
                                  inactiveTrackColor: const Color(0xFFE2E8F0),
                                  thumbColor: const Color(0xFF7C3AED),
                                  overlayColor: const Color(0x337C3AED),
                                  trackHeight: 6,
                                ),
                                child: Slider(
                                  value: _budget,
                                  min: 800,
                                  max: 5000,
                                  divisions: 21,
                                  label: '¥${_budget.round()}',
                                  onChanged: (value) {
                                    setState(() {
                                      _budget = value;
                                    });
                                  },
                                ),
                              ),
                              const SizedBox(height: 6),
                              Row(
                                children: [
                                  _BudgetPill(
                                    text: '轻量',
                                    selected:
                                        _budgetLevel(
                                          _budget,
                                          _tripDays.round(),
                                        ) ==
                                        '轻量预算',
                                  ),
                                  const SizedBox(width: 8),
                                  _BudgetPill(
                                    text: '舒适',
                                    selected:
                                        _budgetLevel(
                                          _budget,
                                          _tripDays.round(),
                                        ) ==
                                        '舒适预算',
                                  ),
                                  const SizedBox(width: 8),
                                  _BudgetPill(
                                    text: '品质',
                                    selected:
                                        _budgetLevel(
                                          _budget,
                                          _tripDays.round(),
                                        ) ==
                                        '品质拉满',
                                  ),
                                ],
                              ),
                            ],
                          ),
                        ),
                        const SizedBox(height: 16),
                        _SectionCard(
                          title: '4. 选择出行偏好',
                          subtitle: '多选标签会影响路线气质、点位排序和 AI 推荐理由。',
                          child: Wrap(
                            spacing: 10,
                            runSpacing: 10,
                            children: _themeOptions
                                .map(
                                  (themeItem) => FilterChip(
                                    label: Text(themeItem),
                                    selected: _selectedThemes.contains(
                                      themeItem,
                                    ),
                                    onSelected: (selected) {
                                      setState(() {
                                        if (selected) {
                                          _selectedThemes.add(themeItem);
                                        } else if (_selectedThemes.length > 1) {
                                          _selectedThemes.remove(themeItem);
                                        } else {
                                          ScaffoldMessenger.of(
                                            context,
                                          ).showSnackBar(
                                            const SnackBar(
                                              behavior:
                                                  SnackBarBehavior.floating,
                                              content: Text('至少保留一个出行偏好。'),
                                            ),
                                          );
                                        }
                                      });
                                    },
                                    showCheckmark: false,
                                    selectedColor: const Color(0xFFE0E7FF),
                                    backgroundColor: const Color(0xFFF8FAFC),
                                    side: BorderSide(
                                      color: _selectedThemes.contains(themeItem)
                                          ? const Color(0xFF6366F1)
                                          : const Color(0xFFE2E8F0),
                                    ),
                                    labelStyle: TextStyle(
                                      color: _selectedThemes.contains(themeItem)
                                          ? const Color(0xFF4338CA)
                                          : const Color(0xFF334155),
                                      fontWeight: FontWeight.w700,
                                    ),
                                    padding: const EdgeInsets.symmetric(
                                      horizontal: 14,
                                      vertical: 12,
                                    ),
                                    shape: RoundedRectangleBorder(
                                      borderRadius: BorderRadius.circular(18),
                                    ),
                                  ),
                                )
                                .toList(growable: false),
                          ),
                        ),
                        const SizedBox(height: 16),
                        _SectionCard(
                          title: '5. 调整出行氛围',
                          subtitle: '这些参数会影响结果页的步行距离、叙事语气和推荐点位倾向。',
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              const Text(
                                '同行类型',
                                style: TextStyle(
                                  fontWeight: FontWeight.w700,
                                  color: Color(0xFF0F172A),
                                ),
                              ),
                              const SizedBox(height: 10),
                              Wrap(
                                spacing: 10,
                                runSpacing: 10,
                                children: _companionOptions
                                    .map(
                                      (item) => ChoiceChip(
                                        label: Text(item.value),
                                        selected: _companionType == item.key,
                                        onSelected: (_) {
                                          setState(() {
                                            _companionType = item.key;
                                          });
                                        },
                                        selectedColor: const Color(0xFFDBEAFE),
                                        backgroundColor: const Color(
                                          0xFFF8FAFC,
                                        ),
                                        side: BorderSide(
                                          color: _companionType == item.key
                                              ? const Color(0xFF60A5FA)
                                              : const Color(0xFFE2E8F0),
                                        ),
                                        labelStyle: TextStyle(
                                          color: _companionType == item.key
                                              ? const Color(0xFF1D4ED8)
                                              : const Color(0xFF334155),
                                          fontWeight: FontWeight.w700,
                                        ),
                                        padding: const EdgeInsets.symmetric(
                                          horizontal: 14,
                                          vertical: 12,
                                        ),
                                        shape: RoundedRectangleBorder(
                                          borderRadius: BorderRadius.circular(
                                            18,
                                          ),
                                        ),
                                      ),
                                    )
                                    .toList(growable: false),
                              ),
                              const SizedBox(height: 18),
                              const Text(
                                '步行强度',
                                style: TextStyle(
                                  fontWeight: FontWeight.w700,
                                  color: Color(0xFF0F172A),
                                ),
                              ),
                              const SizedBox(height: 10),
                              Wrap(
                                spacing: 10,
                                runSpacing: 10,
                                children: _walkingOptions
                                    .map(
                                      (item) => ChoiceChip(
                                        label: Text(item.value),
                                        selected: _walkingLevel == item.key,
                                        onSelected: (_) {
                                          setState(() {
                                            _walkingLevel = item.key;
                                          });
                                        },
                                        selectedColor: const Color(0xFFEDE9FE),
                                        backgroundColor: const Color(
                                          0xFFF8FAFC,
                                        ),
                                        side: BorderSide(
                                          color: _walkingLevel == item.key
                                              ? const Color(0xFFA78BFA)
                                              : const Color(0xFFE2E8F0),
                                        ),
                                        labelStyle: TextStyle(
                                          color: _walkingLevel == item.key
                                              ? const Color(0xFF6D28D9)
                                              : const Color(0xFF334155),
                                          fontWeight: FontWeight.w700,
                                        ),
                                        padding: const EdgeInsets.symmetric(
                                          horizontal: 14,
                                          vertical: 12,
                                        ),
                                        shape: RoundedRectangleBorder(
                                          borderRadius: BorderRadius.circular(
                                            18,
                                          ),
                                        ),
                                      ),
                                    )
                                    .toList(growable: false),
                              ),
                            ],
                          ),
                        ),
                        const SizedBox(height: 16),
                        Container(
                          padding: const EdgeInsets.all(20),
                          decoration: BoxDecoration(
                            color: Colors.white,
                            borderRadius: BorderRadius.circular(28),
                            border: Border.all(color: const Color(0xFFE2E8F0)),
                            boxShadow: const [
                              BoxShadow(
                                color: Color(0x0A0F172A),
                                blurRadius: 18,
                                offset: Offset(0, 10),
                              ),
                            ],
                          ),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              const Text(
                                'AI 画像预览',
                                style: TextStyle(
                                  fontSize: 18,
                                  fontWeight: FontWeight.w800,
                                  color: Color(0xFF0F172A),
                                ),
                              ),
                              const SizedBox(height: 10),
                              Text(
                                '你正在规划一趟 $_selectedCity ${_tripDays.round()} 天的微度假，总预算约 ¥${_budget.round()}，偏向 ${_selectedThemes.join('、')}。AI 会先生成思考过程，再展示最终路线结果。',
                                style: const TextStyle(
                                  fontSize: 14,
                                  height: 1.7,
                                  color: Color(0xFF475569),
                                ),
                              ),
                              const SizedBox(height: 16),
                              Wrap(
                                spacing: 8,
                                runSpacing: 8,
                                children: _selectedThemes
                                    .map(
                                      (item) => Container(
                                        padding: const EdgeInsets.symmetric(
                                          horizontal: 12,
                                          vertical: 8,
                                        ),
                                        decoration: BoxDecoration(
                                          color: const Color(0xFFF1F5F9),
                                          borderRadius: BorderRadius.circular(
                                            999,
                                          ),
                                        ),
                                        child: Text(
                                          item,
                                          style: const TextStyle(
                                            color: Color(0xFF0F172A),
                                            fontWeight: FontWeight.w700,
                                            fontSize: 12,
                                          ),
                                        ),
                                      ),
                                    )
                                    .toList(growable: false),
                              ),
                              const SizedBox(height: 20),
                              SizedBox(
                                width: double.infinity,
                                child: FilledButton(
                                  onPressed: flowState.isPlanning
                                      ? null
                                      : () {
                                          ref
                                              .read(
                                                plannerFlowControllerProvider
                                                    .notifier,
                                              )
                                              .startPlanning(_buildRequest());
                                        },
                                  style: FilledButton.styleFrom(
                                    backgroundColor: const Color(0xFF2563EB),
                                    disabledBackgroundColor: const Color(
                                      0xFF94A3B8,
                                    ),
                                    padding: const EdgeInsets.symmetric(
                                      vertical: 18,
                                    ),
                                    shape: RoundedRectangleBorder(
                                      borderRadius: BorderRadius.circular(22),
                                    ),
                                  ),
                                  child: Row(
                                    mainAxisAlignment: MainAxisAlignment.center,
                                    children: [
                                      const Icon(Icons.auto_awesome_rounded),
                                      const SizedBox(width: 10),
                                      Text(
                                        flowState.isPlanning
                                            ? 'AI 正在思考中...'
                                            : '开始智能规划',
                                        style: const TextStyle(
                                          fontSize: 16,
                                          fontWeight: FontWeight.w800,
                                        ),
                                      ),
                                    ],
                                  ),
                                ),
                              ),
                            ],
                          ),
                        ),
                        const SizedBox(height: 24),
                      ]),
                    ),
                  ),
                ],
              ),
              if (flowState.isPlanning)
                Positioned.fill(
                  child: _PlanningOverlay(
                    message: thinkingMessage,
                    cityName: _selectedCity,
                    tripDays: _tripDays.round(),
                  ),
                ),
            ],
          ),
        ),
      ),
    );
  }

  PlannerRequest _buildRequest() {
    return PlannerRequest(
      cityName: _selectedCity,
      tripDays: _tripDays.round(),
      totalBudget: _budget.roundToDouble(),
      themes: _selectedThemes.toList(growable: false),
      tripDate: PlannerRequest.initial().tripDate,
      startTime: '09:30',
      endTime: '21:30',
      isRainy: false,
      isNight: _selectedThemes.contains('夜游'),
      walkingLevel: _walkingLevel,
      companionType: _companionType,
    );
  }
}

class _PlanningOverlay extends StatelessWidget {
  const _PlanningOverlay({
    required this.message,
    required this.cityName,
    required this.tripDays,
  });

  final String message;
  final String cityName;
  final int tripDays;

  @override
  Widget build(BuildContext context) {
    return ColoredBox(
      color: const Color(0xA60F172A),
      child: BackdropFilter(
        filter: ImageFilter.blur(sigmaX: 10, sigmaY: 10),
        child: Center(
          child: Container(
            width: double.infinity,
            margin: const EdgeInsets.symmetric(horizontal: 24),
            padding: const EdgeInsets.all(24),
            decoration: BoxDecoration(
              color: const Color(0xEFFFFFFF),
              borderRadius: BorderRadius.circular(28),
              border: Border.all(color: const Color(0x33FFFFFF)),
              boxShadow: const [
                BoxShadow(
                  color: Color(0x22000000),
                  blurRadius: 30,
                  offset: Offset(0, 18),
                ),
              ],
            ),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Container(
                  width: 76,
                  height: 76,
                  decoration: BoxDecoration(
                    gradient: const LinearGradient(
                      colors: [Color(0xFF2563EB), Color(0xFF7C3AED)],
                    ),
                    borderRadius: BorderRadius.circular(24),
                  ),
                  child: const Icon(
                    Icons.auto_awesome_rounded,
                    size: 34,
                    color: Colors.white,
                  ),
                ),
                const SizedBox(height: 18),
                Text(
                  'AI 正在为你规划 $cityName $tripDays 天路线',
                  textAlign: TextAlign.center,
                  style: const TextStyle(
                    fontSize: 20,
                    fontWeight: FontWeight.w800,
                    color: Color(0xFF0F172A),
                  ),
                ),
                const SizedBox(height: 10),
                const Text(
                  '请稍等 2-3 秒，我们会模拟真实的 AI 规划思考过程，而不是生硬地立刻展示结果。',
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    fontSize: 13,
                    height: 1.7,
                    color: Color(0xFF64748B),
                  ),
                ),
                const SizedBox(height: 22),
                AnimatedSwitcher(
                  duration: const Duration(milliseconds: 420),
                  switchInCurve: Curves.easeOutCubic,
                  switchOutCurve: Curves.easeInCubic,
                  transitionBuilder: (child, animation) {
                    return FadeTransition(
                      opacity: animation,
                      child: SlideTransition(
                        position: Tween<Offset>(
                          begin: const Offset(0, 0.18),
                          end: Offset.zero,
                        ).animate(animation),
                        child: child,
                      ),
                    );
                  },
                  child: Text(
                    message,
                    key: ValueKey(message),
                    textAlign: TextAlign.center,
                    style: const TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.w800,
                      color: Color(0xFF2563EB),
                    ),
                  ),
                ),
                const SizedBox(height: 18),
                ClipRRect(
                  borderRadius: BorderRadius.circular(999),
                  child: const LinearProgressIndicator(
                    minHeight: 8,
                    backgroundColor: Color(0xFFE2E8F0),
                    valueColor: AlwaysStoppedAnimation<Color>(
                      Color(0xFF2563EB),
                    ),
                  ),
                ),
                const SizedBox(height: 16),
                const Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Icon(
                      Icons.insights_rounded,
                      size: 18,
                      color: Color(0xFF7C3AED),
                    ),
                    SizedBox(width: 8),
                    Text(
                      '正在预加载 Mock 结果页数据',
                      style: TextStyle(
                        fontSize: 12,
                        fontWeight: FontWeight.w700,
                        color: Color(0xFF64748B),
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _SectionCard extends StatelessWidget {
  const _SectionCard({
    required this.title,
    required this.subtitle,
    required this.child,
  });

  final String title;
  final String subtitle;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(28),
        border: Border.all(color: const Color(0xFFE2E8F0)),
        boxShadow: const [
          BoxShadow(
            color: Color(0x0A0F172A),
            blurRadius: 18,
            offset: Offset(0, 10),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            title,
            style: const TextStyle(
              fontSize: 18,
              fontWeight: FontWeight.w800,
              color: Color(0xFF0F172A),
            ),
          ),
          const SizedBox(height: 8),
          Text(
            subtitle,
            style: const TextStyle(
              fontSize: 13,
              height: 1.65,
              color: Color(0xFF64748B),
            ),
          ),
          const SizedBox(height: 18),
          child,
        ],
      ),
    );
  }
}

class _TopMetric extends StatelessWidget {
  const _TopMetric({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
      decoration: BoxDecoration(
        color: const Color(0x14FFFFFF),
        borderRadius: BorderRadius.circular(18),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            label,
            style: const TextStyle(
              fontSize: 11,
              fontWeight: FontWeight.w600,
              color: Color(0xFFCBD5E1),
            ),
          ),
          const SizedBox(height: 6),
          Text(
            value,
            style: const TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.w800,
              color: Colors.white,
            ),
          ),
        ],
      ),
    );
  }
}

class _BudgetPill extends StatelessWidget {
  const _BudgetPill({required this.text, required this.selected});

  final String text;
  final bool selected;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      decoration: BoxDecoration(
        color: selected ? const Color(0xFF1E293B) : const Color(0xFFF8FAFC),
        borderRadius: BorderRadius.circular(999),
        border: Border.all(
          color: selected ? Colors.transparent : const Color(0xFFE2E8F0),
        ),
      ),
      child: Text(
        text,
        style: TextStyle(
          color: selected ? Colors.white : const Color(0xFF334155),
          fontWeight: FontWeight.w700,
          fontSize: 12,
        ),
      ),
    );
  }
}

String _budgetHelper(double budget, int tripDays) {
  final perDay = budget / tripDays;
  if (perDay < 700) {
    return '当前偏向轻量预算，AI 会优先保留免费景点、步行串联与高性价比餐饮。';
  }
  if (perDay > 1800) {
    return '当前偏向品质预算，AI 会更大胆地加入精品商圈、精致餐食和舒适交通。';
  }
  return '当前是舒适预算，路线会在体验感、节奏和成本之间做更均衡的取舍。';
}

String _budgetLevel(double budget, int tripDays) {
  final perDay = budget / tripDays;
  if (perDay < 700) {
    return '轻量预算';
  }
  if (perDay > 1800) {
    return '品质拉满';
  }
  return '舒适预算';
}

String _budgetLabel(double budget, int tripDays) {
  final level = _budgetLevel(budget, tripDays);
  return switch (level) {
    '轻量预算' => '轻量探索',
    '品质拉满' => '品质拉满',
    _ => '舒适均衡',
  };
}
