typedef JsonDecoder<T> = T Function(Object? json);

class ApiResponse<T> {
  const ApiResponse({
    required this.code,
    required this.msg,
    required this.data,
  });

  final int code;
  final String msg;
  final T? data;

  bool get isSuccess => code == 200;

  factory ApiResponse.fromJson(
    Map<String, dynamic> json, {
    JsonDecoder<T>? decoder,
  }) {
    final rawData = json['data'];

    return ApiResponse<T>(
      code: (json['code'] as num?)?.toInt() ?? 500,
      msg: (json['msg'] ?? json['message'] ?? '请求失败').toString(),
      data: decoder == null ? rawData as T? : decoder(rawData),
    );
  }
}
