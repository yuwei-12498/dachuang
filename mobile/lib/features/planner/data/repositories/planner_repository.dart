import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:planner/core/network/api_exception.dart';
import 'package:planner/core/network/dio_client.dart';

import '../models/itinerary_plan.dart';
import '../models/planner_request.dart';

final plannerRepositoryProvider = Provider<PlannerRepository>((ref) {
  return RemotePlannerRepository(ref.watch(apiClientProvider));
});

abstract class PlannerRepository {
  Future<PlannerItinerary> generatePlan(PlannerRequest request);
}

class RemotePlannerRepository implements PlannerRepository {
  RemotePlannerRepository(this._apiClient);

  static const String _generatePath = '/api/itineraries/generate';

  final ApiClient _apiClient;

  @override
  Future<PlannerItinerary> generatePlan(PlannerRequest request) async {
    try {
      final payload = await _requestPlan(path: _generatePath, request: request);
      return PlannerItinerary.fromBackendJson(
        payload,
        fallbackRequest: request,
      );
    } on ApiException {
      rethrow;
    } on FormatException catch (error) {
      throw ApiException.unknown(message: '后端返回的数据格式无法解析：${error.message}');
    } on TypeError catch (_) {
      throw ApiException.unknown(message: '后端响应字段结构与前端模型不一致，请检查 JSON 契约。');
    } catch (error) {
      throw ApiException.unknown(message: '真实路线生成失败：$error');
    }
  }

  Future<Map<String, dynamic>> _requestPlan({
    required String path,
    required PlannerRequest request,
  }) {
    return _apiClient.postObject<Map<String, dynamic>>(
      path,
      data: request.toJson(),
      decoder: (json) => json,
    );
  }
}
