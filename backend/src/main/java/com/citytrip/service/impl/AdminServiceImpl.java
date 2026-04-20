package com.citytrip.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.citytrip.common.BadRequestException;
import com.citytrip.mapper.PoiMapper;
import com.citytrip.mapper.UserMapper;
import com.citytrip.model.entity.Poi;
import com.citytrip.model.entity.User;
import com.citytrip.model.vo.AdminUserVO;
import com.citytrip.service.AdminService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class AdminServiceImpl implements AdminService {

    private static final BigDecimal DEFAULT_PRIORITY_SCORE = new BigDecimal("3.0");
    private static final String DEFAULT_WALKING_LEVEL = "medium";
    private static final String DEFAULT_FROZEN_NOTE = "This POI has been temporarily frozen by an administrator.";
    private static final long MAX_PAGE_SIZE = 100L;

    private final UserMapper userMapper;
    private final PoiMapper poiMapper;

    public AdminServiceImpl(UserMapper userMapper, PoiMapper poiMapper) {
        this.userMapper = userMapper;
        this.poiMapper = poiMapper;
    }

    @Override
    public Page<AdminUserVO> getUserPage(int page, int size, String username) {
        long normalizedPage = normalizePage(page);
        long normalizedSize = normalizeSize(size);

        Page<AdminUserVO> result = new Page<>(normalizedPage, normalizedSize);
        result.setRecords(userMapper.selectAdminUserPageRecords(offset(normalizedPage, normalizedSize), normalizedSize, username)
                .stream()
                .map(this::toAdminUserVO)
                .toList());
        result.setTotal(userMapper.countAdminUsers(username));
        return result;
    }

    @Override
    public void updateUserStatus(Long userId, Integer status) {
        if (!isBinaryStatus(status)) {
            throw new BadRequestException("Invalid user status");
        }

        User user = userMapper.selectAdminUserById(userId);
        if (user == null) {
            throw new BadRequestException("User not found");
        }

        userMapper.updateAdminUserStatus(userId, status);
    }

    @Override
    public Page<Poi> getPoiPage(int page, int size, String name) {
        long normalizedPage = normalizePage(page);
        long normalizedSize = normalizeSize(size);

        Page<Poi> result = new Page<>(normalizedPage, normalizedSize);
        result.setRecords(poiMapper.selectAdminPoiPageRecords(offset(normalizedPage, normalizedSize), normalizedSize, name));
        result.setTotal(poiMapper.countAdminPois(name));
        return result;
    }

    @Override
    public Poi createPoi(Poi poi) {
        Poi prepared = preparePoiForSave(poi, null);
        prepared.setId(null);
        prepared.setStatusSource("admin");
        prepared.setStatusUpdatedAt(LocalDateTime.now());
        poiMapper.insertAdminPoi(prepared);
        return poiMapper.selectAdminPoiById(prepared.getId());
    }

    @Override
    public void updatePoi(Poi poi) {
        if (poi == null || poi.getId() == null) {
            throw new BadRequestException("Invalid POI payload");
        }

        Poi existing = requirePoi(poi.getId());
        Poi prepared = preparePoiForSave(poi, existing);
        prepared.setId(existing.getId());

        if (hasStatusChanged(existing, prepared)) {
            prepared.setStatusSource("admin");
            prepared.setStatusUpdatedAt(LocalDateTime.now());
        } else {
            prepared.setStatusSource(existing.getStatusSource());
            prepared.setStatusUpdatedAt(existing.getStatusUpdatedAt());
        }

        poiMapper.updateAdminPoi(prepared);
    }

    @Override
    public void deletePoi(Long poiId) {
        requirePoi(poiId);
        poiMapper.deleteAdminPoi(poiId);
    }

    @Override
    public void updatePoiTemporaryStatus(Long poiId, Integer temporarilyClosed, String statusNote) {
        if (!isBinaryStatus(temporarilyClosed)) {
            throw new BadRequestException("Invalid temporary status");
        }

        requirePoi(poiId);
        poiMapper.updatePoiTemporaryStatus(poiId, temporarilyClosed, resolveStatusNote(temporarilyClosed, statusNote));
    }

    private Poi requirePoi(Long poiId) {
        Poi poi = poiMapper.selectAdminPoiById(poiId);
        if (poi == null) {
            throw new BadRequestException("POI not found");
        }
        return poi;
    }

    private Poi preparePoiForSave(Poi source, Poi existing) {
        if (source == null) {
            throw new BadRequestException("Invalid POI payload");
        }
        if (!StringUtils.hasText(source.getName())) {
            throw new BadRequestException("POI name is required");
        }
        if (!StringUtils.hasText(source.getCategory())) {
            throw new BadRequestException("POI category is required");
        }
        if (source.getStayDuration() == null || source.getStayDuration() <= 0) {
            throw new BadRequestException("Stay duration must be greater than 0");
        }

        Poi prepared = new Poi();
        prepared.setName(source.getName().trim());
        prepared.setCategory(source.getCategory().trim());
        prepared.setDistrict(trimToNull(source.getDistrict()));
        prepared.setAddress(trimToNull(source.getAddress()));
        prepared.setLatitude(source.getLatitude());
        prepared.setLongitude(source.getLongitude());
        prepared.setOpenTime(source.getOpenTime());
        prepared.setCloseTime(source.getCloseTime());
        prepared.setClosedWeekdays(normalizeClosedWeekdays(source.getClosedWeekdays()));
        prepared.setTemporarilyClosed(normalizeBinaryFlag(source.getTemporarilyClosed(), "Invalid temporary status"));
        prepared.setStatusNote(resolveStatusNote(prepared.getTemporarilyClosed(), source.getStatusNote()));
        prepared.setAvgCost(source.getAvgCost() == null ? BigDecimal.ZERO : source.getAvgCost());
        prepared.setStayDuration(source.getStayDuration());
        prepared.setIndoor(normalizeBinaryFlag(source.getIndoor(), "Invalid indoor flag"));
        prepared.setNightAvailable(normalizeBinaryFlag(source.getNightAvailable(), "Invalid night flag"));
        prepared.setRainFriendly(normalizeBinaryFlag(source.getRainFriendly(), "Invalid rain-friendly flag"));
        prepared.setWalkingLevel(textOrDefault(source.getWalkingLevel(), DEFAULT_WALKING_LEVEL));
        prepared.setTags(trimToNull(source.getTags()));
        prepared.setSuitableFor(trimToNull(source.getSuitableFor()));
        prepared.setDescription(trimToNull(source.getDescription()));
        prepared.setPriorityScore(source.getPriorityScore() == null ? DEFAULT_PRIORITY_SCORE : source.getPriorityScore());

        if (existing != null) {
            prepared.setStatusSource(existing.getStatusSource());
            prepared.setStatusUpdatedAt(existing.getStatusUpdatedAt());
        }
        return prepared;
    }

    private boolean hasStatusChanged(Poi existing, Poi prepared) {
        return !Objects.equals(existing.getTemporarilyClosed(), prepared.getTemporarilyClosed())
                || !Objects.equals(trimToNull(existing.getStatusNote()), prepared.getStatusNote())
                || !Objects.equals(normalizeClosedWeekdays(existing.getClosedWeekdays()), prepared.getClosedWeekdays())
                || !Objects.equals(existing.getOpenTime(), prepared.getOpenTime())
                || !Objects.equals(existing.getCloseTime(), prepared.getCloseTime());
    }

    private String resolveStatusNote(Integer temporarilyClosed, String statusNote) {
        String trimmed = trimToNull(statusNote);
        if (Integer.valueOf(1).equals(temporarilyClosed)) {
            return trimmed != null ? trimmed : DEFAULT_FROZEN_NOTE;
        }
        return trimmed;
    }

    private Integer normalizeBinaryFlag(Integer value, String errorMessage) {
        if (value == null) {
            return 0;
        }
        if (!isBinaryStatus(value)) {
            throw new BadRequestException(errorMessage);
        }
        return value;
    }

    private boolean isBinaryStatus(Integer value) {
        return Integer.valueOf(0).equals(value) || Integer.valueOf(1).equals(value);
    }

    private String normalizeClosedWeekdays(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String normalized = Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(value -> value.toUpperCase(Locale.ROOT))
                .distinct()
                .collect(Collectors.joining(","));
        return normalized.isEmpty() ? null : normalized;
    }

    private String textOrDefault(String value, String defaultValue) {
        String trimmed = trimToNull(value);
        return trimmed == null ? defaultValue : trimmed;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private long normalizePage(int page) {
        return Math.max(page, 1);
    }

    private long normalizeSize(int size) {
        return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    }

    private long offset(long page, long size) {
        return (page - 1) * size;
    }

    private AdminUserVO toAdminUserVO(User user) {
        AdminUserVO vo = new AdminUserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setRole(user.getRole());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        vo.setUpdateTime(user.getUpdateTime());
        return vo;
    }
}
