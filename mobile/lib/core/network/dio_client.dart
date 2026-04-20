import 'dart:io';

import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:planner/core/auth/auth_session.dart';
import 'package:planner/core/config/env/app_env.dart';
import 'package:planner/core/network/api_exception.dart';
import 'package:planner/core/network/api_response.dart';
import 'package:planner/core/network/interceptors/auth_interceptor.dart';
import 'package:planner/core/network/interceptors/error_interceptor.dart';
import 'package:planner/core/storage/token_storage.dart';

final dioProvider = Provider<Dio>((ref) {
  final tokenStorage = ref.watch(tokenStorageProvider);

  final dio = Dio(
    BaseOptions(
      baseUrl: AppEnv.baseUrl,
      connectTimeout: AppEnv.connectTimeout,
      sendTimeout: AppEnv.sendTimeout,
      receiveTimeout: AppEnv.receiveTimeout,
      responseType: ResponseType.json,
      headers: const {
        HttpHeaders.acceptHeader: 'application/json',
        HttpHeaders.contentTypeHeader: 'application/json; charset=UTF-8',
      },
    ),
  );

  dio.interceptors.addAll([
    AuthInterceptor(tokenStorage),
    ErrorInterceptor(
      onUnauthorized: () => ref.read(authSessionProvider.notifier).clearSession(),
    ),
  ]);

  return dio;
});

final apiClientProvider = Provider<ApiClient>((ref) {
  return ApiClient(ref.watch(dioProvider));
});

class ApiClient {
  ApiClient(this._dio);

  final Dio _dio;

  Future<T> getObject<T>(
    String path, {
    Map<String, dynamic>? queryParameters,
    required T Function(Map<String, dynamic> json) decoder,
    bool requiresAuth = true,
    CancelToken? cancelToken,
  }) {
    return _guard(() async {
      final response = await _dio.get<Map<String, dynamic>>(
        path,
        queryParameters: queryParameters,
        cancelToken: cancelToken,
        options: Options(extra: {'requiresAuth': requiresAuth}),
      );

      final payload = response.data ?? <String, dynamic>{};
      return decoder(payload);
    });
  }

  Future<T> getDecoded<T>(
    String path, {
    Map<String, dynamic>? queryParameters,
    required T Function(Object? json) decoder,
    bool requiresAuth = true,
    CancelToken? cancelToken,
  }) {
    return _guard(() async {
      final response = await _dio.get<Object?>(
        path,
        queryParameters: queryParameters,
        cancelToken: cancelToken,
        options: Options(extra: {'requiresAuth': requiresAuth}),
      );

      return decoder(response.data);
    });
  }

  Future<T> postObject<T>(
    String path, {
    Object? data,
    Map<String, dynamic>? queryParameters,
    required T Function(Map<String, dynamic> json) decoder,
    bool requiresAuth = true,
    CancelToken? cancelToken,
  }) {
    return _guard(() async {
      final response = await _dio.post<Map<String, dynamic>>(
        path,
        data: data,
        queryParameters: queryParameters,
        cancelToken: cancelToken,
        options: Options(extra: {'requiresAuth': requiresAuth}),
      );

      final payload = response.data ?? <String, dynamic>{};
      return decoder(payload);
    });
  }

  Future<void> deleteVoid(
    String path, {
    Object? data,
    Map<String, dynamic>? queryParameters,
    bool requiresAuth = true,
    CancelToken? cancelToken,
  }) {
    return _guard(() async {
      await _dio.delete<void>(
        path,
        data: data,
        queryParameters: queryParameters,
        cancelToken: cancelToken,
        options: Options(extra: {'requiresAuth': requiresAuth}),
      );
    });
  }

  Future<ApiResponse<T>> get<T>(
    String path, {
    Map<String, dynamic>? queryParameters,
    JsonDecoder<T>? decoder,
    bool requiresAuth = true,
    CancelToken? cancelToken,
  }) {
    return _guard(() async {
      final response = await _dio.get<Map<String, dynamic>>(
        path,
        queryParameters: queryParameters,
        cancelToken: cancelToken,
        options: Options(extra: {'requiresAuth': requiresAuth}),
      );

      return _transformResponse<T>(response.data, decoder);
    });
  }

  Future<ApiResponse<T>> post<T>(
    String path, {
    Object? data,
    Map<String, dynamic>? queryParameters,
    JsonDecoder<T>? decoder,
    bool requiresAuth = true,
    CancelToken? cancelToken,
  }) {
    return _guard(() async {
      final response = await _dio.post<Map<String, dynamic>>(
        path,
        data: data,
        queryParameters: queryParameters,
        cancelToken: cancelToken,
        options: Options(extra: {'requiresAuth': requiresAuth}),
      );

      return _transformResponse<T>(response.data, decoder);
    });
  }

  Future<ApiResponse<T>> put<T>(
    String path, {
    Object? data,
    Map<String, dynamic>? queryParameters,
    JsonDecoder<T>? decoder,
    bool requiresAuth = true,
    CancelToken? cancelToken,
  }) {
    return _guard(() async {
      final response = await _dio.put<Map<String, dynamic>>(
        path,
        data: data,
        queryParameters: queryParameters,
        cancelToken: cancelToken,
        options: Options(extra: {'requiresAuth': requiresAuth}),
      );

      return _transformResponse<T>(response.data, decoder);
    });
  }

  Future<ApiResponse<T>> delete<T>(
    String path, {
    Object? data,
    Map<String, dynamic>? queryParameters,
    JsonDecoder<T>? decoder,
    bool requiresAuth = true,
    CancelToken? cancelToken,
  }) {
    return _guard(() async {
      final response = await _dio.delete<Map<String, dynamic>>(
        path,
        data: data,
        queryParameters: queryParameters,
        cancelToken: cancelToken,
        options: Options(extra: {'requiresAuth': requiresAuth}),
      );

      return _transformResponse<T>(response.data, decoder);
    });
  }

  ApiResponse<T> _transformResponse<T>(
    Map<String, dynamic>? payload,
    JsonDecoder<T>? decoder,
  ) {
    final json = payload ?? <String, dynamic>{};
    final result = ApiResponse<T>.fromJson(json, decoder: decoder);
    if (!result.isSuccess) {
      throw ApiException.business(
        message: result.msg,
        statusCode: result.code,
      );
    }

    return result;
  }

  Future<T> _guard<T>(Future<T> Function() action) async {
    try {
      return await action();
    } on DioException catch (error) {
      if (error.error is ApiException) {
        throw error.error! as ApiException;
      }

      throw ApiException.unknown(
        message: error.message ?? '请求失败，请稍后重试。',
        statusCode: error.response?.statusCode,
      );
    }
  }
}
