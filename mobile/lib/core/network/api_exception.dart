enum ApiExceptionType {
  unauthorized,
  server,
  network,
  business,
  unknown,
}

class ApiException implements Exception {
  const ApiException({
    required this.type,
    required this.message,
    this.statusCode,
    this.path,
    this.traceId,
  });

  final ApiExceptionType type;
  final String message;
  final int? statusCode;
  final String? path;
  final String? traceId;

  bool get isUnauthorized => type == ApiExceptionType.unauthorized;
  bool get isServerError => type == ApiExceptionType.server;
  bool get isNetworkError => type == ApiExceptionType.network;

  factory ApiException.unauthorized({
    String message = '登录状态已失效，请重新登录。',
    int? statusCode,
    String? path,
    String? traceId,
  }) {
    return ApiException(
      type: ApiExceptionType.unauthorized,
      message: message,
      statusCode: statusCode,
      path: path,
      traceId: traceId,
    );
  }

  factory ApiException.server({
    String message = '服务暂时不可用，请稍后重试。',
    int? statusCode,
    String? path,
    String? traceId,
  }) {
    return ApiException(
      type: ApiExceptionType.server,
      message: message,
      statusCode: statusCode,
      path: path,
      traceId: traceId,
    );
  }

  factory ApiException.network({
    String message = '网络连接异常，请检查网络后重试。',
    int? statusCode,
    String? path,
    String? traceId,
  }) {
    return ApiException(
      type: ApiExceptionType.network,
      message: message,
      statusCode: statusCode,
      path: path,
      traceId: traceId,
    );
  }

  factory ApiException.business({
    required String message,
    int? statusCode,
    String? path,
    String? traceId,
  }) {
    return ApiException(
      type: ApiExceptionType.business,
      message: message,
      statusCode: statusCode,
      path: path,
      traceId: traceId,
    );
  }

  factory ApiException.unknown({
    String message = '发生未知异常，请稍后重试。',
    int? statusCode,
    String? path,
    String? traceId,
  }) {
    return ApiException(
      type: ApiExceptionType.unknown,
      message: message,
      statusCode: statusCode,
      path: path,
      traceId: traceId,
    );
  }

  @override
  String toString() {
    return 'ApiException(type: $type, statusCode: $statusCode, message: $message, path: $path, traceId: $traceId)';
  }
}
