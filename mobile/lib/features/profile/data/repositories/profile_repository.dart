import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:planner/core/network/api_exception.dart';
import 'package:planner/core/network/dio_client.dart';
import 'package:planner/features/auth/data/models/user_session.dart';

import '../models/profile_trip_item.dart';
import '../models/profile_trip_type.dart';

final profileRepositoryProvider = Provider<ProfileRepository>((ref) {
  return ProfileRepository(ref.watch(apiClientProvider));
});

class ProfileRepository {
  ProfileRepository(this._apiClient);

  static const String _mePath = '/api/auth/me';
  static const String _userTripsPath = '/api/user/itineraries';

  final ApiClient _apiClient;

  Future<UserSession> fetchCurrentUser() async {
    try {
      return await _apiClient.getObject<UserSession>(
        _mePath,
        decoder: UserSession.fromJson,
      );
    } on ApiException {
      rethrow;
    } on FormatException catch (error) {
      throw ApiException.unknown(message: '当前用户信息解析失败：${error.message}');
    } on TypeError {
      throw ApiException.unknown(message: '当前用户信息字段类型不匹配，请检查后端返回结构。');
    } catch (error) {
      throw ApiException.unknown(message: '获取当前用户信息失败：$error');
    }
  }

  Future<List<ProfileTripItem>> fetchTrips(ProfileTripType type) async {
    try {
      return await _apiClient.getDecoded<List<ProfileTripItem>>(
        _userTripsPath,
        queryParameters: {'type': type.apiValue},
        decoder: (json) {
          if (json is! List) {
            throw const FormatException('接口没有返回行程数组。');
          }

          return json
              .map((item) {
                if (item is! Map) {
                  throw const FormatException('列表项不是合法对象。');
                }
                return ProfileTripItem.fromJson(
                  Map<String, dynamic>.from(item),
                );
              })
              .toList(growable: false);
        },
      );
    } on ApiException {
      rethrow;
    } on FormatException catch (error) {
      throw ApiException.unknown(
        message: '${type.tabLabel}数据解析失败：${error.message}',
      );
    } on TypeError {
      throw ApiException.unknown(message: '${type.tabLabel}字段类型不匹配，请检查后端返回结构。');
    } catch (error) {
      throw ApiException.unknown(message: '获取${type.tabLabel}失败：$error');
    }
  }
}
