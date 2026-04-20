enum ProfileTripType { generated, saved }

extension ProfileTripTypeX on ProfileTripType {
  String get apiValue =>
      this == ProfileTripType.generated ? 'generated' : 'saved';

  String get tabLabel => this == ProfileTripType.generated ? '我的规划' : '我的收藏';

  String get badgeLabel => this == ProfileTripType.generated ? 'AI规划' : '已收藏';

  String get emptyTitle =>
      this == ProfileTripType.generated ? '你还没有生成过路线' : '你还没有收藏任何路线';

  String get emptyDescription => this == ProfileTripType.generated
      ? '去规划页输入目的地、预算和偏好后，系统会把生成成功的路线沉淀到这里。'
      : '去社区发现里逛一逛，看到喜欢的公开路线后，一键就能保存到你的资产库。';
}
