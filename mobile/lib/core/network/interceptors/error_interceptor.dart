import 'dart:async';

import 'package:dio/dio.dart';
import 'package:planner/core/network/api_exception.dart';

class ErrorInterceptor extends Interceptor {
  ErrorInterceptor({
    required FutureOr<void> Function() onUnauthorized,
  }) : _onUnauthorized = onUnauthorized;

  final FutureOr<void> Function() _onUnauthorized;

  @override
  Future<void> onError(
    DioException err,
    ErrorInterceptorHandler handler,
  ) async {
    final exception = _mapException(err);

    if (exception.isUnauthorized) {
      await _onUnauthorized();
    }

    handler.reject(
      DioException(
        requestOptions: err.requestOptions,
        response: err.response,
        type: err.type,
        error: exception,
        message: exception.message,
        stackTrace: err.stackTrace,
      ),
    );
  }

  ApiException _mapException(DioException error) {
    final statusCode = error.response?.statusCode;
    final payload = error.response?.data;
    final backendMessage = _extractField(payload, 'msg') ??
        _extractField(payload, 'message') ??
        error.message;
    final path = _extractField(payload, 'path');
    final traceId =
        _extractField(payload, 'traceId') ?? error.response?.headers.value('x-trace-id');

    switch (error.type) {
      case DioExceptionType.connectionTimeout:
      case DioExceptionType.sendTimeout:
      case DioExceptionType.receiveTimeout:
        return ApiException.network(
          message: '请求超时，请稍后重试。',
          statusCode: statusCode,
          path: path,
          traceId: traceId,
        );
      case DioExceptionType.connectionError:
        return ApiException.network(
          message: '网络连接失败，请检查网络后重试。',
          statusCode: statusCode,
          path: path,
          traceId: traceId,
        );
      case DioExceptionType.badCertificate:
        return ApiException.unknown(
          message: '服务证书校验失败，请联系管理员。',
          statusCode: statusCode,
          path: path,
          traceId: traceId,
        );
      case DioExceptionType.cancel:
        return ApiException.unknown(
          message: '请求已取消。',
          statusCode: statusCode,
          path: path,
          traceId: traceId,
        );
      case DioExceptionType.badResponse:
        if (statusCode == 401) {
          return ApiException.unauthorized(
            message: backendMessage ?? '登录状态已失效，请重新登录。',
            statusCode: statusCode,
            path: path,
            traceId: traceId,
          );
        }

        if (statusCode == 404) {
          return ApiException.business(
            message: '接口不存在或网关未配置，请检查后端路由。',
            statusCode: statusCode,
            path: path,
            traceId: traceId,
          );
        }

        if (statusCode != null && statusCode >= 500) {
          return ApiException.server(
            message: backendMessage ?? '服务暂时不可用，请稍后重试。',
            statusCode: statusCode,
            path: path,
            traceId: traceId,
          );
        }

        return ApiException.business(
          message: backendMessage ?? '请求处理失败，请稍后重试。',
          statusCode: statusCode,
          path: path,
          traceId: traceId,
        );
      case DioExceptionType.unknown:
        return ApiException.unknown(
          message: backendMessage ?? '发生未知异常，请稍后重试。',
          statusCode: statusCode,
          path: path,
          traceId: traceId,
        );
    }
  }

  String? _extractField(dynamic payload, String key) {
    if (payload is Map<String, dynamic>) {
      final value = payload[key];
      return value?.toString();
    }

    return null;
  }
}
