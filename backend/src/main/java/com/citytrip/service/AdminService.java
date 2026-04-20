package com.citytrip.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.citytrip.model.entity.Poi;
import com.citytrip.model.vo.AdminUserVO;

public interface AdminService {
    Page<AdminUserVO> getUserPage(int page, int size, String username);
    void updateUserStatus(Long userId, Integer status);
    Page<Poi> getPoiPage(int page, int size, String name);
    Poi createPoi(Poi poi);
    void updatePoi(Poi poi);
    void deletePoi(Long poiId);
    void updatePoiTemporaryStatus(Long poiId, Integer temporarilyClosed, String statusNote);
}
