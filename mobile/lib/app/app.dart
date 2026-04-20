import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:planner/core/constants/app_constants.dart';
import 'package:planner/core/routing/app_router.dart';
import 'package:planner/shared/themes/app_theme.dart';

class CityTripApp extends ConsumerWidget {
  const CityTripApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final router = ref.watch(appRouterProvider);

    return MaterialApp.router(
      title: AppConstants.appName,
      debugShowCheckedModeBanner: false,
      theme: AppTheme.light,
      routerConfig: router,
    );
  }
}
