import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:planner/core/network/api_exception.dart';
import 'package:planner/core/network/dio_client.dart';

import '../models/community_detail.dart';
import '../models/community_page_payload.dart';
import '../models/community_saved_result.dart';

final communityRepositoryProvider = Provider<CommunityRepository>((ref) {
  return CommunityRepository(ref.watch(apiClientProvider));
});

class CommunityRepository {
  CommunityRepository(this._apiClient);

  static const String _communityPagePath = '/api/community/itineraries';
  static const String _savePath = '/api/itineraries/save';

  final ApiClient _apiClient;

  Future<CommunityPagePayload> fetchCommunityPage({
    required int page,
    required int size,
  }) async {
    try {
      return await _apiClient.getObject<CommunityPagePayload>(
        _communityPagePath,
        queryParameters: {
          'page': page,
          'size': size,
        },
        decoder: CommunityPagePayload.fromJson,
      );
    } on ApiException {
      rethrow;
    } on FormatException catch (error) {
      throw ApiException.unknown(message: '社区分页数据解析失败：${error.message}');
    } on TypeError catch (_) {
      throw ApiException.unknown(message: '社区分页字段结构异常，请检查后端返回格式。');
    } catch (error) {
      throw ApiException.unknown(message: '社区路线加载失败：$error');
    }
  }

  Future<CommunityDetail> fetchCommunityDetail(int itineraryId) async {
    try {
      return await _apiClient.getObject<CommunityDetail>(
        '$_communityPagePath/$itineraryId',
        decoder: CommunityDetail.fromJson,
      );
    } on ApiException {
      rethrow;
    } on FormatException catch (error) {
      throw ApiException.unknown(message: '社区详情解析失败：${error.message}');
    } on TypeError catch (_) {
      throw ApiException.unknown(message: '社区详情字段结构异常，请检查后端返回格式。');
    } catch (error) {
      throw ApiException.unknown(message: '社区详情加载失败：$error');
    }
  }

  Future<CommunitySavedResult> saveToMyTrips({
    required int sourceItineraryId,
    String? selectedOptionKey,
    String? title,
  }) async {
    try {
      return await _apiClient.postObject<CommunitySavedResult>(
        _savePath,
        data: {
          'sourceItineraryId': sourceItineraryId,
          'selectedOptionKey': selectedOptionKey,
          'title': title,
        },
        decoder: CommunitySavedResult.fromJson,
      );
    } on ApiException {
      rethrow;
    } on FormatException catch (error) {
      throw ApiException.unknown(message: '收藏结果解析失败：${error.message}');
    } on TypeError catch (_) {
      throw ApiException.unknown(message: '收藏接口返回字段结构异常，请检查 JSON 契约。');
    } catch (error) {
      throw ApiException.unknown(message: '保存到我的行程失败：$error');
    }
  }
}
